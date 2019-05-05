package org.aaron.netty.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import mu.KotlinLogging
import org.aaron.netty.http.config.Config
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.handlers.HandlerMap
import org.aaron.netty.http.handlers.IndexHandler
import org.aaron.netty.http.handlers.createHandlersForCommand
import org.aaron.netty.http.handlers.debug.debugHandlerMap
import org.aaron.netty.http.handlers.newStaticFileHandler
import org.aaron.netty.http.server.HttpServerInitializer
import org.aaron.netty.http.server.createEventLoopGroup
import org.aaron.netty.http.server.serverSocketChannelClass
import org.aaron.netty.http.utils.getDeltaTimeSinceSecondsString
import java.time.Instant

private val logger = KotlinLogging.logger {}

object HttpServerMain {

    private fun handlerMap(config: Config): HandlerMap {
        var map: HandlerMap = mapOf()

        map += "/" to IndexHandler

        map += config.staticFileInfo.map { it.url to newStaticFileHandler(it) }

        map += config.commandInfo.flatMap { createHandlersForCommand(it) }

        map += debugHandlerMap()

        return map
    }

    fun run() {
        val runStartTime = Instant.now()

        logger.info { "begin run" }

        val config = ConfigContainer.config

        val handlerMap = handlerMap(config)

        val bossGroup = createEventLoopGroup(1)
        val workerGroup = createEventLoopGroup()

        logger.info { "bossGroup=${bossGroup.javaClass.simpleName} executorCount=${bossGroup.executorCount()}" }
        logger.info { "workerGroup=${workerGroup.javaClass.simpleName} executorCount=${workerGroup.executorCount()}" }
        logger.info { "handlerMap.size=${handlerMap.size}" }

        try {
            val serverChannel = ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(serverSocketChannelClass().java)
                    .handler(LoggingHandler(LogLevel.DEBUG))
                    .childHandler(HttpServerInitializer(handlerMap))
                    .bind(config.serverInfo.listenAddress, config.serverInfo.listenPort)
                    .sync().channel()

            logger.info {
                "server started on ${serverChannel.localAddress()} in ${runStartTime.getDeltaTimeSinceSecondsString()}s"
            }

            serverChannel.closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}

fun main() {
    HttpServerMain.run()
}