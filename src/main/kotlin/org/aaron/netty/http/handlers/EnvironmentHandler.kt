package org.aaron.netty.http.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KLogging
import org.aaron.netty.http.*
import org.aaron.netty.http.environment.EnvironmentContainer
import java.time.Instant

object EnvironmentHandler : Handler, KLogging() {

    private val response: FullHttpResponse

    init {
        logger.debug { "begin init" }

        val bodyString = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(EnvironmentContainer.environment)

        val lastModifiedString: String = Instant.now().formatHttpDate()

        response = newDefaultFullHttpResponse(HttpResponseStatus.OK, bodyString)

        response.setContentTypeHeader(CONTENT_TYPE_TEXT_PLAIN)
        response.setLastModifiedHeader(lastModifiedString)
        response.setCacheControlHeader()

        logger.debug { "end init" }
    }

    override fun handle(requestContext: RequestContext) = requestContext.sendResponse(response.retainedDuplicate())

}