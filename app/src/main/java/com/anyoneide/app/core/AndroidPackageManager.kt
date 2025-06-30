package com.anyoneide.app.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class AndroidPackageManager(context: Context) {
    
    private val packageManager = context.packageManager
    
    suspend fun installApk(apkPath: String): Flow<String> = flow {
        try {
            emit("Starting APK installation: $apkPath")
            
            // Use Android's package manager to install
            val processBuilder = ProcessBuilder("pm", "install", "-r", "-t", "-g", apkPath)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read installation output
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
            
            // Read any errors
            while (errorReader.readLine().also { line = it } != null) {
                emit("ERROR: $line")
            }
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                emit("APK installation completed successfully")
            } else {
                emit("APK installation failed with exit code: $exitCode")
            }
            
        } catch (_: Exception) {
            emit("Installation error")
        }
    }
    
    suspend fun uninstallPackage(packageName: String): Flow<String> = flow {
        try {
            emit("Uninstalling package: $packageName")
            
            val processBuilder = ProcessBuilder("pm", "uninstall", packageName)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                emit("Package uninstalled successfully")
            } else {
                emit("Uninstall failed with exit code: $exitCode")
            }
            
        } catch (_: Exception) {
            emit("Uninstall error")
        }
    }
    
    @SuppressLint("ObsoleteSdkInt")
    suspend fun listInstalledPackages(filter: String = ""): List<PackageInfo> = withContext(Dispatchers.IO) {
        try {
            val packages = mutableListOf<PackageInfo>()
            
            // Get installed packages using Android API
            val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            
            for (packageInfo in installedPackages) {
                val appInfo = packageInfo.applicationInfo
                val packageName = packageInfo.packageName
                val versionName = packageInfo.versionName ?: "Unknown"
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                
                if (filter.isEmpty() || packageName.contains(filter, ignoreCase = true) || 
                    appName.contains(filter, ignoreCase = true)) {
                    
                    // Use proper version code handling for different API levels
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }
                    
                    packages.add(PackageInfo(
                        packageName = packageName,
                        appName = appName,
                        versionName = versionName,
                        versionCode = versionCode,
                        isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                        apkPath = appInfo.sourceDir,
                        dataDir = appInfo.dataDir,
                        targetSdk = appInfo.targetSdkVersion,
                        minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            appInfo.minSdkVersion
                        } else {
                            0
                        }
                    ))
                }
            }
            
            packages.sortedBy { it.appName }
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    @SuppressLint("ObsoleteSdkInt")
    suspend fun getPackageInfo(packageName: String): PackageInfo? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val appInfo = packageInfo.applicationInfo
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            
            // Use proper version code handling for different API levels
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            PackageInfo(
                packageName = packageName,
                appName = appName,
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = versionCode,
                isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                apkPath = appInfo.sourceDir,
                dataDir = appInfo.dataDir,
                targetSdk = appInfo.targetSdkVersion,
                minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo.minSdkVersion
                } else {
                    0
                }
            )
        } catch (_: Exception) {
            null
        }
    }
    
    suspend fun enablePackage(packageName: String): Flow<String> = flow {
        try {
            emit("Enabling package: $packageName")
            
            val processBuilder = ProcessBuilder("pm", "enable", packageName)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
            
            process.waitFor()
            
        } catch (_: Exception) {
            emit("Enable error")
        }
    }
    
    suspend fun disablePackage(packageName: String): Flow<String> = flow {
        try {
            emit("Disabling package: $packageName")
            
            val processBuilder = ProcessBuilder("pm", "disable-user", packageName)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
            
            process.waitFor()
            
        } catch (_: Exception) {
            emit("Disable error")
        }
    }
    
    suspend fun clearPackageData(packageName: String): Flow<String> = flow {
        try {
            emit("Clearing data for package: $packageName")
            
            val processBuilder = ProcessBuilder("pm", "clear", packageName)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
            
            process.waitFor()
            
        } catch (_: Exception) {
            emit("Clear data error")
        }
    }
    
    suspend fun grantPermission(packageName: String, permission: String): Flow<String> = flow {
        try {
            emit("Granting permission $permission to $packageName")
            
            val processBuilder = ProcessBuilder("pm", "grant", packageName, permission)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
            
            process.waitFor()
            
        } catch (_: Exception) {
            emit("Grant permission error")
        }
    }
    
    suspend fun revokePermission(packageName: String, permission: String): Flow<String> = flow {
        try {
            emit("Revoking permission $permission from $packageName")
            
            val processBuilder = ProcessBuilder("pm", "revoke", packageName, permission)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                emit(line!!)
            }
            
            process.waitFor()
            
        } catch (_: Exception) {
            emit("Revoke permission error")
        }
    }
}

data class PackageInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val apkPath: String,
    val dataDir: String,
    val targetSdk: Int,
    val minSdk: Int
)