// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Ensure proper permissions for Gradle wrapper
tasks.register("fixGradlePermissions") {
    doLast {
        val gradlewFile = file("gradlew")
        if (gradlewFile.exists()) {
            gradlewFile.setExecutable(true)
        }
    }
}

// Run permission fix before any build task
tasks.matching { it.name.startsWith("assemble") || it.name.startsWith("build") }.configureEach {
    dependsOn("fixGradlePermissions")
}

// Task to build the Rust native build system
tasks.register("buildRustNative") {
    doLast {
        val rustNativeBuildDir = file("rust_native_build")
        if (rustNativeBuildDir.exists()) {
            // Check if cargo is available
            val cargoInstalled = try {
                val process = ProcessBuilder("cargo", "--version").start()
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
            
            if (cargoInstalled) {
                // Build the Rust library
                println("Building Rust native build system...")
                val buildProcess = ProcessBuilder("cargo", "build", "--release")
                    .directory(rustNativeBuildDir)
                    .redirectErrorStream(true)
                    .start()
                
                buildProcess.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        println(line)
                    }
                }
                
                val exitCode = buildProcess.waitFor()
                if (exitCode == 0) {
                    println("Rust native build system built successfully")
                    
                    // Copy the library to the jniLibs directory
                    val targetDir = file("app/src/main/jniLibs")
                    val architectures = listOf(
                        "aarch64-linux-android" to "arm64-v8a",
                        "armv7-linux-androideabi" to "armeabi-v7a",
                        "i686-linux-android" to "x86",
                        "x86_64-linux-android" to "x86_64"
                    )
                    
                    architectures.forEach { (rustArch, androidArch) ->
                        val sourceDir = file("rust_native_build/target/$rustArch/release")
                        val destDir = file("$targetDir/$androidArch")
                        
                        if (sourceDir.exists()) {
                            destDir.mkdirs()
                            
                            sourceDir.listFiles()?.forEach { sourceFile ->
                                if (sourceFile.name.startsWith("librust_native_build") && 
                                    (sourceFile.name.endsWith(".so") || sourceFile.name.endsWith(".dylib") || sourceFile.name.endsWith(".dll"))) {
                                    val destFile = file("$destDir/librust_native_build.so")
                                    sourceFile.copyTo(destFile, overwrite = true)
                                    println("Copied $sourceFile to $destFile")
                                }
                            }
                        } else {
                            println("Source directory $sourceDir does not exist")
                        }
                    }
                } else {
                    println("Failed to build Rust native build system: exit code $exitCode")
                }
            } else {
                println("Cargo not found. Skipping Rust native build system build.")
            }
        } else {
            println("Rust native build directory not found. Skipping Rust native build system build.")
        }
    }
}

// Make the Android build depend on the Rust build
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("buildRustNative")
}

// Task to install Rust targets for Android
tasks.register("installRustAndroidTargets") {
    doLast {
        // Check if cargo is available
        val cargoInstalled = try {
            val process = ProcessBuilder("cargo", "--version").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
        
        if (cargoInstalled) {
            // Check if rustup is available
            val rustupInstalled = try {
                val process = ProcessBuilder("rustup", "--version").start()
                process.waitFor() == 0
            } catch (e: Exception) {
                false
            }
            
            if (rustupInstalled) {
                println("Installing Rust targets for Android...")
                
                val androidTargets = listOf(
                    "aarch64-linux-android",
                    "armv7-linux-androideabi",
                    "i686-linux-android",
                    "x86_64-linux-android"
                )
                
                androidTargets.forEach { target ->
                    println("Installing target: $target")
                    val process = ProcessBuilder("rustup", "target", "add", target)
                        .redirectErrorStream(true)
                        .start()
                    
                    process.inputStream.bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            println(line)
                        }
                    }
                    
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        println("Successfully installed target: $target")
                    } else {
                        println("Failed to install target: $target (exit code: $exitCode)")
                    }
                }
            } else {
                println("Rustup not found. Skipping Rust Android targets installation.")
            }
        } else {
            println("Cargo not found. Skipping Rust Android targets installation.")
        }
    }
}

// Task to create .cargo/config.toml for Android targets
tasks.register("createCargoConfig") {
    doLast {
        val rustNativeBuildDir = file("rust_native_build")
        val cargoConfigDir = file("${rustNativeBuildDir}/.cargo")
        cargoConfigDir.mkdirs()
        
        val configContent = """
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

            [build]
            target = ["aarch64-linux-android", "armv7-linux-androideabi", "x86_64-linux-android", "i686-linux-android"]
        """.trimIndent()
        
        file("${cargoConfigDir}/config.toml").writeText(configContent)
        println("Created .cargo/config.toml for Android targets")
    }
}

// Task to setup Rust environment for Android
tasks.register("setupRustAndroid") {
    dependsOn("createCargoConfig", "installRustAndroidTargets")
}

// Make the Rust build depend on the Rust Android setup
tasks.matching { it.name == "buildRustNative" }.configureEach {
    dependsOn("setupRustAndroid")
}