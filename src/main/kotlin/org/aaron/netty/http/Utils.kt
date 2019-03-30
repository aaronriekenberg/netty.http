package org.aaron.netty.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import org.aaron.netty.http.logging.HttpRequestLogger
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

data class RequestContext(
        val ctx: ChannelHandlerContext,
        val request: FullHttpRequest,
        val keepAlive: Boolean,
        val startTime: Instant
)

const val CONTENT_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8"
const val CONTENT_TYPE_TEXT_PLAIN = "text/plain; charset=UTF-8"
const val CONTENT_TYPE_TEXT_HTML = "text/html; charset=UTF-8"

fun RequestContext.sendResponse(
        response: FullHttpResponse) {

    setDefaultHeaders(response)

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(
            response = response,
            keepAlive = keepAlive)
}

fun RequestContext.sendNotModified() {

    val response = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.NOT_MODIFIED)

    setDefaultHeaders(response)

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(response, keepAlive)
}


fun RequestContext.sendError(status: HttpResponseStatus) {

    val response = newDefaultFullHttpResponse(
            status = status,
            body = "Failure: $status\r\n")

    response.setContentTypeHeader(CONTENT_TYPE_TEXT_PLAIN)
    setDefaultHeaders(response)

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(
            response = response,
            keepAlive = if (statusDropsConnection(status)) false else keepAlive)
}

private fun RequestContext.setDefaultHeaders(response: FullHttpResponse) {

    HttpUtil.setContentLength(response, response.content().readableBytes().toLong())
    response.setDateHeaderIfNotSet()

    if (!keepAlive) {
        // We're going to close the connection as soon as the response is sent,
        // so we should also make it clear for the client.
        response.setConnectionCloseHeader()
    }
}

fun ChannelHandlerContext.sendError(status: HttpResponseStatus) {

    val response = newDefaultFullHttpResponse(
            status = status,
            body = "Failure: $status\r\n")

    response.setContentTypeHeader(CONTENT_TYPE_TEXT_PLAIN)
    response.setConnectionCloseHeader()

    HttpRequestLogger.log(response = response)

    sendResponseAndCleanupConnection(
            response = response,
            keepAlive = false)
}

private fun ChannelHandlerContext.sendResponseAndCleanupConnection(
        response: FullHttpResponse,
        keepAlive: Boolean) {

    val flushPromise = writeAndFlush(response)

    if (!keepAlive) {
        // Close the connection as soon as the response is sent.
        flushPromise.addListener(ChannelFutureListener.CLOSE)
    }
}

fun newDefaultFullHttpResponse(status: HttpResponseStatus, body: String) =
        DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(body, CharsetUtil.UTF_8))

fun newDefaultFullHttpResponse(status: HttpResponseStatus, body: ByteArray) =
        DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(body))

private const val HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
private const val HTTP_DATE_GMT_TIMEZONE = "GMT"

private val HTTP_DATE_FORMATTER = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
        val simpleDateFormat = SimpleDateFormat(HTTP_DATE_FORMAT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE)
        return simpleDateFormat
    }
}

fun String.parseHttpDate(): Date = HTTP_DATE_FORMATTER.get().parse(this)

fun HttpResponse.setDateHeaderIfNotSet(date: Date? = null) {
    if (!headers().contains(HttpHeaderNames.DATE)) {
        headers().set(HttpHeaderNames.DATE, HTTP_DATE_FORMATTER.get().format(date ?: Date()))
    }
}

fun HttpResponse.setLastModifiedHeader(instant: Instant) {
    setLastModifiedHeader(Date(instant.toEpochMilli()))
}

fun HttpResponse.setLastModifiedHeader(date: Date) {
    headers().set(HttpHeaderNames.LAST_MODIFIED, HTTP_DATE_FORMATTER.get().format(date))
}

private const val HTTP_CACHE_SECONDS = 60

fun HttpResponse.setCacheControlHeader(value: String = "private, max-age=$HTTP_CACHE_SECONDS") {
    headers().set(HttpHeaderNames.CACHE_CONTROL, value)
}

fun HttpResponse.setContentTypeHeader(value: String) {
    headers().set(HttpHeaderNames.CONTENT_TYPE, value)
}

fun HttpResponse.setConnectionCloseHeader() {
    headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
}

// copied from Tomcat
// https://github.com/apache/tomcat/blob/3e5ce3108e2684bc25013d9a84a7966a6dcd6e14/java/org/apache/coyote/http11/Http11Processor.java#L220-L233
private fun statusDropsConnection(status: HttpResponseStatus): Boolean =
        when (status) {
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