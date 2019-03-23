package org.aaron.netty.http

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContext
import io.netty.handler.stream.ChunkedWriteHandler

class HttpServerInitializer(private val sslCtx: SslContext?) : ChannelInitializer<SocketChannel>() {

    private val maxContentLength = 65_536

    public override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()))
        }
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(HttpObjectAggregator(maxContentLength))
        pipeline.addLast(ChunkedWriteHandler())
        pipeline.addLast(HttpStaticFileServerHandler())
    }
}
