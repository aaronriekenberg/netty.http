package org.aaron.netty.http.handlers.debug

import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.handlers.TemplateHTMLHandler
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.RequestContext
import org.aaron.netty.http.netty.sendJSONResponseOK
import org.aaron.netty.http.templates.HandlebarsContainer

fun createConfigHandlers(): List<Pair<String, Handler>> =
        listOf(
                "/debug/config" to ConfigHTMLHandler,
                "/api/debug/config" to ConfigAPIHandler
        )

private object ConfigHTMLHandler : TemplateHTMLHandler(
        template = HandlebarsContainer.handlebars.compile("debug"),
        templateData = mapOf("id" to "config"))

private object ConfigAPIHandler : Handler {

    private val configJSON = ObjectMapperContainer.objectMapper.writeValueAsString(ConfigContainer.config)

    override fun handle(requestContext: RequestContext) {
        requestContext.sendJSONResponseOK(configJSON)
    }

}