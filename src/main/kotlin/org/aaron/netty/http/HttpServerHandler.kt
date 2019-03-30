package org.aaron.netty.http

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpUtil
import mu.KLogging
import org.aaron.netty.http.handlers.HandlerMap
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

    public override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {

        logger.debug { "channelRead0 request=$request" }

        val requestContext = RequestContext(
                ctx = ctx,
                request = request,
                keepAlive = HttpUtil.isKeepAlive(request),
                startTime = Instant.now()
        )

        if (!request.decoderResult().isSuccess) {
            requestContext.sendError(HttpResponseStatus.BAD_REQUEST)
            return
        }

        if (HttpMethod.GET != request.method()) {
            requestContext.sendError(HttpResponseStatus.METHOD_NOT_ALLOWED)
            return
        }

        val uri = request.uri()

        val handler = handlerMap[uri]
        if (handler == null) {
            requestContext.sendError(HttpResponseStatus.NOT_FOUND)
        } else {
            handler.handle(requestContext)
        }

    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {

        logger.warn(cause) { "exceptionCaught ctx=$ctx" }

        if (ctx.channel().isActive) {
            ctx.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        }
    }

}
