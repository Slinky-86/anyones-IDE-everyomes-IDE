package com.anyoneide.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager for the native Rust-based extension system
 * This is an experimental feature that uses JNI to call into a Rust library
 */
class RustExtensionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RustExtensionManager"
        private val isLibraryLoaded = AtomicBoolean(false)
        
        // Load the native library
        init {
            try {
                System.loadLibrary("rust_native_build")
                isLibraryLoaded.set(true)
                Log.d(TAG, "Native extension library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                // Library not loaded - will be handled gracefully
                Log.e(TAG, "Failed to load native extension library", e)
            } catch (e: Exception) {
                // Other exceptions - will be handled gracefully
                Log.e(TAG, "Exception loading native extension library", e)
            }
        }
        
        // Native method declarations
        @JvmStatic external fun nativeRegisterExtension(extensionId: String, extensionType: String, extensionData: String): Boolean
        @JvmStatic external fun nativeUnregisterExtension(extensionId: String): Boolean
        @JvmStatic external fun nativeGetRegisteredExtensions(): String
        @JvmStatic external fun nativeExecuteExtension(extensionId: String, data: String): String
    }
    
    /**
     * Check if the native extension system is available
     */
    fun isNativeExtensionSystemAvailable(): Boolean {
        return isLibraryLoaded.get()
    }
    
    /**
     * Register an extension
     */
    suspend fun registerExtension(
        extensionId: String,
        extensionType: String,
        extensionData: ExtensionMetadata
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native extension library not loaded"))
        }
        
        try {
            val extensionDataJson = JSONObject().apply {
                put("id", extensionData.id)
                put("name", extensionData.name)
                put("description", extensionData.description)
                put("version", extensionData.version)
                put("author", extensionData.author)
                put("extension_type", extensionData.extensionType.name)
                put("supported_languages", extensionData.supportedLanguages)
                put("dependencies", extensionData.dependencies)
                put("configuration", JSONObject(extensionData.configuration))
            }.toString()
            
            val success = nativeRegisterExtension(extensionId, extensionType, extensionDataJson)
            Result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering extension", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unregister an extension
     */
    suspend fun unregisterExtension(extensionId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native extension library not loaded"))
        }
        
        try {
            val success = nativeUnregisterExtension(extensionId)
            Result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering extension", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get registered extensions
     */
    suspend fun getRegisteredExtensions(): Result<List<ExtensionMetadata>> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native extension library not loaded"))
        }
        
        try {
            val extensionsJson = nativeGetRegisteredExtensions()
            val extensions = parseExtensions(extensionsJson)
            Result.success(extensions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting registered extensions", e)
            Result.failure(e)
        }
    }
    
    /**
     * Execute an extension
     */
    suspend fun executeExtension(extensionId: String, data: String): Result<ExtensionResult> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native extension library not loaded"))
        }
        
        try {
            val resultJson = nativeExecuteExtension(extensionId, data)
            val result = parseExtensionResult(resultJson)
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing extension", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse extensions from JSON
     */
    private fun parseExtensions(json: String): List<ExtensionMetadata> {
        val extensions = mutableListOf<ExtensionMetadata>()
        
        try {
            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val extension = jsonArray.getJSONObject(i)
                
                val extensionType = try {
                    ExtensionType.valueOf(extension.getString("extension_type"))
                } catch (e: Exception) {
                    ExtensionType.CUSTOM
                }
                
                val supportedLanguages = mutableListOf<String>()
                val languagesArray = extension.getJSONArray("supported_languages")
                for (j in 0 until languagesArray.length()) {
                    supportedLanguages.add(languagesArray.getString(j))
                }
                
                val dependencies = mutableListOf<String>()
                val dependenciesArray = extension.getJSONArray("dependencies")
                for (j in 0 until dependenciesArray.length()) {
                    dependencies.add(dependenciesArray.getString(j))
                }
                
                val configuration = mutableMapOf<String, String>()
                val configObject = extension.getJSONObject("configuration")
                val keys = configObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    configuration[key] = configObject.getString(key)
                }
                
                extensions.add(
                    ExtensionMetadata(
                        id = extension.getString("id"),
                        name = extension.getString("name"),
                        description = extension.getString("description"),
                        version = extension.getString("version"),
                        author = extension.getString("author"),
                        extensionType = extensionType,
                        supportedLanguages = supportedLanguages,
                        dependencies = dependencies,
                        configuration = configuration
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing extensions JSON", e)
        }
        
        return extensions
    }
    
    /**
     * Parse extension result from JSON
     */
    private fun parseExtensionResult(json: String): ExtensionResult {
        try {
            val result = JSONObject(json)
            return ExtensionResult(
                success = result.getBoolean("success"),
                data = result.getString("data"),
                error = if (result.has("error") && !result.isNull("error")) result.getString("error") else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing extension result JSON", e)
            return ExtensionResult(
                success = false,
                data = "",
                error = "Failed to parse result: ${e.message}"
            )
        }
    }
    
    /**
     * Extension metadata
     */
    data class ExtensionMetadata(
        val id: String,
        val name: String,
        val description: String,
        val version: String,
        val author: String,
        val extensionType: ExtensionType,
        val supportedLanguages: List<String>,
        val dependencies: List<String>,
        val configuration: Map<String, String>
    )
    
    /**
     * Extension type
     */
    enum class ExtensionType {
        LANGUAGE_SUPPORT,
        THEME,
        FORMATTER,
        LINTER,
        DEBUGGER,
        BUILD_SYSTEM,
        VCS_PROVIDER,
        TERMINAL_ENHANCEMENT,
        EDITOR_ENHANCEMENT,
        SNIPPET,
        CUSTOM
    }
    
    /**
     * Extension result
     */
    data class ExtensionResult(
        val success: Boolean,
        val data: String,
        val error: String?
    )
}