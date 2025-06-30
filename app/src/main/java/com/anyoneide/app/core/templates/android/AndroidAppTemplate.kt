package com.anyoneide.app.core.templates.android

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating a basic Android application
 */
class AndroidAppTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "android_basic"
    
    override fun getName(): String = "Basic Android App"
    
    override fun getDescription(): String = "A simple Android application with basic navigation and Material Design components."
    
    override fun getCategory(): String = "Android"
    
    override fun create(projectDir: File, projectName: String, packageName: String): Boolean {
        try {
            // Create project structure
            createBasicProjectStructure(projectDir, packageName)
            
            // Create app module
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
                        android:theme="@style/Theme.MyApp">
                        
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
            
            // Create MainActivity.kt
            val mainActivity = """
                package $packageName
                
                import android.os.Bundle
                import androidx.appcompat.app.AppCompatActivity
                
                class MainActivity : AppCompatActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContentView(R.layout.activity_main)
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
                <androidx.constraintlayout.widget.ConstraintLayout 
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:app="http://schemas.android.com/apk/res-auto"
                    xmlns:tools="http://schemas.android.com/tools"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    tools:context=".MainActivity">
                    
                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Hello World!"
                        app:layout_constraintBottom_toBottomOf="parent"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toTopOf="parent" />
                    
                </androidx.constraintlayout.widget.ConstraintLayout>
            """.trimIndent()
            
            File(layoutDir, "activity_main.xml").writeText(activityMain)
            
            // Create strings.xml
            val strings = """
                <resources>
                    <string name="app_name">$projectName</string>
                </resources>
            """.trimIndent()
            
            File(valuesDir, "strings.xml").writeText(strings)
            
            // Create proguard-rules.pro
            val proguardRules = """
                # Add project specific ProGuard rules here.
                # You can control the set of applied configuration files using the
                # proguardFiles setting in build.gradle.
                #
                # For more details, see
                #   http://developer.android.com/guide/developing/tools/proguard.html
            """.trimIndent()
            
            File(appDir, "proguard-rules.pro").writeText(proguardRules)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}