package com.anyoneide.app.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager class for Shizuku integration
 * Shizuku allows executing commands with elevated privileges without root
 */
class ShizukuManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ShizukuManager"
        private const val PERMISSION_REQUEST_CODE = 1001
    }
    
    private val isShizukuAvailable = AtomicBoolean(false)
    private val isShizukuPermissionGranted = AtomicBoolean(false)
    
    // Listener for Shizuku permission changes
    private val permissionListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            if (requestCode == PERMISSION_REQUEST_CODE) {
                isShizukuPermissionGranted.set(grantResult == PackageManager.PERMISSION_GRANTED)
                Log.d(TAG, "Shizuku permission granted: ${isShizukuPermissionGranted.get()}")
            }
        }
    }
    
    // Listener for Shizuku service connection changes
    private val serviceConnectionListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            isShizukuAvailable.set(true)
            checkPermission()
            Log.d(TAG, "Shizuku service connected")
        }
    }
    
    private val serviceDeadListener = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() {
            isShizukuAvailable.set(false)
            isShizukuPermissionGranted.set(false)
            Log.d(TAG, "Shizuku service disconnected")
        }
    }
    
    init {
        // Register listeners
        Shizuku.addBinderReceivedListener(serviceConnectionListener)
        Shizuku.addBinderDeadListener(serviceDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        
        // Check if Shizuku is available
        isShizukuAvailable.set(Shizuku.pingBinder())
        
        // Check permission if Shizuku is available
        if (isShizukuAvailable.get()) {
            checkPermission()
        }
    }
    
    /**
     * Check if Shizuku is installed and running
     */
    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(ShizukuProvider.MANAGER_APPLICATION_ID, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Check if Shizuku is available (running and connected)
     */
    fun isShizukuAvailable(): Boolean {
        return isShizukuAvailable.get()
    }
    
    /**
     * Check if Shizuku permission is granted
     */
    fun isShizukuPermissionGranted(): Boolean {
        return isShizukuPermissionGranted.get()
    }
    
    /**
     * Request Shizuku permission
     * @return true if permission is already granted or successfully requested
     */
    fun requestPermission(): Boolean {
        if (!isShizukuAvailable.get()) {
            Log.d(TAG, "Shizuku is not available")
            return false
        }
        
        return try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                isShizukuPermissionGranted.set(true)
                true
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                // User denied permission before, show rationale
                Log.d(TAG, "Should show permission rationale")
                false
            } else {
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
                // Result will be delivered to the listener
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
            false
        }
    }
    
    /**
     * Check if Shizuku permission is granted
     */
    private fun checkPermission() {
        try {
            isShizukuPermissionGranted.set(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Shizuku permission", e)
            isShizukuPermissionGranted.set(false)
        }
    }
    
    /**
     * Execute a command using Shizuku
     * @param command The command to execute
     * @param workingDirectory The working directory for the command
     * @return A flow of terminal output
     */
    suspend fun executeShizukuCommand(
        command: String,
        workingDirectory: String? = null
    ): Flow<TerminalOutputInternal> = flow {
        if (!isShizukuAvailable.get()) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Shizuku is not available"))
            return@flow
        }
        
        if (!isShizukuPermissionGranted.get()) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Shizuku permission not granted"))
            return@flow
        }
        
        try {
            emit(TerminalOutputInternal(TerminalOutputType.COMMAND, "$ $command"))
            
            // Get the shell service using Shizuku
            val shellService = getShellService()
            
            // Prepare the command
            val commandArgs = command.split("\\s+".toRegex()).toTypedArray()
            
            // Set up the process
            val processBuilder = ProcessBuilder(*commandArgs)
            if (workingDirectory != null) {
                processBuilder.directory(java.io.File(workingDirectory))
            }
            
            // Execute the command
            val process = processBuilder.start()
            
            // Read the output
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read stdout
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                }
            }
            
            // Read stderr
            while (errorReader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, currentLine))
                }
            }
            
            // Wait for the process to complete
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Command exited with code $exitCode"))
            }
            
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Error executing command: ${e.message}"))
            Log.e(TAG, "Error executing Shizuku command", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Execute a root command using Shizuku
     * @param command The command to execute
     * @return A flow of terminal output
     */
    suspend fun executeRootCommand(command: String): Flow<TerminalOutputInternal> = flow {
        if (!isShizukuAvailable.get()) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Shizuku is not available"))
            return@flow
        }
        
        if (!isShizukuPermissionGranted.get()) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Shizuku permission not granted"))
            return@flow
        }
        
        try {
            emit(TerminalOutputInternal(TerminalOutputType.COMMAND, "# $command"))
            
            // Get the shell service using Shizuku
            val shellService = getShellService()
            
            // Execute the command with su
            val processBuilder = ProcessBuilder("su", "-c", command)
            val process = processBuilder.start()
            
            // Read the output
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read stdout
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                }
            }
            
            // Read stderr
            while (errorReader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, currentLine))
                }
            }
            
            // Wait for the process to complete
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Command exited with code $exitCode"))
            }
            
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Error executing root command: ${e.message}"))
            Log.e(TAG, "Error executing Shizuku root command", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get the shell service using Shizuku
     */
    private fun getShellService(): IBinder {
        return SystemServiceHelper.getSystemService("shell")
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        Shizuku.removeBinderReceivedListener(serviceConnectionListener)
        Shizuku.removeBinderDeadListener(serviceDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
    }
}