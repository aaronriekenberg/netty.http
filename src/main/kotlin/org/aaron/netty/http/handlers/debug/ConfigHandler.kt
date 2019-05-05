package org.aaron.netty.http.handlers.debug

import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.handlers.HandlerPairList
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.server.RequestContext
import org.aaron.netty.http.server.sendJSONResponseOK
import org.aaron.netty.http.templates.TemplateHTMLHandler

fun configHandlers(): HandlerPairList =
        listOf(
                "/debug/config" to ConfigHTMLHandler,
                "/api/debug/config" to ConfigAPIHandler
        )

private object ConfigHTMLHandler : TemplateHTMLHandler(
        templateName = "debug",
        templateData = mapOf("id" to "config"))

private object ConfigAPIHandler : Handler {

    private val configJSON = ObjectMapperContainer.objectMapper.writeValueAsString(ConfigContainer.config)

    override fun handle(requestContext: RequestContext) {
        requestContext.sendJSONResponseOK(configJSON)
    }

}