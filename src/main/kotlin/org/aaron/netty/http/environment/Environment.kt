package org.aaron.netty.http.environment

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KLogging

data class Environment(
        @JsonProperty("env_vars")
        val envVars: Map<String, String>
)

object EnvironmentContainer : KLogging() {

    val environment = Environment(
            envVars = System.getenv().toSortedMap()
    )

    init {
        logger.info { "environment=$environment" }
    }

}