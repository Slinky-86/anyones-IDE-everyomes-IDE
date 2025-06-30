package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.content.ComponentName
import android.app.ActivityManager
import androidx.annotation.RequiresPermission

class TerminalManager(private val context: Context) {
    
    private val sessions = ConcurrentHashMap<String, TerminalSessionInternal>()
    private val processes = ConcurrentHashMap<String, Process>()
    private val shizukuManager = ShizukuManager(context)
    
    suspend fun createSession(workingDirectory: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sessionId = UUID.randomUUID().toString()
            val workDir = workingDirectory ?: context.filesDir.absolutePath
            
            val session = TerminalSessionInternal(
                id = sessionId,
                workingDirectory = workDir,
                environment = getDefaultEnvironment(),
                isActive = true,
                history = mutableListOf(),
                currentProcess = null,
                isRunningLongCommand = false
            )
            
            sessions[sessionId] = session
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to create terminal session", e))
        }
    }
    
    suspend fun executeCommand(sessionId: String, command: String): Flow<TerminalOutputInternal> = flow {
        val session = sessions[sessionId]
        if (session == null) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Session not found: $sessionId"))
            return@flow
        }
        
        // Add to history
        session.history.add(command)
        
        // Check if we need to stop a running command
        if (session.isRunningLongCommand && session.currentProcess != null) {
            stopCommand(sessionId)
            emit(TerminalOutputInternal(TerminalOutputType.SYSTEM, "Previous command terminated"))
        }
        
        try {
            // Check if this is a long-running command
            val isLongRunningCommand = isLongRunningCommand(command)
            session.isRunningLongCommand = isLongRunningCommand
            
            // Check if we should use Shizuku
            if (shizukuManager.isShizukuAvailable() && shizukuManager.isShizukuPermissionGranted()) {
                // Execute with Shizuku
                shizukuManager.executeShizukuCommand(command, session.workingDirectory).collect { output ->
                    emit(output)
                }
            } else {
                // Execute with standard shell
                executeRealCommand(command, session).forEach { output ->
                    emit(output)
                }
            }
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Command execution failed: ${e.message}"))
        }
    }
    
    suspend fun stopCommand(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = sessions[sessionId]
            if (session == null) {
                return@withContext Result.failure(Exception("Session not found: $sessionId"))
            }
            
            val process = session.currentProcess
            if (process != null) {
                process.destroyForcibly()
                session.currentProcess = null
                session.isRunningLongCommand = false
                Result.success(Unit)
            } else {
                Result.failure(Exception("No running process to stop"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to stop command", e))
        }
    }
    
    private suspend fun executeRealCommand(command: String, session: TerminalSessionInternal): List<TerminalOutputInternal> = withContext(Dispatchers.IO) {
        val outputs = mutableListOf<TerminalOutputInternal>()
        val parts = command.trim().split("\\s+".toRegex())
        
        if (parts.isEmpty()) return@withContext outputs
        
        when (parts[0]) {
            "cd" -> {
                val newDir = if (parts.size > 1) {
                    if (parts[1].startsWith("/")) {
                        parts[1]
                    } else {
                        File(session.workingDirectory, parts[1]).absolutePath
                    }
                } else {
                    session.environment["HOME"] ?: context.filesDir.absolutePath
                }
                
                val targetDir = File(newDir)
                if (targetDir.exists() && targetDir.isDirectory) {
                    session.workingDirectory = targetDir.absolutePath
                    outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, ""))
                } else {
                    outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "cd: $newDir: No such file or directory"))
                }
            }
            
            "clear" -> {
                outputs.add(TerminalOutputInternal(TerminalOutputType.CLEAR, ""))
            }
            
            "pm" -> {
                // Use Android's package manager
                executeAndroidPM(parts.drop(1), outputs)
            }
            
            "am" -> {
                // Use Android's activity manager
                executeAndroidAM(parts.drop(1), outputs)
            }
            
            "dumpsys" -> {
                // Use Android's dumpsys
                executeAndroidDumpsys(parts.drop(1), outputs)
            }
            
            "getprop", "setprop" -> {
                // Android system properties
                executeAndroidProps(parts, outputs)
            }
            
            "su" -> {
                // Handle root commands
                if (parts.size > 1) {
                    executeRootCommand(parts.drop(1).joinToString(" "), outputs)
                } else {
                    outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "Root shell not available in this context"))
                }
            }
            
            else -> {
                // Execute as regular shell command
                executeShellCommand(command, session, outputs)
            }
        }
        
        outputs
    }
    
    private suspend fun executeShellCommand(command: String, session: TerminalSessionInternal, outputs: MutableList<TerminalOutputInternal>) = withContext(Dispatchers.IO) {
        try {
            // Check if there's a running process and kill it
            if (session.currentProcess != null) {
                session.currentProcess?.destroyForcibly()
                session.currentProcess = null
            }
            
            val processBuilder = ProcessBuilder("sh", "-c", command)
            processBuilder.directory(File(session.workingDirectory))
            
            // Set environment variables
            val env = processBuilder.environment()
            session.environment.forEach { (key, value) ->
                env[key] = value
            }
            
            val process = processBuilder.start()
            session.currentProcess = process
            processes[session.id] = process
            
            // Read stdout
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read stdout in separate thread
            val stdoutLines = mutableListOf<String>()
            val stderrLines = mutableListOf<String>()
            
            val stdoutThread = Thread {
                try {
                    var line: String?
                    while (stdoutReader.readLine().also { line = it } != null) {
                        val currentLine = line
                        if (currentLine != null) {
                            stdoutLines.add(currentLine)
                        }
                    }
                } catch (e: Exception) {
                    // Handle reading errors
                }
            }
            
            val stderrThread = Thread {
                try {
                    var line: String?
                    while (stderrReader.readLine().also { line = it } != null) {
                        val currentLine = line
                        if (currentLine != null) {
                            stderrLines.add(currentLine)
                        }
                    }
                } catch (e: Exception) {
                    // Handle reading errors
                }
            }
            
            stdoutThread.start()
            stderrThread.start()
            
            // Wait for process to complete with timeout
            val isLongRunning = isLongRunningCommand(command)
            val timeout = if (isLongRunning) Long.MAX_VALUE else 30L
            
            val finished = process.waitFor(timeout, TimeUnit.SECONDS)
            
            if (!finished && !isLongRunning) {
                process.destroyForcibly()
                outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "Command timed out"))
                return@withContext
            }
            
            // Wait for reading threads to complete
            stdoutThread.join(1000)
            stderrThread.join(1000)
            
            // Add outputs
            stdoutLines.forEach { line ->
                outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
            }
            
            stderrLines.forEach { line ->
                outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, line))
            }
            
            val exitCode = if (finished) process.exitValue() else -1
            if (exitCode != 0 && stdoutLines.isEmpty() && stderrLines.isEmpty()) {
                outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "Command failed with exit code: $exitCode"))
            }
            
            // Clean up if not a long-running command
            if (!isLongRunning || finished) {
                session.currentProcess = null
                processes.remove(session.id)
            }
            
        } catch (e: Exception) {
            outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "Shell execution error: ${e.message}"))
        }
    }
    
    private suspend fun executeAndroidPM(args: List<String>, outputs: MutableList<TerminalOutputInternal>) = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "Package manager (pm) commands:"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  list packages [-f] [-d] [-e] [-s] [-3] [-i] [-u] [FILTER]"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  install [-r] [-t] [-d] [-g] [PATH]"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  uninstall [-k] [PACKAGE]"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  path PACKAGE"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  dump PACKAGE"))
            return@withContext
        }
        
        try {
            when (args[0]) {
                "install" -> {
                    if (args.size > 1) {
                        val apkPath = args.last()
                        installApk(apkPath).collect { output ->
                            outputs.add(output)
                        }
                    } else {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "pm install: missing APK path"))
                    }
                }
                "uninstall" -> {
                    if (args.size > 1) {
                        val packageName = args.last()
                        uninstallPackage(packageName).collect { output ->
                            outputs.add(output)
                        }
                    } else {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "pm uninstall: missing package name"))
                    }
                }
                "list" -> {
                    if (args.size > 1 && args[1] == "packages") {
                        listInstalledPackages().collect { output ->
                            outputs.add(output)
                        }
                    } else {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "pm list: unknown command"))
                    }
                }
                "path" -> {
                    if (args.size > 1) {
                        val packageName = args[1]
                        try {
                            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "package:${packageInfo.applicationInfo.sourceDir}"))
                        } catch (e: Exception) {
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "Package not found: $packageName"))
                        }
                    } else {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "pm path: missing package name"))
                    }
                }
                "dump" -> {
                    if (args.size > 1) {
                        val packageName = args[1]
                        try {
                            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                            val dump = "Package: ${packageInfo.packageName}\n" +
                                    "Version: ${packageInfo.versionName} (${packageInfo.versionCode})\n" +
                                    "Install location: ${packageInfo.installLocation}\n" +
                                    "First install time: ${packageInfo.firstInstallTime}\n" +
                                    "Last update time: ${packageInfo.lastUpdateTime}\n" +
                                    "APK path: ${packageInfo.applicationInfo.sourceDir}"
                            
                            dump.lines().forEach { line ->
                                outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                            }
                        } catch (e: Exception) {
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "Package not found: $packageName"))
                        }
                    } else {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "pm dump: missing package name"))
                    }
                }
                else -> {
                    // Execute generic pm command
                    val command = listOf("pm") + args
                    val processBuilder = ProcessBuilder(command)
                    val process = processBuilder.start()
                    
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                    
                    // Read output
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line
                        if (currentLine != null) {
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                        }
                    }
                    
                    // Read errors
                    while (errorReader.readLine().also { line = it } != null) {
                        val currentLine = line
                        if (currentLine != null) {
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, currentLine))
                        }
                    }
                    
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "pm command failed with exit code: $exitCode"))
                    }
                }
            }
        } catch (e: Exception) {
            outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "pm execution error: ${e.message}"))
        }
    }
    
    @RequiresPermission(android.Manifest.permission.KILL_BACKGROUND_PROCESSES)
    private suspend fun executeAndroidAM(args: List<String>, outputs: MutableList<TerminalOutputInternal>): Unit = withContext(Dispatchers.IO) {
        if (args.isEmpty()) {
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "Activity manager (am) commands:"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  start [-D] [-W] [-P <FILE>] [--start-profiler <FILE>]"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  startservice [--user <USER_ID>] <INTENT>"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  force-stop [--user <USER_ID>] <PACKAGE>"))
            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "  kill [--user <USER_ID>] <PACKAGE>"))
            return@withContext
        }
        
        try {
            when (args[0]) {
                "start" -> {
                    if (args.size > 1) {
                        val componentName = args.last()
                        try {
                            val intent = Intent()
                            intent.setComponent(ComponentName.unflattenFromString(componentName))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "Activity started: $componentName"))
                        } catch (e: Exception) {
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "Failed to start activity: ${e.message}"))
                        }
                    } else {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "am start: missing component name"))
                    }
                }
                "startservice" -> {
                    if (args.size > 1) {
                        val componentName = args.last()
                        try {
                            val intent = Intent()
                            intent.setComponent(ComponentName.unflattenFromString(componentName))
                            context.startService(intent)
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "Service started: $componentName"))
                        } catch (e: Exception) {
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "Failed to start service: ${e.message}"))
                        }
                    } else {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "am startservice: missing component name"))
                    }
                }
                "force-stop", "kill" -> {
                    if (args.size > 1) {
                        val packageName = args.last()
                        try {
                            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                            am.killBackgroundProcesses(packageName)
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "${args[0]}: $packageName"))
                        } catch (e: Exception) {
                            outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "Failed to ${args[0]}: ${e.message}"))
                        }
                    } else {
                        outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "am ${args[0]}: missing package name"))
                    }
                }
                else -> {
                    outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, "Unknown am command: ${args[0]}"))
                }
            }
        } catch (e: Exception) {
            outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "am execution error: ${e.message}"))
        }
    }
    
    private suspend fun executeAndroidDumpsys(args: List<String>, outputs: MutableList<TerminalOutputInternal>) = withContext(Dispatchers.IO) {
        try {
            val command = listOf("dumpsys") + args
            val processBuilder = ProcessBuilder(command)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var lineCount = 0
            
            while (reader.readLine().also { line = it } != null && lineCount < 50) { // Limit output
                val currentLine = line
                if (currentLine != null) {
                    outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                    lineCount++
                }
            }
            
            if (lineCount >= 50) {
                outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, "... (output truncated, use 'dumpsys ${args.joinToString(" ")} | head -n 100' for more)"))
            }
            
            process.destroyForcibly() // Kill process to prevent hanging
            
        } catch (e: Exception) {
            outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "dumpsys execution error: ${e.message}"))
        }
    }
    
    private suspend fun executeAndroidProps(parts: List<String>, outputs: MutableList<TerminalOutputInternal>) = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder(parts)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                }
            }
            
            process.waitFor()
            
        } catch (e: Exception) {
            outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "${parts[0]} execution error: ${e.message}"))
        }
    }
    
    private suspend fun executeRootCommand(command: String, outputs: MutableList<TerminalOutputInternal>) = withContext(Dispatchers.IO) {
        // Try to use Shizuku first if available
        if (shizukuManager.isShizukuAvailable() && shizukuManager.isShizukuPermissionGranted()) {
            shizukuManager.executeRootCommand(command).collect { output ->
                outputs.add(output)
            }
            return@withContext
        }
        
        // Fall back to traditional su command
        try {
            val processBuilder = ProcessBuilder("su", "-c", command)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                }
            }
            
            while (errorReader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    outputs.add(TerminalOutputInternal(TerminalOutputType.STDERR, currentLine))
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "Root command failed with exit code: $exitCode"))
            } else {
                // Add an empty success message if needed
                outputs.add(TerminalOutputInternal(TerminalOutputType.STDOUT, ""))
            }
            
        } catch (e: Exception) {
            outputs.add(TerminalOutputInternal(TerminalOutputType.ERROR, "Root execution error. Root access may not be available: ${e.message}"))
        }
    }
    
    suspend fun installApk(apkPath: String): Flow<TerminalOutputInternal> = flow {
        try {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Installing APK: $apkPath"))
            
            val processBuilder = ProcessBuilder("pm", "install", "-r", "-t", apkPath)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                }
            }
            
            while (errorReader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, currentLine))
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "APK installation completed successfully"))
            } else {
                emit(TerminalOutputInternal(TerminalOutputType.ERROR, "APK installation failed with exit code: $exitCode"))
            }
            
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "APK installation error: ${e.message}"))
        }
    }
    
    suspend fun uninstallPackage(packageName: String): Flow<TerminalOutputInternal> = flow {
        try {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Uninstalling package: $packageName"))
            
            val processBuilder = ProcessBuilder("pm", "uninstall", packageName)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                }
            }
            
            while (errorReader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, currentLine))
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Package uninstalled successfully"))
            } else {
                emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Package uninstall failed with exit code: $exitCode"))
            }
            
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Package uninstall error: ${e.message}"))
        }
    }
    
    suspend fun listInstalledPackages(): Flow<TerminalOutputInternal> = flow {
        try {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Listing installed packages..."))
            
            val processBuilder = ProcessBuilder("pm", "list", "packages")
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                }
            }
            
            while (errorReader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, currentLine))
                }
            }
            
            process.waitFor()
            
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Package listing error: ${e.message}"))
        }
    }
    
    suspend fun saveTerminalOutput(sessionId: String, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val session = sessions[sessionId] ?: return@withContext Result.failure(Exception("Session not found"))
            
            // Create output directory if it doesn't exist
            val outputDir = File(context.getExternalFilesDir(null), "terminal_logs")
            outputDir.mkdirs()
            
            // Create output file
            val outputFile = File(outputDir, fileName)
            
            // Get terminal output
            val terminalOutput = getSessionOutput(sessionId)
            
            // Write to file
            outputFile.writeText(terminalOutput)
            
            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save terminal output", e))
        }
    }
    
    suspend fun getSessionOutput(sessionId: String): String = withContext(Dispatchers.IO) {
        val session = sessions[sessionId] ?: return@withContext ""
        
        // Get all terminal output for the session
        val output = StringBuilder()
        
        // Add command history and output
        session.history.forEach { command ->
            output.appendLine("$ $command")
            // In a real implementation, you would also include the output for each command
        }
        
        output.toString()
    }
    
    suspend fun closeSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val process = processes.remove(sessionId)
            process?.destroyForcibly()
            
            val session = sessions.remove(sessionId)
            if (session != null) {
                session.isActive = false
                session.currentProcess?.destroyForcibly()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Session not found: $sessionId"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to close session", e))
        }
    }
    
    fun getSession(sessionId: String): TerminalSessionInternal? {
        return sessions[sessionId]
    }
    
    fun getAllSessions(): List<TerminalSessionInternal> {
        return sessions.values.toList()
    }
    
    /**
     * Check if a command is likely to be long-running
     */
    private fun isLongRunningCommand(command: String): Boolean {
        val longRunningCommands = listOf(
            "top", "htop", "watch", "tail -f", "logcat",
            "ping", "traceroute", "tcpdump", "netstat",
            "adb logcat", "adb shell", "ssh", "telnet"
        )
        
        return longRunningCommands.any { command.startsWith(it) }
    }
    
    private fun getDefaultEnvironment(): MutableMap<String, String> {
        return mutableMapOf(
            "PATH" to "/system/bin:/system/xbin:/vendor/bin:/data/data/com.anyoneide.app/files/usr/bin",
            "HOME" to context.filesDir.absolutePath,
            "USER" to "anyoneide",
            "SHELL" to "/system/bin/sh",
            "TERM" to "xterm-256color",
            "LANG" to "en_US.UTF-8",
            "PWD" to context.filesDir.absolutePath,
            "ANDROID_DATA" to "/data",
            "ANDROID_ROOT" to "/system",
            "EXTERNAL_STORAGE" to "/storage/emulated/0"
        )
    }
}

data class TerminalSessionInternal(
    val id: String,
    var workingDirectory: String,
    val environment: MutableMap<String, String>,
    var isActive: Boolean,
    val history: MutableList<String>,
    var currentProcess: Process? = null,
    var isRunningLongCommand: Boolean = false
)

data class TerminalOutputInternal(
    val type: TerminalOutputType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TerminalOutputType {
    STDOUT,
    STDERR,
    COMMAND,
    ERROR,
    SYSTEM,
    CLEAR
}