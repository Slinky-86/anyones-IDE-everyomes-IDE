package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern

class BuildManager(private val context: Context) {
    annotation class runGradleTask(val projectPath: String, val taskName: String)

    companion object {
        // Error and warning patterns for better parsing
        private val ERROR_PATTERN = Pattern.compile("(?i)(error|exception|failure|failed):\\s*(.*)")
        private val WARNING_PATTERN = Pattern.compile("(?i)(warning):\\s*(.*)")
        private val TASK_PATTERN = Pattern.compile("> Task :([\\w:]+)")
        private val FILE_LINE_PATTERN = Pattern.compile("([\\w./]+\\.\\w+):(\\d+)(?::(\\d+))?:\\s*(.*)")
    }

    suspend fun buildProject(projectPath: String, buildType: String = "debug"): Flow<BuildOutputMessage> = flow {
        emit(BuildOutputMessage(BuildOutputType.INFO, "Starting build..."))

        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Project directory does not exist: $projectPath"))
                return@flow
            }

            // Check for Gradle wrapper
            val gradlewFile = File(projectDir, if (isWindows()) "gradlew.bat" else "gradlew")
            val useWrapper = gradlewFile.exists()

            val command = if (useWrapper) {
                if (isWindows()) "gradlew.bat" else "./gradlew"
            } else {
                "gradle"
            }

            val task = when (buildType.lowercase()) {
                "debug" -> "assembleDebug"
                "release" -> "assembleRelease"
                "clean" -> "clean"
                "test" -> "test"
                else -> buildType
            }

            emit(BuildOutputMessage(BuildOutputType.INFO, "Executing: $command $task"))

            val processBuilder = ProcessBuilder(command, task, "--console=plain", "--no-daemon")
            processBuilder.directory(projectDir)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            val errors = mutableListOf<String>()
            val warnings = mutableListOf<String>()
            var currentTask: String // Removed redundant initializer

            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    // Parse task information
                    val taskMatcher = TASK_PATTERN.matcher(outputLine)
                    if (taskMatcher.find()) {
                        currentTask = taskMatcher.group(1) ?: ""
                        emit(BuildOutputMessage(BuildOutputType.TASK, "Executing task: $currentTask"))
                        return@let
                    }

                    // Parse file:line:column errors
                    val fileLineMatcher = FILE_LINE_PATTERN.matcher(outputLine)
                    if (fileLineMatcher.find()) {
                        val file = fileLineMatcher.group(1) ?: ""
                        val lineNumberStr = fileLineMatcher.group(2) ?: "" // Renamed 'line' to 'lineNumberStr'
                        // val column = fileLineMatcher.group(3) ?: "0" // Removed unused variable 'column'
                        val message = fileLineMatcher.group(4) ?: ""

                        if (outputLine.lowercase().contains("error")) {
                            val formattedError = "Error in $file at line $lineNumberStr: $message"
                            errors.add(formattedError)
                            emit(BuildOutputMessage(BuildOutputType.ERROR, formattedError))
                            return@let
                        } else if (outputLine.lowercase().contains("warning")) {
                            val formattedWarning = "Warning in $file at line $lineNumberStr: $message"
                            warnings.add(formattedWarning)
                            emit(BuildOutputMessage(BuildOutputType.WARNING, formattedWarning))
                            return@let
                        } else {
                            emit(BuildOutputMessage(BuildOutputType.INFO, outputLine))
                            return@let
                        }
                    }

                    // Parse general errors and warnings
                    val errorMatcher = ERROR_PATTERN.matcher(outputLine)
                    val warningMatcher = WARNING_PATTERN.matcher(outputLine)

