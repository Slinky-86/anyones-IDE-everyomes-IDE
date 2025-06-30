package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class SDKManager(private val context: Context) {
    
    private val sdkDir = File(context.filesDir, "sdk")
    private val androidSdkDir = File(sdkDir, "android")
    private val jdkDir = File(sdkDir, "jdk")
    private val kotlinDir = File(sdkDir, "kotlin")
    private val ndkDir = File(sdkDir, "ndk")
    private val rustDir = File(sdkDir, "rust") // Added Rust directory
    
    init {
        sdkDir.mkdirs()
        androidSdkDir.mkdirs()
        jdkDir.mkdirs()
        kotlinDir.mkdirs()
        ndkDir.mkdirs()
        rustDir.mkdirs() // Create Rust directory
    }
    
    suspend fun installAndroidSDK(version: String = "34"): Flow<InstallationProgress> = flow {
        emit(InstallationProgress.Started("Installing Android SDK $version..."))
        
        try {
            // Download Android SDK command line tools
            val toolsUrl = "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip"
            val toolsZip = File(androidSdkDir, "cmdline-tools.zip")
            
            emit(InstallationProgress.Downloading("Downloading Android SDK tools...", 0))
            
            // Real download with progress tracking
            downloadFileWithProgress(toolsUrl, toolsZip).collect { progress ->
                emit(InstallationProgress.Downloading("Downloading Android SDK tools...", progress))
            }
            
            emit(InstallationProgress.Extracting("Extracting Android SDK tools..."))
            extractZip(toolsZip, androidSdkDir)
            
            // Install SDK components using real sdkmanager
            val sdkManagerPath = File(androidSdkDir, "cmdline-tools/bin/sdkmanager")
            if (sdkManagerPath.exists()) {
                sdkManagerPath.setExecutable(true)
                
                val components = listOf(
                    "platform-tools",
                    "build-tools;$version.0.0",
                    "platforms;android-$version",
                    "sources;android-$version"
                )
                
                for (component in components) {
                    emit(InstallationProgress.Installing("Installing $component..."))
                    installSDKComponent(sdkManagerPath.absolutePath, component)
                }
            }
            
            emit(InstallationProgress.Completed("Android SDK $version installed successfully"))
            
        } catch (_: Exception) {
            emit(InstallationProgress.Failed("Failed to install Android SDK"))
        }
    }
    
    suspend fun installJDK(version: String = "11"): Flow<InstallationProgress> = flow {
        emit(InstallationProgress.Started("Installing JDK $version..."))
        
        try {
            // Download OpenJDK for ARM64 Android
            val jdkUrl = when (version) {
                "11" -> "https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.20%2B8/OpenJDK11U-jdk_aarch64_linux_hotspot_11.0.20_8.tar.gz"
                "17" -> "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.8_7.tar.gz"
                "21" -> "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.1_12.tar.gz"
                else -> throw Exception("Unsupported JDK version: $version")
            }
            
            val jdkArchive = File(jdkDir, "jdk-$version.tar.gz")
            
            emit(InstallationProgress.Downloading("Downloading JDK $version...", 0))
            
            downloadFileWithProgress(jdkUrl, jdkArchive).collect { progress ->
                emit(InstallationProgress.Downloading("Downloading JDK $version...", progress))
            }
            
            emit(InstallationProgress.Extracting("Extracting JDK $version..."))
            // REAL tar.gz extraction using Apache Commons Compress
            extractTarGz(jdkArchive, jdkDir)
            
            emit(InstallationProgress.Installing("Setting up JDK environment..."))
            
            // Make Java executables executable
            val jdkInstallDir = jdkDir.listFiles()?.find { it.isDirectory && it.name.startsWith("jdk") }
            jdkInstallDir?.let { jdkPath ->
                val binDir = File(jdkPath, "bin")
                binDir.listFiles()?.forEach { executable ->
                    if (executable.isFile) {
                        executable.setExecutable(true)
                    }
                }
            }
            
            emit(InstallationProgress.Completed("JDK $version installed successfully"))
            
        } catch (_: Exception) {
            emit(InstallationProgress.Failed("Failed to install JDK"))
        }
    }
    
    suspend fun installKotlinCompiler(version: String = "1.9.20"): Flow<InstallationProgress> = flow {
        emit(InstallationProgress.Started("Installing Kotlin compiler $version..."))
        
        try {
            val kotlinUrl = "https://github.com/JetBrains/kotlin/releases/download/v$version/kotlin-compiler-$version.zip"
            val kotlinZip = File(kotlinDir, "kotlin-compiler-$version.zip")
            
            emit(InstallationProgress.Downloading("Downloading Kotlin compiler...", 0))
            
            downloadFileWithProgress(kotlinUrl, kotlinZip).collect { progress ->
                emit(InstallationProgress.Downloading("Downloading Kotlin compiler...", progress))
            }
            
            emit(InstallationProgress.Extracting("Extracting Kotlin compiler..."))
            extractZip(kotlinZip, kotlinDir)
            
            // Make kotlinc executable
            val kotlincPath = File(kotlinDir, "kotlinc/bin/kotlinc")
            if (kotlincPath.exists()) {
                kotlincPath.setExecutable(true)
            }
            
            // Make all Kotlin tools executable
            val kotlinBinDir = File(kotlinDir, "kotlinc/bin")
            kotlinBinDir.listFiles()?.forEach { executable ->
                if (executable.isFile) {
                    executable.setExecutable(true)
                }
            }
            
            emit(InstallationProgress.Completed("Kotlin compiler $version installed successfully"))
            
        } catch (_: Exception) {
            emit(InstallationProgress.Failed("Failed to install Kotlin compiler"))
        }
    }
    
    suspend fun installNDK(version: String = "25.2.9519653"): Flow<InstallationProgress> = flow {
        emit(InstallationProgress.Started("Installing Android NDK $version..."))
        
        try {
            val ndkUrl = "https://dl.google.com/android/repository/android-ndk-r25c-linux.zip"
            val ndkZip = File(ndkDir, "android-ndk-$version.zip")
            
            emit(InstallationProgress.Downloading("Downloading Android NDK...", 0))
            
            downloadFileWithProgress(ndkUrl, ndkZip).collect { progress ->
                emit(InstallationProgress.Downloading("Downloading Android NDK...", progress))
            }
            
            emit(InstallationProgress.Extracting("Extracting Android NDK..."))
            extractZip(ndkZip, ndkDir)
            
            // Make NDK tools executable
            val ndkInstallDir = ndkDir.listFiles()?.find { it.isDirectory && it.name.startsWith("android-ndk") }
            ndkInstallDir?.let { ndkPath ->
                // Make toolchain executables executable
                val toolchainsDir = File(ndkPath, "toolchains")
                toolchainsDir.walkTopDown().forEach { file ->
                    if (file.isFile && (file.name.endsWith("-gcc") || file.name.endsWith("-g++") || 
                        file.name.endsWith("-clang") || file.name.endsWith("-clang++"))) {
                        file.setExecutable(true)
                    }
                }
                
                // Make build tools executable
                val buildDir = File(ndkPath, "build")
                buildDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.canRead()) {
                        file.setExecutable(true)
                    }
                }
            }
            
            emit(InstallationProgress.Completed("Android NDK $version installed successfully"))
            
        } catch (_: Exception) {
            emit(InstallationProgress.Failed("Failed to install Android NDK"))
        }
    }
    
    suspend fun installRust(channel: String = "stable"): Flow<InstallationProgress> = flow {
        emit(InstallationProgress.Started("Installing Rust $channel..."))
        
        try {
            // Download rustup-init
            val rustupUrl = "https://sh.rustup.rs"
            val rustupScript = File(rustDir, "rustup-init.sh")
            
            emit(InstallationProgress.Downloading("Downloading Rust installer...", 0))
            
            downloadFileWithProgress(rustupUrl, rustupScript).collect { progress ->
                emit(InstallationProgress.Downloading("Downloading Rust installer...", progress))
            }
            
            // Make rustup-init executable
            rustupScript.setExecutable(true)
            
            // Run rustup-init with non-interactive options
            emit(InstallationProgress.Installing("Running Rust installer..."))
            
            val processBuilder = ProcessBuilder(
                rustupScript.absolutePath,
                "-y",
                "--default-toolchain", channel,
                "--no-modify-path"
            )
            
            // Set CARGO_HOME and RUSTUP_HOME
            val env = processBuilder.environment()
            env["CARGO_HOME"] = File(rustDir, "cargo").absolutePath
            env["RUSTUP_HOME"] = File(rustDir, "rustup").absolutePath
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    emit(InstallationProgress.Installing(outputLine))
                }
            }
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                // Add Android targets
                emit(InstallationProgress.Installing("Adding Android targets..."))
                
                val rustupPath = File(rustDir, "cargo/bin/rustup")
                rustupPath.setExecutable(true)
                
                val androidTargets = listOf(
                    "aarch64-linux-android",
                    "armv7-linux-androideabi",
                    "i686-linux-android",
                    "x86_64-linux-android"
                )
                
                for (target in androidTargets) {
                    val targetProcess = ProcessBuilder(
                        rustupPath.absolutePath,
                        "target",
                        "add",
                        target
                    ).start()
                    
                    targetProcess.waitFor()
                }
                
                emit(InstallationProgress.Completed("Rust $channel installed successfully"))
            } else {
                emit(InstallationProgress.Failed("Rust installation failed with exit code: $exitCode"))
            }
            
        } catch (e: Exception) {
            emit(InstallationProgress.Failed("Failed to install Rust: ${e.message}"))
        }
    }
    
    suspend fun installGradle(version: String = "8.4"): Flow<InstallationProgress> = flow {
        emit(InstallationProgress.Started("Installing Gradle $version..."))
        
        try {
            val gradleUrl = "https://services.gradle.org/distributions/gradle-$version-bin.zip"
            val gradleZip = File(sdkDir, "gradle-$version.zip")
            
            emit(InstallationProgress.Downloading("Downloading Gradle $version...", 0))
            
            downloadFileWithProgress(gradleUrl, gradleZip).collect { progress ->
                emit(InstallationProgress.Downloading("Downloading Gradle $version...", progress))
            }
            
            emit(InstallationProgress.Extracting("Extracting Gradle..."))
            val gradleDir = File(sdkDir, "gradle")
            gradleDir.mkdirs()
            extractZip(gradleZip, gradleDir)
            
            // Make Gradle executable
            val gradleInstallDir = gradleDir.listFiles()?.find { it.isDirectory && it.name.startsWith("gradle-") }
            gradleInstallDir?.let { gradlePath ->
                val gradleExecutable = File(gradlePath, "bin/gradle")
                if (gradleExecutable.exists()) {
                    gradleExecutable.setExecutable(true)
                }
            }
            
            emit(InstallationProgress.Completed("Gradle $version installed successfully"))
            
        } catch (_: Exception) {
            emit(InstallationProgress.Failed("Failed to install Gradle"))
        }
    }
    
    fun getAndroidSDKPath(): String? {
        val sdkPath = File(androidSdkDir, "cmdline-tools")
        return if (sdkPath.exists()) androidSdkDir.absolutePath else null
    }
    
    fun getJavaHomePath(): String? {
        val jdkPath = jdkDir.listFiles()?.find { it.isDirectory && it.name.startsWith("jdk") }
        return jdkPath?.absolutePath
    }
    
    fun getKotlinCompilerPath(): String? {
        val kotlincPath = File(kotlinDir, "kotlinc/bin/kotlinc")
        return if (kotlincPath.exists()) kotlincPath.absolutePath else null
    }
    
    fun getJavaCompilerPath(): String? {
        val javaHome = getJavaHomePath() ?: return null
        val javacPath = File(javaHome, "bin/javac")
        return if (javacPath.exists()) javacPath.absolutePath else null
    }
    
    fun getGradlePath(): String? {
        val gradleDir = File(sdkDir, "gradle")
        val gradleInstallDir = gradleDir.listFiles()?.find { it.isDirectory && it.name.startsWith("gradle-") }
        val gradleExecutable = File(gradleInstallDir, "bin/gradle")
        return if (gradleExecutable.exists()) gradleExecutable.absolutePath else null
    }
    
    fun getApkSignerPath(): String? {
        val buildToolsDir = File(androidSdkDir, "build-tools")
        val latestBuildTools = buildToolsDir.listFiles()?.maxByOrNull { it.name }
        val apksignerPath = File(latestBuildTools, "apksigner")
        return if (apksignerPath.exists()) apksignerPath.absolutePath else null
    }
    
    fun getZipalignPath(): String? {
        val buildToolsDir = File(androidSdkDir, "build-tools")
        val latestBuildTools = buildToolsDir.listFiles()?.maxByOrNull { it.name }
        val zipalignPath = File(latestBuildTools, "zipalign")
        return if (zipalignPath.exists()) zipalignPath.absolutePath else null
    }
    
    fun getAaptPath(): String? {
        val buildToolsDir = File(androidSdkDir, "build-tools")
        val latestBuildTools = buildToolsDir.listFiles()?.maxByOrNull { it.name }
        val aaptPath = File(latestBuildTools, "aapt2")
        return if (aaptPath.exists()) aaptPath.absolutePath else null
    }
    
    fun getDxPath(): String? {
        val buildToolsDir = File(androidSdkDir, "build-tools")
        val latestBuildTools = buildToolsDir.listFiles()?.maxByOrNull { it.name }
        val dxPath = File(latestBuildTools, "dx")
        return if (dxPath.exists()) dxPath.absolutePath else null
    }
    
    fun getAdbPath(): String? {
        val adbPath = File(androidSdkDir, "platform-tools/adb")
        return if (adbPath.exists()) adbPath.absolutePath else null
    }
    
    // Rust-specific paths
    fun getRustCargoPath(): String? {
        val cargoPath = File(rustDir, "cargo/bin/cargo")
        return if (cargoPath.exists()) cargoPath.absolutePath else null
    }
    
    fun getRustHomePath(): String? {
        val rustupHome = File(rustDir, "rustup")
        return if (rustupHome.exists()) rustupHome.absolutePath else null
    }
    
    fun getRustCargoHomePath(): String? {
        val cargoHome = File(rustDir, "cargo")
        return if (cargoHome.exists()) cargoHome.absolutePath else null
    }
    
    suspend fun checkSDKStatus(): SDKStatus = withContext(Dispatchers.IO) {
        SDKStatus(
            androidSDKInstalled = getAndroidSDKPath() != null,
            jdkInstalled = getJavaHomePath() != null,
            kotlinInstalled = getKotlinCompilerPath() != null,
            gradleInstalled = getGradlePath() != null,
            ndkInstalled = ndkDir.listFiles()?.any { it.isDirectory && it.name.startsWith("android-ndk") } == true,
            adbAvailable = getAdbPath() != null,
            buildToolsAvailable = getApkSignerPath() != null,
            rustInstalled = getRustCargoPath() != null
        )
    }
    
    private suspend fun downloadFileWithProgress(
        url: String, 
        destination: File
    ): Flow<Int> = flow {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                val contentLength = connection.contentLength
                
                connection.getInputStream().use { input ->
                    destination.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var totalBytesRead = 0
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            if (contentLength > 0) {
                                val progress = (totalBytesRead * 100) / contentLength
                                emit(progress)
                            }
                        }
                    }
                }
                emit(100) // Ensure we emit 100% completion
            } catch (_: Exception) {
                throw Exception("Download failed")
            }
        }
    }
    
    private suspend fun extractZip(zipFile: File, destination: File) = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(zipFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val file = File(destination, entry.name)
                    
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                    }
                    
                    entry = zip.nextEntry
                }
            }
        } catch (_: Exception) {
            throw Exception("Failed to extract ZIP file")
        }
    }
    
    private suspend fun extractTarGz(tarGzFile: File, destination: File) = withContext(Dispatchers.IO) {
        try {
            // REAL tar.gz extraction using Apache Commons Compress
            FileInputStream(tarGzFile).use { fileInput ->
                GzipCompressorInputStream(fileInput).use { gzipInput ->
                    TarArchiveInputStream(gzipInput).use { tarInput ->
                        var entry = tarInput.nextTarEntry
                        
                        while (entry != null) {
                            val file = File(destination, entry.name)
                            
                            if (entry.isDirectory) {
                                file.mkdirs()
                            } else {
                                file.parentFile?.mkdirs()
                                FileOutputStream(file).use { output ->
                                    tarInput.copyTo(output)
                                }
                                
                                // Preserve file permissions
                                if (entry.mode and 0x40 != 0) { // Check if executable bit is set
                                    file.setExecutable(true)
                                }
                            }
                            
                            entry = tarInput.nextTarEntry
                        }
                    }
                }
            }
        } catch (_: Exception) {
            throw Exception("Failed to extract tar.gz file")
        }
    }
    
    private suspend fun installSDKComponent(sdkManagerPath: String, component: String) = withContext(Dispatchers.IO) {
        try {
            val command = listOf(
                sdkManagerPath, 
                "--install", 
                component, 
                "--sdk_root=${androidSdkDir.absolutePath}",
                "--verbose"
            )
            val processBuilder = ProcessBuilder(command)
            processBuilder.environment()["ANDROID_HOME"] = androidSdkDir.absolutePath
            processBuilder.environment()["JAVA_HOME"] = getJavaHomePath() ?: "/system"
            
            val process = processBuilder.start()
            
            // Read output to prevent hanging
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Start threads to consume output
            val outputThread = Thread {
                try {
                    reader.useLines { lines ->
                        lines.forEach { line ->
                            println("SDK Manager: $line")
                        }
                    }
                } catch (_: Exception) {
                    // Ignore
                }
            }
            
            val errorThread = Thread {
                try {
                    errorReader.useLines { lines ->
                        lines.forEach { line ->
                            println("SDK Manager Error: $line")
                        }
                    }
                } catch (_: Exception) {
                    // Ignore
                }
            }
            
            outputThread.start()
            errorThread.start()
            
            // Wait for process with timeout
            val finished = process.waitFor(120, TimeUnit.SECONDS) // Increased timeout for large downloads
            if (!finished) {
                process.destroyForcibly()
                throw Exception("SDK component installation timed out")
            }
            
            // Wait for output threads to finish
            outputThread.join(2000)
            errorThread.join(2000)
            
            if (process.exitValue() != 0) {
                throw Exception("SDK component installation failed with exit code: ${process.exitValue()}")
            }
            
        } catch (_: Exception) {
            throw Exception("Failed to install SDK component $component")
        }
    }
    
    suspend fun executeCommand(command: List<String>, workingDir: File? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder(command)
            workingDir?.let { processBuilder.directory(it) }
            
            // Set environment variables
            val env = processBuilder.environment()
            getAndroidSDKPath()?.let { env["ANDROID_HOME"] = it }
            getJavaHomePath()?.let { env["JAVA_HOME"] = it }
            getGradlePath()?.let { 
                val currentPath = env["PATH"] ?: ""
                env["PATH"] = "${File(it).parent}:$currentPath"
            }
            getRustCargoHomePath()?.let { env["CARGO_HOME"] = it }
            getRustHomePath()?.let { env["RUSTUP_HOME"] = it }
            
            val process = processBuilder.start()
            
            val output = StringBuilder()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val outputThread = Thread {
                try {
                    reader.useLines { lines ->
                        lines.forEach { line ->
                            output.appendLine(line)
                        }
                    }
                } catch (_: Exception) {
                    // Ignore
                }
            }
            
            val errorThread = Thread {
                try {
                    errorReader.useLines { lines ->
                        lines.forEach { line ->
                            output.appendLine("ERROR: $line")
                        }
                    }
                } catch (_: Exception) {
                    // Ignore
                }
            }
            
            outputThread.start()
            errorThread.start()
            
            val finished = process.waitFor(60, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext Result.failure(Exception("Command timed out"))
            }
            
            outputThread.join(1000)
            errorThread.join(1000)
            
            if (process.exitValue() == 0) {
                Result.success(output.toString())
            } else {
                Result.failure(Exception("Command failed with exit code: ${process.exitValue()}\n${output}"))
            }
            
        } catch (_: Exception) {
            Result.failure(Exception("Failed to execute command"))
        }
    }
}

sealed class InstallationProgress {
    data class Started(val message: String) : InstallationProgress()
    data class Downloading(val message: String, val progress: Int) : InstallationProgress()
    data class Extracting(val message: String) : InstallationProgress()
    data class Installing(val message: String) : InstallationProgress()
    data class Completed(val message: String) : InstallationProgress()
    data class Failed(val message: String) : InstallationProgress()
}

data class SDKStatus(
    val androidSDKInstalled: Boolean,
    val jdkInstalled: Boolean,
    val kotlinInstalled: Boolean,
    val gradleInstalled: Boolean,
    val ndkInstalled: Boolean,
    val adbAvailable: Boolean,
    val buildToolsAvailable: Boolean,
    val rustInstalled: Boolean = false
)