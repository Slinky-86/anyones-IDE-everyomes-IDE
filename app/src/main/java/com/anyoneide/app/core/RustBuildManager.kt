package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class RustBuildManager(val context: Context) {
    
    internal val sdkManager = SDKManager(context)
    private val fileManager = FileManager(context)

    /**
     * Build a Rust project using cargo
     */
    fun buildRustProject(
        projectPath: String,
        buildType: String = "debug",
        release: Boolean = false
    ): Flow<RustBuildOutput> = flow {
        emit(RustBuildOutput("Starting Rust build...", RustBuildOutput.Type.INFO))
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(RustBuildOutput("Project directory does not exist: $projectPath", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Check if Rust is installed
            val cargoPath = sdkManager.getRustCargoPath()
            if (cargoPath == null) {
                emit(RustBuildOutput("Rust not found. Installing...", RustBuildOutput.Type.INFO))
                
                sdkManager.installRust().collect { progress ->
                    when (progress) {
                        is InstallationProgress.Started -> 
                            emit(RustBuildOutput(progress.message, RustBuildOutput.Type.INFO))
                        is InstallationProgress.Downloading -> 
                            emit(RustBuildOutput("Downloading Rust: ${progress.progress}%", RustBuildOutput.Type.INFO))
                        is InstallationProgress.Extracting -> 
                            emit(RustBuildOutput(progress.message, RustBuildOutput.Type.INFO))
                        is InstallationProgress.Installing -> 
                            emit(RustBuildOutput(progress.message, RustBuildOutput.Type.INFO))
                        is InstallationProgress.Completed -> 
                            emit(RustBuildOutput(progress.message, RustBuildOutput.Type.SUCCESS))
                        is InstallationProgress.Failed -> 
                            emit(RustBuildOutput(progress.message, RustBuildOutput.Type.ERROR))
                    }
                }
                
                // Check again after installation
                val newCargoPath = sdkManager.getRustCargoPath()
                if (newCargoPath == null) {
                    emit(RustBuildOutput("Failed to install Rust. Cannot build project.", RustBuildOutput.Type.ERROR))
                    return@flow
                }
            }
            
            // Build command
            val cargoCmd = sdkManager.getRustCargoPath() ?: "cargo"
            val buildCmd = if (release) "build --release" else "build"
            
            emit(RustBuildOutput("Running: $cargoCmd $buildCmd", RustBuildOutput.Type.INFO))
            
            val processBuilder = ProcessBuilder(cargoCmd, *buildCmd.split(" ").toTypedArray())
            processBuilder.directory(projectDir)
            
            // Set environment variables
            val env = processBuilder.environment()
            sdkManager.getRustCargoHomePath()?.let { env["CARGO_HOME"] = it }
            sdkManager.getRustHomePath()?.let { env["RUSTUP_HOME"] = it }
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read standard output
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    val type = when {
                        outputLine.contains("error") -> RustBuildOutput.Type.ERROR
                        outputLine.contains("warning") -> RustBuildOutput.Type.WARNING
                        else -> RustBuildOutput.Type.INFO
                    }
                    emit(RustBuildOutput(outputLine, type))
                }
            }
            
            // Read error output
            while (errorReader.readLine().also { line = it } != null) {
                line?.let { errorLine ->
                    emit(RustBuildOutput(errorLine, RustBuildOutput.Type.ERROR))
                }
            }
            
            val exitCode = process.waitFor(300, TimeUnit.SECONDS) // 5 minutes timeout
            
            if (exitCode && process.exitValue() == 0) {
                emit(RustBuildOutput("Build completed successfully", RustBuildOutput.Type.SUCCESS))
                
                // Find generated artifacts
                val targetDir = File(projectDir, "target/${if (release) "release" else "debug"}")
                if (targetDir.exists()) {
                    val artifacts = targetDir.listFiles { file -> 
                        file.isFile && !file.name.endsWith(".d") && !file.name.endsWith(".rlib")
                    }
                    
                    artifacts?.forEach { artifact ->
                        emit(RustBuildOutput("Generated: ${artifact.name} (${formatFileSize(artifact.length())})", RustBuildOutput.Type.SUCCESS))
                    }
                }
            } else {
                emit(RustBuildOutput("Build failed with exit code: ${if (exitCode) process.exitValue() else "timeout"}", RustBuildOutput.Type.ERROR))
            }
            
        } catch (e: Exception) {
            emit(RustBuildOutput("Build error: ${e.message}", RustBuildOutput.Type.ERROR))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clean a Rust project
     */
    fun cleanRustProject(projectPath: String): Flow<RustBuildOutput> = flow {
        emit(RustBuildOutput("Cleaning Rust project...", RustBuildOutput.Type.INFO))
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(RustBuildOutput("Project directory does not exist: $projectPath", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Check if Rust is installed
            val cargoPath = sdkManager.getRustCargoPath()
            if (cargoPath == null) {
                emit(RustBuildOutput("Rust not found. Cannot clean project.", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Clean command
            val processBuilder = ProcessBuilder(cargoPath, "clean")
            processBuilder.directory(projectDir)
            
            // Set environment variables
            val env = processBuilder.environment()
            sdkManager.getRustCargoHomePath()?.let { env["CARGO_HOME"] = it }
            sdkManager.getRustHomePath()?.let { env["RUSTUP_HOME"] = it }
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read standard output
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    emit(RustBuildOutput(outputLine, RustBuildOutput.Type.INFO))
                }
            }
            
            // Read error output
            while (errorReader.readLine().also { line = it } != null) {
                line?.let { errorLine ->
                    emit(RustBuildOutput(errorLine, RustBuildOutput.Type.ERROR))
                }
            }
            
            val exitCode = process.waitFor(60, TimeUnit.SECONDS)
            
            if (exitCode && process.exitValue() == 0) {
                emit(RustBuildOutput("Project cleaned successfully", RustBuildOutput.Type.SUCCESS))
            } else {
                emit(RustBuildOutput("Clean failed with exit code: ${if (exitCode) process.exitValue() else "timeout"}", RustBuildOutput.Type.ERROR))
            }
            
        } catch (e: Exception) {
            emit(RustBuildOutput("Clean error: ${e.message}", RustBuildOutput.Type.ERROR))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Run tests for a Rust project
     */
    fun testRustProject(projectPath: String, release: Boolean = false): Flow<RustBuildOutput> = flow {
        emit(RustBuildOutput("Running Rust tests...", RustBuildOutput.Type.INFO))

        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(RustBuildOutput("Project directory does not exist: $projectPath", RustBuildOutput.Type.ERROR))
                return@flow
            }

            // Check if Rust is installed
            val cargoPath = sdkManager.getRustCargoPath()
            if (cargoPath == null) {
                emit(RustBuildOutput("Rust not found. Cannot run tests.", RustBuildOutput.Type.ERROR))
                return@flow
            }

            // Test command
            val testCmd = if (release) "test --release" else "test"
            val processBuilder = ProcessBuilder(cargoPath, *testCmd.split(" ").toTypedArray())
            processBuilder.directory(projectDir)

            // Set environment variables
            val env = processBuilder.environment()
            sdkManager.getRustCargoHomePath()?.let { env["CARGO_HOME"] = it }
            sdkManager.getRustHomePath()?.let { env["RUSTUP_HOME"] = it }

            val process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            // Read standard output
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    val type = when {
                        outputLine.contains("test result: ok") -> RustBuildOutput.Type.SUCCESS
                        outputLine.contains("test result: FAILED") -> RustBuildOutput.Type.ERROR
                        outputLine.contains("error") -> RustBuildOutput.Type.ERROR
                        outputLine.contains("warning") -> RustBuildOutput.Type.WARNING
                        else -> RustBuildOutput.Type.INFO
                    }
                    emit(RustBuildOutput(outputLine, type))
                }
            }

            // Read error output
            while (errorReader.readLine().also { line = it } != null) {
                line?.let { errorLine ->
                    emit(RustBuildOutput(errorLine, RustBuildOutput.Type.ERROR))
                }
            }

            val exitCode = process.waitFor(300, TimeUnit.SECONDS)

            if (exitCode && process.exitValue() == 0) {
                emit(RustBuildOutput("Tests completed successfully", RustBuildOutput.Type.SUCCESS))
            } else {
                emit(RustBuildOutput("Tests failed with exit code: ${if (exitCode) process.exitValue() else "timeout"}", RustBuildOutput.Type.ERROR))
            }

        } catch (e: Exception) {
            emit(RustBuildOutput("Test error: ${e.message}", RustBuildOutput.Type.ERROR))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Add a dependency to a Rust project
     */
    fun addRustDependency(
        projectPath: String,
        dependencyName: String,
        dependencyVersion: String = ""
    ): Flow<RustBuildOutput> = flow {
        emit(RustBuildOutput("Adding dependency: $dependencyName", RustBuildOutput.Type.INFO))
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(RustBuildOutput("Project directory does not exist: $projectPath", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            val cargoTomlFile = File(projectDir, "Cargo.toml")
            if (!cargoTomlFile.exists()) {
                emit(RustBuildOutput("Cargo.toml not found in project directory", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Read Cargo.toml
            val cargoToml = cargoTomlFile.readText()
            
            // Check if dependency already exists
            val dependencyRegex = "^\\s*$dependencyName\\s*=".toRegex(RegexOption.MULTILINE)
            if (dependencyRegex.find(cargoToml) != null) {
                emit(RustBuildOutput("Dependency $dependencyName already exists in Cargo.toml", RustBuildOutput.Type.WARNING))
                return@flow
            }
            
            // Find dependencies section
            val dependenciesRegex = "\\[dependencies]".toRegex()
            val dependenciesMatch = dependenciesRegex.find(cargoToml)
            
            if (dependenciesMatch == null) {
                // No dependencies section, add one
                val updatedCargoToml = cargoToml + "\n\n[dependencies]\n$dependencyName = ${formatDependencyVersion(dependencyVersion)}\n"
                cargoTomlFile.writeText(updatedCargoToml)
            } else {
                // Add to existing dependencies section
                val dependenciesSectionStart = dependenciesMatch.range.last + 1
                val updatedCargoToml = cargoToml.substring(0, dependenciesSectionStart) +
                        "\n$dependencyName = ${formatDependencyVersion(dependencyVersion)}" +
                        cargoToml.substring(dependenciesSectionStart)
                cargoTomlFile.writeText(updatedCargoToml)
            }
            
            emit(RustBuildOutput("Dependency $dependencyName added successfully", RustBuildOutput.Type.SUCCESS))
            
        } catch (e: Exception) {
            emit(RustBuildOutput("Failed to add dependency: ${e.message}", RustBuildOutput.Type.ERROR))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Remove a dependency from a Rust project
     */
    fun removeRustDependency(
        projectPath: String,
        dependencyName: String
    ): Flow<RustBuildOutput> = flow {
        emit(RustBuildOutput("Removing dependency: $dependencyName", RustBuildOutput.Type.INFO))
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(RustBuildOutput("Project directory does not exist: $projectPath", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            val cargoTomlFile = File(projectDir, "Cargo.toml")
            if (!cargoTomlFile.exists()) {
                emit(RustBuildOutput("Cargo.toml not found in project directory", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Read Cargo.toml
            val cargoToml = cargoTomlFile.readText()
            
            // Find and remove dependency line
            val dependencyRegex = "^\\s*$dependencyName\\s*=.*$".toRegex(RegexOption.MULTILINE)
            val updatedCargoToml = cargoToml.replace(dependencyRegex, "")
            
            if (updatedCargoToml == cargoToml) {
                emit(RustBuildOutput("Dependency $dependencyName not found in Cargo.toml", RustBuildOutput.Type.WARNING))
                return@flow
            }
            
            // Write updated Cargo.toml
            cargoTomlFile.writeText(updatedCargoToml)
            
            emit(RustBuildOutput("Dependency $dependencyName removed successfully", RustBuildOutput.Type.SUCCESS))
            
        } catch (e: Exception) {
            emit(RustBuildOutput("Failed to remove dependency: ${e.message}", RustBuildOutput.Type.ERROR))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get information about a Rust crate
     */
    suspend fun getCrateInfo(projectPath: String): Result<RustCrateInfo> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                return@withContext Result.failure(Exception("Project directory does not exist: $projectPath"))
            }
            
            val cargoTomlFile = File(projectDir, "Cargo.toml")
            if (!cargoTomlFile.exists()) {
                return@withContext Result.failure(Exception("Cargo.toml not found in project directory"))
            }
            
            // Read Cargo.toml
            val cargoToml = cargoTomlFile.readText()
            
            // Parse crate info
            val name = parseCargoTomlValue(cargoToml, "name") ?: "unknown"
            val version = parseCargoTomlValue(cargoToml, "version") ?: "0.1.0"
            val authors = parseCargoTomlArray(cargoToml, "authors")
            val description = parseCargoTomlValue(cargoToml, "description") ?: ""
            val edition = parseCargoTomlValue(cargoToml, "edition") ?: "2021"
            
            // Parse crate type
            val crateType = parseCargoTomlArray(cargoToml, "crate-type").let { crateTypes ->
                when {
                    crateTypes.contains("cdylib") -> "cdylib"
                    crateTypes.contains("staticlib") -> "staticlib"
                    crateTypes.contains("rlib") -> "rlib"
                    else -> "bin"
                }
            }
            
            // Parse dependencies
            val dependencies = parseCargoDependencies(cargoToml)
            
            // Parse features
            val features = parseCargoFeatures(cargoToml)
            
            val crateInfo = RustCrateInfo(
                name = name,
                version = version,
                authors = authors,
                description = description,
                edition = edition,
                crateType = crateType,
                dependencies = dependencies,
                features = features
            )
            
            Result.success(crateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Build a Rust project for a specific Android target
     */
    fun buildRustForAndroidTarget(
        projectPath: String,
        target: String,
        release: Boolean = false
    ): Flow<RustBuildOutput> = flow {
        emit(RustBuildOutput("Building for Android target: $target", RustBuildOutput.Type.INFO))
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(RustBuildOutput("Project directory does not exist: $projectPath", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Check if Rust is installed
            val cargoPath = sdkManager.getRustCargoPath()
            if (cargoPath == null) {
                emit(RustBuildOutput("Rust not found. Cannot build project.", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Build command for specific target
            val buildCmd = if (release) {
                "build --target $target --release"
            } else {
                "build --target $target"
            }
            
            emit(RustBuildOutput("Running: $cargoPath $buildCmd", RustBuildOutput.Type.INFO))
            
            val processBuilder = ProcessBuilder(cargoPath, *buildCmd.split(" ").toTypedArray())
            processBuilder.directory(projectDir)
            
            // Set environment variables
            val env = processBuilder.environment()
            sdkManager.getRustCargoHomePath()?.let { env["CARGO_HOME"] = it }
            sdkManager.getRustHomePath()?.let { env["RUSTUP_HOME"] = it }
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            // Read standard output
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { outputLine ->
                    val type = when {
                        outputLine.contains("error") -> RustBuildOutput.Type.ERROR
                        outputLine.contains("warning") -> RustBuildOutput.Type.WARNING
                        else -> RustBuildOutput.Type.INFO
                    }
                    emit(RustBuildOutput(outputLine, type))
                }
            }
            
            // Read error output
            while (errorReader.readLine().also { line = it } != null) {
                line?.let { errorLine ->
                    emit(RustBuildOutput(errorLine, RustBuildOutput.Type.ERROR))
                }
            }
            
            val exitCode = process.waitFor(300, TimeUnit.SECONDS)
            
            if (exitCode && process.exitValue() == 0) {
                emit(RustBuildOutput("Build for $target completed successfully", RustBuildOutput.Type.SUCCESS))
                
                // Find generated artifacts
                val targetDir = File(projectDir, "target/$target/${if (release) "release" else "debug"}")
                if (targetDir.exists()) {
                    val artifacts = targetDir.listFiles { file -> 
                        file.isFile && !file.name.endsWith(".d") && !file.name.endsWith(".rlib")
                    }
                    
                    artifacts?.forEach { artifact ->
                        emit(RustBuildOutput("Generated: ${artifact.name} (${formatFileSize(artifact.length())})", RustBuildOutput.Type.SUCCESS))
                    }
                }
            } else {
                emit(RustBuildOutput("Build for $target failed with exit code: ${if (exitCode) process.exitValue() else "timeout"}", RustBuildOutput.Type.ERROR))
            }
            
        } catch (e: Exception) {
            emit(RustBuildOutput("Build error: ${e.message}", RustBuildOutput.Type.ERROR))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate JNI bindings for Android
     */
    fun generateRustAndroidBindings(projectPath: String): Flow<RustBuildOutput> = flow {
        emit(RustBuildOutput("Generating JNI bindings for Android...", RustBuildOutput.Type.INFO))
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                emit(RustBuildOutput("Project directory does not exist: $projectPath", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Check if Rust is installed
            val cargoPath = sdkManager.getRustCargoPath()
            if (cargoPath == null) {
                emit(RustBuildOutput("Rust not found. Cannot generate bindings.", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            // Check if jni dependency is added
            val cargoTomlFile = File(projectDir, "Cargo.toml")
            if (!cargoTomlFile.exists()) {
                emit(RustBuildOutput("Cargo.toml not found in project directory", RustBuildOutput.Type.ERROR))
                return@flow
            }
            
            val cargoToml = cargoTomlFile.readText()
            if (!cargoToml.contains("jni =") && !cargoToml.contains("\"jni\"")) {
                emit(RustBuildOutput("JNI dependency not found in Cargo.toml. Adding it...", RustBuildOutput.Type.INFO))
                
                // Add jni dependency
                addRustDependency(projectPath, "jni", "0.21.1").collect { output ->
                    emit(output)
                }
            }
            
            // Create src directory if it doesn't exist
            val srcDir = File(projectDir, "src")
            if (!srcDir.exists()) {
                srcDir.mkdirs()
            }
            
            // Generate lib.rs with JNI bindings
            val libRsFile = File(srcDir, "lib.rs")
            val packageName = getPackageNameFromProject(projectPath)
            
            val libRsContent = """
                use jni::JNIEnv;
                use jni::objects::{JClass, JString};
                use jni::sys::jstring;
                
                #[no_mangle]
                pub extern "C" fn Java_${packageName.replace(".", "_")}_RustLib_getGreeting(env: JNIEnv, _class: JClass) -> jstring {
                    let output = env.new_string("Hello from Rust!").expect("Couldn't create Java string!");
                    output.into_raw()
                }
                
                #[no_mangle]
                pub extern "C" fn Java_${packageName.replace(".", "_")}_RustLib_processString(env: JNIEnv, _class: JClass, input: JString) -> jstring {
                    let input: String = env.get_string(input).expect("Couldn't get Java string!").into();
                    let output = format!("Rust processed: {}", input);
                    let output = env.new_string(output).expect("Couldn't create Java string!");
                    output.into_raw()
                }
            """.trimIndent()
            
            libRsFile.writeText(libRsContent)
            emit(RustBuildOutput("Generated JNI bindings in src/lib.rs", RustBuildOutput.Type.SUCCESS))
            
            // Update Cargo.toml to ensure it's a library with cdylib type
            if (!cargoToml.contains("[lib]") || !cargoToml.contains("crate-type")) {
                emit(RustBuildOutput("Updating Cargo.toml for library configuration...", RustBuildOutput.Type.INFO))
                
                val updatedCargoToml = if (!cargoToml.contains("[lib]")) {
                    cargoToml + """
                        
                        [lib]
                        crate-type = ["cdylib", "staticlib", "rlib"]
                    """.trimIndent()
                } else {
                    cargoToml
                }
                
                cargoTomlFile.writeText(updatedCargoToml)
                emit(RustBuildOutput("Updated Cargo.toml with library configuration", RustBuildOutput.Type.SUCCESS))
            }
            
            // Generate Java wrapper class
            val javaDir = File(projectDir, "android/src/main/java/${packageName.replace(".", "/")}")
            javaDir.mkdirs()
            
            val javaWrapperFile = File(javaDir, "RustLib.java")
            val javaWrapperContent = """
                package $packageName;
                
                import androidx.annotation.NonNull;
                
                /**
                 * Java wrapper for Rust library
                 */
                public class RustLib {
                    
                    static {
                        System.loadLibrary("rust-lib");
                    }
                    
                    /**
                     * Example method that calls into Rust code
                     * @return String returned from Rust
                     */
                    @NonNull
                    public static native String getGreeting();
                    
                    /**
                     * Example method that passes data to Rust
                     * @param input String to process
                     * @return Processed string from Rust
                     */
                    @NonNull
                    public static native String processString(@NonNull String input);
                }
            """.trimIndent()
            
            javaWrapperFile.writeText(javaWrapperContent)
            emit(RustBuildOutput("Generated Java wrapper class: RustLib.java", RustBuildOutput.Type.SUCCESS))
            
            // Create .cargo directory and config.toml for Android targets
            val cargoConfigDir = File(projectDir, ".cargo")
            cargoConfigDir.mkdirs()
            
            val cargoConfig = """
                [target.aarch64-linux-android]
                ar = "aarch64-linux-android-ar"
                linker = "aarch64-linux-android-clang"
                
                [target.armv7-linux-androideabi]
                ar = "arm-linux-androideabi-ar"
                linker = "arm-linux-androideabi-clang"
                
                [target.i686-linux-android]
                ar = "i686-linux-android-ar"
                linker = "i686-linux-android-clang"
                
                [target.x86_64-linux-android]
                ar = "x86_64-linux-android-ar"
                linker = "x86_64-linux-android-clang"
            """.trimIndent()
            
            File(cargoConfigDir, "config.toml").writeText(cargoConfig)
            emit(RustBuildOutput("Created .cargo/config.toml for Android targets", RustBuildOutput.Type.SUCCESS))
            
            // Build the project to verify bindings
            emit(RustBuildOutput("Building project to verify bindings...", RustBuildOutput.Type.INFO))
            
            buildRustProject(projectPath, "debug").collect { output ->
                emit(output)
            }
            
            emit(RustBuildOutput("JNI bindings generation completed", RustBuildOutput.Type.SUCCESS))
            
        } catch (e: Exception) {
            emit(RustBuildOutput("Failed to generate JNI bindings: ${e.message}", RustBuildOutput.Type.ERROR))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Parse Cargo.toml to extract a value
     */
    private fun parseCargoTomlValue(cargoToml: String, key: String): String? {
        val regex = "^\\s*$key\\s*=\\s*\"([^\"]+)\"".toRegex(RegexOption.MULTILINE)
        val match = regex.find(cargoToml)
        return match?.groupValues?.get(1)
    }
    
    /**
     * Parse Cargo.toml to extract an array
     */
    private fun parseCargoTomlArray(cargoToml: String, key: String): List<String> {
        val regex = "^\\s*$key\\s*=\\s*\\[([^]]+)]".toRegex(RegexOption.MULTILINE)
        val match = regex.find(cargoToml)
        
        return match?.groupValues?.get(1)?.split(",")?.map { it.trim().removeSurrounding("\"") } ?: emptyList()
    }
    
    /**
     * Parse Cargo.toml to extract dependencies
     */
    private fun parseCargoDependencies(cargoToml: String): List<RustDependency> {
        val dependencies = mutableListOf<RustDependency>()
        
        // Find dependencies section
        val dependenciesSectionRegex = "\\[dependencies](.*?)(?=\\[\\w+]|$)".toRegex(RegexOption.DOT_MATCHES_ALL)
        val dependenciesSection = dependenciesSectionRegex.find(cargoToml)?.groupValues?.get(1) ?: return dependencies
        
        // Simple dependencies (name = "version")
        val simpleDepRegex = "^\\s*(\\w+)\\s*=\\s*\"([^\"]+)\"".toRegex(RegexOption.MULTILINE)
        simpleDepRegex.findAll(dependenciesSection).forEach { match ->
            val name = match.groupValues[1]
            val version = match.groupValues[2]
            dependencies.add(RustDependency(name, version))
        }
        
        // Complex dependencies (name = { version = "version", features = ["feature1", "feature2"] })
        val complexDepRegex = "^\\s*(\\w+)\\s*=\\s*\\{([^}]+)\\}".toRegex(RegexOption.MULTILINE)
        complexDepRegex.findAll(dependenciesSection).forEach { match ->
            val name = match.groupValues[1]
            val details = match.groupValues[2]
            
            val versionRegex = "version\\s*=\\s*\"([^\"]+)\"".toRegex()
            val version = versionRegex.find(details)?.groupValues?.get(1) ?: ""
            
            val featuresRegex = "features\\s*=\\s*\\[([^]]+)]".toRegex()
            val featuresMatch = featuresRegex.find(details)
            val features = featuresMatch?.groupValues?.get(1)?.split(",")?.map { it.trim().removeSurrounding("\"") } ?: emptyList()
            
            dependencies.add(RustDependency(name, version, features))
        }
        
        return dependencies
    }
    
    /**
     * Parse Cargo.toml to extract features
     */
    private fun parseCargoFeatures(cargoToml: String): List<String> {
        val featuresSectionRegex = "\\[features](.*?)(?=\\[\\w+]|$)".toRegex(RegexOption.DOT_MATCHES_ALL)
        val featuresSection = featuresSectionRegex.find(cargoToml)?.groupValues?.get(1) ?: return emptyList()
        
        val featureRegex = "^\\s*(\\w+)\\s*=".toRegex(RegexOption.MULTILINE)
        return featureRegex.findAll(featuresSection).map { it.groupValues[1] }.toList()
    }
    
    /**
     * Format a dependency version for Cargo.toml
     */
    private fun formatDependencyVersion(version: String): String {
        return if (version.isBlank()) {
            "\"*\""
        } else if (version.contains("{") || version.contains("}")) {
            version
        } else {
            "\"$version\""
        }
    }
    
    /**
     * Format file size for display
     */
    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        
        return when {
            mb >= 1 -> "%.1f MB".format(mb)
            kb >= 1 -> "%.1f KB".format(kb)
            else -> "$bytes bytes"
        }
    }
    
    /**
     * Get package name from project
     */
    private fun getPackageNameFromProject(projectPath: String): String {
        // Try to find package name in Android project
        val androidDir = File(projectPath, "android")
        if (androidDir.exists()) {
            val manifestFile = File(androidDir, "src/main/AndroidManifest.xml")
            if (manifestFile.exists()) {
                val manifestContent = manifestFile.readText()
                val packageRegex = "package=\"([^\"]+)\"".toRegex()
                val packageMatch = packageRegex.find(manifestContent)
                if (packageMatch != null) {
                    return packageMatch.groupValues[1]
                }
            }
        }
        
        // Default package name based on project name
        val projectName = File(projectPath).name.lowercase().replace("-", "_")
        return "com.example.$projectName"
    }
}

/**
 * Data class for Rust build output
 */
data class RustBuildOutput(
    val message: String,
    val type: Type
) {
    enum class Type {
        INFO,
        WARNING,
        ERROR,
        SUCCESS
    }
}

/**
 * Data class for Rust crate information
 */
data class RustCrateInfo(
    val name: String,
    val version: String,
    val authors: List<String>,
    val description: String,
    val edition: String,
    val crateType: String,
    val dependencies: List<RustDependency>,
    val features: List<String>
)

/**
 * Data class for Rust dependency
 */
data class RustDependency(
    val name: String,
    val version: String,
    val features: List<String> = emptyList()
)

/**
 * Create a new Rust project
 */
fun createRustProject(
    rustBuildManager: RustBuildManager, projectName: String,
    projectPath: String,
    template: String = "bin",
    isAndroidLib: Boolean = false
): Flow<RustBuildOutput> = flow {
    emit(RustBuildOutput("Creating new Rust project: $projectName", RustBuildOutput.Type.INFO))

    try {
        // Check if Rust is installed
        val cargoPath = rustBuildManager.sdkManager.getRustCargoPath()
        if (cargoPath == null) {
            emit(RustBuildOutput("Rust not found. Installing...", RustBuildOutput.Type.INFO))

            rustBuildManager.sdkManager.installRust().collect { progress ->
                when (progress) {
                    is InstallationProgress.Started ->
                        emit(RustBuildOutput(progress.message, RustBuildOutput.Type.INFO))
                    is InstallationProgress.Downloading ->
                        emit(RustBuildOutput("Downloading Rust: ${progress.progress}%", RustBuildOutput.Type.INFO))
                    is InstallationProgress.Extracting ->
                        emit(RustBuildOutput(progress.message, RustBuildOutput.Type.INFO))
                    is InstallationProgress.Installing ->
                        emit(RustBuildOutput(progress.message, RustBuildOutput.Type.INFO))
                    is InstallationProgress.Completed ->
                        emit(RustBuildOutput(progress.message, RustBuildOutput.Type.SUCCESS))
                    is InstallationProgress.Failed ->
                        emit(RustBuildOutput(progress.message, RustBuildOutput.Type.ERROR))
                }
            }

            // Check again after installation
            val newCargoPath = rustBuildManager.sdkManager.getRustCargoPath()
            if (newCargoPath == null) {
                emit(RustBuildOutput("Failed to install Rust. Cannot create project.", RustBuildOutput.Type.ERROR))
                return@flow
            }
        }

        // Create project directory
        val projectDir = File(projectPath, projectName)
        if (projectDir.exists()) {
            emit(RustBuildOutput("Project directory already exists: ${projectDir.absolutePath}", RustBuildOutput.Type.ERROR))
            return@flow
        }

        projectDir.mkdirs()

        // Create new Rust project
        val cargoCmd = rustBuildManager.sdkManager.getRustCargoPath() ?: "cargo"
        val newCmd = "new --$template ${projectDir.absolutePath}"

        emit(RustBuildOutput("Running: $cargoCmd $newCmd", RustBuildOutput.Type.INFO))

        val processBuilder = ProcessBuilder(cargoCmd, *newCmd.split(" ").toTypedArray())

        // Set environment variables
        val env = processBuilder.environment()
        rustBuildManager.sdkManager.getRustCargoHomePath()?.let { env["CARGO_HOME"] = it }
        rustBuildManager.sdkManager.getRustHomePath()?.let { env["RUSTUP_HOME"] = it }

        val process = processBuilder.start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))

        // Read standard output
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            line?.let { outputLine ->
                emit(RustBuildOutput(outputLine, RustBuildOutput.Type.INFO))
            }
        }

        // Read error output
        while (errorReader.readLine().also { line = it } != null) {
            line?.let { errorLine ->
                emit(RustBuildOutput(errorLine, RustBuildOutput.Type.ERROR))
            }
        }

        val exitCode = process.waitFor(60, TimeUnit.SECONDS)

        if (exitCode && process.exitValue() == 0) {
            emit(RustBuildOutput("Rust project created successfully", RustBuildOutput.Type.SUCCESS))

            // If Android library, set up additional configuration
            if (isAndroidLib) {
                emit(RustBuildOutput("Setting up Android library configuration...", RustBuildOutput.Type.INFO))

                // Update Cargo.toml for Android library
                val cargoTomlFile = File(projectDir, "Cargo.toml")
                if (cargoTomlFile.exists()) {
                    var cargoToml = cargoTomlFile.readText()

                    // Add crate-type for Android
                    if (!cargoToml.contains("[lib]")) {
                        cargoToml += """
                            
                            [lib]
                            crate-type = ["cdylib", "staticlib", "rlib"]
                        """.trimIndent()
                    } else if (!cargoToml.contains("crate-type")) {
                        cargoToml = cargoToml.replace(
                            "[lib]",
                            "[lib]\ncrate-type = [\"cdylib\", \"staticlib\", \"rlib\"]"
                        )
                    }

                    // Add jni dependency
                    if (!cargoToml.contains("jni =") && !cargoToml.contains("\"jni\"")) {
                        if (cargoToml.contains("[dependencies]")) {
                            cargoToml = cargoToml.replace(
                                "[dependencies]",
                                "[dependencies]\njni = { version = \"0.21.1\", features = [\"invocation\"] }"
                            )
                        } else {
                            cargoToml += """
                                
                                [dependencies]
                                jni = { version = "0.21.1", features = ["invocation"] }
                            """.trimIndent()
                        }
                    }

                    cargoTomlFile.writeText(cargoToml)
                    emit(RustBuildOutput("Updated Cargo.toml for Android library", RustBuildOutput.Type.SUCCESS))
                }

                // Create Android directory structure
                val androidDir = File(projectDir, "android")
                androidDir.mkdirs()

                val androidSrcDir = File(androidDir, "src/main/java")
                androidSrcDir.mkdirs()

                // Create .cargo directory and config.toml for Android targets
                val cargoConfigDir = File(projectDir, ".cargo")
                cargoConfigDir.mkdirs()

                val cargoConfig = """
                    [target.aarch64-linux-android]
                    ar = "aarch64-linux-android-ar"
                    linker = "aarch64-linux-android-clang"
                    
                    [target.armv7-linux-androideabi]
                    ar = "arm-linux-androideabi-ar"
                    linker = "arm-linux-androideabi-clang"
                    
                    [target.i686-linux-android]
                    ar = "i686-linux-android-ar"
                    linker = "i686-linux-android-clang"
                    
                    [target.x86_64-linux-android]
                    ar = "x86_64-linux-android-ar"
                    linker = "x86_64-linux-android-clang"
                """.trimIndent()

                File(cargoConfigDir, "config.toml").writeText(cargoConfig)
                emit(RustBuildOutput("Created .cargo/config.toml for Android targets", RustBuildOutput.Type.SUCCESS))

                // Generate JNI bindings
                rustBuildManager.generateRustAndroidBindings(projectDir.absolutePath)
                    .collect { output ->
                    emit(output)
                }
            }
        } else {
            emit(RustBuildOutput("Failed to create Rust project with exit code: ${if (exitCode) process.exitValue() else "timeout"}", RustBuildOutput.Type.ERROR))
        }

    } catch (e: Exception) {
        emit(RustBuildOutput("Failed to create Rust project: ${e.message}", RustBuildOutput.Type.ERROR))
    }
}.flowOn(Dispatchers.IO)