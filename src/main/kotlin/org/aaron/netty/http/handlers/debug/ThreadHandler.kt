package org.aaron.netty.http.handlers.debug

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KotlinLogging
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.handlers.HandlerPairList
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.RequestContext
import org.aaron.netty.http.netty.sendJSONResponseOK
import org.aaron.netty.http.templates.TemplateHTMLHandler
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo

private val logger = KotlinLogging.logger {}

fun threadHandlers(): HandlerPairList =
        listOf(
                "/debug/thread" to ThreadHTMLHandler,
                "/api/debug/thread" to ThreadAPIHandler
        )

private object ThreadHTMLHandler : TemplateHTMLHandler(
        templateName = "debug",
        templateData = mapOf("id" to "thread"))

private data class ThreadInfoResponse(

        @field:JsonProperty("id")
        val id: Long,

        @field:JsonProperty("name")
        val name: String,

        @field:JsonProperty("daemon")
        val daemon: Boolean,

        @field:JsonProperty("in_native")
        val inNative: Boolean,

        @field:JsonProperty("suspended")
        val suspended: Boolean,

        @field:JsonProperty("state")
        val state: Thread.State,

        @field:JsonProperty("priority")
        val priority: Int

)

private fun ThreadInfo.toThreadInfoResponse(): ThreadInfoResponse =
        ThreadInfoResponse(
                id = threadId,
                name = threadName,
                daemon = isDaemon,
                inNative = isInNative,
                suspended = isSuspended,
                state = threadState,
                priority = priority)


private data class ThreadHandlerResponse(

        @field:JsonProperty("thread_count")
        val threadCount: Int,

        @field:JsonProperty("peak_thread_count")
        val peakThreadCount: Int,

        @field:JsonProperty("total_started_thread_count")
        val totalStartedThreadCount: Long,

        @field:JsonProperty("thread_info")
        val threadInfo: List<ThreadInfoResponse>
)

private object ThreadAPIHandler : Handler {

    private val objectMapper = ObjectMapperContainer.objectMapper

    override fun handle(requestContext: RequestContext) {
        val threadMXBean = ManagementFactory.getThreadMXBean()

        val threadHandlerResponse = ThreadHandlerResponse(
                threadCount = threadMXBean.threadCount,
                peakThreadCount = threadMXBean.peakThreadCount,
                totalStartedThreadCount = threadMXBean.totalStartedThreadCount,
                threadInfo = threadMXBean.getThreadInfo(threadMXBean.allThreadIds)
                        .filterNotNull()
                        .map { it.toThreadInfoResponse() }
                        .sortedBy { it.id }
        )

        val json = objectMapper.writeValueAsString(threadHandlerResponse)

        requestContext.sendJSONResponseOK(json)
    }
}