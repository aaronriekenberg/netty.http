package org.aaron.netty.http.logging

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponse
import mu.KLogging
import org.aaron.netty.http.handlers.RequestContext
import java.time.Duration
import java.time.Instant

object HttpRequestLogger : KLogging() {

    fun log(requestContext: RequestContext, response: HttpResponse) {
        val deltaTime = Duration.between(requestContext.startTime, Instant.now())
        val deltaTimeString = String.format("%.09f", deltaTime.toNanos() / 1e9)
        logger.info { "${requestContext.request.method()} ${requestContext.request.uri()} ${requestContext.request.protocolVersion()} status=${response.status()?.code()} length=${response.headers().get(HttpHeaderNames.CONTENT_LENGTH)} delta=${deltaTimeString}s" }
    }

}