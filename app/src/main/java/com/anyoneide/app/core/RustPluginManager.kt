package com.anyoneide.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager for the native Rust-based plugin system
 * This is an experimental feature that uses JNI to call into a Rust library
 */
class RustPluginManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RustPluginManager"
        private val isLibraryLoaded = AtomicBoolean(false)
        
        // Load the native library
        init {
            try {
                System.loadLibrary("rust_native_build")
                isLibraryLoaded.set(true)
                Log.d(TAG, "Native plugin library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                // Library not loaded - will be handled gracefully
                Log.e(TAG, "Failed to load native plugin library", e)
            } catch (e: Exception) {
                // Other exceptions - will be handled gracefully
                Log.e(TAG, "Exception loading native plugin library", e)
            }
        }
        
        // Native method declarations
        @JvmStatic external fun nativeLoadPlugin(pluginPath: String): String
        @JvmStatic external fun nativeUnloadPlugin(pluginId: String): Boolean
        @JvmStatic external fun nativeGetLoadedPlugins(): String
        @JvmStatic external fun nativeExecutePluginHook(pluginId: String, hookName: String, data: String): String
    }
    
    private val pluginsDir = File(context.filesDir, "plugins")
    
    init {
        pluginsDir.mkdirs()
    }
    
    /**
     * Check if the native plugin system is available
     */
    fun isNativePluginSystemAvailable(): Boolean {
        return isLibraryLoaded.get()
    }
    
    /**
     * Load a plugin
     */
    suspend fun loadPlugin(pluginPath: String): Result<PluginMetadata> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native plugin library not loaded"))
        }
        
        try {
            val metadataJson = nativeLoadPlugin(pluginPath)
            val metadata = parsePluginMetadata(metadataJson)
            Result.success(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading plugin", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unload a plugin
     */
    suspend fun unloadPlugin(pluginId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native plugin library not loaded"))
        }
        
        try {
            val success = nativeUnloadPlugin(pluginId)
            Result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading plugin", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get loaded plugins
     */
    suspend fun getLoadedPlugins(): Result<List<PluginMetadata>> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native plugin library not loaded"))
        }
        
        try {
            val pluginsJson = nativeGetLoadedPlugins()
            val plugins = parsePlugins(pluginsJson)
            Result.success(plugins)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting loaded plugins", e)
            Result.failure(e)
        }
    }
    
    /**
     * Execute plugin hook
     */
    suspend fun executePluginHook(pluginId: String, hookName: String, data: String): Result<PluginHookResult> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native plugin library not loaded"))
        }
        
        try {
            val resultJson = nativeExecutePluginHook(pluginId, hookName, data)
            val result = parsePluginHookResult(resultJson)
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing plugin hook", e)
            Result.failure(e)
        }
    }
    
    /**
     * Install a plugin
     */
    suspend fun installPlugin(pluginUrl: String): Flow<com.anyoneide.app.core.InstallationProgress> = flow {
        emit(com.anyoneide.app.core.InstallationProgress.Started("Installing plugin from $pluginUrl"))
        
        try {
            // Create temporary directory
            val tempDir = File(context.cacheDir, "plugin_install")
            tempDir.mkdirs()
            
            // Download plugin
            emit(com.anyoneide.app.core.InstallationProgress.Downloading("Downloading plugin...", 0))
            
            // TODO: Implement actual download logic
            // For now, we'll assume the plugin is a local file
            val pluginFile = File(pluginUrl)
            if (!pluginFile.exists()) {
                emit(com.anyoneide.app.core.InstallationProgress.Failed("Plugin file not found: $pluginUrl"))
                return@flow
            }
            
            emit(com.anyoneide.app.core.InstallationProgress.Downloading("Plugin downloaded", 100))
            
            // Extract plugin if it's a ZIP file
            if (pluginUrl.endsWith(".zip")) {
                emit(com.anyoneide.app.core.InstallationProgress.Extracting("Extracting plugin..."))
                
                // TODO: Implement extraction logic
                // For now, we'll assume the plugin is already extracted
            }
            
            // Install plugin
            emit(com.anyoneide.app.core.InstallationProgress.Installing("Installing plugin..."))
            
            val pluginPath = if (pluginFile.isDirectory) {
                pluginFile.absolutePath
            } else {
                // Copy plugin file to plugins directory
                val destFile = File(pluginsDir, pluginFile.name)
                pluginFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
            
            // Load plugin
            val result = loadPlugin(pluginPath)
            
            result.fold(
                onSuccess = { metadata ->
                    emit(com.anyoneide.app.core.InstallationProgress.Completed("Plugin ${metadata.name} installed successfully"))
                },
                onFailure = { error ->
                    emit(com.anyoneide.app.core.InstallationProgress.Failed("Failed to install plugin: ${error.message}"))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error installing plugin", e)
            emit(com.anyoneide.app.core.InstallationProgress.Failed("Failed to install plugin: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Uninstall a plugin
     */
    suspend fun uninstallPlugin(pluginId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Unload plugin first
            if (isLibraryLoaded.get()) {
                try {
                    nativeUnloadPlugin(pluginId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unloading plugin", e)
                }
            }
            
            // Delete plugin directory
            val pluginDir = File(pluginsDir, pluginId)
            if (pluginDir.exists() && pluginDir.isDirectory) {
                pluginDir.deleteRecursively()
                return@withContext Result.success(true)
            }
            
            // Delete plugin file
            val pluginFile = File(pluginsDir, "$pluginId.zip")
            if (pluginFile.exists() && pluginFile.isFile) {
                pluginFile.delete()
                return@withContext Result.success(true)
            }
            
            Result.success(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling plugin", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse plugin metadata from JSON
     */
    private fun parsePluginMetadata(json: String): PluginMetadata {
        val jsonObject = JSONObject(json)
        
        val dependencies = mutableListOf<PluginDependency>()
        val dependenciesArray = jsonObject.getJSONArray("dependencies")
        for (i in 0 until dependenciesArray.length()) {
            val dependency = dependenciesArray.getJSONObject(i)
            dependencies.add(
                PluginDependency(
                    id = dependency.getString("id"),
                    minVersion = dependency.getString("min_version"),
                    optional = dependency.getBoolean("optional")
                )
            )
        }
        
        val tags = mutableListOf<String>()
        val tagsArray = jsonObject.getJSONArray("tags")
        for (i in 0 until tagsArray.length()) {
            tags.add(tagsArray.getString(i))
        }
        
        val extensionPoints = mutableListOf<String>()
        val extensionPointsArray = jsonObject.getJSONArray("extension_points")
        for (i in 0 until extensionPointsArray.length()) {
            extensionPoints.add(extensionPointsArray.getString(i))
        }
        
        val screenshots = mutableListOf<String>()
        val screenshotsArray = jsonObject.getJSONArray("screenshots")
        for (i in 0 until screenshotsArray.length()) {
            screenshots.add(screenshotsArray.getString(i))
        }
        
        return PluginMetadata(
            id = jsonObject.getString("id"),
            name = jsonObject.getString("name"),
            description = jsonObject.getString("description"),
            version = jsonObject.getString("version"),
            author = jsonObject.getString("author"),
            category = jsonObject.getString("category"),
            tags = tags,
            extensionPoints = extensionPoints,
            dependencies = dependencies,
            minIdeVersion = jsonObject.getString("min_ide_version"),
            downloadUrl = jsonObject.getString("download_url"),
            iconUrl = if (jsonObject.has("icon_url") && !jsonObject.isNull("icon_url")) 
                jsonObject.getString("icon_url") else null,
            screenshots = screenshots,
            rating = jsonObject.getDouble("rating").toFloat(),
            downloadCount = jsonObject.getLong("download_count")
        )
    }
    
    /**
     * Parse plugins from JSON
     */
    private fun parsePlugins(json: String): List<PluginMetadata> {
        val plugins = mutableListOf<PluginMetadata>()
        
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val pluginJson = jsonArray.getJSONObject(i).toString()
                plugins.add(parsePluginMetadata(pluginJson))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing plugins JSON", e)
        }
        
        return plugins
    }
    
    /**
     * Parse plugin hook result from JSON
     */
    private fun parsePluginHookResult(json: String): PluginHookResult {
        try {
            val result = JSONObject(json)
            return PluginHookResult(
                success = result.getBoolean("success"),
                data = result.getString("data"),
                error = if (result.has("error") && !result.isNull("error")) result.getString("error") else null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing plugin hook result JSON", e)
            return PluginHookResult(
                success = false,
                data = "",
                error = "Failed to parse result: ${e.message}"
            )
        }
    }
    
    /**
     * Plugin metadata
     */
    data class PluginMetadata(
        val id: String,
        val name: String,
        val description: String,
        val version: String,
        val author: String,
        val category: String,
        val tags: List<String>,
        val extensionPoints: List<String>,
        val dependencies: List<PluginDependency>,
        val minIdeVersion: String,
        val downloadUrl: String,
        val iconUrl: String?,
        val screenshots: List<String>,
        val rating: Float,
        val downloadCount: Long
    )
    
    /**
     * Plugin dependency
     */
    data class PluginDependency(
        val id: String,
        val minVersion: String,
        val optional: Boolean
    )
    
    /**
     * Plugin hook result
     */
    data class PluginHookResult(
        val success: Boolean,
        val data: String,
        val error: String?
    )
}