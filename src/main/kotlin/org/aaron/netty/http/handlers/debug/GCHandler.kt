package org.aaron.netty.http.handlers.debug

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KotlinLogging
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.handlers.TemplateHTMLHandler
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.RequestContext
import org.aaron.netty.http.netty.sendJSONResponseOK
import org.aaron.netty.http.templates.HandlebarsContainer
import java.lang.management.GarbageCollectorMXBean
import java.lang.management.ManagementFactory

private val logger = KotlinLogging.logger {}

fun createGCHandlers(): List<Pair<String, Handler>> =
        listOf(
                "/debug/gc" to GCHTMLHandler,
                "/api/debug/gc" to GCAPIHandler
        )

private object GCHTMLHandler : TemplateHTMLHandler(
        template = HandlebarsContainer.handlebars.compile("debug"),
        templateData = mapOf("id" to "gc"))

private data class GCResponse(

        @field:JsonProperty("name")
        val name: String,

        @field:JsonProperty("collection_count")
        val collectionCount: Long,

        @field:JsonProperty("collection_time_milliseconds")
        val collectionTimeMilliseconds: Long,

        @field:JsonProperty("memory_pool_names")
        val memoryPoolNames: List<String>
)

private fun GarbageCollectorMXBean.toGCResponse(): GCResponse =
        GCResponse(
                name = name,
                collectionCount = collectionCount,
                collectionTimeMilliseconds = collectionTime,
                memoryPoolNames = memoryPoolNames.toList()
        )

private data class GCHandlerResponse(

        @field:JsonProperty("gcs")
        val gcResponses: List<GCResponse>
)

private object GCAPIHandler : Handler {

    private val objectMapper = ObjectMapperContainer.objectMapper

    override fun handle(requestContext: RequestContext) {
        val gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans()

        val gcHandlerResponse = GCHandlerResponse(
                gcResponses = gcMXBeans.map { it.toGCResponse() }
        )
        val json = objectMapper.writeValueAsString(gcHandlerResponse)

        requestContext.sendJSONResponseOK(json)
    }

}