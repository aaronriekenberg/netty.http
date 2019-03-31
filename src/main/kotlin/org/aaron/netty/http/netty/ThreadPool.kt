package org.aaron.netty.http.netty

import java.util.concurrent.Executors

object BlockingThreadPoolContainer {

    val blockingThreadPool = Executors.newCachedThreadPool()

}