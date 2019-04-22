package org.aaron.netty.http.handlers.debug

import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KotlinLogging
import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.handlers.RespondIfNotModifiedHandler
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.*

private val logger = KotlinLogging.logger {}

object EnvironmentHandler : RespondIfNotModifiedHandler() {

    private val response: FullHttpResponse

    init {
        logger.debug { "begin init" }

        val bodyString = ObjectMapperContainer.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(EnvironmentContainer.environment)

        response = newDefaultFullHttpResponse(DEFAULT_PROTOCOL_VERSION, HttpResponseStatus.OK, bodyString)

        response.setContentTypeHeader(CONTENT_TYPE_APPLICATION_JSON)
        response.setLastModifiedHeader(lastModified)
        response.setCacheControlHeader()

        logger.debug { "end init" }
    }

    override fun handleModified(requestContext: RequestContext) {
        requestContext.sendRetainedDuplicate(response)
    }

}