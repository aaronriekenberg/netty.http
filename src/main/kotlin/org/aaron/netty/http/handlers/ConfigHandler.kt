package org.aaron.netty.http.handlers

import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KLogging
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.*
import java.time.Instant

object ConfigHandler : Handler, KLogging() {

    private val lastModified: Instant = Instant.now()

    private val response: DefaultFullHttpResponse

    init {
        logger.debug { "begin init" }

        val bodyString = ObjectMapperContainer.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ConfigContainer.config)

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