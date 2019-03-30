package org.aaron.netty.http.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KLogging
import org.aaron.netty.http.*
import org.aaron.netty.http.config.ConfigContainer
import java.time.Instant

object ConfigHandler : Handler, KLogging() {

    private val response: DefaultFullHttpResponse

    init {
        logger.debug { "begin init" }

        val bodyString = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(ConfigContainer.config)

        val lastModifiedString: String = Instant.now().formatHttpDate()

        response = newDefaultFullHttpResponse(HttpResponseStatus.OK, bodyString)

        response.setContentTypeHeader(CONTENT_TYPE_TEXT_PLAIN)
        response.setLastModifiedHeader(lastModifiedString)
        response.setCacheControlHeader()

        logger.debug { "end init" }
    }

    override fun handle(requestContext: RequestContext) = requestContext.sendResponse(response.retainedDuplicate())


}