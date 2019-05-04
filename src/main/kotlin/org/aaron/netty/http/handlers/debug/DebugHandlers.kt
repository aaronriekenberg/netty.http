package org.aaron.netty.http.handlers.debug

import org.aaron.netty.http.handlers.HandlerMap

fun debugHandlerMap(): HandlerMap {
    val list =
            configHandlers() + environmentHandlers() + gcHandlers() + memoryHandlers() + threadHandlers()
    return list.toMap()
}