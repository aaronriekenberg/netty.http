package org.aaron.netty.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import mu.KotlinLogging
import org.aaron.netty.http.config.Config
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.handlers.*
import org.aaron.netty.http.handlers.debug.ConfigHandler
import org.aaron.netty.http.handlers.debug.EnvironmentHandler
import org.aaron.netty.http.handlers.debug.GCHandler
import org.aaron.netty.http.handlers.debug.MemoryHandler
import org.aaron.netty.http.netty.HttpServerInitializer
import org.aaron.netty.http.netty.createEventLoopGroup
import org.aaron.netty.http.netty.serverSocketChannelClass
import org.aaron.netty.http.utils.getDeltaTimeSinceSecondsString
import java.time.Instant

private val logger = KotlinLogging.logger {}

object HttpServerMain {

    private fun handlerMap(config: Config): HandlerMap {
        var map: HandlerMap = mapOf()

        map += "/" to IndexHandler

        map += "/debug/config" to ConfigHandler

        map += "/debug/environment" to EnvironmentHandler

        map += "/debug/gc" to GCHandler

        map += "/debug/memory" to MemoryHandler

        map += config.staticFileInfo.map { it.url to newStaticFileHandler(it) }

        map += config.commandInfo.map { "/commands/${it.id}" to CommandHTMLHandler(it) }
        map += config.commandInfo.map { "/api/commands/${it.id}" to CommandAPIHandler(it) }

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