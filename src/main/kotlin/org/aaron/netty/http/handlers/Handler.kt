package org.aaron.netty.http.handlers

import org.aaron.netty.http.RequestContext

typealias HandlerMap = Map<String, Handler>

interface Handler {
    fun handle(requestContext: RequestContext)
}