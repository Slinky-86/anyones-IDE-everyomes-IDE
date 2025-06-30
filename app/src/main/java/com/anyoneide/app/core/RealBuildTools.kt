package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class RealBuildTools(context: Context) {
    
    private val sdkManager = SDKManager(context)
    
    suspend fun compileKotlinProject(
        projectPath: String,
        sourceFiles: List<String>,
        outputDir: String,
        classpath: List<String> = emptyList()
    ): Flow<String> = flow {
        emit("Starting Kotlin compilation...")
        
        try {
            val kotlincPath = sdkManager.getKotlinCompilerPath()
            if (kotlincPath == null) {
                emit("ERROR: Kotlin compiler not found. Please install Kotlin SDK.")
                return@flow
            }
            
            val outputDirFile = File(outputDir)
            outputDirFile.mkdirs()
            
            val command = buildKotlinCompileCommand(
                kotlincPath,
                sourceFiles,
                outputDir,
                classpath
            )
            
            emit("Executing: ${command.joinToString(" ")}")
            
            val result = sdkManager.executeCommand(command, File(projectPath))
            result.onSuccess { output ->
                emit("Kotlin compilation completed successfully")
                emit(output)
            }
            result.onFailure { error ->
                emit("ERROR: Kotlin compilation failed: ${error.message}")
            }
            
        } catch (_: Exception) {
            emit("Compilation error")
        }
    }
    
    suspend fun compileJavaProject(
        projectPath: String,
        sourceFiles: List<String>,
        outputDir: String,
        classpath: List<String> = emptyList()
    ): Flow<String> = flow {
        emit("Starting Java compilation...")
        
        try {
            val javacPath = sdkManager.getJavaCompilerPath()
            if (javacPath == null) {
                emit("ERROR: Java compiler not found. Please install JDK.")
                return@flow
            }
            
            val outputDirFile = File(outputDir)
            outputDirFile.mkdirs()
            
            val command = buildJavaCompileCommand(
                javacPath,
                sourceFiles,
                outputDir,
                classpath
            )
            
            emit("Executing: ${command.joinToString(" ")}")
            
            val result = sdkManager.executeCommand(command, File(projectPath))
            result.onSuccess { output ->
                emit("Java compilation completed successfully")
                emit(output)
            }
            result.onFailure { error ->
                emit("ERROR: Java compilation failed: ${error.message}")
            }
            
        } catch (_: Exception) {
            emit("Compilation error")
        }
    }
    
    suspend fun runGradleTask(projectPath: String, taskName: String): Flow<String> = flow {
        emit("Running Gradle task: $taskName")
        
        try {
            val projectDir = File(projectPath)
            val gradlewFile = File(projectDir, "gradlew")
            val useWrapper = gradlewFile.exists()
            
            val command = if (useWrapper) {
                if (!gradlewFile.canExecute()) {
                    gradlewFile.setExecutable(true)
                }
                listOf("./gradlew", taskName)
            } else {
                val gradlePath = sdkManager.getGradlePath()
                if (gradlePath == null) {
                    emit("ERROR: Gradle not found.")
                    return@flow
                }
                listOf(gradlePath, taskName)
            }
            
            emit("Executing: ${command.joinToString(" ")}")
            
            val result = sdkManager.executeCommand(command, projectDir)
            result.onSuccess { output ->
                emit("Gradle task '$taskName' completed successfully")
                emit(output)
            }
            result.onFailure { error ->
                emit("ERROR: Gradle task '$taskName' failed: ${error.message}")
            }
            
        } catch (_: Exception) {
            emit("ERROR: Failed to run Gradle task")
        }
    }
    
    private fun buildKotlinCompileCommand(
        kotlincPath: String,
        sourceFiles: List<String>,
        outputDir: String,
        classpath: List<String>
    ): List<String> {
        val command = mutableListOf(kotlincPath)
        
        // Add source files
        command.addAll(sourceFiles)
        
        // Add output directory
        command.addAll(listOf("-d", outputDir))
        
        // Add classpath
        if (classpath.isNotEmpty()) {
            command.addAll(listOf("-cp", classpath.joinToString(":")))
        }
        
        return command
    }
    
    private fun buildJavaCompileCommand(
        javacPath: String,
        sourceFiles: List<String>,
        outputDir: String,
        classpath: List<String>
    ): List<String> {
        val command = mutableListOf(javacPath)
        
        // Add output directory
        command.addAll(listOf("-d", outputDir))
        
        // Add classpath
        if (classpath.isNotEmpty()) {
            command.addAll(listOf("-cp", classpath.joinToString(":")))
        }
        
        // Add source files
        command.addAll(sourceFiles)
        
        return command
    }
}