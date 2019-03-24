package org.aaron.netty.http

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import org.aaron.netty.http.handlers.RequestContext
import org.aaron.netty.http.logging.HttpRequestLogger
import java.text.SimpleDateFormat
import java.util.*

fun RequestContext.sendResponse(
        response: FullHttpResponse) {

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(
            response = response,
            keepAlive = keepAlive)
}

fun RequestContext.sendNotModified() {

    val response = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.NOT_MODIFIED)

    response.setDateHeader()

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(response, keepAlive)
}

fun RequestContext.sendError(status: HttpResponseStatus) {

    val response = newDefaultFullHttpResponse(
            status = status,
            body = "Failure: $status\r\n")
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")

    HttpRequestLogger.log(this, response)

    ctx.sendResponseAndCleanupConnection(response, false)
}

private fun ChannelHandlerContext.sendResponseAndCleanupConnection(
        response: FullHttpResponse,
        keepAlive: Boolean) {

    HttpUtil.setContentLength(response, response.content().readableBytes().toLong())
    if (!keepAlive) {
        // We're going to close the connection as soon as the response is sent,
        // so we should also make it clear for the client.
        response.setConnectionCloseHeader()
    }

    val flushPromise = writeAndFlush(response)

    if (!keepAlive) {
        // Close the connection as soon as the response is sent.
        flushPromise.addListener(ChannelFutureListener.CLOSE)
    }
}


fun ByteBuf.writeStringBuffer(stringBuffer: StringBuffer) {
    val buffer = Unpooled.copiedBuffer(stringBuffer, CharsetUtil.UTF_8)
    try {
        writeBytes(buffer)
    } finally {
        buffer.release()
    }
}

fun newDefaultFullHttpResponse(status: HttpResponseStatus, body: String) =
        DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(body, CharsetUtil.UTF_8))

private const val HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
private const val HTTP_DATE_GMT_TIMEZONE = "GMT"

private val HTTP_DATE_FORMATTER = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
        val simpleDateFormat = SimpleDateFormat(HTTP_DATE_FORMAT)
        simpleDateFormat.timeZone = TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE)
        return simpleDateFormat
    }
}

fun parseHttpDate(string: String): Date =
        HTTP_DATE_FORMATTER.get().parse(string)

fun HttpResponse.setDateHeader(date: Date = Date()) {
    headers().set(HttpHeaderNames.DATE, HTTP_DATE_FORMATTER.get().format(date))
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

const val HTTP_CACHE_SECONDS = 60