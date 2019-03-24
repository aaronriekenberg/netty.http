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

data class MainPageInfo(
        @JsonProperty("title")
        val title: String
)

data class StaticFileInfo(
        @JsonProperty("url")
        val url: String,

        @JsonProperty("file_path")
        val filePath: String,

        @JsonProperty("classpath")
        val classpath: Boolean,

        @JsonProperty("content_type")
        val contentType: String
)

data class CommandInfo(
        @JsonProperty("id")
        val id: String,

        @JsonProperty("description")
        val description: String,

        @JsonProperty("command")
        val command: String,

        @JsonProperty("args")
        val args: List<String>
)

data class Config(
        @JsonProperty("server_info")
        val serverInfo: ServerInfo,

        @JsonProperty("main_page_info")
        val mainPageInfo: MainPageInfo,

        @JsonProperty("static_file_info")
        val staticFileInfo: List<StaticFileInfo>,

        @JsonProperty("command_info")
        val commandInfo: List<CommandInfo>
)


object ConfigContainer : KLogging() {

    val config: Config

    init {
        logger.info { "begin init" }
        val objectMapper = ObjectMapper()
        config = objectMapper.readValue(File("config.json"), Config::class.java)
        logger.info { "end init config = $config" }
    }

}