package org.aaron.netty.http.handlers

import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KLogging
import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.*
import java.time.Instant

object EnvironmentHandler : Handler, KLogging() {

    private val lastModified: Instant = Instant.now()

    private val response: FullHttpResponse

    init {
        logger.debug { "begin init" }

        val bodyString = ObjectMapperContainer.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(EnvironmentContainer.environment)

        response = newDefaultFullHttpResponse(HttpResponseStatus.OK, bodyString)

        response.setContentTypeHeader(CONTENT_TYPE_TEXT_PLAIN)
        response.setLastModifiedHeader(lastModified)
        response.setCacheControlHeader()

        logger.debug { "end init" }
    }

    override fun handle(requestContext: RequestContext) {
        if (!requestContext.respondIfNotModified(lastModified)) {
            requestContext.sendResponse(response.retainedDuplicate())
        }
    }

}