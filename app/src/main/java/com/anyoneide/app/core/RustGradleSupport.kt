package com.anyoneide.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Support class for integrating Rust with Gradle
 * Provides utilities for building Rust libraries and integrating them with Android projects
 */
class RustGradleSupport(private val context: Context) {
    
    companion object {
        private const val TAG = "RustGradleSupport"
    }
    
    private val sdkManager = SDKManager(context)
    private val fileManager = FileManager(context)
    
    /**
     * Generate a build.gradle file for a Rust Android library
     */
    suspend fun generateGradleBuildFile(
        projectPath: String,
        packageName: String,
        libraryName: String
    ): Flow<String> = flow {
        emit("Generating Gradle build file for Rust Android library...")
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                emit("ERROR: Project directory does not exist: $projectPath")
                return@flow
            }
            
            // Create app directory if it doesn't exist
            val appDir = File(projectDir, "app")
            if (!appDir.exists()) {
                appDir.mkdirs()
                emit("Created app directory")
            }
            
            // Create build.gradle file
            val buildGradleContent = """
                plugins {
                    id 'com.android.library'
                    id 'org.jetbrains.kotlin.android'
                }
                
                android {
                    namespace '$packageName'
                    compileSdk 34
                    
                    defaultConfig {
                        minSdk 26
                        targetSdk 34
                        
                        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
                        consumerProguardFiles "consumer-rules.pro"
                        
                        ndk {
                            abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'
                        }
                    }
                    
                    buildTypes {
                        release {
                            minifyEnabled false
                            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                        }
                    }
                    
                    compileOptions {
                        sourceCompatibility JavaVersion.VERSION_1_8
                        targetCompatibility JavaVersion.VERSION_1_8
                    }
                    
                    kotlinOptions {
                        jvmTarget = '1.8'
                    }
                    
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
                }
                
                // Task to build Rust library
                task buildRustLibrary(type: Exec) {
                    workingDir '..'
                    commandLine 'cargo', 'build', '--release'
                }
                
                dependencies {
                    implementation 'androidx.core:core-ktx:1.12.0'
                    implementation 'androidx.appcompat:appcompat:1.6.1'
                    implementation 'com.google.android.material:material:1.11.0'
                    testImplementation 'junit:junit:4.13.2'
                    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
                    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
                }
            """.trimIndent()
            
            val buildGradleFile = File(appDir, "build.gradle")
            buildGradleFile.writeText(buildGradleContent)
            emit("Created app/build.gradle")
            
            // Create settings.gradle file if it doesn't exist
            val settingsGradleFile = File(projectDir, "settings.gradle")
            if (!settingsGradleFile.exists()) {
                val settingsGradleContent = """
                    rootProject.name = "${projectDir.name}"
                    include ':app'
                """.trimIndent()
                
                settingsGradleFile.writeText(settingsGradleContent)
                emit("Created settings.gradle")
            }
            
            // Create root build.gradle file if it doesn't exist
            val rootBuildGradleFile = File(projectDir, "build.gradle")
            if (!rootBuildGradleFile.exists()) {
                val rootBuildGradleContent = """
                    // Top-level build file where you can add configuration options common to all sub-projects/modules.
                    plugins {
                        id 'com.android.application' version '8.1.4' apply false
                        id 'com.android.library' version '8.1.4' apply false
                        id 'org.jetbrains.kotlin.android' version '1.9.20' apply false
                    }
                    
                    task clean(type: Delete) {
                        delete rootProject.buildDir
                    }
                    
                    // Task to build Rust library
                    task buildRust(type: Exec) {
                        workingDir '.'
                        commandLine 'cargo', 'build', '--release'
                    }
                """.trimIndent()
                
                rootBuildGradleFile.writeText(rootBuildGradleContent)
                emit("Created root build.gradle")
            }
            
