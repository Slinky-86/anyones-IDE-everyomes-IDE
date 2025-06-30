package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

class RootManager(private val context: Context) {
    
    private var rootProcess: Process? = null
    private var rootOutputStream: DataOutputStream? = null
    
    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("id\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }
    
    suspend fun requestRootAccess(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            rootProcess = Runtime.getRuntime().exec("su")
            rootOutputStream = DataOutputStream(rootProcess!!.outputStream)
            
            // Test root access
            rootOutputStream!!.writeBytes("id\n")
            rootOutputStream!!.flush()
            
            val reader = BufferedReader(InputStreamReader(rootProcess!!.inputStream))
            val response = reader.readLine()
            
            val hasRoot = response?.contains("uid=0") == true
            Result.success(hasRoot)
        } catch (_: Exception) {
            Result.failure(Exception("Failed to get root access"))
        }
    }
    
    suspend fun executeRootCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (rootProcess == null || rootOutputStream == null) {
                val rootResult = requestRootAccess()
                if (rootResult.isFailure || rootResult.getOrNull() != true) {
                    return@withContext Result.failure(Exception("Root access not available"))
                }
            }
            
            rootOutputStream!!.writeBytes("$command\n")
            rootOutputStream!!.flush()
            
            val reader = BufferedReader(InputStreamReader(rootProcess!!.inputStream))
            val output = StringBuilder()
            var line: String?
            
            // Read output with timeout
            var attempts = 0
            while (attempts < 100) { // Max 1 second wait
                if (reader.ready()) {
                    line = reader.readLine()
                    if (line != null) {
                        output.appendLine(line)
                    }
                } else {
                    Thread.sleep(10)
                    attempts++
                }
            }
            
            Result.success(output.toString().trim())
        } catch (_: Exception) {
            Result.failure(Exception("Failed to execute root command"))
        }
    }
    
    suspend fun installApkAsRoot(apkPath: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("pm install -r \"$apkPath\"")
    }
    
    suspend fun uninstallPackageAsRoot(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("pm uninstall $packageName")
    }
    
    suspend fun mountSystemRW(): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("mount -o remount,rw /system")
    }
    
    suspend fun mountSystemRO(): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("mount -o remount,ro /system")
    }
    
    suspend fun copyFileToSystem(sourcePath: String, destPath: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("cp \"$sourcePath\" \"$destPath\"")
    }
    
    suspend fun changeFilePermissions(filePath: String, permissions: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("chmod $permissions \"$filePath\"")
    }
    
    suspend fun changeFileOwner(filePath: String, owner: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("chown $owner \"$filePath\"")
    }
    
    suspend fun createSystemDirectory(dirPath: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("mkdir -p \"$dirPath\"")
    }
    
    suspend fun listSystemFiles(dirPath: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("ls -la \"$dirPath\"")
    }
    
    suspend fun readSystemFile(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("cat \"$filePath\"")
    }
    
    suspend fun writeSystemFile(filePath: String, content: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("echo '$content' > \"$filePath\"")
    }
    
    suspend fun killProcess(processName: String): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("pkill -f $processName")
    }
    
    suspend fun getRunningProcesses(): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("ps -A")
    }
    
    suspend fun getSystemInfo(): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("uname -a && cat /proc/version")
    }
    
    suspend fun getMemoryInfo(): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("cat /proc/meminfo")
    }
    
    suspend fun getCpuInfo(): Result<String> = withContext(Dispatchers.IO) {
        executeRootCommand("cat /proc/cpuinfo")
    }
    
    fun closeRootSession() {
        try {
            rootOutputStream?.writeBytes("exit\n")
            rootOutputStream?.flush()
            rootOutputStream?.close()
            rootProcess?.destroy()
        } catch (_: Exception) {
            // Ignore cleanup errors
        } finally {
            rootProcess = null
            rootOutputStream = null
        }
    }
}