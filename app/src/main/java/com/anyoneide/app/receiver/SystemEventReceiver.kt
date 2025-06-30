package com.anyoneide.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit

class SystemEventReceiver : BroadcastReceiver() {
    
    private val TAG = "SystemEventReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Handle device boot completion
                // Can be used to restart background services or restore IDE state
                handleBootCompleted(context)
            }
            Intent.ACTION_PACKAGE_ADDED -> {
                // Handle package installation
                val packageName = intent.data?.schemeSpecificPart
                handlePackageAdded(context, packageName)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // Handle package removal
                val packageName = intent.data?.schemeSpecificPart
                handlePackageRemoved(context, packageName)
            }
        }
    }
    
    private fun handleBootCompleted(context: Context) {
        // Initialize any necessary background services
        Log.d(TAG, "Device booted, initializing IDE services")
        
        // Start the background service if needed
        try {
            val serviceIntent = Intent(context, Class.forName("com.anyoneide.app.service.IDEBackgroundService"))
            context.startService(serviceIntent)
            
            // Log the boot completion event
            logEvent(context, "Device booted, IDE services initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background service", e)
        }
        
        // Restore IDE state if needed
        val prefs = context.getSharedPreferences("ide_settings", Context.MODE_PRIVATE)
        val lastProjectPath = prefs.getString("project_last_path", null)
        
        if (lastProjectPath != null) {
            Log.d(TAG, "Last opened project: $lastProjectPath")
            // The app will handle reopening when launched
            logEvent(context, "Last project path detected: $lastProjectPath")
        }
    }
    
    private fun handlePackageAdded(context: Context, packageName: String?) {
        // Update package information in IDE
        Log.d(TAG, "Package installed: $packageName")
        
        if (packageName != null) {
            // Check if it's a development-related package
            val devPackages = listOf(
                "com.android.tools", 
                "org.jetbrains", 
                "com.google.android", 
                "com.google.firebase"
            )
            
            val isDevPackage = devPackages.any { packageName.startsWith(it) }
            
            if (isDevPackage) {
                Log.d(TAG, "Development package installed: $packageName")
                // Could notify the user or update IDE capabilities
                logEvent(context, "Development package installed: $packageName")
                
                // Update IDE capabilities based on the installed package
                updateIDECapabilities(context, packageName)
            }
        }
    }
    
    private fun handlePackageRemoved(context: Context, packageName: String?) {
        // Update package information in IDE
        Log.d(TAG, "Package uninstalled: $packageName")
        
        if (packageName != null) {
            // Check for broken dependencies
            val devPackages = listOf(
                "com.android.tools", 
                "org.jetbrains", 
                "com.google.android", 
                "com.google.firebase"
            )
            
            val isDevPackage = devPackages.any { packageName.startsWith(it) }
            
            if (isDevPackage) {
                Log.d(TAG, "Development package removed: $packageName")
                // Could notify the user about potential issues
                logEvent(context, "Development package removed: $packageName")
                
                // Check for broken dependencies
                checkBrokenDependencies(context, packageName)
            }
        }
    }
    
    private fun updateIDECapabilities(context: Context, packageName: String) {
        // Update IDE capabilities based on the installed package
        // For example, enable features that depend on specific packages
        when {
            packageName.startsWith("com.android.tools") -> {
                // Android SDK tools installed
                val prefs = context.getSharedPreferences("ide_capabilities", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("android_sdk_available", true).apply()
            }
            packageName.startsWith("org.jetbrains.kotlin") -> {
                // Kotlin tools installed
                val prefs = context.getSharedPreferences("ide_capabilities", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("kotlin_available", true).apply()
            }
            packageName.startsWith("com.google.firebase") -> {
                // Firebase tools installed
                val prefs = context.getSharedPreferences("ide_capabilities", Context.MODE_PRIVATE)
                prefs.edit { putBoolean("firebase_available", true) }
            }
        }
    }
    
    private fun checkBrokenDependencies(context: Context, packageName: String) {
        // Check for broken dependencies when a package is removed
        // For example, disable features that depend on specific packages
        when {
            packageName.startsWith("com.android.tools") -> {
                // Android SDK tools removed
                val prefs = context.getSharedPreferences("ide_capabilities", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("android_sdk_available", false).apply()
                
                // Log warning
                logEvent(context, "WARNING: Android SDK tools removed, some features may not work")
            }
            packageName.startsWith("org.jetbrains.kotlin") -> {
                // Kotlin tools removed
                val prefs = context.getSharedPreferences("ide_capabilities", Context.MODE_PRIVATE)
                prefs.edit { putBoolean("kotlin_available", false) }
                
                // Log warning
                logEvent(context, "WARNING: Kotlin tools removed, some features may not work")
            }
            packageName.startsWith("com.google.firebase") -> {
                // Firebase tools removed
                val prefs = context.getSharedPreferences("ide_capabilities", Context.MODE_PRIVATE)
                prefs.edit { putBoolean("firebase_available", false) }
                
                // Log warning
                logEvent(context, "WARNING: Firebase tools removed, some features may not work")
            }
        }
    }
    
    private fun logEvent(context: Context, message: String) {
        // Log event to app's event log
        try {
            val prefs = context.getSharedPreferences("ide_event_log", Context.MODE_PRIVATE)
            val events = prefs.getStringSet("events", mutableSetOf()) ?: mutableSetOf()
            val timestamp = System.currentTimeMillis()
            val event = "$timestamp: $message"
            
            val updatedEvents = events.toMutableSet()
            updatedEvents.add(event)
            
            // Limit the number of events to prevent the set from growing too large
            if (updatedEvents.size > 100) {
                val sortedEvents = updatedEvents.sortedByDescending { it.substringBefore(":").toLongOrNull() ?: 0 }
                updatedEvents.clear()
                updatedEvents.addAll(sortedEvents.take(100))
            }
            
            prefs.edit { putStringSet("events", updatedEvents) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event", e)
        }
    }
}