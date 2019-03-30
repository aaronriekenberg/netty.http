package org.aaron.netty.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import mu.KLogging
import org.aaron.netty.http.config.Config
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.handlers.*
import kotlin.reflect.KClass

class HttpServerMain {

    companion object : KLogging()

    private fun createEventLoopGroup(threads: Int = 0): MultithreadEventLoopGroup =
            when {
                Epoll.isAvailable() -> EpollEventLoopGroup(threads)
                KQueue.isAvailable() -> KQueueEventLoopGroup(threads)
                else -> NioEventLoopGroup(threads)
            }

    private fun serverSocketChannelClass(): KClass<out ServerSocketChannel> =
            when {
                Epoll.isAvailable() -> EpollServerSocketChannel::class
                KQueue.isAvailable() -> KQueueServerSocketChannel::class
                else -> NioServerSocketChannel::class
            }

    private fun handlerMap(config: Config): HandlerMap {
        val handlerMap: MutableMap<String, Handler> = mutableMapOf(
                "/" to IndexHandler,
                "/config" to ConfigHandler
        )

        config.staticFileInfo.forEach {
            handlerMap[it.url] = StaticFileHandler(
                    filePath = it.filePath,
                    classpath = it.classpath,
                    contentType = it.contentType
            )
        }


        config.commandInfo.forEach {
            val htmlPath = "/commands/${it.id}"
            handlerMap[htmlPath] = CommandHTMLHandler(
                    commandInfo = it
            )

            val apiPath = "/api/commands/${it.id}"
            handlerMap[apiPath] = CommandAPIHandler(
                    commandInfo = it
            )
        }

        return handlerMap
    }

    fun run() {
        val config = ConfigContainer.config
        logger.info { "begin run" }

        val handlerMap = handlerMap(config)

        val bossGroup = createEventLoopGroup(1)
        val workerGroup = createEventLoopGroup()

        logger.info { "bossGroup=${bossGroup.javaClass.simpleName} executorCount=${bossGroup.executorCount()}" }
        logger.info { "workerGroup=${workerGroup.javaClass.simpleName} executorCount=${workerGroup.executorCount()}" }
        logger.info { "handlerMap.size=${handlerMap.size}" }

        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(serverSocketChannelClass().java)
                    .handler(LoggingHandler(LogLevel.DEBUG))
                    .childHandler(HttpServerInitializer(handlerMap))


            val ch = b.bind(config.serverInfo.listenAddress, config.serverInfo.listenPort)
                    .sync().channel()

            logger.info {
                "server started on ${config.serverInfo.listenAddress}:${config.serverInfo.listenPort}"
            }

            ch.closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}

fun main() {
    HttpServerMain().run()
}