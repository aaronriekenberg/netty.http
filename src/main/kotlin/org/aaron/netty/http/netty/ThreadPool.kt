package org.aaron.netty.http.netty

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object BlockingThreadPoolContainer {

    val blockingThreadPool: ExecutorService = Executors.newCachedThreadPool()

}