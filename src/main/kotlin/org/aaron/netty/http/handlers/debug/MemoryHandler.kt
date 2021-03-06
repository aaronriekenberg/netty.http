package org.aaron.netty.http.handlers.debug

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KotlinLogging
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.handlers.HandlerPairList
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.server.RequestContext
import org.aaron.netty.http.server.sendJSONResponseOK
import org.aaron.netty.http.templates.TemplateHTMLHandler
import java.lang.management.ManagementFactory
import java.lang.management.MemoryPoolMXBean
import java.lang.management.MemoryType
import java.lang.management.MemoryUsage

private val logger = KotlinLogging.logger {}

fun memoryHandlers(): HandlerPairList =
        listOf(
                "/debug/memory" to MemoryHTMLHandler,
                "/api/debug/memory" to MemoryAPIHandler
        )

private object MemoryHTMLHandler : TemplateHTMLHandler(
        templateName = "debug",
        templateData = mapOf("id" to "memory"))

private data class MemoryUsageResponse(

        @field:JsonProperty("committed_bytes")
        val commmittedBytes: Long,

        @field:JsonProperty("init_bytes")
        val initBytes: Long,

        @field:JsonProperty("max_bytes")
        val maxBytes: Long,

        @field:JsonProperty("used_bytes")
        val usedBytes: Long
)

private fun MemoryUsage.toMemoryUsageResponse(): MemoryUsageResponse =
        MemoryUsageResponse(
                commmittedBytes = committed,
                initBytes = init,
                maxBytes = max,
                usedBytes = used
        )

private data class MemoryPoolResponse(

        @field:JsonProperty("name")
        val name: String,

        @field:JsonProperty("type")
        val type: MemoryType,

        @field:JsonProperty("usage")
        val usage: MemoryUsageResponse
)

private fun MemoryPoolMXBean.toMemoryPoolResponse(): MemoryPoolResponse =
        MemoryPoolResponse(
                name = name,
                type = type,
                usage = usage.toMemoryUsageResponse()
        )

private data class MemoryHandlerResponse(

        @field:JsonProperty("heap_memory_usage")
        val heapMemoryUsage: MemoryUsageResponse,

        @field:JsonProperty("non_heap_memory_usage")
        val nonHeapMemoryUsage: MemoryUsageResponse,

        @field:JsonProperty("memory_pools")
        val memoryPools: List<MemoryPoolResponse>
)

private object MemoryAPIHandler : Handler {

    private val objectMapper = ObjectMapperContainer.objectMapper

    override fun handle(requestContext: RequestContext) {
        val memoryMXBean = ManagementFactory.getMemoryMXBean()

        val memoryHandlerResponse = MemoryHandlerResponse(
                heapMemoryUsage = memoryMXBean.heapMemoryUsage.toMemoryUsageResponse(),
                nonHeapMemoryUsage = memoryMXBean.nonHeapMemoryUsage.toMemoryUsageResponse(),
                memoryPools = ManagementFactory.getMemoryPoolMXBeans().map { it.toMemoryPoolResponse() }
        )

        val json = objectMapper.writeValueAsString(memoryHandlerResponse)

        requestContext.sendJSONResponseOK(json)
    }

}