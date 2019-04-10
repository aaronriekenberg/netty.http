package org.aaron.netty.http.netty

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object BlockingThreadPoolContainer {

    private val threadNumber = AtomicInteger(0)

    val blockingThreadPool: ExecutorService = Executors.newCachedThreadPool { runnable ->
        val thread = Thread(runnable)

        thread.name = "BlockingThreadPool-${threadNumber.getAndIncrement()}"

        if (thread.isDaemon) {
            thread.isDaemon = false
        }

        if (thread.priority != Thread.NORM_PRIORITY) {
            thread.priority = Thread.NORM_PRIORITY
        }

        thread
    }

}