                    when {
                        errorMatcher.find() -> {
                            val errorMessage = errorMatcher.group(2) ?: outputLine
                            errors.add(errorMessage)
                            emit(BuildOutputMessage(BuildOutputType.ERROR, errorMessage))
                        }
                        warningMatcher.find() -> {
                            val warningMessage = warningMatcher.group(2) ?: outputLine
                            warnings.add(warningMessage)
                            emit(BuildOutputMessage(BuildOutputType.WARNING, warningMessage))
                        }
                        else -> {
                            emit(BuildOutputMessage(BuildOutputType.INFO, outputLine))
                        }
                    }
                }
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                emit(BuildOutputMessage(BuildOutputType.SUCCESS, "BUILD SUCCESSFUL", errors = errors, warnings = warnings))

                // Find build artifacts
                val artifacts = findBuildArtifacts(projectDir, buildType)
                artifacts.forEach { artifact ->
                    emit(BuildOutputMessage(BuildOutputType.ARTIFACT, "Generated: ${artifact.name} (${formatFileSize(artifact.size)})"))
                }
            } else {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "BUILD FAILED with exit code: $exitCode", errors = errors, warnings = warnings))
            }

        } catch (e: Exception) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Build error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun cleanProject(projectPath: String): Flow<BuildOutputMessage> = flow {
        emit(BuildOutputMessage(BuildOutputType.INFO, "Cleaning project..."))

        try {
            val projectDir = File(projectPath)
            val buildDir = File(projectDir, "build")

            if (buildDir.exists()) {
                buildDir.deleteRecursively()
                emit(BuildOutputMessage(BuildOutputType.SUCCESS, "Build directory cleaned"))
            }

            // Also run gradle clean
            buildProject(projectPath, "clean").collect { output ->
                emit(output)
            }

        } catch (_: Exception) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Clean error"))
        }
    }

    suspend fun installDebugApk(_projectPath: String): Flow<BuildOutputMessage> = flow { // Renamed parameter
        emit(BuildOutputMessage(BuildOutputType.INFO, "Installing debug APK..."))

        try {
            // First build debug APK
            buildProject(_projectPath, "debug").collect { output -> // Used renamed parameter
                emit(output)
                if (output.type == BuildOutputType.SUCCESS) {
                    // Try to install using ADB
                    installApkWithAdb(_projectPath, "debug").collect { installOutput -> // Used renamed parameter
                        emit(installOutput)
                    }
                }
            }
        } catch (_: Exception) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Install error"))
        }
    }

    private suspend fun installApkWithAdb(_projectPath: String, _buildType: String): Flow<BuildOutputMessage> = flow { // Renamed parameters
        try {
            val apkFile = findApkFile(File(_projectPath), _buildType) // Used renamed parameters
            if (apkFile == null) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "APK file not found"))
                return@flow
            }

            emit(BuildOutputMessage(BuildOutputType.INFO, "Installing ${apkFile.name}..."))

            val processBuilder = ProcessBuilder("adb", "install", "-r", apkFile.absolutePath)
            val process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    emit(BuildOutputMessage(BuildOutputType.INFO, outputLine))
                }
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                emit(BuildOutputMessage(BuildOutputType.SUCCESS, "APK installed successfully"))
            } else {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "APK installation failed"))
            }

        } catch (_: Exception) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "ADB install error"))
        }
    }

    suspend fun getGradleTasks(projectPath: String): Result<List<GradleTask>> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(projectPath)
            val gradlewFile = File(projectDir, if (isWindows()) "gradlew.bat" else "gradlew")
            val useWrapper = gradlewFile.exists()

            val command = if (useWrapper) {
                if (isWindows()) "gradlew.bat" else "./gradlew"
            } else {
                "gradle"
            }

            val processBuilder = ProcessBuilder(command, "tasks", "--all", "--console=plain")
            processBuilder.directory(projectDir)

            // Set environment variables for better performance
            val env = processBuilder.environment()
            env["GRADLE_OPTS"] = "-Dorg.gradle.daemon=true -Dorg.gradle.parallel=true -Dorg.gradle.caching=true -Dorg.gradle.configureondemand=true"

            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            val tasks = mutableListOf<GradleTask>()
            var inTasksSection = false

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { taskLine ->
                    if (taskLine.contains("All tasks runnable from root project")) {
                        inTasksSection = true
                    } else if (inTasksSection && taskLine.startsWith("BUILD SUCCESSFUL")) {
                        return@let
                    } else if (inTasksSection && taskLine.isNotBlank() && !taskLine.startsWith("-")) {
                        val parts = taskLine.split(" - ", limit = 2)
                        if (parts.size >= 2) {
                            val taskName = parts[0].trim()
                            val description = parts[1].trim()

                            val group = when {
                                taskName.startsWith("assemble") -> "Build"
                                taskName.startsWith("test") -> "Verification"
                                taskName.startsWith("clean") -> "Build"
                                taskName.startsWith("install") -> "Install"
                                taskName.startsWith("uninstall") -> "Install"
                                else -> "Other"
                            }

                            tasks.add(GradleTask(taskName, description, group))
                        }
                    }
                }
            }

            process.waitFor()
            Result.success(tasks)

        } catch (_: Exception) {
            Result.failure(Exception("Failed to get Gradle tasks"))
        }
    }

    private fun findBuildArtifacts(projectDir: File, buildType: String): List<BuildArtifact> {
        val artifacts = mutableListOf<BuildArtifact>()

        // Look for APK files
        val apkDir = File(projectDir, "app/build/outputs/apk/$buildType")
        if (apkDir.exists()) {
            apkDir.listFiles { file -> file.extension == "apk" }?.forEach { apkFile ->
                artifacts.add(BuildArtifact(
                    name = apkFile.name,
                    path = apkFile.absolutePath,
                    type = ArtifactType.APK,
                    size = apkFile.length()
                ))
            }
        }

        // Look for AAB files
        val aabDir = File(projectDir, "app/build/outputs/bundle/$buildType")
        if (aabDir.exists()) {
            aabDir.listFiles { file -> file.extension == "aab" }?.forEach { aabFile ->
                artifacts.add(BuildArtifact(
                    name = aabFile.name,
                    path = aabFile.absolutePath,
                    type = ArtifactType.AAB,
                    size = aabFile.length()
                ))
            }
        }

        return artifacts
    }

    private fun findApkFile(projectDir: File, buildType: String): File? {
        val apkDir = File(projectDir, "app/build/outputs/apk/$buildType")
        return apkDir.listFiles { file -> file.extension == "apk" }?.firstOrNull()
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name")?.lowercase()?.contains("windows") == true
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.1f KB".format(kb)
            else -> "$bytes bytes"
        }
    }
}

// Renamed to avoid conflict with model.BuildOutput
data class BuildOutputMessage(
    val type: BuildOutputType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val taskName: String = ""
)

enum class BuildOutputType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS,
    TASK,
    ARTIFACT
}

data class GradleTask(
    val name: String,
    val description: String,
    val group: String
)

data class BuildArtifact(
    val name: String,
    val path: String,
    val type: ArtifactType,
    val size: Long
)

// Extension function to check if a build output message contains errors
fun BuildOutputMessage.hasErrors(): Boolean {
    return type == BuildOutputType.ERROR || errors.isNotEmpty()
}

// Extension function to check if a build output message contains warnings
fun BuildOutputMessage.hasWarnings(): Boolean {
    return type == BuildOutputType.WARNING || warnings.isNotEmpty()
}

enum class ArtifactType {
    APK,
    AAB,
    JAR,
    AAR
}
