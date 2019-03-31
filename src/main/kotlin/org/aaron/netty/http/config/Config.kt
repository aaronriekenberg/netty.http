package org.aaron.netty.http.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import java.io.File

data class ServerInfo(

        @param:JsonProperty("listen_address")
        @get:JsonProperty("listen_address")
        val listenAddress: String,

        @param:JsonProperty("listen_port")
        @get:JsonProperty("listen_port")
        val listenPort: Int,

        @param:JsonProperty("tcp_no_delay")
        @get:JsonProperty("tcp_no_delay")
        val tcpNoDelay: Boolean
)

data class MainPageInfo(

        @param:JsonProperty("title")
        @get:JsonProperty("title")
        val title: String
)

data class StaticFileInfo(

        @param:JsonProperty("url")
        @get:JsonProperty("url")
        val url: String,

        @param:JsonProperty("file_path")
        @get:JsonProperty("file_path")
        val filePath: String,

        @param:JsonProperty("classpath")
        @get:JsonProperty("classpath")
        val classpath: Boolean,

        @param:JsonProperty("content_type")
        @get:JsonProperty("content_type")
        val contentType: String,

        @param:JsonProperty("include_in_main_page")
        @get:JsonProperty("include_in_main_page")
        val includeInMainPage: Boolean
)

data class CommandInfo(

        @param:JsonProperty("id")
        @get:JsonProperty("id")
        val id: String,

        @param:JsonProperty("description")
        @get:JsonProperty("description")
        val description: String,

        @param:JsonProperty("command")
        @get:JsonProperty("command")
        val command: String,

        @param:JsonProperty("args")
        @get:JsonProperty("args")
        val args: List<String>
)

data class Config(

        @param:JsonProperty("server_info")
        @get:JsonProperty("server_info")
        val serverInfo: ServerInfo,

        @param:JsonProperty("main_page_info")
        @get:JsonProperty("main_page_info")
        val mainPageInfo: MainPageInfo,

        @param:JsonProperty("static_file_info")
        @get:JsonProperty("static_file_info")
        val staticFileInfo: List<StaticFileInfo>,

        @param:JsonProperty("command_info")
        @get:JsonProperty("command_info")
        val commandInfo: List<CommandInfo>
)


object ConfigContainer : KLogging() {

    val config: Config

    init {
        logger.info { "begin init" }

        val configFile = System.getProperty("config.file.name") ?: "config.json"
        logger.info { "configFile = '$configFile'" }

        config = ObjectMapper().readValue(File(configFile), Config::class.java)

        logger.info { "end init config = $config" }
    }

}