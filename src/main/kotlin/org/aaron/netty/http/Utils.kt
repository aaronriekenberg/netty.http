package org.aaron.netty.http

import java.time.Duration
import java.time.Instant

fun Instant.getDeltaTimeSinceSecondsString(): String {
    val deltaTimeDuration = Duration.between(this, Instant.now())
    return "%.09f".format(deltaTimeDuration.toNanos() / 1e9)
}