package org.aaron.netty.http.utils

import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

fun File.getLastModifiedInstant(): Instant =
        Instant.ofEpochMilli(lastModified())

fun Instant.getDeltaTimeSinceSecondsString(now: Instant = Instant.now()): String {
    val deltaTimeDuration = Duration.between(this, now)
    return "%.09f".format(deltaTimeDuration.toNanos() / 1e9) + "s"
}

fun Instant.toOffsetDateTime(): OffsetDateTime =
        OffsetDateTime.ofInstant(this, ZoneId.systemDefault())