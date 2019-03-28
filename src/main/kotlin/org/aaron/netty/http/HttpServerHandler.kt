package org.aaron.netty.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import mu.KLogging
import org.aaron.netty.http.handlers.HandlerMap
import org.aaron.netty.http.handlers.RequestContext
import java.time.Instant


/**
 * A simple handler that serves incoming HTTP requests to send their respective
 * HTTP responses.  It also implements `'If-Modified-Since'` header to
 * take advantage of browser cache, as described in
 * [RFC 2616](http://tools.ietf.org/html/rfc2616#section-14.25).
 *
 * <h3>How Browser Caching Works</h3>
 *
 *
 * Web browser caching works with HTTP headers as illustrated by the following
 * sample:
 *
 *  1. Request #1 returns the content of `/file1.txt`.
 *  1. Contents of `/file1.txt` is cached by the browser.
 *  1. Request #2 for `/file1.txt` does not return the contents of the
 * file again. Rather, a 304 Not Modified is returned. This tells the
 * browser to use the contents stored in its cache.
 *  1. The server knows the file has not been modified because the
 * `If-Modified-Since` date is the same as the file's last
 * modified date.
 *
 *
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
</pre> *
 */
class HttpServerHandler(
        private val handlerMap: HandlerMap) : SimpleChannelInboundHandler<FullHttpRequest>() {

    companion object : KLogging()

    @Throws(Exception::class)
    public override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
        val startTime = Instant.now()

        logger.debug { "channelRead0 request=$request" }

        if (!request.decoderResult().isSuccess) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST)
            return
        }

        if (HttpMethod.GET != request.method()) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED)
            return
        }

        val keepAlive = HttpUtil.isKeepAlive(request)
        val uri = request.uri()

        val requestContext = RequestContext(
                ctx = ctx,
                request = request,
                keepAlive = keepAlive,
                startTime = startTime
        )

        val handler = handlerMap[uri]
        if (handler == null) {
            requestContext.sendError(HttpResponseStatus.NOT_FOUND)
        } else {
            handler.handle(requestContext)
        }

    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (ctx.channel().isActive) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
        val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: $status\r\n", CharsetUtil.UTF_8))
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

        sendAndCleanupConnection(ctx, response, false)
    }

    /**
     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
     * and closes the connection after the response being sent.
     */
    private fun sendAndCleanupConnection(ctx: ChannelHandlerContext, response: FullHttpResponse,
                                         keepAlive: Boolean) {
        HttpUtil.setContentLength(response, response.content().readableBytes().toLong())
        if (!keepAlive) {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        }

        val flushPromise = ctx.writeAndFlush(response)

        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE)
        }
    }
}
