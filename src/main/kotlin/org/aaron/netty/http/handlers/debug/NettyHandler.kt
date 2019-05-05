package org.aaron.netty.http.handlers.debug

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.channel.Channel
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.handlers.HandlerPairList
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.*
import org.aaron.netty.http.templates.TemplateHTMLHandler
import org.aaron.netty.http.utils.getDeltaTimeSinceSecondsString
import org.aaron.netty.http.utils.toOffsetDateTime
import java.time.Instant

fun nettyHandlers(): HandlerPairList =
        listOf(
                "/debug/netty" to NettyHTMLHandler,
                "/api/debug/netty" to NettyAPIHandler
        )

private object NettyHTMLHandler : TemplateHTMLHandler(
        templateName = "debug",
        templateData = mapOf("id" to "netty"))

private data class NettyClientChannelResponse(

        @field:JsonProperty("id")
        val id: String,

        @field:JsonProperty("remote_address")
        val remoteAddress: String,

        @field:JsonProperty("local_address")
        val localAddress: String,

        @field:JsonProperty("active_time")
        val activeTime: String,

        @field:JsonProperty("active_duration")
        val activeDuration: String,

        @field:JsonProperty("http_requests")
        val httpRequests: Int

)

private const val UNKNOWN_STRING = "UNKNOWN"

private fun Channel.toNettyClientChannelResponse(): NettyClientChannelResponse =
        NettyClientChannelResponse(
                id = id()?.asLongText() ?: UNKNOWN_STRING,
                remoteAddress = remoteAddress()?.toString() ?: UNKNOWN_STRING,
                localAddress = localAddress()?.toString() ?: UNKNOWN_STRING,
                activeTime = getChannelActiveTime()?.toOffsetDateTime()?.toString() ?: UNKNOWN_STRING,
                activeDuration = getChannelActiveTime()?.getDeltaTimeSinceSecondsString()?.plus("s") ?: UNKNOWN_STRING,
                httpRequests = getHttpRequestsForChannel()
        )

private data class NettyHandlerResponse(

        @field:JsonProperty("client_channels")
        val clientChannels: List<NettyClientChannelResponse>
)

private object NettyAPIHandler : Handler {

    private val objectMapper = ObjectMapperContainer.objectMapper

    private val clientChannelGroup = ClientChannelGroupContainer.clientChannelGroup

    override fun handle(requestContext: RequestContext) {
        val nettyHandlerResponse = NettyHandlerResponse(
                clientChannels = clientChannelGroup
                        .sortedBy { it.getChannelActiveTime() ?: Instant.EPOCH }
                        .map { it.toNettyClientChannelResponse() }
        )

        val json = objectMapper.writeValueAsString(nettyHandlerResponse)

        requestContext.sendJSONResponseOK(json)
    }

}