package com.anyoneide.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Plugin to integrate Rust builds with the Android build process
 */
class RustBuildPlugin(private val context: Context) {
    
    companion object {
        private const val TAG = "RustBuildPlugin"
    }
    
    private val sdkManager = SDKManager(context)
    private val fileManager = FileManager(context)
    
    /**
     * Configure a project for Rust Android integration
     */
    fun configureProject(projectPath: String): Flow<String> = flow {
        emit("Configuring project for Rust Android integration...")
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                emit("ERROR: Project directory does not exist: $projectPath")
                return@flow
            }
            
            // Create .cargo directory and config.toml
            val cargoConfigDir = File(projectDir, ".cargo")
            if (!cargoConfigDir.exists()) {
                cargoConfigDir.mkdirs()
                emit("Created .cargo directory")
            }
            
            val configToml = """
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
            
            val configTomlFile = File(cargoConfigDir, "config.toml")
            configTomlFile.writeText(configToml)
            emit("Created .cargo/config.toml")
            
            // Check if Cargo.toml exists
            val cargoTomlFile = File(projectDir, "Cargo.toml")
            if (!cargoTomlFile.exists()) {
                emit("WARNING: Cargo.toml not found. Creating a basic one...")
                
                val cargoToml = """
                    [package]
                    name = "rust_android_lib"
                    version = "0.1.0"
                    edition = "2021"
                    
                    [lib]
                    crate-type = ["cdylib", "staticlib", "rlib"]
                    
                    [dependencies]
                    jni = { version = "0.21.1", features = ["invocation"] }
                """.trimIndent()
                
                cargoTomlFile.writeText(cargoToml)
                emit("Created Cargo.toml")
            } else {
                // Check if lib section exists
                val cargoTomlContent = cargoTomlFile.readText()
                if (!cargoTomlContent.contains("[lib]")) {
                    emit("Adding [lib] section to Cargo.toml...")
                    
                    val updatedContent = """
                        $cargoTomlContent
                        
                        [lib]
                        crate-type = ["cdylib", "staticlib", "rlib"]
                    """.trimIndent()
                    
                    cargoTomlFile.writeText(updatedContent)
                    emit("Added [lib] section to Cargo.toml")
                } else if (!cargoTomlContent.contains("crate-type")) {
                    emit("Adding crate-type to [lib] section in Cargo.toml...")
                    
                    val updatedContent = cargoTomlContent.replace(
                        "[lib]",
                        "[lib]\ncrate-type = [\"cdylib\", \"staticlib\", \"rlib\"]"
                    )
                    
                    cargoTomlFile.writeText(updatedContent)
                    emit("Added crate-type to [lib] section in Cargo.toml")
                }
                
                // Check if jni dependency exists
                if (!cargoTomlContent.contains("jni =") && !cargoTomlContent.contains("\"jni\"")) {
                    emit("Adding jni dependency to Cargo.toml...")
                    
                    val updatedContent = if (cargoTomlContent.contains("[dependencies]")) {
                        cargoTomlContent.replace(
                            "[dependencies]",
                            "[dependencies]\njni = { version = \"0.21.1\", features = [\"invocation\"] }"
                        )
                    } else {
                        """
                            $cargoTomlContent
                            
                            [dependencies]
                            jni = { version = "0.21.1", features = ["invocation"] }
                        """.trimIndent()
                    }
                    
                    cargoTomlFile.writeText(updatedContent)
                    emit("Added jni dependency to Cargo.toml")
                }
            }
            
            // Create src directory if it doesn't exist
            val srcDir = File(projectDir, "src")
            if (!srcDir.exists()) {
                srcDir.mkdirs()
                emit("Created src directory")
            }
            
            // Check if lib.rs exists
            val libRsFile = File(srcDir, "lib.rs")
            if (!libRsFile.exists()) {
                emit("WARNING: lib.rs not found. Creating a basic one...")
                
                val libRs = """
                    use jni::JNIEnv;
                    use jni::objects::{JClass, JString};
                    use jni::sys::jstring;
                    
                    #[no_mangle]
                    pub extern "C" fn Java_com_example_RustLib_getGreeting(env: JNIEnv, _class: JClass) -> jstring {
                        let output = env.new_string("Hello from Rust!")
                            .expect("Couldn't create Java string!");
                        output.into_raw()
                    }
                    
