package org.aaron.netty.http.handlers

import io.netty.handler.codec.http.HttpResponseStatus
import mu.KLogging
import org.aaron.netty.http.*
import org.aaron.netty.http.config.CommandInfo
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.config.MainPageInfo
import org.aaron.netty.http.config.StaticFileInfo
import org.aaron.netty.http.templates.HandlebarsContainer
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

private data class IndexTemplateData(
        val mainPageInfo: MainPageInfo,
        val commandInfo: List<CommandInfo>,
        val staticFilesInMainPage: List<StaticFileInfo>,
        val lastModified: String
)

object IndexHandler : Handler, KLogging() {

    private val htmlString: String

    private val lastModifiedString: String

    init {
        logger.info { "begin init" }

        val indexTemplate = HandlebarsContainer.handlebars.compile("index")
        val config = ConfigContainer.config

        val lastModified: Instant = Instant.now()
        lastModifiedString = lastModified.formatHttpDate()

        val indexTemplateData = IndexTemplateData(
                mainPageInfo = config.mainPageInfo,
                commandInfo = config.commandInfo,
                staticFilesInMainPage = config.staticFileInfo.filter { it.includeInMainPage },
                lastModified = OffsetDateTime.ofInstant(lastModified, ZoneId.systemDefault()).toString()
        )

        htmlString = indexTemplate.apply(indexTemplateData)

        logger.info { "end init" }
    }

    override fun handle(requestContext: RequestContext) {
        val response = newDefaultFullHttpResponse(HttpResponseStatus.OK, htmlString)

        response.setContentTypeHeader(CONTENT_TYPE_TEXT_HTML)
        response.setLastModifiedHeader(lastModifiedString)
        response.setCacheControlHeader()

        requestContext.sendResponse(response)
    }

}