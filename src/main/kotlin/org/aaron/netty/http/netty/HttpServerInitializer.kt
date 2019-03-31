package org.aaron.netty.http.netty

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.stream.ChunkedWriteHandler
import org.aaron.netty.http.config.ConfigContainer
import org.aaron.netty.http.handlers.HandlerMap

class HttpServerInitializer(
        private val handlerMap: HandlerMap) : ChannelInitializer<SocketChannel>() {

    public override fun initChannel(ch: SocketChannel) {
        ch.config().isTcpNoDelay = TCP_NO_DELAY

        val pipeline = ch.pipeline()
//        if (sslCtx != null) {
//            pipeline.addLast(sslCtx.newHandler(ch.alloc()))
//        }
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(HttpObjectAggregator(MAX_CONTENT_LENGTH))
        pipeline.addLast(ChunkedWriteHandler())
        pipeline.addLast(HttpServerHandler(handlerMap))
    }

    companion object {
        private const val MAX_CONTENT_LENGTH = 65_536
        private val TCP_NO_DELAY = ConfigContainer.config.serverInfo.tcpNoDelay
    }
}