package org.aaron.netty.http.netty

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.util.AsciiString
import io.netty.util.AttributeKey
import io.netty.util.CharsetUtil
import io.netty.util.concurrent.GlobalEventExecutor
import mu.KotlinLogging
import org.aaron.netty.http.logging.HttpRequestLogger
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

fun createEventLoopGroup(threads: Int = 0): MultithreadEventLoopGroup =
        when {
            Epoll.isAvailable() -> EpollEventLoopGroup(threads)
            KQueue.isAvailable() -> KQueueEventLoopGroup(threads)
            else -> NioEventLoopGroup(threads)
        }

fun serverSocketChannelClass(): KClass<out ServerSocketChannel> =
        when {
            Epoll.isAvailable() -> EpollServerSocketChannel::class
            KQueue.isAvailable() -> KQueueServerSocketChannel::class
            else -> NioServerSocketChannel::class
        }

object ClientChannelGroupContainer {

    val clientChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

}

data class RequestContext(
        val ctx: ChannelHandlerContext,
        val requestUri: String,
        val requestMethod: HttpMethod,
        val requestHeaders: HttpHeaders,
        val protocolVersion: HttpVersion,
        val keepAlive: Boolean,
        val requestsForChannel: Int,
        val startTime: Instant
)

val DEFAULT_PROTOCOL_VERSION = HttpVersion.HTTP_1_1!!

fun RequestContext.sendJSONResponseOK(
        json: String) {

    val response = newDefaultFullHttpResponse(protocolVersion, HttpResponseStatus.OK, json)
    response.setContentTypeHeader(CONTENT_TYPE_APPLICATION_JSON)

    sendResponse(response)
}

fun RequestContext.sendRetainedDuplicate(response: FullHttpResponse) {

    val retainedDuplicate = response.retainedDuplicate()

    sendResponse(
            response = if (retainedDuplicate.protocolVersion() == this.protocolVersion) {
                retainedDuplicate
            } else {
                retainedDuplicate.setProtocolVersion(protocolVersion)
            })
}

fun RequestContext.sendNotModified() {

    val response = DefaultFullHttpResponse(
            protocolVersion,
            HttpResponseStatus.NOT_MODIFIED)

    sendResponse(response)
}

fun RequestContext.respondIfNotModified(lastModified: Instant): Boolean {

    try {
        // Cache Validation
        val ifModifiedSince = requestHeaders.get(HttpHeaderNames.IF_MODIFIED_SINCE)
        logger.debug { "ifModifiedSince = $ifModifiedSince" }

        if (!ifModifiedSince.isNullOrEmpty()) {
            val ifModifiedSinceDateTime = ifModifiedSince.parseHttpDate()
            logger.debug { "ifModifiedSinceDateTime = $ifModifiedSinceDateTime" }

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            val ifModifiedSinceDateSeconds = ifModifiedSinceDateTime.toEpochSecond()
            val lastModifiedSeconds = lastModified.epochSecond
            if (ifModifiedSinceDateSeconds == lastModifiedSeconds) {
                sendNotModified()
                return true
            }
        }
    } catch (e: Exception) {
        logger.warn(e) { "respondIfNotModified" }
    }
    return false
}

fun RequestContext.sendResponse(
        response: FullHttpResponse) {

    response.setDefaultHeaders(keepAlive)

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(
            response = response,
            keepAlive = keepAlive)
}

fun RequestContext.sendError(status: HttpResponseStatus) {

    val keepAliveAfterError = if (status.dropsConnection()) false else keepAlive

    val response = newDefaultFullHttpResponse(
            protocolVersion = protocolVersion,
            status = status,
            body = "Failure: $status\r\n")

    response.setContentTypeHeader(CONTENT_TYPE_TEXT_PLAIN)
    response.setDefaultHeaders(keepAliveAfterError)

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(
            response = response,
            keepAlive = keepAliveAfterError)
}

private fun FullHttpResponse.setDefaultHeaders(keepAlive: Boolean) {

    HttpUtil.setContentLength(this, content().readableBytes().toLong())
    setDateHeaderIfNotSet()

    if (!keepAlive) {
        // We're going to close the connection as soon as the response is sent,
        // so we should also make it clear for the client.
        setConnectionCloseHeader()
    }
}

fun ChannelHandlerContext.sendError(status: HttpResponseStatus) {

    val response = newDefaultFullHttpResponse(
            protocolVersion = DEFAULT_PROTOCOL_VERSION,
            status = status,
            body = "Failure: $status\r\n")

    response.setContentTypeHeader(CONTENT_TYPE_TEXT_PLAIN)
    response.setDefaultHeaders(keepAlive = false)

    HttpRequestLogger.log(response = response)

    sendResponseAndCleanupConnection(
            response = response,
            keepAlive = false)
}

private fun ChannelHandlerContext.sendResponseAndCleanupConnection(
        response: FullHttpResponse,
        keepAlive: Boolean) {

    clearInHttpRequest()

    val flushPromise = writeAndFlush(response)

    if (!keepAlive) {
        // Close the connection as soon as the response is sent.
        flushPromise.addListener(ChannelFutureListener.CLOSE)
    }
}

