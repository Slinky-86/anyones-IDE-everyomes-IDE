plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "1.9.20"
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.anyoneide.app"
    compileSdk = 34
    
    // Add this to suppress experimental API warnings
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    defaultConfig {
        applicationId = "com.anyoneide.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Room schema location
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    
    // Configure JNI libraries
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    
    // Add Rust-specific tasks
    tasks.register("compileRustDebug") {
        doLast {
            exec {
                workingDir = rootProject.projectDir
                commandLine("cargo", "build")
            }
        }
    }
    
    tasks.register("compileRustRelease") {
        doLast {
            exec {
                workingDir = rootProject.projectDir
                commandLine("cargo", "build", "--release")
            }
        }
    }
    
    // Add Rust compilation to the build process
    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(":buildRustNative")
    }
}

dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Compose BOM and UI - Updated to latest stable
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    
    // Navigation and ViewModel
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // UI Components
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // File and Storage
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // File operations and compression - REAL IMPLEMENTATIONS
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-compress:1.24.0")
    implementation("org.tukaani:xz:1.9")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // GSON for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    // Add Gson as a KSP dependency to fix the Converters issue
    ksp("com.google.code.gson:gson:2.10.1")

    // Google Gemini API - Keep this as it's used for AI features, not database
    implementation("com.google.ai.client.generativeai:generativeai:0.2.0")
    
    // Advanced text editing
    implementation("androidx.compose.foundation:foundation-layout:1.5.4")
    implementation("androidx.compose.ui:ui-text:1.5.4")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.4")
    
    // Java 8+ API desugaring for older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // ConstraintLayout for custom splash screen
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Shizuku API
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Task to copy Rust libraries to jniLibs directory
tasks.register("copyRustLibs") {
    doLast {
        val rustProjectDir = rootProject.file("rust_native_build")
        val targetDir = file("src/main/jniLibs")
        
        val architectures = mapOf(
            "aarch64-linux-android" to "arm64-v8a",
            "armv7-linux-androideabi" to "armeabi-v7a",
            "i686-linux-android" to "x86",
            "x86_64-linux-android" to "x86_64"
        )
        
        architectures.forEach { (rustArch, androidArch) ->
            val sourceDir = rustProjectDir.resolve("target/$rustArch/release")
            val destDir = targetDir.resolve(androidArch)
            
            if (sourceDir.exists()) {
                destDir.mkdirs()
                
                sourceDir.listFiles()?.forEach { sourceFile ->
                    if (sourceFile.name.startsWith("librust_native_build") && 
                        (sourceFile.name.endsWith(".so") || sourceFile.name.endsWith(".dylib") || sourceFile.name.endsWith(".dll"))) {
                        val destFile = destDir.resolve("librust_native_build.so")
                        sourceFile.copyTo(destFile, overwrite = true)
                        println("Copied ${sourceFile.name} to ${destFile.absolutePath}")
                    }
                }
            } else {
                println("Source directory $sourceDir does not exist")
            }
        }
    }
}

// Add copyRustLibs as a dependency of preBuild
tasks.named("preBuild") {
    dependsOn("copyRustLibs")
}