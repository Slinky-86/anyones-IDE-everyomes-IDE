package com.anyoneide.app.core.templates.rust

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating a Rust Android library
 */
class RustAndroidLibraryTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "rust_android_lib"
    
    override fun getName(): String = "Rust Android Library"
    
    override fun getDescription(): String = "Android application with Rust native library using JNI bindings."
    
    override fun getCategory(): String = "Rust"
    
    override fun create(projectDir: File, projectName: String, packageName: String): Boolean {
        try {
            // Create project structure
            createBasicProjectStructure(projectDir, packageName)
            
            // Create Rust library directory
            val rustDir = File(projectDir, "rust-lib")
            rustDir.mkdirs()
            
            // Create Cargo.toml
            val cargoToml = """
                [package]
                name = "rust-lib"
                version = "0.1.0"
                edition = "2021"
                authors = ["Anyone IDE User"]
                
                [lib]
                crate-type = ["cdylib", "staticlib", "rlib"]
                
                [dependencies]
                jni = { version = "0.21.1", features = ["invocation"] }
            """.trimIndent()
            
            File(rustDir, "Cargo.toml").writeText(cargoToml)
            
            // Create src directory
            val srcDir = File(rustDir, "src")
            srcDir.mkdirs()
            
            // Create lib.rs with JNI exports
            val libRs = """
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
            
            File(srcDir, "lib.rs").writeText(libRs)
            
            // Create .cargo directory and config.toml
            val cargoConfigDir = File(rustDir, ".cargo")
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
            
            // Create Android app module
            val appDir = File(projectDir, "app")
            appDir.mkdirs()
            
            // Create build.gradle for app module
            val appBuildGradle = """
                plugins {
                    id 'com.android.application'
                    id 'org.jetbrains.kotlin.android'
                }
                
                android {
                    namespace '$packageName'
                    compileSdk 34
                    
                    defaultConfig {
                        applicationId "$packageName"
                        minSdk 24
                        targetSdk 34
                        versionCode 1
                        versionName "1.0"
                        
                        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
                        
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
                            jniLibs.srcDirs = ['../rust-lib/target/debug', '../rust-lib/target/release']
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
                    workingDir '../rust-lib'
                    commandLine 'cargo', 'build', '--release'
                }
                
                dependencies {
                    implementation 'androidx.core:core-ktx:1.12.0'
                    implementation 'androidx.appcompat:appcompat:1.6.1'
                    implementation 'com.google.android.material:material:1.11.0'
                    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
                    testImplementation 'junit:junit:4.13.2'
                    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
                    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
                }
            """.trimIndent()
            
            File(appDir, "build.gradle").writeText(appBuildGradle)
            
            // Create src directory structure
            val mainDir = File(appDir, "src/main")
            mainDir.mkdirs()
            
            // Create AndroidManifest.xml
            val manifest = """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    
                    <application
                        android:allowBackup="true"
                        android:icon="@mipmap/ic_launcher"
                        android:label="@string/app_name"
                        android:roundIcon="@mipmap/ic_launcher_round"
                        android:supportsRtl="true"
                        android:theme="@style/Theme.AppCompat.DayNight">
                        
                        <activity
                            android:name=".MainActivity"
                            android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                        
                    </application>
                    
                </manifest>
            """.trimIndent()
            
            File(mainDir, "AndroidManifest.xml").writeText(manifest)
            
            // Create Java directory structure based on package name
            val packagePath = packageName.replace(".", "/")
            val javaDir = File(mainDir, "java/$packagePath")
            javaDir.mkdirs()
            
            // Create RustLib.java
            val rustLib = """
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
            
            File(javaDir, "RustLib.java").writeText(rustLib)
            
            // Create MainActivity.kt
            val mainActivity = """
                package $packageName
                
                import android.os.Bundle
                import android.widget.TextView
                import androidx.appcompat.app.AppCompatActivity
                
                class MainActivity : AppCompatActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContentView(R.layout.activity_main)
                        
                        // Get greeting from Rust
                        val textView = findViewById<TextView>(R.id.textView)
                        textView.text = RustLib.getGreeting()
                        
                        // Process a string with Rust
                        val processedText = RustLib.processString("Hello from Kotlin!")
                        findViewById<TextView>(R.id.processedTextView).text = processedText
                    }
                }
            """.trimIndent()
            
            File(javaDir, "MainActivity.kt").writeText(mainActivity)
            
            // Create res directory structure
            val resDir = File(mainDir, "res")
            val layoutDir = File(resDir, "layout")
            val valuesDir = File(resDir, "values")
            layoutDir.mkdirs()
            valuesDir.mkdirs()
            
            // Create activity_main.xml
            val activityMain = """
                <?xml version="1.0" encoding="utf-8"?>
                <LinearLayout 
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="vertical"
                    android:gravity="center"
                    android:padding="16dp">
                    
                    <TextView
                        android:id="@+id/textView"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        android:layout_marginBottom="16dp" />
                    
                    <TextView
                        android:id="@+id/processedTextView"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textSize="18sp" />
                    
                </LinearLayout>
            """.trimIndent()
            
            File(layoutDir, "activity_main.xml").writeText(activityMain)
            
            // Create strings.xml
            val strings = """
                <resources>
                    <string name="app_name">Rust Android App</string>
                </resources>
            """.trimIndent()
            
            File(valuesDir, "strings.xml").writeText(strings)
            
            // Create settings.gradle
            val settingsGradle = """
                rootProject.name = "${projectDir.name}"
                include ':app'
            """.trimIndent()
            
            File(projectDir, "settings.gradle").writeText(settingsGradle)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}