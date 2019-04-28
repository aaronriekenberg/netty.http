package org.aaron.netty.http.handlers

import com.github.jknack.handlebars.Template
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.netty.*
import java.time.Instant

typealias HandlerMap = Map<String, Handler>

interface Handler {
    fun handle(requestContext: RequestContext)
}

abstract class RespondIfNotModifiedHandler(
        protected val lastModified: Instant = EnvironmentContainer.environment.startTime) : Handler {

    protected abstract fun handleModified(requestContext: RequestContext)

    final override fun handle(requestContext: RequestContext) {
        if (!requestContext.respondIfNotModified(lastModified)) {
            handleModified(requestContext)
        }
    }

}

abstract class TemplateHTMLHandler(
        template: Template,
        templateData: Any
) : RespondIfNotModifiedHandler() {

    private val response: DefaultFullHttpResponse

    init {
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