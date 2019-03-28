package org.aaron.netty.http.handlers

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KLogging
import org.aaron.netty.http.*
import org.aaron.netty.http.config.CommandInfo
import org.aaron.netty.http.templates.HandlebarsContainer
import java.io.InputStreamReader
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.Executors

private data class CommandTemplateData(
        val commandInfo: CommandInfo
)

class CommandHTMLHandler(commandInfo: CommandInfo) : Handler {

    companion object : KLogging()

    private val htmlString: String

    private val lastModified: Instant

    init {
        logger.info { "begin init" }

        val commandTemplate = HandlebarsContainer.handlebars.compile("command")

        lastModified = Instant.now()

        val commandTemplateData = CommandTemplateData(
                commandInfo = commandInfo
        )

        htmlString = commandTemplate.apply(commandTemplateData)

        logger.info { "end init" }
    }

    override fun handle(requestContext: RequestContext) {
        val response = newDefaultFullHttpResponse(HttpResponseStatus.OK, htmlString)

        response.setContentTypeHeader("text/html; charset=UTF-8")
        response.setLastModifiedHeader(lastModified)
        response.setCacheControlHeader()

        requestContext.sendResponse(response)
    }

}

data class CommandAPIResult(
        @JsonProperty("command_info")
        val commandInfo: CommandInfo,

        @JsonProperty("now")
        val now: String,

        @JsonProperty("output")
        val output: String,

        @JsonProperty("exit_value")
        val exitValue: Int
)

class CommandAPIHandler(private val commandInfo: CommandInfo) : Handler {

    companion object : KLogging()

    override fun handle(requestContext: RequestContext) {
        CommandRunner.runCommand(requestContext, commandInfo)
    }
}

object CommandRunner : KLogging() {

    private val objectMapper = ObjectMapper()

    private val runCommandScheduler = Executors.newCachedThreadPool()

    fun runCommand(requestContext: RequestContext, commandInfo: CommandInfo) {
        runCommandScheduler.execute {
            val commandAPIResult = try {
                val commandAndArgs = listOf(commandInfo.command) + commandInfo.args
                val processBuilder = ProcessBuilder(commandAndArgs)
                processBuilder.redirectErrorStream(true)
                logger.debug { "start process $commandAndArgs" }
                val process = processBuilder.start()
                val exitValue = process.waitFor()
                val output = InputStreamReader(process.inputStream)
                        .readLines()
                        .joinToString(separator = "\n")
                logger.debug { "exitValue = $exitValue" }
                CommandAPIResult(
                        commandInfo = commandInfo,
                        now = OffsetDateTime.now().toString(),
                        output = output,
                        exitValue = exitValue)
            } catch (e: Exception) {
                logger.warn(e) { "runCommand $commandInfo" }
                CommandAPIResult(
                        commandInfo = commandInfo,
                        now = OffsetDateTime.now().toString(),
                        output = "command error ${e.message}",
                        exitValue = -1)
            }

            val json = objectMapper.writeValueAsString(commandAPIResult)

            val response = newDefaultFullHttpResponse(HttpResponseStatus.OK, json)
            response.setContentTypeHeader("application/json; charset=UTF-8")

            requestContext.sendResponse(response)
        }
    }
}