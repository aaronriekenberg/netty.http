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
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.util.SelfSignedCertificate
import mu.KLogging
import kotlin.reflect.KClass

class HttpServerMain {

    companion object : KLogging()

    private val SSL = System.getProperty("ssl") != null
    private val PORT = Integer.parseInt(System.getProperty("port", if (SSL) "8443" else "8080"))

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

    fun run() {
        logger.info { "begin run" }

        // Configure SSL.
        val sslCtx: SslContext?
        if (SSL) {
            val ssc = SelfSignedCertificate()
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                    .sslProvider(SslProvider.JDK).build()
        } else {
            sslCtx = null
        }

        val bossGroup = createEventLoopGroup(1)
        val workerGroup = createEventLoopGroup()

        logger.info { "bossGroup=${bossGroup.javaClass.simpleName} executorCount=${bossGroup.executorCount()}" }
        logger.info { "workerGroup=${workerGroup.javaClass.simpleName} executorCount=${workerGroup.executorCount()}" }

        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(serverSocketChannelClass().java)
                    .handler(LoggingHandler(LogLevel.INFO))
                    .childHandler(HttpServerInitializer(sslCtx))

            val ch = b.bind(PORT).sync().channel()

            logger.info {
                "Open your web browser and navigate to ${if (SSL) "https" else "http"}://127.0.0.1:$PORT/"
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