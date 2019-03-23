package org.aaron.netty.http.handlers

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest

typealias HandlerMap = Map<String, Handler>

interface Handler {
    fun handle(ctx: ChannelHandlerContext, request: FullHttpRequest, keepAlive: Boolean)
}