fun newDefaultFullHttpResponse(
        protocolVersion: HttpVersion,
        status: HttpResponseStatus,
        body: String) =
        DefaultFullHttpResponse(
                protocolVersion,
                status,
                Unpooled.copiedBuffer(body, CharsetUtil.UTF_8))

fun newDefaultFullHttpResponse(
        protocolVersion: HttpVersion,
        status: HttpResponseStatus,
        body: ByteArray) =
        DefaultFullHttpResponse(
                protocolVersion,
                status,
                Unpooled.copiedBuffer(body))

private const val HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
private val HTTP_DATE_GMT_ZONE_ID = ZoneId.of("GMT")
private val HTTP_DATE_FORMATTER = DateTimeFormatter.ofPattern(HTTP_DATE_FORMAT).withZone(HTTP_DATE_GMT_ZONE_ID)

fun String.parseHttpDate(): ZonedDateTime = ZonedDateTime.parse(this, HTTP_DATE_FORMATTER)

fun Instant.formatHttpDate(): String = ZonedDateTime.ofInstant(this, HTTP_DATE_GMT_ZONE_ID).format(HTTP_DATE_FORMATTER)

fun HttpResponse.setDateHeaderIfNotSet() {
    if (!headers().contains(HttpHeaderNames.DATE)) {
        headers().set(HttpHeaderNames.DATE, Instant.now().formatHttpDate())
    }
}

fun HttpResponse.setLastModifiedHeader(instant: Instant) {
    headers().set(HttpHeaderNames.LAST_MODIFIED, instant.formatHttpDate())
}

private const val HTTP_CACHE_SECONDS = 60

fun HttpResponse.setCacheControlHeader(value: String = "private, max-age=$HTTP_CACHE_SECONDS") {
    headers().set(HttpHeaderNames.CACHE_CONTROL, value)
}

val CONTENT_TYPE_APPLICATION_JSON = AsciiString.cached("application/json; charset=UTF-8")!!
val CONTENT_TYPE_TEXT_PLAIN = AsciiString.cached("text/plain; charset=UTF-8")!!
val CONTENT_TYPE_TEXT_HTML = AsciiString.cached("text/html; charset=UTF-8")!!

fun HttpResponse.setContentTypeHeader(value: AsciiString) {
    headers().set(HttpHeaderNames.CONTENT_TYPE, value)
}

fun HttpResponse.setConnectionCloseHeader() {
    headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
}

// copied from Tomcat
// https://github.com/apache/tomcat/blob/3e5ce3108e2684bc25013d9a84a7966a6dcd6e14/java/org/apache/coyote/http11/Http11Processor.java#L220-L233
private fun HttpResponseStatus.dropsConnection(): Boolean =
        when (this) {
            HttpResponseStatus.BAD_REQUEST -> true
            HttpResponseStatus.REQUEST_TIMEOUT -> true
            HttpResponseStatus.LENGTH_REQUIRED -> true
            HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE -> true
            HttpResponseStatus.REQUEST_URI_TOO_LONG -> true
            HttpResponseStatus.INTERNAL_SERVER_ERROR -> true
            HttpResponseStatus.SERVICE_UNAVAILABLE -> true
            HttpResponseStatus.NOT_IMPLEMENTED -> true
            else -> false
        }

private val CHANNEL_ACTIVE_TIME_ATTRIBUTE_KEY: AttributeKey<Instant> = AttributeKey.valueOf("org.aaron.netty.http.netty.channelActiveTime")

fun ChannelHandlerContext.setChannelActiveTime() {
    channel().attr(CHANNEL_ACTIVE_TIME_ATTRIBUTE_KEY).set(Instant.now())
}

fun Channel.getChannelActiveTime(): Instant? {
    return attr(CHANNEL_ACTIVE_TIME_ATTRIBUTE_KEY).get()
}

val IN_HTTP_REQUEST_ATTRIBUTE_KEY: AttributeKey<Boolean> = AttributeKey.valueOf("org.aaron.netty.http.netty.inHttpRequest")

fun ChannelHandlerContext.isInHttpRequest(): Boolean {
    return channel().attr(IN_HTTP_REQUEST_ATTRIBUTE_KEY).get() ?: false
}

fun ChannelHandlerContext.setInHttpRequest() {
    channel().attr(IN_HTTP_REQUEST_ATTRIBUTE_KEY).set(true)
}

fun ChannelHandlerContext.clearInHttpRequest() {
    channel().attr(IN_HTTP_REQUEST_ATTRIBUTE_KEY).set(false)
}

val HTTP_REQUESTS_FOR_CHANNEL_ATTRIBUTE_KEY: AttributeKey<Int> = AttributeKey.valueOf("org.aaron.netty.http.netty.httpRequestsForChannel")

fun ChannelHandlerContext.incrementAndGetHttpRequestsForChannel(): Int {
    val attr = channel().attr(HTTP_REQUESTS_FOR_CHANNEL_ATTRIBUTE_KEY)
    val requests = (attr.get() ?: 0) + 1
    attr.set(requests)
    return requests
}

fun Channel.getHttpRequestsForChannel(): Int {
    return attr(HTTP_REQUESTS_FOR_CHANNEL_ATTRIBUTE_KEY).get() ?: 0
}