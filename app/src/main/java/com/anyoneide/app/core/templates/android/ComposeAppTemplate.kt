package com.anyoneide.app.core.templates.android

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating a Jetpack Compose application
 */
class ComposeAppTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "compose_app"
    
    override fun getName(): String = "Jetpack Compose App"
    
    override fun getDescription(): String = "Modern Android app built with Jetpack Compose for declarative UI development."
    
    override fun getCategory(): String = "Compose"
    
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
                        vectorDrawables {
                            useSupportLibrary true
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
                    
                    buildFeatures {
                        compose true
                    }
                    
                    composeOptions {
                        kotlinCompilerExtensionVersion '1.5.4'
                    }
                    
                    packaging {
                        resources {
                            excludes += '/META-INF/{AL2.0,LGPL2.1}'
                        }
                    }
                }
                
                dependencies {
                    implementation 'androidx.core:core-ktx:1.12.0'
                    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
                    implementation 'androidx.activity:activity-compose:1.8.2'
                    implementation platform('androidx.compose:compose-bom:2024.02.00')
                    implementation 'androidx.compose.ui:ui'
                    implementation 'androidx.compose.ui:ui-graphics'
                    implementation 'androidx.compose.ui:ui-tooling-preview'
                    implementation 'androidx.compose.material3:material3'
                    implementation 'androidx.navigation:navigation-compose:2.7.5'
                    testImplementation 'junit:junit:4.13.2'
                    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
                    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
                    androidTestImplementation platform('androidx.compose:compose-bom:2024.02.00')
                    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
                    debugImplementation 'androidx.compose.ui:ui-tooling'
                    debugImplementation 'androidx.compose.ui:ui-test-manifest'
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
                        android:theme="@style/Theme.AppCompat.DayNight.NoActionBar">
                        
                        <activity
                            android:name=".MainActivity"
                            android:exported="true"
                            android:theme="@style/Theme.AppCompat.DayNight.NoActionBar">
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
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.foundation.layout.fillMaxSize
                import androidx.compose.material3.MaterialTheme
                import androidx.compose.material3.Surface
                import androidx.compose.material3.Text
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.tooling.preview.Preview
                import $packageName.ui.theme.AppTheme
                
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            AppTheme {
                                // A surface container using the 'background' color from the theme
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    MainScreen()
                                }
                            }
                        }
                    }
                }
                
                @Composable
                fun MainScreen() {
                    Text(text = "Hello Compose!")
                }
                
                @Preview(showBackground = true)
                @Composable
                fun DefaultPreview() {
                    AppTheme {
                        MainScreen()
                    }
                }
            """.trimIndent()
            
            File(javaDir, "MainActivity.kt").writeText(mainActivity)
            
            // Create theme directory
            val themeDir = File(javaDir, "ui/theme")
            themeDir.mkdirs()
            
            // Create Color.kt
            val colorKt = """
                package $packageName.ui.theme
                
                import androidx.compose.ui.graphics.Color
                
                val Purple80 = Color(0xFFD0BCFF)
                val PurpleGrey80 = Color(0xFFCCC2DC)
                val Pink80 = Color(0xFFEFB8C8)
                
                val Purple40 = Color(0xFF6650a4)
                val PurpleGrey40 = Color(0xFF625b71)
                val Pink40 = Color(0xFF7D5260)
            """.trimIndent()
            
            File(themeDir, "Color.kt").writeText(colorKt)
            
            // Create Theme.kt
            val themeKt = """
                package $packageName.ui.theme
                
                import android.app.Activity
                import android.os.Build
                import androidx.compose.foundation.isSystemInDarkTheme
                import androidx.compose.material3.MaterialTheme
                import androidx.compose.material3.darkColorScheme
                import androidx.compose.material3.dynamicDarkColorScheme
                import androidx.compose.material3.dynamicLightColorScheme
                import androidx.compose.material3.lightColorScheme
                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.SideEffect
                import androidx.compose.ui.graphics.toArgb
                import androidx.compose.ui.platform.LocalContext
                import androidx.compose.ui.platform.LocalView
                import androidx.core.view.WindowCompat
                
                private val DarkColorScheme = darkColorScheme(
                    primary = Purple80,
                    secondary = PurpleGrey80,
                    tertiary = Pink80
                )
                
                private val LightColorScheme = lightColorScheme(
                    primary = Purple40,
                    secondary = PurpleGrey40,
                    tertiary = Pink40
                )
                
                @Composable
                fun AppTheme(
                    darkTheme: Boolean = isSystemInDarkTheme(),
                    // Dynamic color is available on Android 12+
                    dynamicColor: Boolean = true,
                    content: @Composable () -> Unit
                ) {
                    val colorScheme = when {
                        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                            val context = LocalContext.current
                            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                        }
                        darkTheme -> DarkColorScheme
                        else -> LightColorScheme
                    }
                    val view = LocalView.current
                    if (!view.isInEditMode) {
                        SideEffect {
                            val window = (view.context as Activity).window
                            window.statusBarColor = colorScheme.primary.toArgb()
                            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
                        }
                    }
                
                    MaterialTheme(
                        colorScheme = colorScheme,
                        typography = Typography,
                        content = content
                    )
                }
            """.trimIndent()
            
            File(themeDir, "Theme.kt").writeText(themeKt)
            
            // Create Type.kt
            val typeKt = """
                package $packageName.ui.theme
                
                import androidx.compose.material3.Typography
                import androidx.compose.ui.text.TextStyle
                import androidx.compose.ui.text.font.FontFamily
                import androidx.compose.ui.text.font.FontWeight
                import androidx.compose.ui.unit.sp
                
                // Set of Material typography styles to start with
                val Typography = Typography(
                    bodyLarge = TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        letterSpacing = 0.5.sp
                    )
                )
            """.trimIndent()
            
            File(themeDir, "Type.kt").writeText(typeKt)
            
            // Create res directory structure
            val resDir = File(mainDir, "res")
            val valuesDir = File(resDir, "values")
            valuesDir.mkdirs()
            
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