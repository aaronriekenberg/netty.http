package org.aaron.netty.http.templates

import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.aaron.netty.http.handlers.RespondIfNotModifiedHandler
import org.aaron.netty.http.netty.*

abstract class TemplateHTMLHandler(
        templateName: String,
        templateData: Any
) : RespondIfNotModifiedHandler() {

    private val response: DefaultFullHttpResponse

    init {
        val template = HandlebarsContainer.handlebars.compile(templateName)
        val htmlString = template.apply(templateData)

        response = newDefaultFullHttpResponse(DEFAULT_PROTOCOL_VERSION, HttpResponseStatus.OK, htmlString)

        response.setContentTypeHeader(CONTENT_TYPE_TEXT_HTML)
        response.setLastModifiedHeader(lastModified)
        response.setCacheControlHeader()
    }

    final override fun handleModified(requestContext: RequestContext) {
        requestContext.sendRetainedDuplicate(response)
    }

}