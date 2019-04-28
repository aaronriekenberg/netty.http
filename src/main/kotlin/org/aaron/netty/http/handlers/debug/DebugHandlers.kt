package org.aaron.netty.http.handlers.debug

import org.aaron.netty.http.handlers.HandlerMap

fun debugHandlerMap(): HandlerMap {
    val list =
            createConfigHandlers() + createEnvironmentHandlers() + createGCHandlers() + createMemoryHandlers() + createThreadHandlers()
    return list.toMap()
}