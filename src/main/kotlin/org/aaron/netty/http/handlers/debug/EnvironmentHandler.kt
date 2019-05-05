package org.aaron.netty.http.handlers.debug

import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.handlers.Handler
import org.aaron.netty.http.handlers.HandlerPairList
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.server.RequestContext
import org.aaron.netty.http.server.sendJSONResponseOK
import org.aaron.netty.http.templates.TemplateHTMLHandler

fun environmentHandlers(): HandlerPairList =
        listOf(
                "/debug/environment" to EnvironmentHTMLHandler,
                "/api/debug/environment" to EnvironmentAPIHandler
        )

private object EnvironmentHTMLHandler : TemplateHTMLHandler(
        templateName = "debug",
        templateData = mapOf("id" to "environment"))

private object EnvironmentAPIHandler : Handler {

    private val environmentJSON = ObjectMapperContainer.objectMapper.writeValueAsString(EnvironmentContainer.environment)

    override fun handle(requestContext: RequestContext) {
        requestContext.sendJSONResponseOK(environmentJSON)
    }

}