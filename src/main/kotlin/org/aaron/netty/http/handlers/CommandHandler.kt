package org.aaron.netty.http.handlers

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import mu.KotlinLogging
import org.aaron.netty.http.config.CommandInfo
import org.aaron.netty.http.json.ObjectMapperContainer
import org.aaron.netty.http.netty.*
import org.aaron.netty.http.templates.HandlebarsContainer
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}

private data class CommandTemplateData(
        val commandInfo: CommandInfo
)

class CommandHTMLHandler(commandInfo: CommandInfo) : RespondIfNotModifiedHandler() {

    private val response: DefaultFullHttpResponse

    init {
        logger.debug { "begin init" }

        val commandTemplate = HandlebarsContainer.handlebars.compile("command")

        val commandTemplateData = CommandTemplateData(
                commandInfo = commandInfo
        )

        val htmlString = commandTemplate.apply(commandTemplateData)

        response = newDefaultFullHttpResponse(HttpResponseStatus.OK, htmlString)

        response.setContentTypeHeader(CONTENT_TYPE_TEXT_HTML)
        response.setLastModifiedHeader(lastModified)
        response.setCacheControlHeader()

        logger.debug { "end init" }
    }

    override fun handleModified(requestContext: RequestContext) {
        requestContext.sendResponse(response.retainedDuplicate())
    }

}

class CommandAPIHandler(private val commandInfo: CommandInfo) : Handler {

    private val commandAndArgs = listOf(commandInfo.command) + commandInfo.args

    override fun handle(requestContext: RequestContext) {
        CommandRunner.runCommand(requestContext, commandInfo, commandAndArgs)
    }
}

private data class CommandAPIResult(

        @field:JsonProperty("output_lines")
        val outputLines: List<String>,

        @field:JsonProperty("exit_value")
        val exitValue: Int
)

private object CommandRunner {

    private val objectMapper = ObjectMapperContainer.objectMapper

    private val blockingThreadPool = BlockingThreadPoolContainer.blockingThreadPool

    private fun runCommandBlocking(requestContext: RequestContext, commandInfo: CommandInfo, commandAndArgs: List<String>) {
        val commandAPIResult = try {

            val processBuilder = ProcessBuilder(commandAndArgs)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            logger.debug { "started process $commandAndArgs $process" }

            val exitValue = process.waitFor()
            logger.debug { "exitValue = $exitValue" }

            val outputLines = InputStreamReader(process.inputStream).readLines()

            CommandAPIResult(
                    outputLines = outputLines,
                    exitValue = exitValue)
        } catch (e: Exception) {
            logger.warn(e) { "runCommand $commandInfo" }

            CommandAPIResult(
                    outputLines = listOf("command error ${e.message}"),
                    exitValue = -1)
        }

        val json = objectMapper.writeValueAsString(commandAPIResult)

        val response = newDefaultFullHttpResponse(HttpResponseStatus.OK, json)
        response.setContentTypeHeader(CONTENT_TYPE_APPLICATION_JSON)

        requestContext.sendResponse(response)
    }

    fun runCommand(requestContext: RequestContext, commandInfo: CommandInfo, commandAndArgs: List<String>) {
        blockingThreadPool.execute {
            try {
                runCommandBlocking(
                        requestContext = requestContext,
                        commandInfo = commandInfo,
                        commandAndArgs = commandAndArgs)
            } catch (e: Exception) {
                logger.warn(e) { "runCommand" }
                requestContext.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }
}