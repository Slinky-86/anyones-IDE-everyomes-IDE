package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class CompilerService(context: Context) {
    
    private val sdkManager = SDKManager(context)
    
    suspend fun compileKotlin(
        sourceFiles: List<String>,
        outputDir: String,
        classpath: List<String> = emptyList(),
        options: CompilerOptions = CompilerOptions()
    ): Flow<CompilationResult> = flow {
        emit(CompilationResult.Progress("Starting Kotlin compilation..."))
        
        try {
            // Ensure Kotlin compiler is available
            val kotlincPath = sdkManager.getKotlinCompilerPath()
            if (kotlincPath == null) {
                emit(CompilationResult.Error("Kotlin compiler not found. Please install Kotlin SDK."))
                return@flow
            }
            
            val outputDirFile = File(outputDir)
            outputDirFile.mkdirs()
            
            val command = buildKotlinCompileCommand(
                kotlincPath,
                sourceFiles,
                outputDir,
                classpath,
                options
            )
            
            emit(CompilationResult.Progress("Executing: ${command.joinToString(" ")}"))
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val output = StringBuilder()
            
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    output.appendLine(outputLine)
                    emit(CompilationResult.Progress(outputLine))
                }
            }
            
            val exitCode = process.waitFor(30, TimeUnit.SECONDS)
            
            if (exitCode && process.exitValue() == 0) {
                emit(CompilationResult.Success("Kotlin compilation completed successfully", output.toString()))
            } else {
                emit(CompilationResult.Error("Kotlin compilation failed", output.toString()))
            }
            
        } catch (_: Exception) {
            emit(CompilationResult.Error("Compilation error"))
        }
    }
    
    suspend fun compileJava(
        sourceFiles: List<String>,
        outputDir: String,
        classpath: List<String> = emptyList(),
        options: CompilerOptions = CompilerOptions()
    ): Flow<CompilationResult> = flow {
        emit(CompilationResult.Progress("Starting Java compilation..."))
        
        try {
            val javacPath = sdkManager.getJavaCompilerPath()
            if (javacPath == null) {
                emit(CompilationResult.Error("Java compiler not found. Please install JDK."))
                return@flow
            }
            
            val outputDirFile = File(outputDir)
            outputDirFile.mkdirs()
            
            val command = buildJavaCompileCommand(
                javacPath,
                sourceFiles,
                outputDir,
                classpath,
                options
            )
            
            emit(CompilationResult.Progress("Executing: ${command.joinToString(" ")}"))
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val output = StringBuilder()
            
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    output.appendLine(outputLine)
                    emit(CompilationResult.Progress(outputLine))
                }
            }
            
            val exitCode = process.waitFor(30, TimeUnit.SECONDS)
            
            if (exitCode && process.exitValue() == 0) {
                emit(CompilationResult.Success("Java compilation completed successfully", output.toString()))
            } else {
                emit(CompilationResult.Error("Java compilation failed", output.toString()))
            }
            
        } catch (_: Exception) {
            emit(CompilationResult.Error("Compilation error"))
        }
    }
    
    suspend fun buildAndroidProject(
        projectPath: String,
        buildType: String = "debug",
        gradleArgs: List<String> = emptyList()
    ): Flow<CompilationResult> = flow {
        emit(CompilationResult.Progress("Starting Android project build..."))
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(CompilationResult.Error("Project directory does not exist: $projectPath"))
                return@flow
            }
            
            // Check for Gradle wrapper
            val gradlewFile = File(projectDir, "gradlew")
            val useWrapper = gradlewFile.exists()
            
            if (useWrapper && !gradlewFile.canExecute()) {
                gradlewFile.setExecutable(true)
            }
            
            val command = if (useWrapper) {
                listOf("./gradlew") + "assemble${buildType.replaceFirstChar { it.uppercase() }}" + gradleArgs
            } else {
                listOf("gradle") + "assemble${buildType.replaceFirstChar { it.uppercase() }}" + gradleArgs
            }
            
            emit(CompilationResult.Progress("Executing: ${command.joinToString(" ")}"))
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.directory(projectDir)
            processBuilder.redirectErrorStream(true)
            
            // Set environment variables
            val env = processBuilder.environment()
            sdkManager.getAndroidSDKPath()?.let { env["ANDROID_HOME"] = it }
            sdkManager.getJavaHomePath()?.let { env["JAVA_HOME"] = it }
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val output = StringBuilder()
            
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    output.appendLine(outputLine)
                    emit(CompilationResult.Progress(outputLine))
                }
            }
            
            val exitCode = process.waitFor(300, TimeUnit.SECONDS) // 5 minutes timeout
            
            if (exitCode && process.exitValue() == 0) {
                // Find generated APK
                val apkPath = findGeneratedApk(projectDir, buildType)
                emit(CompilationResult.Success(
                    "Android build completed successfully",
                    output.toString(),
                    apkPath
                ))
            } else {
                emit(CompilationResult.Error("Android build failed", output.toString()))
            }
            
        } catch (_: Exception) {
            emit(CompilationResult.Error("Build error"))
        }
    }
    
    suspend fun signApk(
        unsignedApkPath: String,
        signedApkPath: String,
        keystorePath: String,
        keystorePassword: String,
        keyAlias: String,
        keyPassword: String
    ): Flow<CompilationResult> = flow {
        emit(CompilationResult.Progress("Starting APK signing..."))
        
        try {
            val apksignerPath = sdkManager.getApkSignerPath()
            if (apksignerPath == null) {
                emit(CompilationResult.Error("apksigner not found. Please install Android SDK build tools."))
                return@flow
            }
            
            val command = listOf(
                apksignerPath,
                "sign",
                "--ks", keystorePath,
                "--ks-pass", "pass:$keystorePassword",
                "--ks-key-alias", keyAlias,
                "--key-pass", "pass:$keyPassword",
                "--out", signedApkPath,
                unsignedApkPath
            )
            
            emit(CompilationResult.Progress("Signing APK..."))
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val output = StringBuilder()
            
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    output.appendLine(outputLine)
                    emit(CompilationResult.Progress(outputLine))
                }
            }
            
            val exitCode = process.waitFor(60, TimeUnit.SECONDS)
            
            if (exitCode && process.exitValue() == 0) {
                emit(CompilationResult.Success("APK signed successfully", output.toString(), signedApkPath))
            } else {
                emit(CompilationResult.Error("APK signing failed", output.toString()))
            }
            
        } catch (_: Exception) {
            emit(CompilationResult.Error("Signing error"))
        }
    }
    
    suspend fun optimizeApk(
        inputApkPath: String,
        outputApkPath: String
    ): Flow<CompilationResult> = flow {
        emit(CompilationResult.Progress("Starting APK optimization..."))
        
        try {
            val zipalignPath = sdkManager.getZipalignPath()
            if (zipalignPath == null) {
                emit(CompilationResult.Error("zipalign not found. Please install Android SDK build tools."))
                return@flow
            }
            
            val command = listOf(
                zipalignPath,
                "-v", "4",
                inputApkPath,
                outputApkPath
            )
            
            emit(CompilationResult.Progress("Optimizing APK..."))
            
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val output = StringBuilder()
            
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    output.appendLine(outputLine)
                    emit(CompilationResult.Progress(outputLine))
                }
            }
            
            val exitCode = process.waitFor(60, TimeUnit.SECONDS)
            
            if (exitCode && process.exitValue() == 0) {
                emit(CompilationResult.Success("APK optimized successfully", output.toString(), outputApkPath))
            } else {
                emit(CompilationResult.Error("APK optimization failed", output.toString()))
            }
            
        } catch (_: Exception) {
            emit(CompilationResult.Error("Optimization error"))
        }
    }
    
    private fun buildKotlinCompileCommand(
        kotlincPath: String,
        sourceFiles: List<String>,
        outputDir: String,
        classpath: List<String>,
        options: CompilerOptions
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
        
        // Add compiler options
        if (options.verbose) {
            command.add("-verbose")
        }
        
        if (options.includeRuntime) {
            command.add("-include-runtime")
        }
        
        if (options.jvmTarget.isNotEmpty()) {
            command.addAll(listOf("-jvm-target", options.jvmTarget))
        }
        
        return command
    }
    
    private fun buildJavaCompileCommand(
        javacPath: String,
        sourceFiles: List<String>,
        outputDir: String,
        classpath: List<String>,
        options: CompilerOptions
    ): List<String> {
        val command = mutableListOf(javacPath)
        
        // Add output directory
        command.addAll(listOf("-d", outputDir))
        
        // Add classpath
        if (classpath.isNotEmpty()) {
            command.addAll(listOf("-cp", classpath.joinToString(":")))
        }
        
        // Add compiler options
        if (options.verbose) {
            command.add("-verbose")
        }
        
        if (options.jvmTarget.isNotEmpty()) {
            command.addAll(listOf("-target", options.jvmTarget))
            command.addAll(listOf("-source", options.jvmTarget))
        }
        
        // Add source files
        command.addAll(sourceFiles)
        
        return command
    }
    
    private fun findGeneratedApk(projectDir: File, buildType: String): String? {
        val apkDir = File(projectDir, "app/build/outputs/apk/$buildType")
        if (apkDir.exists()) {
            val apkFiles = apkDir.listFiles { file -> file.extension == "apk" }
            return apkFiles?.firstOrNull()?.absolutePath
        }
        return null
    }
}

sealed class CompilationResult {
    data class Progress(val message: String) : CompilationResult()
    data class Success(val message: String, val output: String = "", val artifactPath: String? = null) : CompilationResult()
    data class Error(val message: String, val output: String = "") : CompilationResult()
}

data class CompilerOptions(
    val verbose: Boolean = false,
    val includeRuntime: Boolean = false,
    val jvmTarget: String = "1.8",
    val optimizationLevel: Int = 1,
    val generateDebugInfo: Boolean = true
)