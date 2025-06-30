package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Class for setting up the terminal environment
 */
class TerminalSetup(private val context: Context) {
    
    private val baseDir = File(context.filesDir, "terminal")
    private val binDir = File(baseDir, "bin")
    private val etcDir = File(baseDir, "etc")
    private val homeDir = File(baseDir, "home")
    private val tmpDir = File(baseDir, "tmp")
    private val usrDir = File(baseDir, "usr")
    
    /**
     * Initialize the terminal environment
     */
    fun initializeTerminalEnvironment(): Flow<SetupProgress> = flow {
        emit(SetupProgress.Started("Initializing terminal environment"))
        
        try {
            // Create base directories
            createDirectories()
            
            // Create basic configuration files
            createConfigFiles()
            
            // Install basic utilities
            installBasicUtilities().collect { progress ->
                emit(progress)
            }
            
            // Set up environment variables
            setupEnvironmentVariables()
            
            emit(SetupProgress.Completed("Terminal environment initialized successfully"))
        } catch (e: Exception) {
            emit(SetupProgress.Failed("Failed to initialize terminal environment: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    private fun createDirectories() {
        baseDir.mkdirs()
        binDir.mkdirs()
        etcDir.mkdirs()
        homeDir.mkdirs()
        tmpDir.mkdirs()
        usrDir.mkdirs()
        
        // Create additional directories
        File(usrDir, "bin").mkdirs()
        File(usrDir, "lib").mkdirs()
        File(usrDir, "share").mkdirs()
        File(homeDir, ".config").mkdirs()
        File(homeDir, ".local/bin").mkdirs()
    }
    
    private fun createConfigFiles() {
        // Create .bashrc
        val bashrcFile = File(homeDir, ".bashrc")
        if (!bashrcFile.exists()) {
            bashrcFile.writeText("""
                # .bashrc
                
                # Source global definitions
                if [ -f /etc/bashrc ]; then
                    . /etc/bashrc
                fi
                
                # User specific environment
                PATH="${'$'}HOME/.local/bin:${'$'}PATH"
                export PATH
                
                # User specific aliases and functions
                alias ll='ls -la'
                alias la='ls -a'
                alias l='ls -CF'
                alias cls='clear'
                
                # Android SDK
                export ANDROID_HOME="${context.filesDir.absolutePath}/sdk/android"
                export PATH="${'$'}PATH:${'$'}ANDROID_HOME/platform-tools:${'$'}ANDROID_HOME/tools:${'$'}ANDROID_HOME/tools/bin"
                
                # Java
                export JAVA_HOME="${context.filesDir.absolutePath}/sdk/jdk"
                export PATH="${'$'}PATH:${'$'}JAVA_HOME/bin"
                
                # Kotlin
                export KOTLIN_HOME="${context.filesDir.absolutePath}/sdk/kotlin"
                export PATH="${'$'}PATH:${'$'}KOTLIN_HOME/bin"
                
                # Gradle
                export GRADLE_HOME="${context.filesDir.absolutePath}/sdk/gradle"
                export PATH="${'$'}PATH:${'$'}GRADLE_HOME/bin"
                
                # Rust
                export CARGO_HOME="${context.filesDir.absolutePath}/sdk/rust/cargo"
                export RUSTUP_HOME="${context.filesDir.absolutePath}/sdk/rust/rustup"
                export PATH="${'$'}PATH:${'$'}CARGO_HOME/bin"
                
                # Welcome message
                echo "Welcome to Anyone IDE Terminal"
                echo "Type 'help' for available commands"
            """.trimIndent())
        }
        
        // Create profile
        val profileFile = File(etcDir, "profile")
        if (!profileFile.exists()) {
            profileFile.writeText("""
                # /etc/profile
                
                # System wide environment and startup programs
                
                PATH="/system/bin:/system/xbin:/data/data/com.anyoneide.app/files/terminal/bin:/data/data/com.anyoneide.app/files/terminal/usr/bin"
                export PATH
                
                # Set default umask
                umask 022
                
                # Load profiles from /etc/profile.d
                if [ -d /etc/profile.d/ ]; then
                  for profile in /etc/profile.d/*.sh; do
                    if [ -r "${'$'}profile" ]; then
                      . "${'$'}profile"
                    fi
                  done
                  unset profile
                fi
                
                # Source ~/.bashrc for interactive bash shells
                if [ -n "${'$'}BASH_VERSION" -a -n "${'$'}PS1" -a -r "${'$'}HOME/.bashrc" ]; then
                  . "${'$'}HOME/.bashrc"
                fi
            """.trimIndent())
        }
        
        // Create help file
        val helpFile = File(usrDir, "share/help.txt")
        helpFile.parentFile?.mkdirs()
        if (!helpFile.exists()) {
            helpFile.writeText("""
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
                  
                SDK Management:
                  sdk-install <component> - Install SDK component
                  sdk-list           - List available components
                  sdk-update         - Update SDK components
                  
                Package Management:
                  apt <command>      - Package manager
                  dpkg <command>     - Package installer
                  
                Other:
                  echo <text>        - Print text
                  clear              - Clear screen
                  help               - Show this help
            """.trimIndent())
        }
    }
    
    private fun installBasicUtilities(): Flow<SetupProgress> = flow {
        // This is a simplified implementation
        // In a real implementation, we would download and install actual binaries
        
        // Create symbolic links to system binaries
        val systemBinaries = listOf(
            "ls", "cd", "pwd", "mkdir", "rm", "cp", "mv", "cat", "grep", "find",
            "ps", "kill", "chmod", "which", "uname", "whoami", "date", "clear",
            "echo", "sh", "bash", "touch", "ln", "du", "df", "tar", "gzip", "gunzip"
        )
        
        for (binary in systemBinaries) {
            val systemPath = "/system/bin/$binary"
            val linkPath = File(binDir, binary)
            
            if (!linkPath.exists() && File(systemPath).exists()) {
                try {
                    // Create a shell script that calls the system binary
                    linkPath.writeText("""
                        #!/system/bin/sh
                        exec /system/bin/$binary "${'$'}@"
                    """.trimIndent())
                    linkPath.setExecutable(true)
                } catch (e: Exception) {
                    emit(SetupProgress.Warning("Failed to create link for $binary: ${e.message}"))
                }
            }
        }
        
        // Create help command
        val helpCommand = File(binDir, "help")
        if (!helpCommand.exists()) {
            helpCommand.writeText("""
                #!/system/bin/sh
                cat ${usrDir.absolutePath}/share/help.txt
            """.trimIndent())
            helpCommand.setExecutable(true)
        }
        
        // Create SDK installation commands
        val sdkInstallCommand = File(binDir, "sdk-install")
        if (!sdkInstallCommand.exists()) {
            sdkInstallCommand.writeText("""
                #!/system/bin/sh
                
                if [ -z "${'$'}1" ]; then
                    echo "Usage: sdk-install <component>"
                    echo "Available components: android-sdk, jdk, kotlin, gradle, ndk, rust"
                    exit 1
                fi
                
                component="${'$'}1"
                
                case "${'$'}component" in
                    android-sdk)
                        echo "Installing Android SDK..."
                        # This would call into the SDKManager to install Android SDK
                        ;;
                    jdk)
                        echo "Installing JDK..."
                        # This would call into the SDKManager to install JDK
                        ;;
                    kotlin)
                        echo "Installing Kotlin..."
                        # This would call into the SDKManager to install Kotlin
                        ;;
                    gradle)
                        echo "Installing Gradle..."
                        # This would call into the SDKManager to install Gradle
                        ;;
                    ndk)
                        echo "Installing NDK..."
                        # This would call into the SDKManager to install NDK
                        ;;
                    rust)
                        echo "Installing Rust..."
                        # This would call into the SDKManager to install Rust
                        ;;
                    *)
                        echo "Unknown component: ${'$'}component"
                        echo "Available components: android-sdk, jdk, kotlin, gradle, ndk, rust"
                        exit 1
                        ;;
                esac
            """.trimIndent())
            sdkInstallCommand.setExecutable(true)
        }
        
        // Create SDK list command
        val sdkListCommand = File(binDir, "sdk-list")
        if (!sdkListCommand.exists()) {
            sdkListCommand.writeText("""
                #!/system/bin/sh
                
                echo "Available SDK components:"
                echo "  android-sdk - Android SDK"
                echo "  jdk - Java Development Kit"
                echo "  kotlin - Kotlin Compiler"
                echo "  gradle - Gradle Build Tool"
                echo "  ndk - Android Native Development Kit"
                echo "  rust - Rust Programming Language"
            """.trimIndent())
            sdkListCommand.setExecutable(true)
        }
        
        // Create SDK update command
        val sdkUpdateCommand = File(binDir, "sdk-update")
        if (!sdkUpdateCommand.exists()) {
            sdkUpdateCommand.writeText("""
                #!/system/bin/sh
                
                echo "Updating SDK components..."
                echo "This feature is not yet implemented."
            """.trimIndent())
            sdkUpdateCommand.setExecutable(true)
        }
        
        // Create apt command (simulated)
        val aptCommand = File(binDir, "apt")
        if (!aptCommand.exists()) {
            aptCommand.writeText("""
                #!/system/bin/sh
                
                if [ "${'$'}1" = "update" ]; then
                    echo "Reading package lists..."
                    echo "Building dependency tree..."
                    echo "Reading state information..."
                    echo "All packages are up to date."
                elif [ "${'$'}1" = "install" ]; then
                    if [ -z "${'$'}2" ]; then
                        echo "apt install: missing package name"
                        exit 1
                    fi
                    echo "Reading package lists..."
                    echo "Building dependency tree..."
                    echo "Reading state information..."
                    echo "Package ${'$'}2 is not available, but is referred to by another package."
                    echo "This may mean that the package is missing, has been obsoleted, or"
                    echo "is only available from another source"
                    echo ""
                    echo "E: Package '${'$'}2' has no installation candidate"
                elif [ "${'$'}1" = "search" ]; then
                    if [ -z "${'$'}2" ]; then
                        echo "apt search: missing search term"
                        exit 1
                    fi
                    echo "Sorting... Done"
                    echo "Full Text Search... Done"
                    echo "No packages found"
                elif [ "${'$'}1" = "list" ]; then
                    echo "Listing... Done"
                    echo "bash/stable,now 5.0-6 arm64 [installed]"
                    echo "coreutils/stable,now 8.30-3 arm64 [installed]"
                    echo "libc6/stable,now 2.28-10 arm64 [installed]"
                else
                    echo "apt: '${'$'}1' is not an apt command. See 'apt --help'."
                fi
            """.trimIndent())
            aptCommand.setExecutable(true)
        }
        
        // Create dpkg command (simulated)
        val dpkgCommand = File(binDir, "dpkg")
        if (!dpkgCommand.exists()) {
            dpkgCommand.writeText("""
                #!/system/bin/sh
                
                if [ "${'$'}1" = "-l" ] || [ "${'$'}1" = "--list" ]; then
                    echo "Desired=Unknown/Install/Remove/Purge/Hold"
                    echo "| Status=Not/Inst/Conf-files/Unpacked/halF-conf/Half-inst/trig-aWait/Trig-pend"
                    echo "|/ Err?=(none)/Reinst-required (Status,Err: uppercase=bad)"
                    echo "||/ Name                       Version                    Architecture Description"
                    echo "+++-==========================-==========================-============-=================================================="
                    echo "ii  bash                       5.0-6                      arm64        GNU Bourne Again SHell"
                    echo "ii  coreutils                  8.30-3                     arm64        GNU core utilities"
                    echo "ii  libc6                      2.28-10                    arm64        GNU C Library: Shared libraries"
                elif [ "${'$'}1" = "-i" ] || [ "${'$'}1" = "--install" ]; then
                    if [ -z "${'$'}2" ]; then
                        echo "dpkg: error: need an action option"
                        exit 1
                    fi
                    echo "dpkg: error: cannot access archive '${'$'}2': No such file or directory"
                else
                    echo "Type dpkg --help for help about installing and deinstalling packages;"
                    echo "Use 'apt' for user-friendly package management;"
                fi
            """.trimIndent())
            dpkgCommand.setExecutable(true)
        }
        
        emit(SetupProgress.Completed("Basic utilities installed"))
    }
    
    private fun setupEnvironmentVariables() {
        // This would set up environment variables for the terminal
        // In a real implementation, this would modify the environment for the terminal process
        
        // For now, we just create a file with environment variables
        val envFile = File(etcDir, "environment")
        envFile.writeText("""
            # Terminal environment variables
            
            # Android SDK
            ANDROID_HOME=${context.filesDir.absolutePath}/sdk/android
            
            # Java
            JAVA_HOME=${context.filesDir.absolutePath}/sdk/jdk
            
            # Kotlin
            KOTLIN_HOME=${context.filesDir.absolutePath}/sdk/kotlin
            
            # Gradle
            GRADLE_HOME=${context.filesDir.absolutePath}/sdk/gradle
            
            # Rust
            CARGO_HOME=${context.filesDir.absolutePath}/sdk/rust/cargo
            RUSTUP_HOME=${context.filesDir.absolutePath}/sdk/rust/rustup
            
            # Path
            PATH=/system/bin:/system/xbin:/data/data/com.anyoneide.app/files/terminal/bin:/data/data/com.anyoneide.app/files/terminal/usr/bin:${'$'}ANDROID_HOME/platform-tools:${'$'}ANDROID_HOME/tools:${'$'}ANDROID_HOME/tools/bin:${'$'}JAVA_HOME/bin:${'$'}KOTLIN_HOME/bin:${'$'}GRADLE_HOME/bin:${'$'}CARGO_HOME/bin
        """.trimIndent())
    }
    
    /**
     * Download a file with progress tracking
     */
    private suspend fun downloadFile(url: String, destination: File): Flow<SetupProgress> = flow {
        emit(SetupProgress.Downloading("Downloading ${destination.name}", 0))
        
        try {
            val connection = URL(url).openConnection()
            val contentLength = connection.contentLength
            var downloadedBytes = 0
            
            connection.getInputStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val progress = if (contentLength > 0) {
                            (downloadedBytes * 100) / contentLength
                        } else {
                            -1
                        }
                        
                        emit(SetupProgress.Downloading("Downloading ${destination.name}", progress))
                    }
                }
            }
            
            emit(SetupProgress.Downloading("Downloaded ${destination.name}", 100))
        } catch (e: Exception) {
            emit(SetupProgress.Failed("Failed to download ${destination.name}: ${e.message}"))
        }
    }
    
