package org.aaron.netty.http.handlers.debug

import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.handlers.HandlerPairList
import org.aaron.netty.http.handlers.TemplateHTMLHandler
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.RequestContext
import org.aaron.netty.http.netty.sendJSONResponseOK
import org.aaron.netty.http.templates.HandlebarsContainer

fun createEnvironmentHandlers(): HandlerPairList =
        listOf(
                "/debug/environment" to EnvironmentHTMLHandler,
                "/api/debug/environment" to EnvironmentAPIHandler
        )

private object EnvironmentHTMLHandler : TemplateHTMLHandler(
        template = HandlebarsContainer.handlebars.compile("debug"),
        templateData = mapOf("id" to "environment"))

private object EnvironmentAPIHandler : Handler {

    private val environmentJSON = ObjectMapperContainer.objectMapper.writeValueAsString(EnvironmentContainer.environment)

    override fun handle(requestContext: RequestContext) {
        requestContext.sendJSONResponseOK(environmentJSON)
    }

}