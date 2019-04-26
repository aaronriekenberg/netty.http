package org.aaron.netty.http.handlers.debug

import org.aaron.netty.http.handlers.HandlerMap

fun debugHandlerMap(): HandlerMap = mapOf(

        "/debug/config" to ConfigHandler,

        "/debug/environment" to EnvironmentHandler,

        "/debug/gc" to GCHandler,

        "/debug/memory" to MemoryHandler,

        "/debug/thread" to ThreadHandler
)