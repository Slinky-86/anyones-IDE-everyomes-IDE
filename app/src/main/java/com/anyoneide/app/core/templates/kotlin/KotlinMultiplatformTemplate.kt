package com.anyoneide.app.core.templates.kotlin

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating a Kotlin Multiplatform project
 */
class KotlinMultiplatformTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "kotlin_multiplatform"
    
    override fun getName(): String = "Kotlin Multiplatform"
    
    override fun getDescription(): String = "Cross-platform Kotlin project targeting Android, iOS, and JVM."
    
    override fun getCategory(): String = "Kotlin"
    
    override fun create(projectDir: File, projectName: String, packageName: String): Boolean {
        try {
            // Create project structure
            val srcCommonMain = File(projectDir, "src/commonMain/kotlin")
            val srcCommonTest = File(projectDir, "src/commonTest/kotlin")
            val srcAndroidMain = File(projectDir, "src/androidMain/kotlin")
            val srcAndroidTest = File(projectDir, "src/androidTest/kotlin")
            val srcJvmMain = File(projectDir, "src/jvmMain/kotlin")
            val srcJvmTest = File(projectDir, "src/jvmTest/kotlin")
            
            val packageDir = packageName.replace(".", "/")
            val commonMainPackageDir = File(srcCommonMain, packageDir)
            val commonTestPackageDir = File(srcCommonTest, packageDir)
            val androidMainPackageDir = File(srcAndroidMain, packageDir)
            val androidTestPackageDir = File(srcAndroidTest, packageDir)
            val jvmMainPackageDir = File(srcJvmMain, packageDir)
            val jvmTestPackageDir = File(srcJvmTest, packageDir)
            
            commonMainPackageDir.mkdirs()
            commonTestPackageDir.mkdirs()
            androidMainPackageDir.mkdirs()
            androidTestPackageDir.mkdirs()
            jvmMainPackageDir.mkdirs()
            jvmTestPackageDir.mkdirs()
            
            // Create build.gradle.kts
            val buildGradle = """
                plugins {
                    kotlin("multiplatform") version "1.9.20"
                    id("com.android.library") version "8.1.4"
                    id("maven-publish")
                }
                
                group = "$packageName"
                version = "0.1.0"
                
                repositories {
                    google()
                    mavenCentral()
                }
                
                kotlin {
                    jvm {
                        jvmToolchain(8)
                        testRuns["test"].executionTask.configure {
                            useJUnitPlatform()
                        }
                    }
                    
                    androidTarget {
                        compilations.all {
                            kotlinOptions {
                                jvmTarget = "1.8"
                            }
                        }
                    }
                    
                    listOf(
                        iosX64(),
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "$projectName"
                        }
                    }
                    
                    sourceSets {
                        val commonMain by getting {
                            dependencies {
                                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                            }
                        }
                        val commonTest by getting {
                            dependencies {
                                implementation(kotlin("test"))
                            }
                        }
                        val jvmMain by getting
                        val jvmTest by getting
                        val androidMain by getting {
                            dependencies {
                                implementation("androidx.core:core-ktx:1.12.0")
                            }
                        }
                        val androidUnitTest by getting {
                            dependencies {
                                implementation("junit:junit:4.13.2")
                            }
                        }
                        val iosX64Main by getting
                        val iosArm64Main by getting
                        val iosSimulatorArm64Main by getting
                        val iosMain by creating {
                            dependsOn(commonMain)
                            iosX64Main.dependsOn(this)
                            iosArm64Main.dependsOn(this)
                            iosSimulatorArm64Main.dependsOn(this)
                        }
                        val iosX64Test by getting
                        val iosArm64Test by getting
                        val iosSimulatorArm64Test by getting
                        val iosTest by creating {
                            dependsOn(commonTest)
                            iosX64Test.dependsOn(this)
                            iosArm64Test.dependsOn(this)
                            iosSimulatorArm64Test.dependsOn(this)
                        }
                    }
                }
                
                android {
                    namespace = "$packageName"
                    compileSdk = 34
                    
                    defaultConfig {
                        minSdk = 24
                        
                        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                        consumerProguardFiles("consumer-rules.pro")
                    }
                    
                    buildTypes {
                        release {
                            isMinifyEnabled = false
                            proguardFiles(
                                getDefaultProguardFile("proguard-android-optimize.txt"),
                                "proguard-rules.pro"
                            )
                        }
                    }
                    
                    compileOptions {
                        sourceCompatibility = JavaVersion.VERSION_1_8
                        targetCompatibility = JavaVersion.VERSION_1_8
                    }
                }
                
                publishing {
                    repositories {
                        maven {
                            name = "GitHubPackages"
                            url = uri("https://maven.pkg.github.com/yourusername/${projectName.lowercase()}")
                            credentials {
                                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                            }
                        }
                    }
                }
            """.trimIndent()
            
            File(projectDir, "build.gradle.kts").writeText(buildGradle)
            
            // Create settings.gradle.kts
            val settingsGradle = """
                pluginManagement {
                    repositories {
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                
                dependencyResolutionManagement {
                    repositories {
                        google()
                        mavenCentral()
                    }
                }
                
                rootProject.name = "$projectName"
            """.trimIndent()
            
            File(projectDir, "settings.gradle.kts").writeText(settingsGradle)
            
            // Create gradle.properties
            val gradleProperties = """
                kotlin.code.style=official
                android.useAndroidX=true
                kotlin.mpp.enableCInteropCommonization=true
                kotlin.mpp.androidSourceSetLayoutVersion=2
                org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
            """.trimIndent()
            
            File(projectDir, "gradle.properties").writeText(gradleProperties)
            
            // Create .gitignore
            val gitignore = """
                .gradle
                build/
                !gradle/wrapper/gradle-wrapper.jar
                !**/src/main/**/build/
                !**/src/test/**/build/
                
                ### IntelliJ IDEA ###
                .idea/
                *.iws
                *.iml
                *.ipr
                out/
                !**/src/main/**/out/
                !**/src/test/**/out/
                
                ### Android Studio ###
                local.properties
                captures/
                .externalNativeBuild
                .cxx
                
                ### VS Code ###
                .vscode/
                
                ### Mac OS ###
                .DS_Store
                
                ### Xcode ###
                xcuserdata/
                *.xcodeproj/*
                !*.xcodeproj/project.pbxproj
                !*.xcodeproj/xcshareddata/
                !*.xcworkspace/contents.xcworkspacedata
                **/xcshareddata/WorkspaceSettings.xcsettings
            """.trimIndent()
            
            File(projectDir, ".gitignore").writeText(gitignore)
            
            // Create README.md
            val readme = """
                # $projectName
                
                A Kotlin Multiplatform library that works on Android, iOS, and JVM.
                
                ## Features
                
                * Cross-platform code sharing
                * Platform-specific implementations
                * Unit tests for all platforms
                
                ## Setup
                
                ### Android
                
                ```kotlin
                dependencies {
                    implementation("$packageName:$projectName:0.1.0")
                }
                ```
                
                ### iOS
                
                ```swift
                import $projectName
                
                let platform = Platform()
                print(platform.name)
                ```
                
                ### JVM
                
                ```kotlin
                dependencies {
                    implementation("$packageName:$projectName-jvm:0.1.0")
                }
                ```
                
                ## Usage
                
                ```kotlin
                import $packageName.Platform
                
                val platform = Platform()
                println(platform.name)
                ```
                
                ## License
                
                This project is licensed under the MIT License - see the LICENSE file for details.
            """.trimIndent()
            
            File(projectDir, "README.md").writeText(readme)
            
            // Create LICENSE
            val license = """
                MIT License
                
                Copyright (c) ${java.time.Year.now().value} Your Name
                
                Permission is hereby granted, free of charge, to any person obtaining a copy
                of this software and associated documentation files (the "Software"), to deal
                in the Software without restriction, including without limitation the rights
                to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                copies of the Software, and to permit persons to whom the Software is
                furnished to do so, subject to the following conditions:
                
                The above copyright notice and this permission notice shall be included in all
                copies or substantial portions of the Software.
                
                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
                IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
                FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
                AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
                LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
                OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
                SOFTWARE.
            """.trimIndent()
            
            File(projectDir, "LICENSE").writeText(license)
            
            // Create Platform.kt (common)
            val platformCommon = """
                package $packageName
                
                expect class Platform() {
                    val name: String
                }
            """.trimIndent()
            
            File(commonMainPackageDir, "Platform.kt").writeText(platformCommon)
            
            // Create Platform.kt (Android)
            val platformAndroid = """
                package $packageName
                
                actual class Platform actual constructor() {
                    actual val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
                }
            """.trimIndent()
            
            File(androidMainPackageDir, "Platform.kt").writeText(platformAndroid)
            
            // Create Platform.kt (JVM)
            val platformJvm = """
                package $packageName
                
                actual class Platform actual constructor() {
                    actual val name: String = "JVM ${System.getProperty("java.version")}"
                }
            """.trimIndent()
            
            File(jvmMainPackageDir, "Platform.kt").writeText(platformJvm)
            
            // Create Platform.kt (iOS)
            val platformIos = """
                package $packageName
                
                import platform.UIKit.UIDevice
                
                actual class Platform actual constructor() {
                    actual val name: String = 
                        "iOS ${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"
                }
            """.trimIndent()
            
            File(File(srcCommonMain.parentFile, "iosMain/kotlin/$packageDir"), "Platform.kt").writeText(platformIos)
            
            // Create Greeting.kt (common)
            val greetingCommon = """
                package $packageName
                
                class Greeting {
                    private val platform: Platform = Platform()
                    
                    fun greet(name: String): String {
                        return "Hello, ${'$'}name! You are running on ${'$'}{platform.name}"
                    }
                }
            """.trimIndent()
            
            File(commonMainPackageDir, "Greeting.kt").writeText(greetingCommon)
            
            // Create DateTimeUtils.kt (common)
            val dateTimeUtils = """
                package $packageName
                
                import kotlinx.datetime.Clock
                import kotlinx.datetime.Instant
                import kotlinx.datetime.TimeZone
                import kotlinx.datetime.toLocalDateTime
                
                /**
                 * Utility functions for date and time operations.
                 */
                object DateTimeUtils {
                    
                    /**
                     * Gets the current date and time as a formatted string.
                     *
                     * @return A string representation of the current date and time
                     */
                    fun getCurrentDateTime(): String {
                        val now = Clock.System.now()
                        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
                        return "${'$'}{localDateTime.year}-${'$'}{localDateTime.monthNumber.toString().padStart(2, '0')}-${'$'}{localDateTime.dayOfMonth.toString().padStart(2, '0')} " +
                               "${'$'}{localDateTime.hour.toString().padStart(2, '0')}:${'$'}{localDateTime.minute.toString().padStart(2, '0')}:${'$'}{localDateTime.second.toString().padStart(2, '0')}"
                    }
                    
                    /**
                     * Formats an Instant to a string representation.
                     *
                     * @param instant The Instant to format
                     * @return A string representation of the Instant
                     */
                    fun formatInstant(instant: Instant): String {
                        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                        return "${'$'}{localDateTime.year}-${'$'}{localDateTime.monthNumber.toString().padStart(2, '0')}-${'$'}{localDateTime.dayOfMonth.toString().padStart(2, '0')} " +
                               "${'$'}{localDateTime.hour.toString().padStart(2, '0')}:${'$'}{localDateTime.minute.toString().padStart(2, '0')}:${'$'}{localDateTime.second.toString().padStart(2, '0')}"
                    }
                    
                    /**
                     * Calculates the difference in days between two Instants.
                     *
                     * @param start The start Instant
                     * @param end The end Instant
                     * @return The difference in days
                     */
                    fun daysBetween(start: Instant, end: Instant): Int {
                        val startDay = start.toLocalDateTime(TimeZone.UTC).date
                        val endDay = end.toLocalDateTime(TimeZone.UTC).date
                        return endDay.toEpochDays() - startDay.toEpochDays()
                    }
                }
            """.trimIndent()
            
            File(commonMainPackageDir, "DateTimeUtils.kt").writeText(dateTimeUtils)
            
            // Create GreetingTest.kt (common)
            val greetingTest = """
                package $packageName
                
                import kotlin.test.Test
                import kotlin.test.assertTrue
                
                class GreetingTest {
                    
                    @Test
                    fun testGreeting() {
                        val greeting = Greeting()
                        val result = greeting.greet("Kotlin")
                        assertTrue(result.contains("Hello, Kotlin!"))
                        assertTrue(result.contains("You are running on"))
                    }
                }
            """.trimIndent()
            
            File(commonTestPackageDir, "GreetingTest.kt").writeText(greetingTest)
            
            // Create DateTimeUtilsTest.kt (common)
            val dateTimeUtilsTest = """
                package $packageName
                
                import kotlinx.datetime.Clock
                import kotlinx.datetime.Instant
                import kotlin.test.Test
                import kotlin.test.assertEquals
                import kotlin.test.assertTrue
                
                class DateTimeUtilsTest {
                    
                    @Test
                    fun testGetCurrentDateTime() {
                        val dateTime = DateTimeUtils.getCurrentDateTime()
                        assertTrue(dateTime.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
                    }
                    
                    @Test
                    fun testFormatInstant() {
                        val instant = Instant.parse("2023-01-01T12:00:00Z")
                        val formatted = DateTimeUtils.formatInstant(instant)
                        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")))
                    }
                    
                    @Test
                    fun testDaysBetween() {
                        val start = Instant.parse("2023-01-01T00:00:00Z")
                        val end = Instant.parse("2023-01-10T00:00:00Z")
                        assertEquals(9, DateTimeUtils.daysBetween(start, end))
                        
                        val sameDay = Instant.parse("2023-01-01T12:00:00Z")
                        assertEquals(0, DateTimeUtils.daysBetween(start, sameDay))
                    }
                }
            """.trimIndent()
            
            File(commonTestPackageDir, "DateTimeUtilsTest.kt").writeText(dateTimeUtilsTest)
            
            // Create proguard-rules.pro
            val proguardRules = """
                # Add project specific ProGuard rules here.
                # You can control the set of applied configuration files using the
                # proguardFiles setting in build.gradle.
                #
                # For more details, see
                #   http://developer.android.com/guide/developing/tools/proguard.html
            """.trimIndent()
            
            File(projectDir, "proguard-rules.pro").writeText(proguardRules)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}