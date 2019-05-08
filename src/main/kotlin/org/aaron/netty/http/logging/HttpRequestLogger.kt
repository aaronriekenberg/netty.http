package org.aaron.netty.http.logging

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import mu.KotlinLogging
import org.aaron.netty.http.server.RequestContext
import org.aaron.netty.http.utils.getDeltaTimeSinceSecondsString

private val logger = KotlinLogging.logger {}

object HttpRequestLogger {

    private fun formatRemoteAddress(requestContext: RequestContext): String {
        val remoteAddressString = requestContext.ctx.channel()?.remoteAddress()?.toString() ?: "UNKNOWN"
        return "$remoteAddressString[${requestContext.requestsForChannel}]"
    }

    fun log(requestContext: RequestContext? = null, response: HttpResponse) {
        if (requestContext == null) {
            logger.info { "status=${response.status()?.code()} len=${response.headers().get(HttpHeaderNames.CONTENT_LENGTH)}" }
        } else {
            val deltaTimeString = requestContext.startTime.getDeltaTimeSinceSecondsString()
            logger.info { "${formatRemoteAddress(requestContext)} ${requestContext.requestMethod} ${requestContext.requestUri} ${response.protocolVersion()} status=${response.status()?.code()} len=${response.headers().get(HttpHeaderNames.CONTENT_LENGTH)} delta=${deltaTimeString}" }
        }
    }

}