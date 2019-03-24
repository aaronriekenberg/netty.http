package org.aaron.netty.http.handlers

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import java.time.Instant

typealias HandlerMap = Map<String, Handler>

data class RequestContext(
        val ctx: ChannelHandlerContext,
        val request: FullHttpRequest,
        val keepAlive: Boolean,
        val startTime: Instant
)

interface Handler {
    fun handle(requestContext: RequestContext)
}