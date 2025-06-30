package com.anyoneide.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager for the native Rust build system
 * This is an experimental feature that uses JNI to call into a Rust library
 */
class RustNativeBuildManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RustNativeBuildManager"
        private val isLibraryLoaded = AtomicBoolean(false)
        
        // Load the native library
        init {
            try {
                System.loadLibrary("rust_native_build")
                isLibraryLoaded.set(true)
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                // Library not loaded - will be handled gracefully
                Log.e(TAG, "Failed to load native library", e)
            } catch (e: Exception) {
                // Other exceptions - will be handled gracefully
                Log.e(TAG, "Exception loading native library", e)
            }
        }
        
        // Native method declarations - these will throw UnsatisfiedLinkError if the library is not loaded
        @JvmStatic external fun nativeBuildProject(projectPath: String, buildType: String): String
        @JvmStatic external fun nativeCleanProject(projectPath: String): String
        @JvmStatic external fun nativeTestProject(projectPath: String, release: Boolean): String
        @JvmStatic external fun nativeCheckRustInstalled(): Boolean
        @JvmStatic external fun nativeGetRustVersion(): String
        @JvmStatic external fun nativeGetBuildSystemStatus(): String
        @JvmStatic external fun nativeIsValidRustProject(projectPath: String): Boolean
        @JvmStatic external fun nativeGetProjectInfo(projectPath: String): String
        @JvmStatic external fun nativeCheckBuildSystemHealth(): String
        @JvmStatic external fun nativeBuildForAndroidTarget(projectPath: String, target: String, release: Boolean): String
        @JvmStatic external fun nativeGenerateAndroidBindings(projectPath: String, packageName: String): String
    }
    
    private val sdkManager = SDKManager(context)
    
    /**
     * Check if the native build system is available
     */
    suspend fun isNativeBuildSystemAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext false
        }
        
        try {
            nativeCheckRustInstalled()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Rust installation", e)
            false
        }
    }
    
    /**
     * Get the Rust version
     */
    suspend fun getRustVersion(): String = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext "Native library not loaded"
        }
        
        try {
            nativeGetRustVersion()
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            "Native library not loaded"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Rust version", e)
            "Error: ${e.message}"
        }
    }
    
    /**
     * Get the build system status
     */
    suspend fun getBuildSystemStatus(): Map<String, Any> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext mapOf(
                "available" to false,
                "error" to "Native library not loaded"
            )
        }
        
        try {
            val statusJson = nativeGetBuildSystemStatus()
            val jsonObject = JSONObject(statusJson)
            val result = mutableMapOf<String, Any>()
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = when {
                    jsonObject.isNull(key) -> "null"
                    else -> jsonObject.get(key)
                }
            }
            
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            mapOf(
                "available" to false,
                "error" to "Native library not loaded"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting build system status", e)
            mapOf(
                "available" to false,
                "error" to e.message.toString()
            )
        }
    }
    
    /**
     * Check if a project is a valid Rust project
     */
    suspend fun isValidRustProject(projectPath: String): Boolean = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext false
        }
        
        try {
            nativeIsValidRustProject(projectPath)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if project is valid", e)
            false
        }
    }
    
    /**
     * Get project information
     */
    suspend fun getProjectInfo(projectPath: String): Map<String, Any> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext mapOf(
                "error" to "Native library not loaded"
            )
        }
        
        try {
            val infoJson = nativeGetProjectInfo(projectPath)
            val jsonObject = JSONObject(infoJson)
            val result = mutableMapOf<String, Any>()
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = when {
                    jsonObject.isNull(key) -> "null"
                    else -> jsonObject.get(key)
                }
            }
            
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            mapOf(
                "error" to "Native library not loaded"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting project info", e)
            mapOf(
                "error" to e.message.toString()
            )
        }
    }
    
    /**
     * Check build system health
     */
    suspend fun checkBuildSystemHealth(): Map<String, Any> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext mapOf(
                "status" to "unhealthy",
                "message" to "Native library not loaded"
            )
        }
        
        try {
            val healthJson = nativeCheckBuildSystemHealth()
            val jsonObject = JSONObject(healthJson)
            val result = mutableMapOf<String, Any>()
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = when {
                    jsonObject.isNull(key) -> "null"
                    key == "checks" && jsonObject.get(key) is JSONObject -> {
                        val checksObj = jsonObject.getJSONArray(key)
                        val checks = mutableListOf<Map<String, Any>>()
                        
                        for (i in 0 until checksObj.length()) {
                            val checkObj = checksObj.getJSONObject(i)
                            val check = mutableMapOf<String, Any>()
                            
                            val checkKeys = checkObj.keys()
                            while (checkKeys.hasNext()) {
                                val checkKey = checkKeys.next()
                                check[checkKey] = checkObj.get(checkKey)
                            }
                            
                            checks.add(check)
                        }
                        
                        checks
                    }
                    else -> jsonObject.get(key)
                }
            }
            
            result
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            mapOf(
                "status" to "unhealthy",
                "message" to "Native library not loaded"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking build system health", e)
            mapOf(
                "status" to "unhealthy",
                "message" to e.message.toString()
            )
        }
    }
    
    /**
     * Build a project using the native Rust build system
     */
    fun buildProject(
        projectPath: String,
        buildType: String = "debug"
    ): Flow<BuildOutputMessage> = flow {
        emit(BuildOutputMessage(BuildOutputType.INFO, "Starting Rust native build...", taskName = "rust-native-build"))
        
        if (!isLibraryLoaded.get()) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: library not loaded"))
            return@flow
        }
        
        try {
            // Check if Rust is installed
            if (!isNativeBuildSystemAvailable()) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Rust native build system not available. Installing Rust..."))
                
                sdkManager.installRust().collect { progress ->
                    when (progress) {
                        is InstallationProgress.Started -> 
                            emit(BuildOutputMessage(BuildOutputType.INFO, progress.message))
                        is InstallationProgress.Downloading -> 
                            emit(BuildOutputMessage(BuildOutputType.INFO, "Downloading Rust: ${progress.progress}%"))
                        is InstallationProgress.Extracting -> 
                            emit(BuildOutputMessage(BuildOutputType.INFO, progress.message))
                        is InstallationProgress.Installing -> 
                            emit(BuildOutputMessage(BuildOutputType.INFO, progress.message))
                        is InstallationProgress.Completed -> 
                            emit(BuildOutputMessage(BuildOutputType.SUCCESS, progress.message))
                        is InstallationProgress.Failed -> 
                            emit(BuildOutputMessage(BuildOutputType.ERROR, progress.message))
                    }
                }
                
                // Check again after installation
                if (!isNativeBuildSystemAvailable()) {
                    emit(BuildOutputMessage(BuildOutputType.ERROR, "Failed to install Rust. Cannot build project."))
                    return@flow
                }
            }
            
            // Validate project
            if (!isValidRustProject(projectPath)) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Not a valid Rust project: $projectPath"))
                return@flow
            }
            
            // Call the native build function
            val resultJson = nativeBuildProject(projectPath, buildType)
            val result = parseBuildResult(resultJson)
            
            // Emit all output messages
            result.outputMessages.forEach { message ->
                val type = when (message.messageType) {
                    "ERROR" -> BuildOutputType.ERROR
                    "WARNING" -> BuildOutputType.WARNING
                    "SUCCESS" -> BuildOutputType.SUCCESS
                    else -> BuildOutputType.INFO
                }
                emit(BuildOutputMessage(type, message.content))
            }
            
            // Emit final status
            if (result.success) {
                emit(BuildOutputMessage(
                    BuildOutputType.SUCCESS, 
                    "Build completed successfully in ${result.durationMs}ms",
                    errors = emptyList(),
                    warnings = emptyList()
                ))
                
                // Emit artifacts
                result.artifacts.forEach { artifactPath ->
                    val file = File(artifactPath)
                    emit(BuildOutputMessage(
                        BuildOutputType.ARTIFACT,
                        "Generated: ${file.name} (${formatFileSize(file.length())})"
                    ))
                }
            } else {
                emit(BuildOutputMessage(
                    BuildOutputType.ERROR,
                    "Build failed",
                    errors = result.outputMessages
                        .filter { it.messageType == "ERROR" }
                        .map { it.content },
                    warnings = result.outputMessages
                        .filter { it.messageType == "WARNING" }
                        .map { it.content }
                ))
            }
            
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error building project", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Build error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clean a project using the native Rust build system
     */
    fun cleanProject(projectPath: String): Flow<BuildOutputMessage> = flow {
        emit(BuildOutputMessage(BuildOutputType.INFO, "Cleaning project with Rust native build system..."))
        
        if (!isLibraryLoaded.get()) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: library not loaded"))
            return@flow
        }
        
        try {
            // Check if Rust is installed
            if (!isNativeBuildSystemAvailable()) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Rust native build system not available"))
                return@flow
            }
            
            // Validate project
            if (!isValidRustProject(projectPath)) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Not a valid Rust project: $projectPath"))
                return@flow
            }
            
            // Call the native clean function
            val resultJson = nativeCleanProject(projectPath)
            val result = parseBuildResult(resultJson)
            
            // Emit all output messages
            result.outputMessages.forEach { message ->
                val type = when (message.messageType) {
                    "ERROR" -> BuildOutputType.ERROR
                    "WARNING" -> BuildOutputType.WARNING
                    "SUCCESS" -> BuildOutputType.SUCCESS
                    else -> BuildOutputType.INFO
                }
                emit(BuildOutputMessage(type, message.content))
            }
            
            // Emit final status
            if (result.success) {
                emit(BuildOutputMessage(
                    BuildOutputType.SUCCESS, 
                    "Project cleaned successfully in ${result.durationMs}ms"
                ))
            } else {
                emit(BuildOutputMessage(
                    BuildOutputType.ERROR,
                    "Clean failed",
                    errors = result.outputMessages
                        .filter { it.messageType == "ERROR" }
                        .map { it.content }
                ))
            }
            
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning project", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Clean error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Test a project using the native Rust build system
     */
    fun testProject(projectPath: String, release: Boolean = false): Flow<BuildOutputMessage> = flow {
        emit(BuildOutputMessage(BuildOutputType.INFO, "Testing project with Rust native build system..."))
        
        if (!isLibraryLoaded.get()) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: library not loaded"))
            return@flow
        }
        
        try {
            // Check if Rust is installed
            if (!isNativeBuildSystemAvailable()) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Rust native build system not available"))
                return@flow
            }
            
            // Validate project
            if (!isValidRustProject(projectPath)) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Not a valid Rust project: $projectPath"))
                return@flow
            }
            
            // Call the native test function
            val resultJson = nativeTestProject(projectPath, release)
            val result = parseBuildResult(resultJson)
            
            // Emit all output messages
            result.outputMessages.forEach { message ->
                val type = when (message.messageType) {
                    "ERROR" -> BuildOutputType.ERROR
                    "WARNING" -> BuildOutputType.WARNING
                    "SUCCESS" -> BuildOutputType.SUCCESS
                    else -> BuildOutputType.INFO
                }
                emit(BuildOutputMessage(type, message.content))
            }
            
            // Emit final status
            if (result.success) {
                emit(BuildOutputMessage(
                    BuildOutputType.SUCCESS, 
                    "Tests completed successfully in ${result.durationMs}ms"
                ))
            } else {
                emit(BuildOutputMessage(
                    BuildOutputType.ERROR,
                    "Tests failed",
                    errors = result.outputMessages
                        .filter { it.messageType == "ERROR" }
                        .map { it.content }
                ))
            }
            
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error testing project", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Test error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Build for a specific Android target
     */
    fun buildForAndroidTarget(
        projectPath: String,
        target: String,
        release: Boolean = false
    ): Flow<BuildOutputMessage> = flow {
        emit(BuildOutputMessage(BuildOutputType.INFO, "Building for Android target: $target"))
        
        if (!isLibraryLoaded.get()) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: library not loaded"))
            return@flow
        }
        
        try {
            // Check if Rust is installed
            if (!isNativeBuildSystemAvailable()) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Rust native build system not available"))
                return@flow
            }
            
            // Validate project
            if (!isValidRustProject(projectPath)) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Not a valid Rust project: $projectPath"))
                return@flow
            }
            
            // Call the native build for Android target function
            val resultJson = nativeBuildForAndroidTarget(projectPath, target, release)
            val result = parseBuildResult(resultJson)
            
            // Emit all output messages
            result.outputMessages.forEach { message ->
                val type = when (message.messageType) {
                    "ERROR" -> BuildOutputType.ERROR
                    "WARNING" -> BuildOutputType.WARNING
                    "SUCCESS" -> BuildOutputType.SUCCESS
                    else -> BuildOutputType.INFO
                }
                emit(BuildOutputMessage(type, message.content))
            }
            
            // Emit final status
            if (result.success) {
                emit(BuildOutputMessage(
                    BuildOutputType.SUCCESS, 
                    "Build for $target completed successfully in ${result.durationMs}ms"
                ))
                
                // Emit artifacts
                result.artifacts.forEach { artifactPath ->
                    val file = File(artifactPath)
                    emit(BuildOutputMessage(
                        BuildOutputType.ARTIFACT,
                        "Generated: ${file.name} (${formatFileSize(file.length())})"
                    ))
                }
            } else {
                emit(BuildOutputMessage(
                    BuildOutputType.ERROR,
                    "Build for $target failed",
                    errors = result.outputMessages
                        .filter { it.messageType == "ERROR" }
                        .map { it.content }
                ))
            }
            
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error building for Android target", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Build error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate Android bindings
     */
    fun generateAndroidBindings(
        projectPath: String,
        packageName: String = "com.example.rustlib"
    ): Flow<BuildOutputMessage> = flow {
        emit(BuildOutputMessage(BuildOutputType.INFO, "Generating Android bindings..."))
        
        if (!isLibraryLoaded.get()) {
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: library not loaded"))
            return@flow
        }
        
        try {
            // Check if Rust is installed
            if (!isNativeBuildSystemAvailable()) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Rust native build system not available"))
                return@flow
            }
            
            // Validate project
            if (!isValidRustProject(projectPath)) {
                emit(BuildOutputMessage(BuildOutputType.ERROR, "Not a valid Rust project: $projectPath"))
                return@flow
            }
            
            // Call the native generate Android bindings function
            val resultJson = nativeGenerateAndroidBindings(projectPath, packageName)
            val result = parseBuildResult(resultJson)
            
            // Emit all output messages
            result.outputMessages.forEach { message ->
                val type = when (message.messageType) {
                    "ERROR" -> BuildOutputType.ERROR
                    "WARNING" -> BuildOutputType.WARNING
                    "SUCCESS" -> BuildOutputType.SUCCESS
                    else -> BuildOutputType.INFO
                }
                emit(BuildOutputMessage(type, message.content))
            }
            
            // Emit final status
            if (result.success) {
                emit(BuildOutputMessage(
                    BuildOutputType.SUCCESS, 
                    "Android bindings generated successfully in ${result.durationMs}ms"
                ))
                
                // Emit artifacts
                result.artifacts.forEach { artifactPath ->
                    val file = File(artifactPath)
                    emit(BuildOutputMessage(
                        BuildOutputType.ARTIFACT,
                        "Generated: ${file.name}"
                    ))
                }
            } else {
                emit(BuildOutputMessage(
                    BuildOutputType.ERROR,
                    "Failed to generate Android bindings",
                    errors = result.outputMessages
                        .filter { it.messageType == "ERROR" }
                        .map { it.content }
                ))
            }
            
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native method not found", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Native build system not available: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Android bindings", e)
            emit(BuildOutputMessage(BuildOutputType.ERROR, "Error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Parse the JSON result from the native build functions
     */
    private fun parseBuildResult(resultJson: String): NativeBuildResult {
        val jsonObject = JSONObject(resultJson)
        
        val success = jsonObject.getBoolean("success")
        val durationMs = jsonObject.getLong("duration_ms")
        
        val outputMessagesArray = jsonObject.getJSONArray("output_messages")
        val outputMessages = mutableListOf<OutputMessage>()
        
        for (i in 0 until outputMessagesArray.length()) {
            val messageObj = outputMessagesArray.getJSONObject(i)
            outputMessages.add(
                OutputMessage(
                    messageType = messageObj.getString("message_type"),
                    content = messageObj.getString("content"),
                    timestamp = messageObj.getLong("timestamp")
                )
            )
        }
        
        val artifactsArray = jsonObject.getJSONArray("artifacts")
        val artifacts = mutableListOf<String>()
        
        for (i in 0 until artifactsArray.length()) {
            artifacts.add(artifactsArray.getString(i))
        }
        
        return NativeBuildResult(
            success = success,
            outputMessages = outputMessages,
            durationMs = durationMs,
            artifacts = artifacts
        )
    }
    
    /**
     * Format file size for display
     */
    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        
        return when {
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.1f KB".format(kb)
            else -> "$bytes bytes"
        }
    }
    
    /**
     * Data class for build result from native code
     */
    data class NativeBuildResult(
        val success: Boolean,
        val outputMessages: List<OutputMessage>,
        val durationMs: Long,
        val artifacts: List<String>
    )
    
    /**
     * Data class for output message from native code
     */
    data class OutputMessage(
        val messageType: String,
        val content: String,
        val timestamp: Long
    )
}