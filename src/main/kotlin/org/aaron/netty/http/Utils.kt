package org.aaron.netty.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil

fun ChannelHandlerContext.sendAndCleanupConnection(
        response: FullHttpResponse,
        keepAlive: Boolean) {

    HttpUtil.setContentLength(response, response.content().readableBytes().toLong())
    if (!keepAlive) {
        // We're going to close the connection as soon as the response is sent,
        // so we should also make it clear for the client.
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    }

    val flushPromise = writeAndFlush(response)

    if (!keepAlive) {
        // Close the connection as soon as the response is sent.
        flushPromise.addListener(ChannelFutureListener.CLOSE)
    }
}

fun ChannelHandlerContext.sendError(status: HttpResponseStatus) {
    val response = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: $status\r\n", CharsetUtil.UTF_8))
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

    sendAndCleanupConnection(response, false)
}
