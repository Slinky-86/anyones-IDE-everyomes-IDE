package com.anyoneide.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.anyoneide.app.core.TerminalSetup
import com.anyoneide.app.core.SetupProgress
import com.anyoneide.app.ui.screens.EnhancedMainScreen
import com.anyoneide.app.ui.theme.AnyoneIDETheme
import com.anyoneide.app.viewmodel.EnhancedMainViewModel
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: EnhancedMainViewModel
    private var permissionsGranted = false
    
    // Permission request launchers
    private val requestManageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Handle manage storage permission result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                viewModel.logActivity("Permission granted: MANAGE_EXTERNAL_STORAGE")
                checkSystemAlertWindowPermission()
            } else {
                viewModel.logActivity("Permission denied: MANAGE_EXTERNAL_STORAGE")
                // Continue with limited functionality
                checkSystemAlertWindowPermission()
            }
        }
        checkManageStoragePermission()
    }
    
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle multiple permissions result
        val grantedPermissions = permissions.filter { it.value }
        val deniedPermissions = permissions.filter { !it.value }
        
        viewModel.logActivity("Permissions granted", grantedPermissions.keys.associateWith { "true" })
        if (deniedPermissions.isNotEmpty()) {
            viewModel.logActivity("Permissions denied", deniedPermissions.keys.associateWith { "false" })
        }
        
        handlePermissionResults(permissions)
    }
    
    @SuppressLint("ObsoleteSdkInt")
    private val requestSystemAlertWindowLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Handle system alert window permission result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                viewModel.logActivity("Permission granted: SYSTEM_ALERT_WINDOW")
            } else {
                viewModel.logActivity("Permission denied: SYSTEM_ALERT_WINDOW")
            }
        }
        checkSystemAlertWindowPermission()
    }
    
    @SuppressLint("ObsoleteSdkInt")
    private val requestWriteSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Handle write settings permission result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.System.canWrite(this)) {
                viewModel.logActivity("Permission granted: WRITE_SETTINGS")
            } else {
                viewModel.logActivity("Permission denied: WRITE_SETTINGS")
            }
        }
        checkWriteSettingsPermission()
    }
    
    @SuppressLint("ObsoleteSdkInt")
    private val requestInstallPackagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Handle install packages permission result
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.canRequestPackageInstalls()) {
                viewModel.logActivity("Permission granted: REQUEST_INSTALL_PACKAGES")
            } else {
                viewModel.logActivity("Permission denied: REQUEST_INSTALL_PACKAGES")
            }
        }
        checkInstallPackagesPermission()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize ViewModel
        val application = application as AnyoneIDEApplication
        viewModel = application.mainViewModel
        
        // Create necessary directories for persistent storage
        createAppDirectories()
        
        // Check and request permissions
        checkAndRequestPermissions()
    }
    
    private fun createAppDirectories() {
        try {
            // Internal storage directories
            val projectsDir = getExternalFilesDir("projects")
            val sdkDir = getExternalFilesDir("sdk")
            val tempDir = getExternalFilesDir("temp")
            val pluginsDir = getExternalFilesDir("plugins")
            val themesDir = getExternalFilesDir("themes")
            val templatesDir = getExternalFilesDir("templates")
            val terminalLogsDir = getExternalFilesDir("terminal_logs")
            
            projectsDir?.mkdirs()
            sdkDir?.mkdirs()
            tempDir?.mkdirs()
            pluginsDir?.mkdirs()
            themesDir?.mkdirs()
            templatesDir?.mkdirs()
            terminalLogsDir?.mkdirs()
            
            // External storage directories (if permission granted)
            if (hasStoragePermission()) {
                val externalProjectsDir =
                    File(Environment.getExternalStorageDirectory(), "AnyoneIDE/Projects")
                val externalSdkDir = File(Environment.getExternalStorageDirectory(), "AnyoneIDE/SDK")
                val externalTempDir = File(Environment.getExternalStorageDirectory(), "AnyoneIDE/Temp")
                val externalTerminalLogsDir = File(Environment.getExternalStorageDirectory(), "AnyoneIDE/TerminalLogs")
                
                externalProjectsDir.mkdirs()
                externalSdkDir.mkdirs()
                externalTempDir.mkdirs()
                externalTerminalLogsDir.mkdirs()
            }
        } catch (e: Exception) {
            // Log error but continue - app can work with internal storage only
            e.printStackTrace()
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Check standard runtime permissions
        val runtimePermissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.VIBRATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE
        )
        
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Check which permissions are not granted
        for (permission in runtimePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        
        // Request runtime permissions first
        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // If runtime permissions are granted, check special permissions
            checkSpecialPermissions()
        }
    }
    
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys
        
        if (deniedPermissions.isNotEmpty()) {
            // Some permissions were denied - show explanation or continue with limited functionality
            // For now, we'll continue but log the denied permissions
            println("Denied permissions: $deniedPermissions")
        }
        
        // Check special permissions regardless of runtime permission results
        checkSpecialPermissions()
    }
    
    @SuppressLint("ObsoleteSdkInt")
    private fun checkSpecialPermissions() {
        // Check MANAGE_EXTERNAL_STORAGE for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission()
                return
            }
        }
        
        // Check SYSTEM_ALERT_WINDOW permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                requestSystemAlertWindowPermission()
                return
            }
        }
        
        // Check WRITE_SETTINGS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                requestWriteSettingsPermission()
                return
            }
        }
        
        // Check REQUEST_INSTALL_PACKAGES permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                requestInstallPackagesPermission()
                return
            }
        }
        
        // All permissions checked - initialize app
        initializeApp()
    }
    
    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                requestManageStorageLauncher.launch(intent)
            } catch (_: Exception) {
                // Fallback to general storage settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                requestManageStorageLauncher.launch(intent)
            }
        } else {
            checkSpecialPermissions()
        }
    }
    
    private fun checkManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                // Permission granted, continue with other permissions
                checkSpecialPermissions()
            } else {
                // Permission denied, continue anyway but with limited functionality
                checkSpecialPermissions()
            }
        }
    }
    
    @SuppressLint("ObsoleteSdkInt")
    private fun requestSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = "package:$packageName".toUri()
                requestSystemAlertWindowLauncher.launch(intent)
            } catch (_: Exception) {
                // Continue without this permission
                checkSpecialPermissions()
            }
        } else {
            checkSpecialPermissions()
        }
    }
    
    private fun checkSystemAlertWindowPermission() {
        // Continue with next permission check
        checkSpecialPermissions()
    }
    
    @SuppressLint("ObsoleteSdkInt")
    private fun requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = "package:$packageName".toUri()
                requestWriteSettingsLauncher.launch(intent)
            } catch (_: Exception) {
                // Continue without this permission
                checkSpecialPermissions()
            }
        } else {
            checkSpecialPermissions()
        }
    }
    
    private fun checkWriteSettingsPermission() {
        // Continue with next permission check
        checkSpecialPermissions()
    }
    
    @SuppressLint("ObsoleteSdkInt")
    private fun requestInstallPackagesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = "package:$packageName".toUri()
                requestInstallPackagesLauncher.launch(intent)
            } catch (_: Exception) {
                // Continue without this permission
                checkSpecialPermissions()
            }
        } else {
            checkSpecialPermissions()
        }
    }
    
    private fun checkInstallPackagesPermission() {
        // All permissions checked - initialize app
        initializeApp()
    }
    
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun initializeApp() {
        permissionsGranted = true
        
        // Initialize terminal environment
        lifecycleScope.launch {
            val terminalSetup = TerminalSetup(this@MainActivity)
            terminalSetup.initializeTerminalEnvironment().collect { progress ->
                when (progress) {
                    is SetupProgress.Started -> 
                        viewModel.logActivity("Terminal setup: ${progress.message}")
                    is SetupProgress.Downloading -> 
                        viewModel.logActivity("Terminal setup: ${progress.message} (${progress.progress}%)")
                    is SetupProgress.Extracting -> 
                        viewModel.logActivity("Terminal setup: ${progress.message}")
                    is SetupProgress.Installing -> 
                        viewModel.logActivity("Terminal setup: ${progress.message}")
                    is SetupProgress.Completed -> 
                        viewModel.logActivity("Terminal setup: ${progress.message}")
                    is SetupProgress.Failed -> 
                        viewModel.logActivity("Terminal setup failed: ${progress.message}")
                    is SetupProgress.Warning ->
                        viewModel.logActivity("Terminal setup warning: ${progress.message}")
                }
            }
        }
        
        // Initialize Shizuku if enabled in settings
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.settings.enableShizukuIntegration) {
                    val shizukuManager = (application as AnyoneIDEApplication).shizukuManager
                    if (shizukuManager.isShizukuAvailable() && !shizukuManager.isShizukuPermissionGranted()) {
                        shizukuManager.requestPermission()
                    }
                }
            }
        }
        
        // Initialize root access if enabled in settings
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.settings.enableRootFeatures && !viewModel.isRootAccessRequested) {
                    viewModel.requestRootAccess()
                }
            }
        }
        
        // Set up the UI
        setContent {
            AnyoneIDETheme(darkTheme = viewModel.uiState.collectAsState().value.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EnhancedMainScreen(viewModel = viewModel)
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Ensure all files are saved when app is paused
        if (::viewModel.isInitialized) {
            viewModel.autoSaveOnExit()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Ensure all files are saved when app is destroyed
        if (::viewModel.isInitialized) {
            viewModel.autoSaveOnExit()
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        
        // Handle file opening from external apps
        intent?.let { handleFileIntent(it) }
    }
    
    private fun handleFileIntent(intent: Intent) {
        if (!::viewModel.isInitialized) return
        
        when (intent.action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> {
                intent.data?.let { uri ->
                    lifecycleScope.launch {
                        try {
                            // Get file path from URI
                            val filePath = getFilePathFromUri(uri)
                            if (filePath != null) {
                                viewModel.openFile(filePath)
                            } else {
                                // If we can't get a direct file path, try to read the content
                                val content = readContentFromUri(uri)
                                if (content != null) {
                                    // Create a temporary file
                                    val fileName = getFileNameFromUri(uri) ?: "temp.txt"
                                    val tempFile = File(filesDir, fileName)
                                    tempFile.writeText(content)
                                    viewModel.openFile(tempFile.absolutePath)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    
    private fun getFilePathFromUri(uri: Uri): String? {
        // Try to get file path from URI
        return when (uri.scheme) {
            "file" -> uri.path
            "content" -> {
                // Try to get file path from content URI
                val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
                val cursor = contentResolver.query(uri, projection, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val columnIndex = it.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)
                        return it.getString(columnIndex)
                    }
                }
                null
            }
            else -> null
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        // Try to get file name from URI
        return when (uri.scheme) {
            "file" -> File(uri.path ?: "").name
            "content" -> {
                // Try to get file name from content URI
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            return it.getString(displayNameIndex)
                        }
                    }
                }
                uri.lastPathSegment
            }
            else -> uri.lastPathSegment
        }
    }
    
    private fun readContentFromUri(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}