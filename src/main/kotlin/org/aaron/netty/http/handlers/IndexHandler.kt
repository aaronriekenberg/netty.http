package org.aaron.netty.http.handlers

import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import mu.KotlinLogging
import org.aaron.netty.http.config.CommandInfo
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.config.MainPageInfo
import org.aaron.netty.http.config.StaticFileInfo
import org.aaron.netty.http.netty.*
import org.aaron.netty.http.templates.HandlebarsContainer
import java.time.OffsetDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

private data class IndexTemplateData(
        val mainPageInfo: MainPageInfo,
        val commandInfo: List<CommandInfo>,
        val staticFilesInMainPage: List<StaticFileInfo>,
        val lastModified: String
)

object IndexHandler : RespondIfNotModifiedHandler() {

    private val response: DefaultFullHttpResponse

    init {
        logger.debug { "begin init" }

        val indexTemplate = HandlebarsContainer.handlebars.compile("index")
        val config = ConfigContainer.config

        val indexTemplateData = IndexTemplateData(
                mainPageInfo = config.mainPageInfo,
                commandInfo = config.commandInfo,
                staticFilesInMainPage = config.staticFileInfo.filter { it.includeInMainPage },
                lastModified = OffsetDateTime.ofInstant(lastModified, ZoneId.systemDefault()).toString()
        )

        val htmlString = indexTemplate.apply(indexTemplateData)

        response = newDefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, htmlString)

        response.setContentTypeHeader(CONTENT_TYPE_TEXT_HTML)
        response.setLastModifiedHeader(lastModified)
        response.setCacheControlHeader()

        logger.debug { "end init" }
    }

    override fun handleModified(requestContext: RequestContext) {
        requestContext.sendRetainedDuplicate(response)
    }

}