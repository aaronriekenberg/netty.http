package org.aaron.netty.http.config

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KLogging
import org.aaron.netty.http.json.ObjectMapperContainer
import java.io.File

data class ServerInfo(

        @field:JsonProperty("listen_address")
        val listenAddress: String,

        @field:JsonProperty("listen_port")
        val listenPort: Int,

        @field:JsonProperty("tcp_no_delay")
        val tcpNoDelay: Boolean
)

data class MainPageInfo(

        @field:JsonProperty("title")
        val title: String
)

data class StaticFileInfo(

        @field:JsonProperty("url")
        val url: String,

        @field:JsonProperty("file_path")
        val filePath: String,

        @field:JsonProperty("classpath")
        val classpath: Boolean,

        @field:JsonProperty("content_type")
        val contentType: String,

        @field:JsonProperty("include_in_main_page")
        val includeInMainPage: Boolean
)

data class CommandInfo(

        @field:JsonProperty("id")
        val id: String,

        @field:JsonProperty("description")
        val description: String,

        @field:JsonProperty("command")
        val command: String,

        @field:JsonProperty("args")
        val args: List<String>
)

data class Config(

        @field:JsonProperty("server_info")
        val serverInfo: ServerInfo,

        @field:JsonProperty("main_page_info")
        val mainPageInfo: MainPageInfo,

        @field:JsonProperty("static_file_info")
        val staticFileInfo: List<StaticFileInfo>,

        @field:JsonProperty("command_info")
        val commandInfo: List<CommandInfo>
)


object ConfigContainer : KLogging() {

    val config: Config

    init {
        logger.info { "begin init" }

        val configFile = System.getProperty("config.file.name") ?: "config.json"
        logger.info { "configFile = '$configFile'" }

        config = ObjectMapperContainer.objectMapper.readValue(File(configFile), Config::class.java)

        logger.info { "end init config = $config" }
    }

}