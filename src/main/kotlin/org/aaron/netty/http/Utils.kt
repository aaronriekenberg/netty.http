package org.aaron.netty.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import org.aaron.netty.http.handlers.RequestContext
import org.aaron.netty.http.logging.HttpRequestLogger
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

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

    response.setContentTypeHeader("text/plain; charset=UTF-8")
    setDefaultHeaders(response)

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(response, false)
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

fun HttpResponse.setCacheControlHeader(value: String = "private, max-age=$HTTP_CACHE_SECONDS") {
    headers().set(HttpHeaderNames.CACHE_CONTROL, value)
}

fun HttpResponse.setContentTypeHeader(value: String) {
    headers().set(HttpHeaderNames.CONTENT_TYPE, value)
}

fun HttpResponse.setConnectionCloseHeader() {
    headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
}

private const val HTTP_CACHE_SECONDS = 60