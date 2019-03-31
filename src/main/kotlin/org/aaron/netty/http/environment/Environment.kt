package org.aaron.netty.http.environment

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KLogging

data class Environment(

        @param:JsonProperty("env_vars")
        @get:JsonProperty("env_vars")
        val envVars: Map<String, String>,

        @param:JsonProperty("properties")
        @get:JsonProperty("properties")
        val properties: Map<String, String>
)

object EnvironmentContainer : KLogging() {

    val environment = Environment(
            envVars = System.getenv().toSortedMap(),
            properties = System.getProperties().stringPropertyNames().sorted().map { it to System.getProperty(it) }.toMap()
    )

    init {
        logger.info { "environment=$environment" }
    }

}