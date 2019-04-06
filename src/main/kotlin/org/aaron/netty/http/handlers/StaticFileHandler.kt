package org.aaron.netty.http.handlers

import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import mu.KotlinLogging
import org.aaron.netty.http.config.StaticFileInfo
import org.aaron.netty.http.logging.HttpRequestLogger
import org.aaron.netty.http.netty.*
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.time.Instant

private val logger = KotlinLogging.logger {}

class StaticFileHandler(staticFileInfo: StaticFileInfo) : Handler {

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

private class ClasspathStaticFileHandler(
        filePath: String,
        private val contentType: String) : RespondIfNotModifiedHandler() {

    private val response: DefaultFullHttpResponse

    init {
        val fileBuffer = javaClass.getResourceAsStream(filePath).use {
            it.readBytes()
        }

        logger.info { "init filePath = $filePath contentType = $contentType fileBuffer.size = ${fileBuffer.size}" }

        response = newDefaultFullHttpResponse(HttpResponseStatus.OK, fileBuffer)

        response.setContentTypeHeader(contentType)
        response.setCacheControlHeader()
        response.setLastModifiedHeader(lastModified)
    }

    override fun handleModified(requestContext: RequestContext) {
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

        val fileLastModifiedInstant = Instant.ofEpochMilli(file.lastModified())
        if (requestContext.respondIfNotModified(fileLastModifiedInstant)) {
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
        response.setLastModifiedHeader(fileLastModifiedInstant)

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

    companion object {
        private const val CHUNK_SIZE = 8_192
    }
}