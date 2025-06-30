package com.anyoneide.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Native Rust implementation of terminal commands
 * This is an experimental feature that uses JNI to call into a Rust library
 */
class RustTerminalManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RustTerminalManager"
        private val isLibraryLoaded = AtomicBoolean(false)
        
        // Load the native library
        init {
            try {
                System.loadLibrary("rust_terminal")
                isLibraryLoaded.set(true)
                Log.d(TAG, "Native terminal library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                // Library not loaded - will be handled gracefully
                Log.e(TAG, "Failed to load native terminal library", e)
            } catch (e: Exception) {
                // Other exceptions - will be handled gracefully
                Log.e(TAG, "Exception loading native terminal library", e)
            }
        }
        
        // Native method declarations
        @JvmStatic external fun nativeExecuteCommand(command: String, workingDir: String): String
        @JvmStatic external fun nativeExecuteRootCommand(command: String): String
        @JvmStatic external fun nativeIsRootAvailable(): Boolean
        @JvmStatic external fun nativeGetTerminalInfo(): String
        @JvmStatic external fun nativeCreateSession(workingDir: String): String
        @JvmStatic external fun nativeCloseSession(sessionId: String): Boolean
        @JvmStatic external fun nativeGetEnvironmentVariables(): String
        @JvmStatic external fun nativeSetEnvironmentVariable(name: String, value: String): Boolean
        @JvmStatic external fun nativeGetWorkingDirectory(sessionId: String): String
        @JvmStatic external fun nativeChangeDirectory(sessionId: String, directory: String): Boolean
        @JvmStatic external fun nativeStopCommand(sessionId: String): Boolean
    }
    
    private val sdkManager = SDKManager(context)
    private val shizukuManager = ShizukuManager(context)
    private val fallbackTerminalManager = TerminalManager(context)
    
    /**
     * Check if the native terminal is available
     */
    fun isNativeTerminalAvailable(): Boolean {
        return isLibraryLoaded.get()
    }
    
    /**
     * Create a new terminal session
     */
    suspend fun createSession(workingDirectory: String? = null): Result<String> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext fallbackTerminalManager.createSession(workingDirectory)
        }
        
        try {
            val workDir = workingDirectory ?: context.filesDir.absolutePath
            val sessionId = nativeCreateSession(workDir)
            Result.success(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating native terminal session", e)
            // Fall back to regular terminal manager
            fallbackTerminalManager.createSession(workingDirectory)
        }
    }
    
    /**
     * Execute a command in a terminal session
     */
    suspend fun executeCommand(sessionId: String, command: String): Flow<TerminalOutputInternal> = flow {
        if (!isLibraryLoaded.get()) {
            fallbackTerminalManager.executeCommand(sessionId, command).collect { output ->
                emit(output)
            }
            return@flow
        }
        
        try {
            // Add to history
            emit(TerminalOutputInternal(TerminalOutputType.COMMAND, command))
            
            // Get working directory
            val workingDirectory = try {
                nativeGetWorkingDirectory(sessionId)
            } catch (e: Exception) {
                context.filesDir.absolutePath
            }
            
            // Handle built-in commands
            when {
                command == "clear" -> {
                    emit(TerminalOutputInternal(TerminalOutputType.CLEAR, ""))
                    return@flow
                }
                command.startsWith("cd ") -> {
                    val targetDir = command.substring(3).trim()
                    try {
                        val success = nativeChangeDirectory(sessionId, targetDir)
                        if (success) {
                            val newDir = nativeGetWorkingDirectory(sessionId)
                            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, ""))
                            emit(TerminalOutputInternal(TerminalOutputType.SYSTEM, "Changed directory to: $newDir"))
                        } else {
                            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cd: $targetDir: No such file or directory"))
                        }
                    } catch (e: Exception) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cd: Error changing directory: ${e.message}"))
                    }
                    return@flow
                }
                command.startsWith("su ") || command == "su" -> {
                    // Root command
                    val rootCommand = if (command == "su") "" else command.substring(3)
                    try {
                        val output = nativeExecuteRootCommand(rootCommand)
                        parseCommandOutput(output).forEach { line ->
                            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                        }
                    } catch (e: Exception) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "Root execution error: ${e.message}"))
                    }
                    return@flow
                }
            }
            
            // Execute regular command
            try {
                val output = nativeExecuteCommand(command, workingDirectory)
                parseCommandOutput(output).forEach { line ->
                    if (line.startsWith("ERROR:")) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, line.substring(6)))
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                    }
                }
            } catch (e: Exception) {
                // Fall back to regular terminal execution
                Log.w(TAG, "Falling back to regular terminal execution", e)
                emit(TerminalOutputInternal(TerminalOutputType.SYSTEM, "Native execution failed, falling back to standard execution"))
                
                fallbackTerminalManager.getSession(sessionId)?.let { session ->
                    session.workingDirectory = workingDirectory
                    fallbackTerminalManager.executeCommand(sessionId, command).collect { output ->
                        emit(output)
                    }
                }
            }
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Command execution failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Stop a running command
     */
    suspend fun stopCommand(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext fallbackTerminalManager.stopCommand(sessionId)
        }
        
        try {
            val success = nativeStopCommand(sessionId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to stop command"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping command", e)
            // Fall back to regular terminal manager
            fallbackTerminalManager.stopCommand(sessionId)
        }
    }
    
    /**
     * Close a terminal session
     */
    suspend fun closeSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext fallbackTerminalManager.closeSession(sessionId)
        }
        
        try {
            val success = nativeCloseSession(sessionId)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to close session"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error closing session", e)
            // Fall back to regular terminal manager
            fallbackTerminalManager.closeSession(sessionId)
        }
    }
    
    /**
     * Get terminal information
     */
    suspend fun getTerminalInfo(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.success(mapOf(
                "available" to false,
                "message" to "Native terminal not available"
            ))
        }
        
        try {
            val infoJson = nativeGetTerminalInfo()
            val info = parseJsonToMap(infoJson)
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting terminal info", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get environment variables
     */
    suspend fun getEnvironmentVariables(): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native terminal not available"))
        }
        
        try {
            val envJson = nativeGetEnvironmentVariables()
            val env = parseJsonToMap(envJson).mapValues { it.value.toString() }
            Result.success(env)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting environment variables", e)
            Result.failure(e)
        }
    }
    
    /**
     * Set an environment variable
     */
    suspend fun setEnvironmentVariable(name: String, value: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native terminal not available"))
        }
        
        try {
            val success = nativeSetEnvironmentVariable(name, value)
            Result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting environment variable", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if root is available
     */
    suspend fun isRootAvailable(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Fall back to checking with su command
            return@withContext try {
                val process = ProcessBuilder("su", "-c", "id").start()
                val exitCode = process.waitFor()
                Result.success(exitCode == 0)
            } catch (e: Exception) {
                Result.success(false)
            }
        }
        
        try {
            val isRoot = nativeIsRootAvailable()
            Result.success(isRoot)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking root availability", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse command output into lines
     */
    private fun parseCommandOutput(output: String): List<String> {
        return output.split("\n")
    }
    
    /**
     * Parse JSON to a map
     */
    private fun parseJsonToMap(json: String): Map<String, Any> {
        return try {
            val map = mutableMapOf<String, Any>()
            val jsonObject = org.json.JSONObject(json)
            val keys = jsonObject.keys()
            
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                map[key] = value
            }
            
            map
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON", e)
            mapOf("error" to "Failed to parse JSON: ${e.message}")
        }
    }
}