package org.aaron.netty.http.handlers

import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import mu.KLogging
import org.aaron.netty.http.config.StaticFileInfo
import org.aaron.netty.http.logging.HttpRequestLogger
import org.aaron.netty.http.netty.*
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.time.Instant
import java.util.*

class StaticFileHandler(staticFileInfo: StaticFileInfo) : Handler {

    companion object : KLogging()

    private val delegate: Handler = if (staticFileInfo.classpath) {
        ClasspathStaticFileHandler(
                filePath = staticFileInfo.filePath,
                contentType = staticFileInfo.contentType)
    } else {
        NonClasspathStaticFileHandler(
                filePath = staticFileInfo.filePath,
                contentType = staticFileInfo.contentType)
    }

    override fun handle(requestContext: RequestContext) = delegate.handle(requestContext)
}

private fun respondIfNotModified(requestContext: RequestContext, lastModifiedMS: Long): Boolean {

    // Cache Validation
    val ifModifiedSince = requestContext.requestHeaders.get(HttpHeaderNames.IF_MODIFIED_SINCE)
    StaticFileHandler.logger.debug { "ifModifiedSince = $ifModifiedSince" }

    if (!ifModifiedSince.isNullOrEmpty()) {
        val ifModifiedSinceDate = ifModifiedSince.parseHttpDate()
        StaticFileHandler.logger.debug { "ifModifiedSinceDate = $ifModifiedSinceDate" }

        // Only compare up to the second because the datetime format we send to the client
        // does not have milliseconds
        val ifModifiedSinceDateSeconds = ifModifiedSinceDate.time / 1_000L
        val fileLastModifiedSeconds = lastModifiedMS / 1_000L
        if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
            requestContext.sendNotModified()
            return true
        }
    }
    return false
}

private class ClasspathStaticFileHandler(
        filePath: String,
        private val contentType: String) : Handler {

    companion object : KLogging()

    private val lastModified = Instant.now()

    private val response: DefaultFullHttpResponse

    init {
        val fileBuffer = javaClass.getResourceAsStream(filePath).use {
            it.readBytes()
        }

        logger.info { "init filePath = $filePath contentType = $contentType fileBuffer.size = ${fileBuffer.size}" }

        response = newDefaultFullHttpResponse(HttpResponseStatus.OK, fileBuffer)

        response.setContentTypeHeader(contentType)
        response.setCacheControlHeader()
        response.setLastModifiedHeader(lastModified.formatHttpDate())
    }

    override fun handle(requestContext: RequestContext) {

        if (respondIfNotModified(requestContext, lastModified.toEpochMilli())) {
            return
        }

        requestContext.sendResponse(response.retainedDuplicate())
    }
}


private class NonClasspathStaticFileHandler(
        private val filePath: String,
        private val contentType: String) : Handler {

    private val blockingThreadPool = BlockingThreadPoolContainer.blockingThreadPool

    init {
        logger.info { "init filePath = $filePath contentType = $contentType" }
    }

    override fun handle(requestContext: RequestContext) {
        blockingThreadPool.execute {
            try {
                handleBlocking(requestContext)
            } catch (e: Exception) {
                logger.warn(e) { "handle" }
                requestContext.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    private fun handleBlocking(requestContext: RequestContext) {

        val file = File(filePath)

        val ctx = requestContext.ctx

        if (file.isHidden || !file.exists()) {
            requestContext.sendError(HttpResponseStatus.NOT_FOUND)
            return
        }

        if (file.isDirectory || !file.isFile) {
            requestContext.sendError(HttpResponseStatus.FORBIDDEN)
            return
        }

        if (respondIfNotModified(requestContext, file.lastModified())) {
            return
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
        response.setDateHeaderIfNotSet()
        response.setCacheControlHeader()
        response.setLastModifiedHeader(Date(file.lastModified()))

        if (!requestContext.keepAlive) {
            response.setConnectionCloseHeader()
        }

        // Write the initial line and the header.
        HttpRequestLogger.log(requestContext, response)

        ctx.setHasSentHttpResponse()
        ctx.write(response)

        // Write the content.
        val sendFileFuture: ChannelFuture
        val lastContentFuture: ChannelFuture
        if (ctx.pipeline().get(SslHandler::class.java) == null) {
            sendFileFuture = ctx.write(DefaultFileRegion(raf.channel, 0, fileLength), ctx.newProgressivePromise())
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
        } else {
            sendFileFuture = ctx.writeAndFlush(HttpChunkedInput(ChunkedFile(raf, 0, fileLength, CHUNK_SIZE)),
                    ctx.newProgressivePromise())
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture
        }

        if (logger.isDebugEnabled) {
            sendFileFuture.addListener(object : ChannelProgressiveFutureListener {
                override fun operationProgressed(future: ChannelProgressiveFuture, progress: Long, total: Long) {
                    logger.debug { "${future.channel()} transfer progress=$progress total=$total" }
                }

                override fun operationComplete(future: ChannelProgressiveFuture) {
                    logger.debug { "${future.channel()} transfer complete" }
                }
            })
        }

        // Decide whether to close the connection or not.
        if (!requestContext.keepAlive) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE)
        }

    }

    companion object : KLogging() {
        private const val CHUNK_SIZE = 8_192
    }
}