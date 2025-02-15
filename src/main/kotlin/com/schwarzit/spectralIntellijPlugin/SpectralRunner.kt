package com.schwarzit.spectralIntellijPlugin

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.schwarzit.spectralIntellijPlugin.settings.ProjectSettingsState
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.time.Duration
import java.util.concurrent.ExecutionException

@Service(Service.Level.PROJECT)
class SpectralRunner(private val project: Project) {
    companion object {
        private val logger = getLogger()
        private val timeout = Duration.ofSeconds(30)
    }

    private val executor = project.service<CommandLineExecutor>()
    private val parser = project.service<SpectralOutputParser>()
    private val settings = project.service<ProjectSettingsState>()

    fun run(content: String): List<SpectralIssue> {
        val tempFile = try {
            // It's necessary to write the content to be linted into a temp file as the linted content otherwise might be outdated
            File.createTempFile("spectral-intellij-input-", ".tmp").apply { writeText(content) }
        } catch (e: IOException) {
            throw SpectralException("Failed to create temporary file", e)
        }

        return try {
            createCommand()
                .withWorkDirectory(project.basePath)
                .withParameters("-r", settings.ruleset)
                .withParameters("-f", "json")
                .withParameters("lint")
                .withInput(tempFile.absoluteFile)
                .execute()
        } finally {
            if (!tempFile.delete()) {
                logger.debug("Failed to delete temporary file ${tempFile.canonicalPath}")
            }
        }
    }

    private fun createCommand(): GeneralCommandLine {
        // NODE_OPTIONS need to be set as the spectral cli currently uses a deprecated dependency (See: https://github.com/stoplightio/spectral/issues/2622)
        // By default this warning would be printed when executing the command making the plugin fail when attempting to parse the command response
        return GeneralCommandLine("spectral").withCharset(StandardCharsets.UTF_8).withEnvironment("NODE_OPTIONS", "--no-warnings")
    }

    @Throws(SpectralException::class)
    private fun GeneralCommandLine.execute(): List<SpectralIssue> {
        val output = try {
            executor.execute(this, timeout)
        } catch (e: ExecutionException) {
            throw SpectralException("Failed to execute command", e)
        }

        val successExitCodes = (0..2).toList()

        if (output.exitCode !in successExitCodes) {
            throw SpectralException("Spectral finished with exit code ${output.exitCode} but expected one of $successExitCodes\n${output.stderr}")
        }

        if (output.stderr.isNotBlank()) {
            throw SpectralException("An unexpected error occurred:\n${output.stderr}")
        }

        try {
            return parser.parse(output.stdout)
        } catch (e: ParseException) {
            throw SpectralException("Failed to parse output", e)
        }

    }
}