            // Create gradle.properties file if it doesn't exist
            val gradlePropertiesFile = File(projectDir, "gradle.properties")
            if (!gradlePropertiesFile.exists()) {
                val gradlePropertiesContent = """
                    # Project-wide Gradle settings.
                    org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
                    android.useAndroidX=true
                    kotlin.code.style=official
                    android.nonTransitiveRClass=true
                    
                    # Rust-specific settings
                    rust.cargoCommand=cargo
                    rust.targets=arm64-v8a,armeabi-v7a,x86,x86_64
                """.trimIndent()
                
                gradlePropertiesFile.writeText(gradlePropertiesContent)
                emit("Created gradle.properties")
            }
            
            // Create Java wrapper class
            val javaDir = File(appDir, "src/main/java/${packageName.replace(".", "/")}")
            javaDir.mkdirs()
            
            val javaWrapperContent = """
                package $packageName;
                
                import androidx.annotation.NonNull;
                
                /**
                 * Java wrapper for Rust library
                 */
                public class $libraryName {
                    
                    static {
                        System.loadLibrary("${libraryName.toLowerCase()}");
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
            
            val javaWrapperFile = File(javaDir, "$libraryName.java")
            javaWrapperFile.writeText(javaWrapperContent)
            emit("Created Java wrapper class: $libraryName.java")
            
            // Create AndroidManifest.xml
            val manifestDir = File(appDir, "src/main")
            manifestDir.mkdirs()
            
            val manifestContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                </manifest>
            """.trimIndent()
            
            val manifestFile = File(manifestDir, "AndroidManifest.xml")
            manifestFile.writeText(manifestContent)
            emit("Created AndroidManifest.xml")
            
            // Create proguard-rules.pro
            val proguardContent = """
                # Add project specific ProGuard rules here.
                # You can control the set of applied configuration files using the
                # proguardFiles setting in build.gradle.
                #
                # For more details, see
                #   http://developer.android.com/guide/developing/tools/proguard.html
            """.trimIndent()
            
            val proguardFile = File(appDir, "proguard-rules.pro")
            proguardFile.writeText(proguardContent)
            emit("Created proguard-rules.pro")
            
            // Create consumer-rules.pro
            val consumerRulesFile = File(appDir, "consumer-rules.pro")
            consumerRulesFile.writeText("# Consumer rules for the library")
            emit("Created consumer-rules.pro")
            
