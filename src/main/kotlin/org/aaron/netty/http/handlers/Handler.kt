package org.aaron.netty.http.handlers

import org.aaron.netty.http.netty.RequestContext

typealias HandlerMap = Map<String, Handler>

interface Handler {
    fun handle(requestContext: RequestContext)
}