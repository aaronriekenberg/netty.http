package org.aaron.netty.http.handlers.debug

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.handler.codec.http.HttpResponseStatus
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.*
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo

private data class ThreadInfoResponse(

        @field:JsonProperty("id")
        val id: Long,

        @field:JsonProperty("name")
        val name: String,

        @field:JsonProperty("in_native")
        val inNative: Boolean,

        @field:JsonProperty("suspended")
        val suspended: Boolean,

        @field:JsonProperty("state")
        val state: Thread.State

)

private fun ThreadInfo.toThreadInfoResponse(): ThreadInfoResponse =
        ThreadInfoResponse(
                id = threadId,
                name = threadName,
                inNative = isInNative,
                suspended = isSuspended,
                state = threadState)


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

object ThreadHandler : Handler {

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
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(threadHandlerResponse)

        val response = newDefaultFullHttpResponse(requestContext.protocolVersion, HttpResponseStatus.OK, json)
        response.setContentTypeHeader(CONTENT_TYPE_APPLICATION_JSON)

        requestContext.sendResponse(response)
    }
}