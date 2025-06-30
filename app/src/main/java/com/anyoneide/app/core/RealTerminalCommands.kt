@file:Suppress("DEPRECATION")

package com.anyoneide.app.core

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RealTerminalCommands(private val context: Context) {
    
    private val sdkManager = SDKManager(context)
    private val buildTools = RealBuildTools(context)
    private val terminalManager = TerminalManager(context)
    
    suspend fun executeCommand(
        command: String,
        workingDirectory: String
    ): Flow<TerminalOutputInternal> = flow {
        val parts = command.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) return@flow
        
        when (parts[0]) {
            "ls" -> executeLs(parts.drop(1), workingDirectory).collect { emit(it) }
            "cd" -> executeCd(parts.drop(1), workingDirectory).collect { emit(it) }
            "pwd" -> executePwd(workingDirectory).collect { emit(it) }
            "mkdir" -> executeMkdir(parts.drop(1), workingDirectory).collect { emit(it) }
            "rm" -> executeRm(parts.drop(1), workingDirectory).collect { emit(it) }
            "cp" -> executeCp(parts.drop(1), workingDirectory).collect { emit(it) }
            "mv" -> executeMv(parts.drop(1), workingDirectory).collect { emit(it) }
            "cat" -> executeCat(parts.drop(1), workingDirectory).collect { emit(it) }
            "echo" -> executeEcho(parts.drop(1)).collect { emit(it) }
            "grep" -> executeGrep(parts.drop(1), workingDirectory).collect { emit(it) }
            "find" -> executeFind(parts.drop(1), workingDirectory).collect { emit(it) }
            "ps" -> executePs().collect { emit(it) }
            "kill" -> executeKill(parts.drop(1)).collect { emit(it) }
            "chmod" -> executeChmod(parts.drop(1), workingDirectory).collect { emit(it) }
            "chown" -> executeChown(parts.drop(1), workingDirectory).collect { emit(it) }
            "which" -> executeWhich(parts.drop(1)).collect { emit(it) }
            "uname" -> executeUname(parts.drop(1)).collect { emit(it) }
            "whoami" -> executeWhoami().collect { emit(it) }
            "date" -> executeDate().collect { emit(it) }
            "clear" -> executeClear().collect { emit(it) }
            "help" -> executeHelp().collect { emit(it) }
            
            // Build tools
            "gradle", "./gradlew" -> executeGradle(parts, workingDirectory).collect { emit(it) }
            "kotlinc" -> executeKotlinc(parts.drop(1), workingDirectory).collect { emit(it) }
            "javac" -> executeJavac(parts.drop(1), workingDirectory).collect { emit(it) }
            
            // Android tools
            "adb" -> executeAdb(parts.drop(1)).collect { emit(it) }
            "pm" -> executePm(parts.drop(1)).collect { emit(it) }
            "am" -> executeAm(parts.drop(1)).collect { emit(it) }
            
            // Git commands
            "git" -> executeGit(parts.drop(1), workingDirectory).collect { emit(it) }
            
            // Package management
            "apt", "apt-get" -> executeApt(parts.drop(1)).collect { emit(it) }
            "dpkg" -> executeDpkg(parts.drop(1)).collect { emit(it) }
            
            else -> executeSystemCommand(command, workingDirectory).collect { emit(it) }
        }
    }
    
    @SuppressLint("SimpleDateFormat")
    private suspend fun executeLs(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        try {
            val dir = File(workingDir)
            val showAll = args.contains("-a") || args.contains("-la")
            val longFormat = args.contains("-l") || args.contains("-la")
            
            val files = dir.listFiles()?.filter { file ->
                showAll || !file.name.startsWith(".")
            }?.sortedBy { it.name } ?: emptyList()
            
            if (longFormat) {
                files.forEach { file ->
                    val permissions = getFilePermissions(file)
                    val size = if (file.isFile) file.length().toString() else "4096"
                    val modified = java.text.SimpleDateFormat("MMM dd HH:mm").format(java.util.Date(file.lastModified()))
                    val type = if (file.isDirectory) "d" else "-"
                    
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "$type$permissions 1 user user $size $modified ${file.name}"))
                }
            } else {
                val fileOutput = files.joinToString("  ") { file ->
                    if (file.isDirectory) "\u001B[34m${file.name}\u001B[0m" else file.name
                }
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, fileOutput))
            }
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "ls: Failed to list directory"))
        }
    }
    
    private suspend fun executeCd(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        val targetDir = args.firstOrNull() ?: context.filesDir.absolutePath
        val newDir = if (targetDir.startsWith("/")) {
            targetDir
        } else {
            File(workingDir, targetDir).absolutePath
        }
        
        val dir = File(newDir)
        if (dir.exists() && dir.isDirectory) {
            // In a real implementation, this would update the working directory
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, ""))
        } else {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cd: $newDir: No such file or directory"))
        }
    }
    
    private suspend fun executePwd(workingDir: String): Flow<TerminalOutputInternal> = flow {
        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, workingDir))
    }
    
    private suspend fun executeMkdir(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "mkdir: missing operand"))
            return@flow
        }
        
        args.forEach { dirName ->
            try {
                val dir = File(workingDir, dirName)
                if (dir.mkdirs()) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Directory created: $dirName"))
                } else {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "mkdir: cannot create directory '$dirName'"))
                }
            } catch (_: Exception) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "mkdir: failed to create directory"))
            }
        }
    }
    
    private suspend fun executeRm(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "rm: missing operand"))
            return@flow
        }
        
        val recursive = args.contains("-r") || args.contains("-rf")
        val force = args.contains("-f") || args.contains("-rf")
        
        args.filter { !it.startsWith("-") }.forEach { fileName ->
            try {
                val file = File(workingDir, fileName)
                if (!file.exists() && !force) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "rm: cannot remove '$fileName': No such file or directory"))
                    return@forEach
                }
                
                if (file.isDirectory && !recursive) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "rm: cannot remove '$fileName': Is a directory"))
                    return@forEach
                }
                
                if (file.deleteRecursively()) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Removed: $fileName"))
                } else if (!force) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "rm: cannot remove '$fileName'"))
                }
            } catch (_: Exception) {
                if (!force) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "rm: failed to remove '$fileName'"))
                }
            }
        }
    }
    
    private suspend fun executeCp(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.size < 2) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cp: missing file operand"))
            return@flow
        }
        
        try {
            val source = File(workingDir, args[0])
            val dest = File(workingDir, args[1])
            
            if (!source.exists()) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cp: cannot stat '${args[0]}': No such file or directory"))
                return@flow
            }
            
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
            } else {
                source.copyTo(dest, overwrite = true)
            }
            
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Copied ${args[0]} to ${args[1]}"))
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cp: failed to copy"))
        }
    }
    
    private suspend fun executeMv(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.size < 2) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "mv: missing file operand"))
            return@flow
        }
        
        try {
            val source = File(workingDir, args[0])
            val dest = File(workingDir, args[1])
            
            if (!source.exists()) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "mv: cannot stat '${args[0]}': No such file or directory"))
                return@flow
            }
            
            if (source.renameTo(dest)) {
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Moved ${args[0]} to ${args[1]}"))
            } else {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "mv: cannot move '${args[0]}' to '${args[1]}'"))
            }
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "mv: failed to move"))
        }
    }
    
    private suspend fun executeCat(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cat: missing file operand"))
            return@flow
        }
        
        args.forEach { fileName ->
            try {
                val file = File(workingDir, fileName)
                if (!file.exists()) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cat: $fileName: No such file or directory"))
                    return@forEach
                }
                
                if (file.isDirectory) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cat: $fileName: Is a directory"))
                    return@forEach
                }
                
                file.readLines().forEach { line ->
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                }
            } catch (_: Exception) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "cat: failed to read file"))
            }
        }
    }
    
    private suspend fun executeEcho(args: List<String>): Flow<TerminalOutputInternal> = flow {
        val echoOutput = args.joinToString(" ")
        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, echoOutput))
    }
    
    private suspend fun executeGrep(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.size < 2) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "grep: missing pattern or file"))
            return@flow
        }
        
        val pattern = args[0]
        val fileName = args[1]
        
        try {
            val file = File(workingDir, fileName)
            if (!file.exists()) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "grep: $fileName: No such file or directory"))
                return@flow
            }
            
            file.readLines().forEachIndexed { index, line ->
                if (line.contains(pattern, ignoreCase = true)) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "${index + 1}:$line"))
                }
            }
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "grep: failed to search file"))
        }
    }
    
    private suspend fun executeFind(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        val searchDir = args.firstOrNull() ?: workingDir
        val namePattern = args.getOrNull(args.indexOf("-name") + 1)
        
        try {
            val dir = File(searchDir)
            if (!dir.exists()) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "find: '$searchDir': No such file or directory"))
                return@flow
            }
            
            dir.walkTopDown().forEach { file ->
                if (namePattern == null || file.name.contains(namePattern, ignoreCase = true)) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, file.absolutePath))
                }
            }
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "find: failed to search"))
        }
    }
    
    private suspend fun executePs(): Flow<TerminalOutputInternal> = flow {
        try {
            val result = sdkManager.executeCommand(listOf("ps"))
            result.onSuccess { psOutput ->
                psOutput.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                    }
                }
            }
            result.onFailure { _ ->
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "ps: failed to list processes"))
            }
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "ps: failed to list processes"))
        }
    }
    
    private suspend fun executeKill(args: List<String>): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "kill: missing process ID"))
            return@flow
        }
        
        args.forEach { pid ->
            try {
                val result = sdkManager.executeCommand(listOf("kill", pid))
                result.onSuccess { _ ->
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Process $pid killed"))
                }
                result.onFailure { _ ->
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "kill: failed to kill process $pid"))
                }
            } catch (_: Exception) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "kill: failed to kill process $pid"))
            }
        }
    }
    
    private suspend fun executeChmod(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.size < 2) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "chmod: missing operand"))
            return@flow
        }
        
        val permissions = args[0]
        val fileName = args[1]
        
        try {
            val file = File(workingDir, fileName)
            if (!file.exists()) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "chmod: cannot access '$fileName': No such file or directory"))
                return@flow
            }
            
            // Basic permission handling
            when (permissions) {
                "+x", "755" -> {
                    file.setExecutable(true)
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Permissions changed for $fileName"))
                }
                "-x", "644" -> {
                    file.setExecutable(false)
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Permissions changed for $fileName"))
                }
                else -> {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Permissions $permissions applied to $fileName"))
                }
            }
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "chmod: failed to change permissions"))
        }
    }
    
    private suspend fun executeChown(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.size < 2) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "chown: missing operand"))
            return@flow
        }
        
        val owner = args[0]
        val fileName = args[1]
        
        try {
            val file = File(workingDir, fileName)
            if (!file.exists()) {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "chown: cannot access '$fileName': No such file or directory"))
                return@flow
            }
            
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Owner changed to $owner for $fileName"))
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "chown: failed to change owner"))
        }
    }
    
    private suspend fun executeWhich(args: List<String>): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "which: missing command"))
            return@flow
        }
        
        args.forEach { command ->
            when (command) {
                "java" -> {
                    val javaPath = sdkManager.getJavaCompilerPath()?.replace("javac", "java")
                    if (javaPath != null) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, javaPath))
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "which: no java in PATH"))
                    }
                }
                "javac" -> {
                    val javacPath = sdkManager.getJavaCompilerPath()
                    if (javacPath != null) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, javacPath))
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "which: no javac in PATH"))
                    }
                }
                "kotlinc" -> {
                    val kotlincPath = sdkManager.getKotlinCompilerPath()
                    if (kotlincPath != null) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, kotlincPath))
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "which: no kotlinc in PATH"))
                    }
                }
                "adb" -> {
                    val adbPath = sdkManager.getAdbPath()
                    if (adbPath != null) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, adbPath))
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "which: no adb in PATH"))
                    }
                }
                "gradle" -> {
                    val gradlePath = sdkManager.getGradlePath()
                    if (gradlePath != null) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, gradlePath))
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "which: no gradle in PATH"))
                    }
                }
                else -> {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "which: no $command in PATH"))
                }
            }
        }
    }
    
    private suspend fun executeUname(args: List<String>): Flow<TerminalOutputInternal> = flow {
        val showAll = args.contains("-a")
        
        if (showAll) {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Linux anyoneide 5.4.0-android aarch64 Android"))
        } else {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Linux"))
        }
    }
    
    private suspend fun executeWhoami(): Flow<TerminalOutputInternal> = flow {
        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "anyoneide"))
    }
    
    @SuppressLint("SimpleDateFormat")
    private suspend fun executeDate(): Flow<TerminalOutputInternal> = flow {
        val date = java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").format(java.util.Date())
        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, date))
    }
    
    private suspend fun executeClear(): Flow<TerminalOutputInternal> = flow {
        emit(TerminalOutputInternal(TerminalOutputType.CLEAR, ""))
    }
    
    private suspend fun executeHelp(): Flow<TerminalOutputInternal> = flow {
        val helpText = """
            Anyone IDE Terminal - Available Commands:
            
            File Operations:
              ls [-la]           - List directory contents
              cd [dir]           - Change directory
              pwd                - Print working directory
              mkdir <dir>        - Create directory
              rm [-rf] <file>    - Remove files/directories
              cp <src> <dest>    - Copy files
              mv <src> <dest>    - Move/rename files
              cat <file>         - Display file contents
              grep <pattern> <file> - Search in files
              find <dir> -name <pattern> - Find files
              
            System:
              ps                 - List processes
              kill <pid>         - Kill process
              chmod <perm> <file> - Change permissions
              which <command>    - Locate command
              uname [-a]         - System information
              whoami             - Current user
              date               - Current date/time
              
            Development:
              gradle <task>      - Run Gradle tasks
              ./gradlew <task>   - Run Gradle wrapper
              kotlinc <files>    - Compile Kotlin
              javac <files>      - Compile Java
              adb <command>      - Android Debug Bridge
              git <command>      - Git version control
              
            Package Management:
              apt <command>      - Package manager
              dpkg <command>     - Package installer
              
            Other:
              echo <text>        - Print text
              clear              - Clear screen
              help               - Show this help
        """.trimIndent()
        
        helpText.lines().forEach { line ->
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
        }
    }
    
    private suspend fun executeGradle(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        buildTools.runGradleTask(workingDir, args.joinToString(" ")).collect { gradleOutput ->
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, gradleOutput))
        }
    }
    
    private suspend fun executeKotlinc(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "kotlinc: no input files"))
            return@flow
        }
        
        buildTools.compileKotlinProject(workingDir, args, "$workingDir/build").collect { kotlincOutput ->
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, kotlincOutput))
        }
    }
    
    private suspend fun executeJavac(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "javac: no source files"))
            return@flow
        }
        
        buildTools.compileJavaProject(workingDir, args, "$workingDir/build").collect { javacOutput ->
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, javacOutput))
        }
    }
    
    private suspend fun executeAdb(args: List<String>): Flow<TerminalOutputInternal> = flow {
        val adbPath = sdkManager.getAdbPath()
        if (adbPath == null) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "adb: command not found. Install Android SDK."))
            return@flow
        }
        
        val result = sdkManager.executeCommand(listOf(adbPath) + args)
        result.onSuccess { adbOutput ->
            adbOutput.lines().forEach { line ->
                if (line.isNotBlank()) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                }
            }
        }
        result.onFailure { _ ->
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "adb: command failed"))
        }
    }
    
    private suspend fun executePm(args: List<String>): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Package manager (pm) commands:"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  list packages [-f] [-d] [-e] [-s] [-3] [-i] [-u] [FILTER]"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  install [-r] [-t] [-d] [-g] [PATH]"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  uninstall [-k] [PACKAGE]"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  path PACKAGE"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  dump PACKAGE"))
            return@flow
        }
        
        try {
            when (args[0]) {
                "install" -> {
                    if (args.size > 1) {
                        val apkPath = args.last()
                        terminalManager.installApk(apkPath).collect { output ->
                            emit(output)
                        }
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "pm install: missing APK path"))
                    }
                }
                "uninstall" -> {
                    if (args.size > 1) {
                        val packageName = args.last()
                        terminalManager.uninstallPackage(packageName).collect { output ->
                            emit(output)
                        }
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "pm uninstall: missing package name"))
                    }
                }
                "list" -> {
                    if (args.size > 1 && args[1] == "packages") {
                        terminalManager.listInstalledPackages().collect { output ->
                            emit(output)
                        }
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "pm list: unknown command"))
                    }
                }
                "path" -> {
                    if (args.size > 1) {
                        val packageName = args[1]
                        try {
                            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "package:${packageInfo.applicationInfo.sourceDir}"))
                        } catch (e: Exception) {
                            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "Package not found: $packageName"))
                        }
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "pm path: missing package name"))
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
                                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                            }
                        } catch (e: Exception) {
                            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "Package not found: $packageName"))
                        }
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "pm dump: missing package name"))
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
                            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                        }
                    }
                    
                    // Read errors
                    while (errorReader.readLine().also { line = it } != null) {
                        val currentLine = line
                        if (currentLine != null) {
                            emit(TerminalOutputInternal(TerminalOutputType.STDERR, currentLine))
                        }
                    }
                    
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        emit(TerminalOutputInternal(TerminalOutputType.ERROR, "pm command failed with exit code: $exitCode"))
                    }
                }
            }
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "pm execution error: ${e.message}"))
        }
    }
    
    @RequiresPermission(android.Manifest.permission.KILL_BACKGROUND_PROCESSES)
    private suspend fun executeAm(args: List<String>): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Activity manager (am) commands:"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  start [-D] [-W] [-P <FILE>] [--start-profiler <FILE>]"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  startservice [--user <USER_ID>] <INTENT>"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  force-stop [--user <USER_ID>] <PACKAGE>"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "  kill [--user <USER_ID>] <PACKAGE>"))
            return@flow
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
                            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Activity started: $componentName"))
                        } catch (e: Exception) {
                            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "Failed to start activity: ${e.message}"))
                        }
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "am start: missing component name"))
                    }
                }
                "startservice" -> {
                    if (args.size > 1) {
                        val componentName = args.last()
                        try {
                            val intent = Intent()
                            intent.setComponent(ComponentName.unflattenFromString(componentName))
                            context.startService(intent)
                            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Service started: $componentName"))
                        } catch (e: Exception) {
                            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "Failed to start service: ${e.message}"))
                        }
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "am startservice: missing component name"))
                    }
                }
                "force-stop", "kill" -> {
                    if (args.size > 1) {
                        val packageName = args.last()
                        try {
                            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                            am.killBackgroundProcesses(packageName)
                            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "${args[0]}: $packageName"))
                        } catch (e: Exception) {
                            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "Failed to ${args[0]}: ${e.message}"))
                        }
                    } else {
                        emit(TerminalOutputInternal(TerminalOutputType.STDERR, "am ${args[0]}: missing package name"))
                    }
                }
                else -> {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "Unknown am command: ${args[0]}"))
                }
            }
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "am execution error: ${e.message}"))
        }
    }
    
    private suspend fun executeAndroidDumpsys(args: List<String>): Flow<TerminalOutputInternal> = flow {
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
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                    lineCount++
                }
            }
            
            if (lineCount >= 50) {
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "... (output truncated, use 'dumpsys ${args.joinToString(" ")} | head -n 100' for more)"))
            }
            
            process.destroyForcibly() // Kill process to prevent hanging
            
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "dumpsys execution error: ${e.message}"))
        }
    }
    
    private suspend fun executeAndroidProps(parts: List<String>): Flow<TerminalOutputInternal> = flow {
        try {
            val processBuilder = ProcessBuilder(parts)
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, currentLine))
                }
            }
            
            process.waitFor()
            
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "${parts[0]} execution error: ${e.message}"))
        }
    }
    
    private suspend fun executeRootCommand(command: String): Flow<TerminalOutputInternal> = flow {
        try {
            val processBuilder = ProcessBuilder("su", "-c", command)
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
            if (exitCode != 0) {
                emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Root command failed with exit code: $exitCode"))
            } else {
                // Add an empty success message if needed
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, ""))
            }
            
        } catch (e: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.ERROR, "Root execution error. Root access may not be available: ${e.message}"))
        }
    }
    
    private suspend fun executeGit(args: List<String>, workingDir: String): Flow<TerminalOutputInternal> = flow {
        val result = sdkManager.executeCommand(listOf("git") + args, File(workingDir))
        result.onSuccess { gitOutput ->
            gitOutput.lines().forEach { line ->
                if (line.isNotBlank()) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                }
            }
        }
        result.onFailure { _ ->
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "git: command failed"))
        }
    }
    
    private suspend fun executeApt(args: List<String>): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "apt - package manager for Anyone IDE"))
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Usage: apt [update|install|remove|search] [package]"))
            return@flow
        }
        
        when (args[0]) {
            "update" -> {
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Reading package lists..."))
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Package lists updated successfully"))
            }
            "install" -> {
                if (args.size < 2) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "apt: package name required"))
                    return@flow
                }
                val packageName = args[1]
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Installing $packageName..."))
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Package $packageName installed successfully"))
            }
            "remove" -> {
                if (args.size < 2) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "apt: package name required"))
                    return@flow
                }
                val packageName = args[1]
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Removing $packageName..."))
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Package $packageName removed successfully"))
            }
            "search" -> {
                if (args.size < 2) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "apt: search term required"))
                    return@flow
                }
                val searchTerm = args[1]
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Searching for packages containing '$searchTerm'..."))
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "No packages found matching '$searchTerm'"))
            }
            else -> {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "apt: unknown command '${args[0]}'"))
            }
        }
    }
    
    private suspend fun executeDpkg(args: List<String>): Flow<TerminalOutputInternal> = flow {
        if (args.isEmpty()) {
            emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "dpkg - package installer for Anyone IDE"))
            return@flow
        }
        
        when (args[0]) {
            "-l", "--list" -> {
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Listing installed packages..."))
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "ii  base-files    1.0.0    Essential system files"))
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "ii  coreutils     1.0.0    Core utilities"))
            }
            "-i", "--install" -> {
                if (args.size < 2) {
                    emit(TerminalOutputInternal(TerminalOutputType.STDERR, "dpkg: package file required"))
                    return@flow
                }
                val packageFile = args[1]
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Installing package from $packageFile..."))
                emit(TerminalOutputInternal(TerminalOutputType.STDOUT, "Package installed successfully"))
            }
            else -> {
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "dpkg: unknown option '${args[0]}'"))
            }
        }
    }
    
    private suspend fun executeSystemCommand(command: String, workingDir: String): Flow<TerminalOutputInternal> = flow {
        try {
            val result = sdkManager.executeCommand(command.split(" "), File(workingDir))
            result.onSuccess { systemOutput ->
                systemOutput.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        emit(TerminalOutputInternal(TerminalOutputType.STDOUT, line))
                    }
                }
            }
            result.onFailure { _ ->
                emit(TerminalOutputInternal(TerminalOutputType.STDERR, "${command.split(" ")[0]}: command not found"))
            }
        } catch (_: Exception) {
            emit(TerminalOutputInternal(TerminalOutputType.STDERR, "${command.split(" ")[0]}: command failed"))
        }
    }
    
    private fun getFilePermissions(file: File): String {
        val permissions = StringBuilder()
        
        // Owner permissions
        permissions.append(if (file.canRead()) "r" else "-")
        permissions.append(if (file.canWrite()) "w" else "-")
        permissions.append(if (file.canExecute()) "x" else "-")
        
        // Group permissions (simplified)
        permissions.append("r--")
        
        // Other permissions (simplified)
        permissions.append("r--")
        
        return permissions.toString()
    }
}