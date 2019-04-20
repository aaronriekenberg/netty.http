package org.aaron.netty.http.environment

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import io.netty.util.Version
import mu.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

data class Environment(

        @field:JsonProperty("start_time")
        @field:JsonSerialize(using = ToStringSerializer::class)
        val startTime: Instant,

        @field:JsonProperty("env_vars")
        val envVars: Map<String, String>,

        @field:JsonProperty("netty_verison")
        val nettyVersion: Map<String, String>,

        @field:JsonProperty("properties")
        val properties: Map<String, String>
)

object EnvironmentContainer {

    val environment = Environment(
            startTime = Instant.now(),
            envVars = System.getenv().toSortedMap(),
            nettyVersion = Version.identify().mapValues { it.value.toString() },
            properties = System.getProperties().stringPropertyNames().sorted().map { it to System.getProperty(it) }.toMap()
    )

    init {
        logger.info { "environment=$environment" }
    }

}