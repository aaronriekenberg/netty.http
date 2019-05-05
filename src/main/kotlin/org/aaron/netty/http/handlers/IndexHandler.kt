package org.aaron.netty.http.handlers

import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.templates.TemplateHTMLHandler
import java.time.OffsetDateTime
import java.time.ZoneId

object IndexHandler : TemplateHTMLHandler(
        templateName = "index",
        templateData = mapOf(
                "mainPageInfo" to ConfigContainer.config.mainPageInfo,
                "commandInfo" to ConfigContainer.config.commandInfo,
                "staticFilesInMainPage" to ConfigContainer.config.staticFileInfo.filter { it.includeInMainPage },
                "lastModified" to OffsetDateTime.ofInstant(EnvironmentContainer.environment.startTime, ZoneId.systemDefault()).toString()
        )
)