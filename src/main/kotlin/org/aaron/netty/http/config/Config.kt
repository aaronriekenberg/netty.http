package org.aaron.netty.http.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import java.io.File

data class ServerInfo(
        @JsonProperty("listen_address")
        val listenAddress: String,

        @JsonProperty("listen_port")
        val listenPort: Int
)

data class Config(
        @JsonProperty("server_info")
        val serverInfo: ServerInfo
)


object ConfigContainer : KLogging() {

    val config: Config

    init {
        logger.info { "begin init" }
        val objectMapper = ObjectMapper()
        config = objectMapper.readValue(File("config.json"), Config::class.java)
        logger.info { "config = $config" }
    }

}