    /**
     * Extract a ZIP file
     */
    private suspend fun extractZip(zipFile: File, destination: File): Flow<SetupProgress> = flow {
        emit(SetupProgress.Extracting("Extracting ${zipFile.name}"))
        
        try {
            destination.mkdirs()
            
            ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                
                while (entry != null) {
                    val filePath = File(destination, entry.name)
                    
                    if (!entry.isDirectory) {
                        // Create parent directories if they don't exist
                        filePath.parentFile?.mkdirs()
                        
                        // Extract file
                        FileOutputStream(filePath).use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            
                            while (zipIn.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                        }
                    } else {
                        filePath.mkdirs()
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            emit(SetupProgress.Extracting("Extracted ${zipFile.name}"))
        } catch (e: Exception) {
            emit(SetupProgress.Failed("Failed to extract ${zipFile.name}: ${e.message}"))
        }
    }
    
    /**
     * Extract a tar.gz file
     */
    private suspend fun extractTarGz(tarGzFile: File, destination: File): Flow<SetupProgress> = flow {
        emit(SetupProgress.Extracting("Extracting ${tarGzFile.name}"))
        
        try {
            destination.mkdirs()
            
            FileInputStream(tarGzFile).use { fileIn ->
                GzipCompressorInputStream(fileIn).use { gzipIn ->
                    TarArchiveInputStream(gzipIn).use { tarIn ->
                        var entry = tarIn.nextTarEntry
                        
                        while (entry != null) {
                            val filePath = File(destination, entry.name)
                            
                            if (entry.isDirectory) {
                                filePath.mkdirs()
                            } else {
                                // Create parent directories if they don't exist
                                filePath.parentFile?.mkdirs()
                                
                                // Extract file
                                FileOutputStream(filePath).use { output ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    
                                    while (tarIn.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                    }
                                }
                                
                                // Set executable permission if needed
                                if (entry.mode and 0x100 != 0) {
                                    filePath.setExecutable(true)
                                }
                            }
                            
                            entry = tarIn.nextTarEntry
                        }
                    }
                }
            }
            
            emit(SetupProgress.Extracting("Extracted ${tarGzFile.name}"))
        } catch (e: Exception) {
            emit(SetupProgress.Failed("Failed to extract ${tarGzFile.name}: ${e.message}"))
        }
    }
}

/**
 * Sealed class for terminal setup progress
 */
sealed class SetupProgress {
    data class Started(val message: String) : SetupProgress()
    data class Downloading(val message: String, val progress: Int) : SetupProgress()
    data class Extracting(val message: String) : SetupProgress()
    data class Installing(val message: String) : SetupProgress()
    data class Warning(val message: String) : SetupProgress()
    data class Completed(val message: String) : SetupProgress()
    data class Failed(val message: String) : SetupProgress()
}