package org.aaron.netty.http.handlers

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest

typealias HandlerMap = Map<String, Handler>

data class RequestContext(
        val ctx: ChannelHandlerContext,
        val request: FullHttpRequest,
        val keepAlive: Boolean
)

interface Handler {
    fun handle(requestContext: RequestContext)
}