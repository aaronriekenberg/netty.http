package org.aaron.netty.http.handlers

import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.templates.TemplateHTMLHandler
import org.aaron.netty.http.utils.toOffsetDateTime

object IndexHandler : TemplateHTMLHandler(
        templateName = "index",
        templateData = mapOf(
                "mainPageInfo" to ConfigContainer.config.mainPageInfo,
                "commandInfo" to ConfigContainer.config.commandInfo,
                "staticFilesInMainPage" to ConfigContainer.config.staticFileInfo.filter { it.includeInMainPage },
                "lastModified" to EnvironmentContainer.environment.startTime.toOffsetDateTime().toString()
        )
)