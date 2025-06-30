package com.anyoneide.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anyoneide.app.core.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for SDK Manager functionality
 */
class SdkManagerViewModel(context: Context) : ViewModel() {
    
    private val rustSdkManager = RustSdkManager(context)
    private val fallbackSdkManager = SDKManager(context)
    
    private val _sdkStatus = MutableStateFlow<SdkManagerStatus?>(null)
    val sdkStatus: StateFlow<SdkManagerStatus?> = _sdkStatus.asStateFlow()
    
    private val _availableComponents = MutableStateFlow<List<SdkComponent>>(emptyList())
    val availableComponents: StateFlow<List<SdkComponent>> = _availableComponents.asStateFlow()
    
    private val _installedComponents = MutableStateFlow<List<SdkComponent>>(emptyList())
    val installedComponents: StateFlow<List<SdkComponent>> = _installedComponents.asStateFlow()
    
    private val _installationProgress = MutableStateFlow<Map<String, InstallationProgress>>(emptyMap())
    val installationProgress: StateFlow<Map<String, InstallationProgress>> = _installationProgress.asStateFlow()
    
    private val _isNativeSdkManagerAvailable = MutableStateFlow(false)
    val isNativeSdkManagerAvailable: StateFlow<Boolean> = _isNativeSdkManagerAvailable.asStateFlow()
    
    init {
        _isNativeSdkManagerAvailable.value = rustSdkManager.isNativeSdkManagerAvailable()
        refreshSdkStatus()
    }
    
    /**
     * Refresh SDK status
     */
    fun refreshSdkStatus() {
        viewModelScope.launch {
            try {
                val status = rustSdkManager.getSdkStatus()
                _sdkStatus.value = status
                _availableComponents.value = status.availableComponents
                _installedComponents.value = status.installedComponents
            } catch (e: Exception) {
                // Fall back to basic status
                fallbackSdkManager.checkSDKStatus().let { status ->
                    _sdkStatus.value = SdkManagerStatus(
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
        }
    }
    
    /**
     * Install SDK component
     */
    fun installComponent(componentId: String) {
        viewModelScope.launch {
            // Clear previous progress for this component
            updateProgress(componentId, null)
            
            rustSdkManager.installSdkComponent(componentId).collect { progress ->
                updateProgress(componentId, progress)
                
                // If installation completed or failed, refresh SDK status
                if (progress is InstallationProgress.Completed || progress is InstallationProgress.Failed) {
                    refreshSdkStatus()
                    
                    // Remove progress after a delay
                    kotlinx.coroutines.delay(3000)
                    updateProgress(componentId, null)
                }
            }
        }
    }
    
    /**
     * Uninstall SDK component
     */
    fun uninstallComponent(componentId: String) {
        viewModelScope.launch {
            try {
                rustSdkManager.uninstallSdkComponent(componentId)
                refreshSdkStatus()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Execute SDK command
     */
    fun executeSdkCommand(command: List<String>, workingDir: String): Flow<String> = flow {
        try {
            val result = rustSdkManager.executeSdkCommand(command, workingDir)
            result.fold(
                onSuccess = { output ->
                    emit(output)
                },
                onFailure = { error ->
                    emit("Error: ${error.message}")
                }
            )
        } catch (e: Exception) {
            emit("Error: ${e.message}")
        }
    }
    
    /**
     * Update installation progress
     */
    private fun updateProgress(componentId: String, progress: InstallationProgress?) {
        val currentProgress = _installationProgress.value.toMutableMap()
        if (progress == null) {
            currentProgress.remove(componentId)
        } else {
            currentProgress[componentId] = progress
        }
        _installationProgress.value = currentProgress
    }
    
    /**
     * Install Android SDK
     */
    fun installAndroidSdk(version: String = "34") {
        installComponent("android-sdk")
    }
    
    /**
     * Install JDK
     */
    fun installJdk(version: String = "17") {
        installComponent("jdk-$version")
    }
    
    /**
     * Install Kotlin
     */
    fun installKotlin(version: String = "1.9.20") {
        installComponent("kotlin")
    }
    
    /**
     * Install Gradle
     */
    fun installGradle(version: String = "8.4") {
        installComponent("gradle")
    }
    
    /**
     * Install NDK
     */
    fun installNdk(version: String = "25.2.9519653") {
        installComponent("ndk;$version")
    }
    
    /**
     * Install Rust
     */
    fun installRust(channel: String = "stable") {
        installComponent("rust")
    }
}