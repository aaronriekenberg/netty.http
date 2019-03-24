package org.aaron.netty.http.handlers

import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import mu.KLogging
import org.aaron.netty.http.*
import org.aaron.netty.http.logging.HttpRequestLogger
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.util.*

class StaticFileHandler(
        private val filePath: String,
        private val classpath: Boolean,
        private val contentType: String) : Handler {

    init {
        logger.info { "init filePath = $filePath" }
    }

    override fun handle(requestContext: RequestContext) {

        val file =
                if (!classpath) {
                    File(filePath)
                } else {
                    File(javaClass.getResource(filePath).toURI())
                }

        val ctx = requestContext.ctx

        if (file.isHidden || !file.exists()) {
            requestContext.sendError(HttpResponseStatus.NOT_FOUND)
            return
        }

        if (file.isDirectory || !file.isFile) {
            requestContext.sendError(HttpResponseStatus.FORBIDDEN)
            return
        }

        // Cache Validation
        val ifModifiedSince = requestContext.request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE)
        logger.debug { "ifModifiedSince = $ifModifiedSince" }
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            val ifModifiedSinceDate = parseHttpDate(ifModifiedSince)
            logger.debug { "ifModifiedSinceDate = $ifModifiedSinceDate" }

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            val ifModifiedSinceDateSeconds = ifModifiedSinceDate.time / 1_000L
            val fileLastModifiedSeconds = file.lastModified() / 1_000L
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                requestContext.sendNotModified()
                return
            }
        }

        val raf: RandomAccessFile
        try {
            raf = RandomAccessFile(file, "r")
        } catch (ignore: FileNotFoundException) {
            requestContext.sendError(HttpResponseStatus.NOT_FOUND)
            return
        }

        val fileLength = raf.length()

        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)

        HttpUtil.setContentLength(response, fileLength)
        response.setContentTypeHeader(contentType)
        response.setDateHeader()
        response.setCacheControlHeader()
        response.setLastModifiedHeader(Date(file.lastModified()))

        if (!requestContext.keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        }

        // Write the initial line and the header.
        HttpRequestLogger.log(requestContext, response)
        ctx.write(response)

        // Write the content.
        val sendFileFuture: ChannelFuture
        val lastContentFuture: ChannelFuture
        if (ctx.pipeline().get(SslHandler::class.java) == null) {
            sendFileFuture = ctx.write(DefaultFileRegion(raf.channel, 0, fileLength), ctx.newProgressivePromise())
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
        } else {
            sendFileFuture = ctx.writeAndFlush(HttpChunkedInput(ChunkedFile(raf, 0, fileLength, 8_192)),
                    ctx.newProgressivePromise())
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture
        }

        sendFileFuture.addListener(object : ChannelProgressiveFutureListener {
            override fun operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long) {
                logger.debug { "${future.channel()} transfer progress= $progress total = $total" }
            }

            override fun operationComplete(future: ChannelProgressiveFuture) {
                logger.debug { "${future.channel()} transfer complete" }
            }
        })

        // Decide whether to close the connection or not.
        if (!requestContext.keepAlive) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE)
        }
    }

    companion object : KLogging() {
        private const val HTTP_CACHE_SECONDS = 60
    }
}