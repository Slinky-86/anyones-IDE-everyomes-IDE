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
 * Manager for the native Rust-based SDK management
 * This is an experimental feature that uses JNI to call into a Rust library
 */
class RustSdkManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RustSdkManager"
        private val isLibraryLoaded = AtomicBoolean(false)
        
        // Load the native library
        init {
            try {
                System.loadLibrary("rust_native_build")
                isLibraryLoaded.set(true)
                Log.d(TAG, "Native SDK manager library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                // Library not loaded - will be handled gracefully
                Log.e(TAG, "Failed to load native SDK manager library", e)
            } catch (e: Exception) {
                // Other exceptions - will be handled gracefully
                Log.e(TAG, "Exception loading native SDK manager library", e)
            }
        }
        
        // Native method declarations
        @JvmStatic external fun nativeGetSdkManagerStatus(sdkRoot: String): String
        @JvmStatic external fun nativeInstallSdkComponent(sdkRoot: String, componentId: String): String
        @JvmStatic external fun nativeUninstallSdkComponent(sdkRoot: String, componentId: String): Boolean
        @JvmStatic external fun nativeExecuteSdkCommand(sdkRoot: String, commandJson: String, workingDir: String): String
    }
    
    private val sdkRoot = File(context.filesDir, "sdk")
    private val fallbackSdkManager = SDKManager(context)
    
    init {
        // Create SDK root directory if it doesn't exist
        sdkRoot.mkdirs()
    }
    
    /**
     * Check if the native SDK manager is available
     */
    fun isNativeSdkManagerAvailable(): Boolean {
        return isLibraryLoaded.get()
    }
    
    /**
     * Get SDK status
     */
    suspend fun getSdkStatus(): SdkManagerStatus = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            val status = fallbackSdkManager.checkSDKStatus()
            return@withContext SdkManagerStatus(
                androidSdkInstalled = status.androidSDKInstalled,
                jdkInstalled = status.jdkInstalled,
                kotlinInstalled = status.kotlinInstalled,
                gradleInstalled = status.gradleInstalled,
                ndkInstalled = status.ndkInstalled,
                rustInstalled = status.rustInstalled,
                androidSdkPath = if (status.androidSDKInstalled) fallbackSdkManager.getAndroidSDKPath() else null,
                jdkPath = if (status.jdkInstalled) fallbackSdkManager.getJavaHomePath() else null,
                kotlinPath = if (status.kotlinInstalled) fallbackSdkManager.getKotlinCompilerPath() else null,
                gradlePath = if (status.gradleInstalled) fallbackSdkManager.getGradlePath() else null,
                ndkPath = if (status.ndkInstalled) "Not available" else null,
                rustPath = if (status.rustInstalled) fallbackSdkManager.getRustCargoPath() else null,
                availableComponents = emptyList(),
                installedComponents = emptyList()
            )
        }
        
        try {
            val statusJson = nativeGetSdkManagerStatus(sdkRoot.absolutePath)
            parseSdkManagerStatus(statusJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SDK status", e)
            // Fall back to Java implementation
            val status = fallbackSdkManager.checkSDKStatus()
            SdkManagerStatus(
                androidSdkInstalled = status.androidSDKInstalled,
                jdkInstalled = status.jdkInstalled,
                kotlinInstalled = status.kotlinInstalled,
                gradleInstalled = status.gradleInstalled,
                ndkInstalled = status.ndkInstalled,
                rustInstalled = status.rustInstalled,
                androidSdkPath = if (status.androidSDKInstalled) fallbackSdkManager.getAndroidSDKPath() else null,
                jdkPath = if (status.jdkInstalled) fallbackSdkManager.getJavaHomePath() else null,
                kotlinPath = if (status.kotlinInstalled) fallbackSdkManager.getKotlinCompilerPath() else null,
                gradlePath = if (status.gradleInstalled) fallbackSdkManager.getGradlePath() else null,
                ndkPath = if (status.ndkInstalled) "Not available" else null,
                rustPath = if (status.rustInstalled) fallbackSdkManager.getRustCargoPath() else null,
                availableComponents = emptyList(),
                installedComponents = emptyList()
            )
        }
    }
    
    /**
     * Install SDK component
     */
    fun installSdkComponent(componentId: String): Flow<com.anyoneide.app.core.InstallationProgress> = flow {
        emit(com.anyoneide.app.core.InstallationProgress.Started("Installing component: $componentId"))
        
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            when (componentId) {
                "android-sdk" -> {
                    fallbackSdkManager.installAndroidSDK().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "jdk-17" -> {
                    fallbackSdkManager.installJDK("17").collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "kotlin" -> {
                    fallbackSdkManager.installKotlinCompiler().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "gradle" -> {
                    fallbackSdkManager.installGradle().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "ndk" -> {
                    fallbackSdkManager.installNDK().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "rust" -> {
                    fallbackSdkManager.installRust().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                else -> {
                    emit(com.anyoneide.app.core.InstallationProgress.Failed("Component not supported by fallback implementation: $componentId"))
                }
            }
            return@flow
        }
        
        try {
            val progressJson = nativeInstallSdkComponent(sdkRoot.absolutePath, componentId)
            val progressArray = JSONArray(progressJson)
            
            for (i in 0 until progressArray.length()) {
                val progressObj = progressArray.getJSONObject(i)
                val progress = parseInstallationProgress(progressObj)
                emit(progress)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error installing SDK component", e)
            emit(com.anyoneide.app.core.InstallationProgress.Failed("Failed to install component: ${e.message}"))
            
            // Fall back to Java implementation
            when (componentId) {
                "android-sdk" -> {
                    fallbackSdkManager.installAndroidSDK().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "jdk-17" -> {
                    fallbackSdkManager.installJDK("17").collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "kotlin" -> {
                    fallbackSdkManager.installKotlinCompiler().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "gradle" -> {
                    fallbackSdkManager.installGradle().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "ndk" -> {
                    fallbackSdkManager.installNDK().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                "rust" -> {
                    fallbackSdkManager.installRust().collect { progress ->
                        emit(convertProgress(progress))
                    }
                }
                else -> {
                    emit(com.anyoneide.app.core.InstallationProgress.Failed("Component not supported by fallback implementation: $componentId"))
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Uninstall SDK component
     */
    suspend fun uninstallSdkComponent(componentId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            return@withContext Result.failure(Exception("Native SDK manager not available"))
        }
        
        try {
            val success = nativeUninstallSdkComponent(sdkRoot.absolutePath, componentId)
            Result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling SDK component", e)
            Result.failure(e)
        }
    }
    
    /**
     * Execute SDK command
     */
    suspend fun executeSdkCommand(command: List<String>, workingDir: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            return@withContext fallbackSdkManager.executeCommand(command, File(workingDir))
        }
        
        try {
            val commandJson = JSONArray(command).toString()
            val resultJson = nativeExecuteSdkCommand(sdkRoot.absolutePath, commandJson, workingDir)
            val resultObj = JSONObject(resultJson)
            
            if (resultObj.optBoolean("success", false)) {
                Result.success(resultObj.optString("output", ""))
            } else {
                Result.failure(Exception(resultObj.optString("error", "Unknown error")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing SDK command", e)
            // Fall back to Java implementation
            fallbackSdkManager.executeCommand(command, File(workingDir))
        }
    }
    
    /**
     * Get available components
     */
    suspend fun getAvailableComponents(): List<SdkComponent> = withContext(Dispatchers.IO) {
        val status = getSdkStatus()
        status.availableComponents
    }
    
    /**
     * Get installed components
     */
    suspend fun getInstalledComponents(): List<SdkComponent> = withContext(Dispatchers.IO) {
        val status = getSdkStatus()
        status.installedComponents
    }
    
    /**
     * Parse SDK manager status from JSON
     */
    private fun parseSdkManagerStatus(json: String): SdkManagerStatus {
        val obj = JSONObject(json)
        
        val availableComponents = mutableListOf<SdkComponent>()
        val installedComponents = mutableListOf<SdkComponent>()
        
        val availableComponentsArray = obj.optJSONArray("available_components") ?: JSONArray()
        for (i in 0 until availableComponentsArray.length()) {
            val componentObj = availableComponentsArray.getJSONObject(i)
            val component = parseSdkComponent(componentObj)
            availableComponents.add(component)
        }
        
        val installedComponentsArray = obj.optJSONArray("installed_components") ?: JSONArray()
        for (i in 0 until installedComponentsArray.length()) {
            val componentObj = installedComponentsArray.getJSONObject(i)
            val component = parseSdkComponent(componentObj)
            installedComponents.add(component)
        }
        
        return SdkManagerStatus(
            androidSdkInstalled = obj.optBoolean("android_sdk_installed", false),
            jdkInstalled = obj.optBoolean("jdk_installed", false),
            kotlinInstalled = obj.optBoolean("kotlin_installed", false),
            gradleInstalled = obj.optBoolean("gradle_installed", false),
            ndkInstalled = obj.optBoolean("ndk_installed", false),
            rustInstalled = obj.optBoolean("rust_installed", false),
            androidSdkPath = obj.optString("android_sdk_path", null),
            jdkPath = obj.optString("jdk_path", null),
            kotlinPath = obj.optString("kotlin_path", null),
            gradlePath = obj.optString("gradle_path", null),
            ndkPath = obj.optString("ndk_path", null),
            rustPath = obj.optString("rust_path", null),
            availableComponents = availableComponents,
            installedComponents = installedComponents
        )
    }
    
    /**
     * Parse SDK component from JSON
     */
    private fun parseSdkComponent(json: JSONObject): SdkComponent {
        val dependencies = mutableListOf<String>()
        val dependenciesArray = json.optJSONArray("dependencies") ?: JSONArray()
        for (i in 0 until dependenciesArray.length()) {
            dependencies.add(dependenciesArray.getString(i))
        }
        
        return SdkComponent(
            id = json.getString("id"),
            name = json.getString("name"),
            version = json.getString("version"),
            componentType = parseComponentType(json.getString("component_type")),
            path = json.getString("path"),
            installed = json.getBoolean("installed"),
            sizeMb = json.getDouble("size_mb"),
            description = json.getString("description"),
            dependencies = dependencies
        )
    }
    
    /**
     * Parse component type from string
     */
    private fun parseComponentType(type: String): SdkComponentType {
        return when (type) {
            "AndroidSdk" -> SdkComponentType.AndroidSdk
            "BuildTools" -> SdkComponentType.BuildTools
            "PlatformTools" -> SdkComponentType.PlatformTools
            "Platform" -> SdkComponentType.Platform
            "SystemImages" -> SdkComponentType.SystemImages
            "Emulator" -> SdkComponentType.Emulator
            "Jdk" -> SdkComponentType.Jdk
            "Kotlin" -> SdkComponentType.Kotlin
            "Gradle" -> SdkComponentType.Gradle
            "Ndk" -> SdkComponentType.Ndk
            "Cmake" -> SdkComponentType.Cmake
            "Rust" -> SdkComponentType.Rust
            "Cargo" -> SdkComponentType.Cargo
            else -> {
                if (type.startsWith("Other(")) {
                    val otherType = type.substring(6, type.length - 1)
                    SdkComponentType.Other(otherType)
                } else {
                    SdkComponentType.Other(type)
                }
            }
        }
    }
    
    /**
     * Parse installation progress from JSON
     */
    private fun parseInstallationProgress(json: JSONObject): com.anyoneide.app.core.InstallationProgress {
        return when (json.getString("type")) {
            "Started" -> com.anyoneide.app.core.InstallationProgress.Started(json.getString("message"))
            "Downloading" -> com.anyoneide.app.core.InstallationProgress.Downloading(
                json.getString("progress"),
                json.getLong("total_size")
            )
            "Extracting" -> com.anyoneide.app.core.InstallationProgress.Extracting(json.getString("message"))
            "Installing" -> com.anyoneide.app.core.InstallationProgress.Installing(json.getString("message"))
            "Completed" -> com.anyoneide.app.core.InstallationProgress.Completed(json.getString("message"))
            "Failed" -> com.anyoneide.app.core.InstallationProgress.Failed(
                json.getString("message")
            )
            else -> com.anyoneide.app.core.InstallationProgress.Failed("Unknown progress type: ${json.getString("type")}")
        }
    }
    
    /**
     * Convert from Java InstallationProgress to Kotlin InstallationProgress
     */
    private fun convertProgress(progress: InstallationProgress): com.anyoneide.app.core.InstallationProgress {
        return when (progress) {
            is InstallationProgress.Started -> 
                com.anyoneide.app.core.InstallationProgress.Started(progress.message)
            is InstallationProgress.Downloading -> 
                com.anyoneide.app.core.InstallationProgress.Downloading(progress.progress.toString(), 100_000_000)
            is InstallationProgress.Extracting -> 
                com.anyoneide.app.core.InstallationProgress.Extracting(progress.message)
            is InstallationProgress.Installing -> 
                com.anyoneide.app.core.InstallationProgress.Installing(progress.message)
            is InstallationProgress.Completed -> 
                com.anyoneide.app.core.InstallationProgress.Completed(progress.message)
            is InstallationProgress.Failed -> 
                com.anyoneide.app.core.InstallationProgress.Failed(progress.message)
        }
    }
}

/**
 * SDK Manager status
 */
data class SdkManagerStatus(
    val androidSdkInstalled: Boolean,
    val jdkInstalled: Boolean,
    val kotlinInstalled: Boolean,
    val gradleInstalled: Boolean,
    val ndkInstalled: Boolean,
    val rustInstalled: Boolean,
    val androidSdkPath: String?,
    val jdkPath: String?,
    val kotlinPath: String?,
    val gradlePath: String?,
    val ndkPath: String?,
    val rustPath: String?,
    val availableComponents: List<SdkComponent>,
    val installedComponents: List<SdkComponent>
)

/**
 * SDK Component
 */
data class SdkComponent(
    val id: String,
    val name: String,
    val version: String,
    val componentType: SdkComponentType,
    val path: String,
    val installed: Boolean,
    val sizeMb: Double,
    val description: String,
    val dependencies: List<String>
)

/**
 * SDK Component type
 */
sealed class SdkComponentType {
    object AndroidSdk : SdkComponentType()
    object BuildTools : SdkComponentType()
    object PlatformTools : SdkComponentType()
    object Platform : SdkComponentType()
    object SystemImages : SdkComponentType()
    object Emulator : SdkComponentType()
    object Jdk : SdkComponentType()
    object Kotlin : SdkComponentType()
    object Gradle : SdkComponentType()
    object Ndk : SdkComponentType()
    object Cmake : SdkComponentType()
    object Rust : SdkComponentType()
    object Cargo : SdkComponentType()
    data class Other(val type: String) : SdkComponentType()
}