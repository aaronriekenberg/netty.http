package org.aaron.netty.http.handlers

import org.aaron.netty.http.environment.EnvironmentContainer
import org.aaron.netty.http.netty.RequestContext
import org.aaron.netty.http.netty.respondIfNotModified
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