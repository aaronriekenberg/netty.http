package org.aaron.netty.http.handlers

import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KotlinLogging
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.*

private val logger = KotlinLogging.logger {}

object ConfigHandler : RespondIfNotModifiedHandler() {

    private val response: DefaultFullHttpResponse

    init {
        logger.debug { "begin init" }

        val bodyString = ObjectMapperContainer.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ConfigContainer.config)

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