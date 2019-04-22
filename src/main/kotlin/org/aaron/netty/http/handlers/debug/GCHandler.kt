package org.aaron.netty.http.handlers.debug

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.handler.codec.http.HttpResponseStatus
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.*
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory

private data class GCResponse(

        @field:JsonProperty("name")
        val name: String,

        @field:JsonProperty("collection_count")
        val collectionCount: Long,

        @field:JsonProperty("collection_time_milliseconds")
        val collectionTimeMilliseconds: Long
)

private fun GarbageCollectorMXBean.toGCResponse(): GCResponse =
        GCResponse(
                name = name,
                collectionCount = collectionCount,
                collectionTimeMilliseconds = collectionTime
        )

private data class GCHandlerResponse(

        @field:JsonProperty("gcs")
        val gcResponses: List<GCResponse>
)

object GCHandler : Handler {

    private val objectMapper = ObjectMapperContainer.objectMapper

    override fun handle(requestContext: RequestContext) {
        val gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()

        val gcHandlerResponse = GCHandlerResponse(
                gcResponses = gcMXBeans.map { it.toGCResponse() }
        )
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(gcHandlerResponse)

        val response = newDefaultFullHttpResponse(requestContext.protocolVersion, HttpResponseStatus.OK, json)
        response.setContentTypeHeader(CONTENT_TYPE_APPLICATION_JSON)

        requestContext.sendResponse(response)
    }

}