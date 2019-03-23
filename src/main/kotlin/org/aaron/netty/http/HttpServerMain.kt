package org.aaron.netty.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.util.SelfSignedCertificate
import mu.KLogging

class HttpServerMain {

    companion object : KLogging()

    private val SSL = System.getProperty("ssl") != null
    private val PORT = Integer.parseInt(System.getProperty("port", if (SSL) "8443" else "8080"))


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

        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .handler(LoggingHandler(LogLevel.INFO))
                    .childHandler(HttpStaticFileServerInitializer(sslCtx))

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