            emit("Gradle build files generated successfully")
            
        } catch (e: Exception) {
            emit("ERROR: Failed to generate Gradle build files: ${e.message}")
            Log.e(TAG, "Error generating Gradle build files", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate a Cargo.toml file for a Rust Android library
     */
    suspend fun generateCargoToml(
        projectPath: String,
        libraryName: String
    ): Flow<String> = flow {
        emit("Generating Cargo.toml for Rust Android library...")
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                emit("ERROR: Project directory does not exist: $projectPath")
                return@flow
            }
            
            // Create Cargo.toml file
            val cargoTomlContent = """
                [package]
                name = "${libraryName.toLowerCase()}"
                version = "0.1.0"
                edition = "2021"
                authors = ["Anyone IDE User"]
                
                [lib]
                crate-type = ["cdylib", "staticlib", "rlib"]
                
                [dependencies]
                jni = { version = "0.21.1", features = ["invocation"] }
                
                [profile.release]
                lto = true
                codegen-units = 1
                opt-level = 3
                strip = true
            """.trimIndent()
            
            val cargoTomlFile = File(projectDir, "Cargo.toml")
            cargoTomlFile.writeText(cargoTomlContent)
            emit("Created Cargo.toml")
            
            // Create .cargo directory and config.toml for Android targets
            val cargoConfigDir = File(projectDir, ".cargo")
            cargoConfigDir.mkdirs()
            
            val cargoConfigContent = """
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
            
            val cargoConfigFile = File(cargoConfigDir, "config.toml")
            cargoConfigFile.writeText(cargoConfigContent)
            emit("Created .cargo/config.toml")
            
            emit("Cargo.toml generated successfully")
            
        } catch (e: Exception) {
            emit("ERROR: Failed to generate Cargo.toml: ${e.message}")
            Log.e(TAG, "Error generating Cargo.toml", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate a lib.rs file with JNI bindings
     */
    suspend fun generateLibRs(
        projectPath: String,
        packageName: String,
        libraryName: String
    ): Flow<String> = flow {
        emit("Generating lib.rs with JNI bindings...")
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                emit("ERROR: Project directory does not exist: $projectPath")
                return@flow
            }
            
            // Create src directory if it doesn't exist
            val srcDir = File(projectDir, "src")
            if (!srcDir.exists()) {
                srcDir.mkdirs()
                emit("Created src directory")
            }
            
            // Create lib.rs file
            val libRsContent = """
                use jni::JNIEnv;
                use jni::objects::{JClass, JString};
                use jni::sys::jstring;
                
                #[no_mangle]
                pub extern "C" fn Java_${packageName.replace(".", "_")}_${libraryName}_getGreeting(env: JNIEnv, _class: JClass) -> jstring {
                    let output = env.new_string("Hello from Rust!")
                        .expect("Couldn't create Java string!");
                    output.into_raw()
                }
                
                #[no_mangle]
                pub extern "C" fn Java_${packageName.replace(".", "_")}_${libraryName}_processString(env: JNIEnv, _class: JClass, input: JString) -> jstring {
                    let input: String = env.get_string(input)
                        .expect("Couldn't get Java string!")
                        .into();
                    let output = format!("Rust processed: {}", input);
                    let output = env.new_string(output)
                        .expect("Couldn't create Java string!");
                    output.into_raw()
                }
            """.trimIndent()
            
            val libRsFile = File(srcDir, "lib.rs")
            libRsFile.writeText(libRsContent)
            emit("Created src/lib.rs with JNI bindings")
            
        } catch (e: Exception) {
            emit("ERROR: Failed to generate lib.rs: ${e.message}")
            Log.e(TAG, "Error generating lib.rs", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Create a complete Rust Android library project
     */
    suspend fun createRustAndroidLibraryProject(
        projectPath: String,
        packageName: String,
        libraryName: String
    ): Flow<String> = flow {
        emit("Creating Rust Android library project...")
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                projectDir.mkdirs()
                emit("Created project directory: $projectPath")
            }
            
            // Generate Cargo.toml
            generateCargoToml(projectPath, libraryName).collect { message ->
                emit(message)
            }
            
            // Generate lib.rs
            generateLibRs(projectPath, packageName, libraryName).collect { message ->
                emit(message)
            }
            
            // Generate Gradle build files
            generateGradleBuildFile(projectPath, packageName, libraryName).collect { message ->
                emit(message)
            }
            
            // Create .gitignore
            val gitignoreContent = """
                # Rust
                /target
                Cargo.lock
                
                # Gradle
                .gradle
                build/
                
                # Android
                /local.properties
                /.idea
                *.iml
                .DS_Store
                /captures
                .externalNativeBuild
                .cxx
                
                # Generated files
                bin/
                gen/
                out/
            """.trimIndent()
            
            val gitignoreFile = File(projectDir, ".gitignore")
            gitignoreFile.writeText(gitignoreContent)
            emit("Created .gitignore")
            
            // Create README.md
            val readmeContent = """
                # $libraryName
                
                A Rust library for Android with JNI bindings.
                
                ## Building
                
                To build the Rust library:
                
                ```
                cargo build --release
                ```
                
                To build the Android library:
                
                ```
                ./gradlew build
                ```
                
                ## Usage
                
                ```kotlin
                // Kotlin
                val greeting = $libraryName.getGreeting()
                val processed = $libraryName.processString("Hello from Kotlin!")
                ```
                
                ```java
                // Java
                String greeting = $libraryName.getGreeting();
                String processed = $libraryName.processString("Hello from Java!");
                ```
            """.trimIndent()
            
            val readmeFile = File(projectDir, "README.md")
            readmeFile.writeText(readmeContent)
            emit("Created README.md")
            
            emit("Rust Android library project created successfully")
            
        } catch (e: Exception) {
            emit("ERROR: Failed to create Rust Android library project: ${e.message}")
            Log.e(TAG, "Error creating Rust Android library project", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Build a Rust Android library
     */
    suspend fun buildRustAndroidLibrary(
        projectPath: String,
        buildType: String = "debug"
    ): Flow<String> = flow {
        emit("Building Rust Android library...")
        
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists()) {
                emit("ERROR: Project directory does not exist: $projectPath")
                return@flow
            }
            
            // Check if Cargo.toml exists
            val cargoTomlFile = File(projectDir, "Cargo.toml")
            if (!cargoTomlFile.exists()) {
                emit("ERROR: Cargo.toml not found. Not a valid Rust project.")
                return@flow
            }
            
            // Find cargo
            val cargoPath = sdkManager.getRustCargoPath()
            if (cargoPath == null) {
                emit("ERROR: Cargo not found. Please install Rust.")
                return@flow
            }
            
            // Build Rust library
            emit("Building Rust library with cargo...")
            
            val cargoArgs = if (buildType == "release") {
                listOf("build", "--release")
            } else {
                listOf("build")
            }
            
            val cargoProcess = ProcessBuilder(cargoPath, *cargoArgs.toTypedArray())
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val cargoReader = BufferedReader(InputStreamReader(cargoProcess.inputStream))
            var line: String?
            
            while (cargoReader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(currentLine)
                }
            }
            
            val cargoExitCode = cargoProcess.waitFor()
            if (cargoExitCode != 0) {
                emit("ERROR: Cargo build failed with exit code: $cargoExitCode")
                return@flow
            }
            
            emit("Rust library built successfully")
            
            // Check if Gradle wrapper exists
            val gradlewFile = File(projectDir, "gradlew")
            if (!gradlewFile.exists()) {
                emit("Gradle wrapper not found. Generating...")
                
                // Generate Gradle wrapper
                val gradlePath = sdkManager.getGradlePath()
                if (gradlePath != null) {
                    val wrapperProcess = ProcessBuilder(gradlePath, "wrapper")
                        .directory(projectDir)
                        .redirectErrorStream(true)
                        .start()
                    
                    val wrapperReader = BufferedReader(InputStreamReader(wrapperProcess.inputStream))
                    
                    while (wrapperReader.readLine().also { line = it } != null) {
                        val currentLine = line
                        if (currentLine != null) {
                            emit(currentLine)
                        }
                    }
                    
                    val wrapperExitCode = wrapperProcess.waitFor()
                    if (wrapperExitCode != 0) {
                        emit("WARNING: Failed to generate Gradle wrapper")
                    } else {
                        emit("Gradle wrapper generated successfully")
                        
                        // Make gradlew executable
                        gradlewFile.setExecutable(true)
                    }
                } else {
                    emit("WARNING: Gradle not found. Cannot generate wrapper.")
                }
            }
            
            // Build Android library with Gradle
            emit("Building Android library with Gradle...")
            
            val gradleCommand = if (gradlewFile.exists() && gradlewFile.canExecute()) {
                "./gradlew"
            } else {
                sdkManager.getGradlePath() ?: "gradle"
            }
            
            val gradleArgs = if (buildType == "release") {
                listOf("assembleRelease")
            } else {
                listOf("assembleDebug")
            }
            
            val gradleProcess = ProcessBuilder(gradleCommand, *gradleArgs.toTypedArray())
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            
            val gradleReader = BufferedReader(InputStreamReader(gradleProcess.inputStream))
            
            while (gradleReader.readLine().also { line = it } != null) {
                val currentLine = line
                if (currentLine != null) {
                    emit(currentLine)
                }
            }
            
            val gradleExitCode = gradleProcess.waitFor()
            if (gradleExitCode != 0) {
                emit("ERROR: Gradle build failed with exit code: $gradleExitCode")
                return@flow
            }
            
            emit("Android library built successfully")
            
            // Find generated AAR file
            val aarDir = File(projectDir, "app/build/outputs/aar")
            if (aarDir.exists()) {
                val aarFiles = aarDir.listFiles { file -> file.extension == "aar" }
                if (aarFiles != null && aarFiles.isNotEmpty()) {
                    val aarFile = aarFiles.first()
                    emit("Generated AAR: ${aarFile.absolutePath}")
                }
            }
            
        } catch (e: Exception) {
            emit("ERROR: Failed to build Rust Android library: ${e.message}")
            Log.e(TAG, "Error building Rust Android library", e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Install Rust Android targets
     */
    suspend fun installRustAndroidTargets(): Flow<String> = flow {
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
}