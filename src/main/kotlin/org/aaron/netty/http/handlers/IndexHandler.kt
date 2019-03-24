package org.aaron.netty.http.handlers

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KLogging
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.config.MainPageInfo
import org.aaron.netty.http.formatHttpDate
import org.aaron.netty.http.newDefaultFullHttpResponse
import org.aaron.netty.http.sendResponse
import org.aaron.netty.http.templates.HandlebarsContainer
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

data class IndexTemplateData(
        val mainPageInfo: MainPageInfo,
        val lastModified: String
)

class IndexHandler : Handler {

    companion object : KLogging()

    private val htmlString: String

    private val lastModified: Instant

    init {
        logger.info { "begin init" }

        val indexTemplate = HandlebarsContainer.handlebars.compile("index")

        lastModified = Instant.now()

        val indexTemplateData = IndexTemplateData(
                mainPageInfo = ConfigContainer.config.mainPageInfo,
                lastModified = OffsetDateTime.ofInstant(lastModified, ZoneId.systemDefault()).toString()
        )

        htmlString = indexTemplate.apply(indexTemplateData)

        logger.info { "end init" }
    }

    override fun handle(requestContext: RequestContext) {
        val response = newDefaultFullHttpResponse(HttpResponseStatus.OK, htmlString)
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8")
        response.headers().set(HttpHeaderNames.LAST_MODIFIED, formatHttpDate(lastModified))

        requestContext.sendResponse(response)
    }

}