package org.aaron.netty.http.handlers

import com.github.jknack.handlebars.Template
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.netty.*
import java.time.Instant

typealias HandlerPairList = List<Pair<String, Handler>>
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