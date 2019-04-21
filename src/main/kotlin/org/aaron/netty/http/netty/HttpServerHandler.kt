package org.aaron.netty.http.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedWriteHandler
import mu.KotlinLogging
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.handlers.HandlerMap
import java.time.Instant

private val logger = KotlinLogging.logger {}

class HttpServerInitializer(
        private val handlerMap: HandlerMap) : ChannelInitializer<SocketChannel>() {

    public override fun initChannel(ch: SocketChannel) {
        ch.config().isTcpNoDelay = TCP_NO_DELAY

        val pipeline = ch.pipeline()
//        if (sslCtx != null) {
//            pipeline.addLast(sslCtx.newHandler(ch.alloc()))
//        }
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(HttpObjectAggregator(MAX_CONTENT_LENGTH))
        pipeline.addLast(ChunkedWriteHandler())
        pipeline.addLast(HttpServerHandler(handlerMap))
    }

    companion object {
        private const val MAX_CONTENT_LENGTH = 65_536
        private val TCP_NO_DELAY = ConfigContainer.config.serverInfo.tcpNoDelay
    }
}

private class HttpServerHandler(
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
