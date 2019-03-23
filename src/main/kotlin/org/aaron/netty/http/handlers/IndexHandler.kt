package org.aaron.netty.http.handlers

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import mu.KLogging
import org.aaron.netty.http.sendAndCleanupConnection

class IndexHandler : Handler {

    companion object : KLogging()

    override fun handle(ctx: ChannelHandlerContext, request: FullHttpRequest, keepAlive: Boolean) {
        val htmlBuffer = StringBuffer()
        htmlBuffer.append("<html>")
        htmlBuffer.append("<body>")
        htmlBuffer.append("<h1>Hello World</h1>")
        htmlBuffer.append("</body>")
        htmlBuffer.append("</html>")

        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8")

        val buffer = Unpooled.copiedBuffer(htmlBuffer, CharsetUtil.UTF_8)
        response.content().writeBytes(buffer)
        buffer.release()

        ctx.sendAndCleanupConnection(response, keepAlive)
    }

}