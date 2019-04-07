package org.aaron.netty.http.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpUtil
import mu.KotlinLogging
import org.aaron.netty.http.handlers.HandlerMap
import java.time.Instant

private val logger = KotlinLogging.logger {}

class HttpServerHandler(
        private val handlerMap: HandlerMap) : SimpleChannelInboundHandler<FullHttpRequest>() {

    public override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {

        val startTime = Instant.now()

        ctx.clearHasSentHttpResponse()

        logger.debug { "channelRead0 request=$request" }

        val requestContext = RequestContext(
                ctx = ctx,
                requestUri = request.uri(),
                requestMethod = request.method(),
                requestHeaders = request.headers().copy(),
                protocolVersion = request.protocolVersion(),
                keepAlive = HttpUtil.isKeepAlive(request),
                startTime = startTime
        )

        if (!request.decoderResult().isSuccess) {
            requestContext.sendError(HttpResponseStatus.BAD_REQUEST)
            return
        }

        if (requestContext.requestMethod != HttpMethod.GET) {
            requestContext.sendError(HttpResponseStatus.METHOD_NOT_ALLOWED)
            return
        }

        val handler = handlerMap[requestContext.requestUri]
        if (handler == null) {
            requestContext.sendError(HttpResponseStatus.NOT_FOUND)
        } else {
            handler.handle(requestContext)
        }

    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {

        logger.debug(cause) { "exceptionCaught ctx=$ctx" }

        if (ctx.channel().isActive && (!ctx.hasSentHttpResponse())) {
            ctx.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        } else {
            ctx.close()
        }
    }

}
