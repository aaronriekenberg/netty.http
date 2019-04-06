package org.aaron.netty.http.environment

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KLogging
import java.time.Instant

data class Environment(

        @field:JsonProperty("start_time")
        val startTime: Instant,

        @field:JsonProperty("env_vars")
        val envVars: Map<String, String>,

        @field:JsonProperty("properties")
        val properties: Map<String, String>
)

object EnvironmentContainer : KLogging() {

    val environment = Environment(
            startTime = Instant.now(),
            envVars = System.getenv().toSortedMap(),
            properties = System.getProperties().stringPropertyNames().sorted().map { it to System.getProperty(it) }.toMap()
    )

    init {
        logger.info { "environment=$environment" }
    }

}

fun getStartTime(): Instant = EnvironmentContainer.environment.startTime