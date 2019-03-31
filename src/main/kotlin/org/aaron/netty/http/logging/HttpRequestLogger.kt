package org.aaron.netty.http.logging

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import mu.KLogging
import org.aaron.netty.http.netty.RequestContext
import java.time.Duration
import java.time.Instant

object HttpRequestLogger : KLogging() {

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
            val deltaTime = Duration.between(requestContext.startTime, Instant.now())
            val deltaTimeString = "%.09f".format(deltaTime.toNanos() / 1e9)
            logger.info { "${formatRemoteAddress(requestContext.ctx)} ${requestContext.requestMethod} ${requestContext.requestUri} ${requestContext.protocolVersion} status=${response.status()?.code()} length=${response.headers().get(HttpHeaderNames.CONTENT_LENGTH)} delta=${deltaTimeString}s" }
        }
    }

}