                    #[no_mangle]
                    pub extern "C" fn Java_com_example_RustLib_processString(env: JNIEnv, _class: JClass, input: JString) -> jstring {
                        let input: String = env.get_string(input)
                            .expect("Couldn't get Java string!")
                            .into();
                        let output = format!("Rust processed: {}", input);
                        let output = env.new_string(output)
                            .expect("Couldn't create Java string!");
                        output.into_raw()
                    }
                """.trimIndent()
                
                libRsFile.writeText(libRs)
                emit("Created src/lib.rs")
            }
            
            // Check if build.gradle exists in app directory
            val appDir = File(projectDir, "app")
            if (appDir.exists()) {
                val buildGradleFile = File(appDir, "build.gradle")
                if (buildGradleFile.exists()) {
                    emit("Checking app/build.gradle for Rust integration...")
                    
                    val buildGradleContent = buildGradleFile.readText()
                    
                    // Check if jniLibs is configured
                    if (!buildGradleContent.contains("jniLibs")) {
                        emit("Adding jniLibs configuration to app/build.gradle...")
                        
                        val updatedContent = if (buildGradleContent.contains("android {")) {
                            buildGradleContent.replace(
                                "android {",
                                """
                                android {
                                    // Load native libraries from the Rust project
                                    sourceSets {
                                        main {
                                            jniLibs.srcDirs = ['../target/debug', '../target/release']
                                        }
                                    }
                                    
                                    // Task to build Rust library before building Android app
                                    tasks.whenTaskAdded { task ->
                                        if (task.name == 'preBuild') {
                                            task.dependsOn 'buildRustLibrary'
                                        }
                                    }
                                """.trimIndent()
                            )
                        } else {
                            buildGradleContent
                        }
                        
                        // Add buildRustLibrary task if it doesn't exist
                        val finalContent = if (!updatedContent.contains("task buildRustLibrary")) {
                            """
                                $updatedContent
                                
                                // Task to build Rust library
                                task buildRustLibrary(type: Exec) {
                                    workingDir '..'
                                    commandLine 'cargo', 'build', '--release'
                                }
                            """.trimIndent()
                        } else {
                            updatedContent
                        }
                        
                        buildGradleFile.writeText(finalContent)
                        emit("Updated app/build.gradle with Rust integration")
                    }
                }
            }
            
            emit("Project configured for Rust Android integration")
            
        } catch (e: Exception) {
            emit("ERROR: Failed to configure project: ${e.message}")
            Log.e(TAG, "Error configuring project", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Install Rust Android targets
     */
    fun installAndroidTargets(): Flow<String> = flow {
        emit("Installing Rust Android targets...")
        
        try {
            // Find rustup
            val rustupPath = sdkManager.getRustCargoPath()?.let { cargoPath ->
                File(cargoPath).parentFile?.resolve("rustup")?.absolutePath
            }
            
            if (rustupPath == null) {
                emit("ERROR: Rustup not found. Please install Rust.")
                return@flow
            }
            
            val androidTargets = listOf(
                "aarch64-linux-android",
                "armv7-linux-androideabi",
                "i686-linux-android",
                "x86_64-linux-android"
            )
            
            for (target in androidTargets) {
                emit("Installing target: $target")
                
                val process = ProcessBuilder(rustupPath, "target", "add", target)
                    .redirectErrorStream(true)
                    .start()
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line
                    if (currentLine != null) {
                        emit(currentLine)
                    }
                }
                
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    emit("WARNING: Failed to install target: $target (exit code: $exitCode)")
                } else {
                    emit("Successfully installed target: $target")
                }
            }
            
            emit("Rust Android targets installation completed")
            
        } catch (e: Exception) {
            emit("ERROR: Failed to install Rust Android targets: ${e.message}")
            Log.e(TAG, "Error installing Rust Android targets", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Build Rust library for Android
     */
    fun buildRustLibrary(projectPath: String, release: Boolean = false): Flow<String> = flow {
        emit("Building Rust library for Android...")
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                emit("ERROR: Project directory does not exist: $projectPath")
                return@flow
            }
            
            // Find cargo
            val cargoPath = sdkManager.getRustCargoPath()
            if (cargoPath == null) {
                emit("ERROR: Cargo not found. Please install Rust.")
                return@flow
            }
            
            // Build for all Android targets
            val androidTargets = listOf(
                "aarch64-linux-android",
                "armv7-linux-androideabi",
                "i686-linux-android",
                "x86_64-linux-android"
            )
            
            for (target in androidTargets) {
                emit("Building for target: $target")
                
                val buildArgs = mutableListOf("build", "--target", target)
                if (release) {
                    buildArgs.add("--release")
                }
                
                val process = ProcessBuilder(cargoPath, *buildArgs.toTypedArray())
                    .directory(projectDir)
                    .redirectErrorStream(true)
                    .start()
                
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line
                    if (currentLine != null) {
                        emit(currentLine)
                    }
                }
                
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    emit("ERROR: Failed to build for target: $target (exit code: $exitCode)")
                } else {
                    emit("Successfully built for target: $target")
                    
                    // Find generated libraries
                    val targetDir = File(projectDir, "target/$target/${if (release) "release" else "debug"}")
                    if (targetDir.exists()) {
                        val libs = targetDir.listFiles { file -> 
                            file.isFile && (file.name.endsWith(".so") || file.name.endsWith(".a"))
                        }
                        
                        if (libs != null && libs.isNotEmpty()) {
                            for (lib in libs) {
                                emit("Generated library: ${lib.absolutePath}")
                            }
                        }
                    }
                }
            }
            
            // Copy libraries to jniLibs directory if app directory exists
            val appDir = File(projectDir, "app")
            if (appDir.exists()) {
                emit("Copying libraries to jniLibs directory...")
                
                val jniLibsDir = File(appDir, "src/main/jniLibs")
                if (!jniLibsDir.exists()) {
                    jniLibsDir.mkdirs()
                }
                
                val targetMapping = mapOf(
                    "aarch64-linux-android" to "arm64-v8a",
                    "armv7-linux-androideabi" to "armeabi-v7a",
                    "i686-linux-android" to "x86",
                    "x86_64-linux-android" to "x86_64"
                )
                
                for ((rustTarget, androidAbi) in targetMapping) {
                    val sourceDir = File(projectDir, "target/$rustTarget/${if (release) "release" else "debug"}")
                    val destDir = File(jniLibsDir, androidAbi)
                    
                    if (sourceDir.exists()) {
                        destDir.mkdirs()
                        
                        val libs = sourceDir.listFiles { file -> 
                            file.isFile && file.name.endsWith(".so")
                        }
                        
                        if (libs != null && libs.isNotEmpty()) {
                            for (lib in libs) {
                                val destFile = File(destDir, lib.name)
                                lib.copyTo(destFile, overwrite = true)
                                emit("Copied ${lib.name} to ${destFile.absolutePath}")
                            }
                        }
                    }
                }
            }
            
            emit("Rust library build completed")
            
        } catch (e: Exception) {
            emit("ERROR: Failed to build Rust library: ${e.message}")
            Log.e(TAG, "Error building Rust library", e)
        }
    }.flowOn(Dispatchers.IO)
}