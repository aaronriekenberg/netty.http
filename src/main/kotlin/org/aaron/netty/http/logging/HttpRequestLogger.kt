package org.aaron.netty.http.logging

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import mu.KotlinLogging
import org.aaron.netty.http.utils.getDeltaTimeSinceSecondsString
import org.aaron.netty.http.netty.RequestContext

private val logger = KotlinLogging.logger {}

object HttpRequestLogger {

    private fun formatRemoteAddress(ctx: ChannelHandlerContext): String {
        val remoteAddress = ctx.channel()?.remoteAddress()
        return if (remoteAddress != null) {
            remoteAddress.toString()
        } else {
            "UNKNOWN"
        }
    }

    fun log(requestContext: RequestContext? = null, response: HttpResponse) {
        if (requestContext == null) {
            logger.info { "status=${response.status()?.code()} length=${response.headers().get(HttpHeaderNames.CONTENT_LENGTH)}" }
        } else {
            val deltaTimeString = requestContext.startTime.getDeltaTimeSinceSecondsString()
            logger.info { "${formatRemoteAddress(requestContext.ctx)} ${requestContext.requestMethod} ${requestContext.requestUri} ${response.protocolVersion()} status=${response.status()?.code()} length=${response.headers().get(HttpHeaderNames.CONTENT_LENGTH)} delta=${deltaTimeString}s" }
        }
    }

}