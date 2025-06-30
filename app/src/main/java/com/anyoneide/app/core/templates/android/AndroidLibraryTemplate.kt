package com.anyoneide.app.core.templates.android

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating an Android library
 */
class AndroidLibraryTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "android_library"
    
    override fun getName(): String = "Android Library"
    
    override fun getDescription(): String = "Reusable Android library module with documentation and sample app."
    
    override fun getCategory(): String = "Android"
    
    override fun create(projectDir: File, projectName: String, packageName: String): Boolean {
        try {
            // Create project structure
            createBasicProjectStructure(projectDir, packageName)
            
            // Create library module
            val libraryDir = File(projectDir, "library")
            libraryDir.mkdirs()
            
            // Create sample app module
            val sampleDir = File(projectDir, "sample")
            sampleDir.mkdirs()
            
            // Create build.gradle for library module
            val libraryBuildGradle = """
                plugins {
                    id 'com.android.library'
                    id 'org.jetbrains.kotlin.android'
                    id 'maven-publish'
                }
                
                android {
                    namespace '$packageName'
                    compileSdk 34
                    
                    defaultConfig {
                        minSdk 24
                        targetSdk 34
                        
                        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
                        consumerProguardFiles "consumer-rules.pro"
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
                        viewBinding true
                    }
                }
                
                dependencies {
                    implementation 'androidx.core:core-ktx:1.12.0'
                    implementation 'androidx.appcompat:appcompat:1.6.1'
                    implementation 'com.google.android.material:material:1.11.0'
                    
                    testImplementation 'junit:junit:4.13.2'
                    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
                    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
                }
                
                afterEvaluate {
                    publishing {
                        publications {
                            release(MavenPublication) {
                                from components.release
                                
                                groupId = '$packageName'
                                artifactId = '$projectName'
                                version = '0.1.0'
                                
                                pom {
                                    name = '$projectName'
                                    description = 'A reusable Android library'
                                    url = 'https://github.com/yourusername/$projectName'
                                    
                                    licenses {
                                        license {
                                            name = 'MIT License'
                                            url = 'https://opensource.org/licenses/MIT'
                                        }
                                    }
                                    
                                    developers {
                                        developer {
                                            id = 'yourusername'
                                            name = 'Your Name'
                                            email = 'your.email@example.com'
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
            
            File(libraryDir, "build.gradle").writeText(libraryBuildGradle)
            
            // Create build.gradle for sample app module
            val sampleBuildGradle = """
                plugins {
                    id 'com.android.application'
                    id 'org.jetbrains.kotlin.android'
                }
                
                android {
                    namespace '$packageName.sample'
                    compileSdk 34
                    
                    defaultConfig {
                        applicationId "$packageName.sample"
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
                    
                    buildFeatures {
                        viewBinding true
                    }
                }
                
                dependencies {
                    implementation project(':library')
                    
                    implementation 'androidx.core:core-ktx:1.12.0'
                    implementation 'androidx.appcompat:appcompat:1.6.1'
                    implementation 'com.google.android.material:material:1.11.0'
                    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
                    
                    testImplementation 'junit:junit:4.13.2'
                    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
                    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
                }
            """.trimIndent()
            
            File(sampleDir, "build.gradle").writeText(sampleBuildGradle)
            
            // Update settings.gradle
            val settingsGradle = """
                rootProject.name = "$projectName"
                include ':library'
                include ':sample'
            """.trimIndent()
            
            File(projectDir, "settings.gradle").writeText(settingsGradle)
            
            // Create library module structure
            val libMainDir = File(libraryDir, "src/main")
            libMainDir.mkdirs()
            
            // Create AndroidManifest.xml for library
            val libManifest = """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                
                </manifest>
            """.trimIndent()
            
            File(libMainDir, "AndroidManifest.xml").writeText(libManifest)
            
            // Create Java directory structure based on package name
            val packagePath = packageName.replace(".", "/")
            val libJavaDir = File(libMainDir, "java/$packagePath")
            libJavaDir.mkdirs()
            
            // Create CustomView.kt
            val customView = """
                package $packageName
                
                import android.content.Context
                import android.graphics.Canvas
                import android.graphics.Color
                import android.graphics.Paint
                import android.graphics.RectF
                import android.util.AttributeSet
                import android.view.View
                import kotlin.math.min
                
                /**
                 * A custom view that draws a circular progress indicator.
                 */
                class CircularProgressView @JvmOverloads constructor(
                    context: Context,
                    attrs: AttributeSet? = null,
                    defStyleAttr: Int = 0
                ) : View(context, attrs, defStyleAttr) {
                    
                    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 20f
                        strokeCap = Paint.Cap.ROUND
                    }
                    
                    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 20f
                        color = Color.LTGRAY
                    }
                    
                    private val rect = RectF()
                    
                    /**
                     * The progress value, from 0 to 100.
                     */
                    var progress: Float = 0f
                        set(value) {
                            field = value.coerceIn(0f, 100f)
                            invalidate()
                        }
                    
                    /**
                     * The color of the progress indicator.
                     */
                    var progressColor: Int = Color.BLUE
                        set(value) {
                            field = value
                            paint.color = value
                            invalidate()
                        }
                    
                    init {
                        paint.color = progressColor
                    }
                    
                    override fun onDraw(canvas: Canvas) {
                        super.onDraw(canvas)
                        
                        val width = width.toFloat()
                        val height = height.toFloat()
                        val size = min(width, height)
                        
                        val strokeWidth = paint.strokeWidth
                        val halfStrokeWidth = strokeWidth / 2
                        
                        // Set the bounds for the progress circle
                        rect.set(
                            halfStrokeWidth,
                            halfStrokeWidth,
                            size - halfStrokeWidth,
                            size - halfStrokeWidth
                        )
                        
                        // Draw background circle
                        canvas.drawArc(rect, 0f, 360f, false, backgroundPaint)
                        
                        // Draw progress arc
                        val sweepAngle = 360f * progress / 100f
                        canvas.drawArc(rect, -90f, sweepAngle, false, paint)
                    }
                    
                    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                        val width = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
                        val height = resolveSize(suggestedMinimumHeight, heightMeasureSpec)
                        val size = min(width, height)
                        setMeasuredDimension(size, size)
                    }
                }
            """.trimIndent()
            
            File(libJavaDir, "CircularProgressView.kt").writeText(customView)
            
            // Create Utils.kt
            val utils = """
                package $packageName
                
                import android.content.Context
                import android.util.TypedValue
                import android.widget.Toast
                
                /**
                 * Utility functions for the library.
                 */
                object Utils {
                    
                    /**
                     * Shows a toast message.
                     *
                     * @param context The context
                     * @param message The message to show
                     * @param duration The duration of the toast
                     */
                    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
                        Toast.makeText(context, message, duration).show()
                    }
                    
                    /**
                     * Converts dp to pixels.
                     *
                     * @param context The context
                     * @param dp The value in dp
                     * @return The value in pixels
                     */
                    fun dpToPx(context: Context, dp: Float): Float {
                        return TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            dp,
                            context.resources.displayMetrics
                        )
                    }
                    
                    /**
                     * Converts pixels to dp.
                     *
                     * @param context The context
                     * @param px The value in pixels
                     * @return The value in dp
                     */
                    fun pxToDp(context: Context, px: Float): Float {
                        return px / context.resources.displayMetrics.density
                    }
                }
            """.trimIndent()
            
            File(libJavaDir, "Utils.kt").writeText(utils)
            
            // Create sample app module structure
            val sampleMainDir = File(sampleDir, "src/main")
            sampleMainDir.mkdirs()
            
            // Create AndroidManifest.xml for sample app
            val sampleManifest = """
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
            
            File(sampleMainDir, "AndroidManifest.xml").writeText(sampleManifest)
            
            // Create Java directory structure for sample app
            val samplePackagePath = "$packageName.sample".replace(".", "/")
            val sampleJavaDir = File(sampleMainDir, "java/$samplePackagePath")
            sampleJavaDir.mkdirs()
            
            // Create MainActivity.kt for sample app
            val mainActivity = """
                package $packageName.sample
                
                import android.graphics.Color
                import android.os.Bundle
                import android.widget.SeekBar
                import androidx.appcompat.app.AppCompatActivity
                import $packageName.Utils
                import $packageName.sample.databinding.ActivityMainBinding
                
                class MainActivity : AppCompatActivity() {
                    
                    private lateinit var binding: ActivityMainBinding
                    
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        binding = ActivityMainBinding.inflate(layoutInflater)
                        setContentView(binding.root)
                        
                        setupUI()
                    }
                    
                    private fun setupUI() {
                        // Set initial progress
                        binding.circularProgressView.progress = 50f
                        binding.circularProgressView.progressColor = Color.BLUE
                        
                        // Set up progress seekbar
                        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                binding.circularProgressView.progress = progress.toFloat()
                                binding.textViewProgress.text = "${'$'}progress%"
                            }
                            
                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                        })
                        
                        // Set up color buttons
                        binding.buttonBlue.setOnClickListener {
                            binding.circularProgressView.progressColor = Color.BLUE
                            Utils.showToast(this, "Color set to Blue")
                        }
                        
                        binding.buttonRed.setOnClickListener {
                            binding.circularProgressView.progressColor = Color.RED
                            Utils.showToast(this, "Color set to Red")
                        }
                        
                        binding.buttonGreen.setOnClickListener {
                            binding.circularProgressView.progressColor = Color.GREEN
                            Utils.showToast(this, "Color set to Green")
                        }
                    }
                }
            """.trimIndent()
            
            File(sampleJavaDir, "MainActivity.kt").writeText(mainActivity)
            
            // Create res directory structure for sample app
            val sampleResDir = File(sampleMainDir, "res")
            val sampleLayoutDir = File(sampleResDir, "layout")
            val sampleValuesDir = File(sampleResDir, "values")
            sampleLayoutDir.mkdirs()
            sampleValuesDir.mkdirs()
            
            // Create activity_main.xml for sample app
            val activityMain = """
                <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout 
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:app="http://schemas.android.com/apk/res-auto"
                    xmlns:tools="http://schemas.android.com/tools"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:padding="16dp"
                    tools:context=".MainActivity">
                    
                    <TextView
                        android:id="@+id/textViewTitle"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Circular Progress View Demo"
                        android:textSize="20sp"
                        android:textStyle="bold"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toTopOf="parent" />
                    
                    <$packageName.CircularProgressView
                        android:id="@+id/circularProgressView"
                        android:layout_width="200dp"
                        android:layout_height="200dp"
                        android:layout_marginTop="32dp"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toBottomOf="@id/textViewTitle" />
                    
                    <TextView
                        android:id="@+id/textViewProgress"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="50%"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        app:layout_constraintBottom_toBottomOf="@id/circularProgressView"
                        app:layout_constraintEnd_toEndOf="@id/circularProgressView"
                        app:layout_constraintStart_toStartOf="@id/circularProgressView"
                        app:layout_constraintTop_toTopOf="@id/circularProgressView" />
                    
                    <TextView
                        android:id="@+id/textViewProgressLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="32dp"
                        android:text="Progress:"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toBottomOf="@id/circularProgressView" />
                    
                    <SeekBar
                        android:id="@+id/seekBarProgress"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        android:max="100"
                        android:progress="50"
                        app:layout_constraintBottom_toBottomOf="@id/textViewProgressLabel"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toEndOf="@id/textViewProgressLabel"
                        app:layout_constraintTop_toTopOf="@id/textViewProgressLabel" />
                    
                    <TextView
                        android:id="@+id/textViewColorLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="24dp"
                        android:text="Color:"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toBottomOf="@id/textViewProgressLabel" />
                    
                    <Button
                        android:id="@+id/buttonBlue"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        android:text="Blue"
                        android:backgroundTint="#2196F3"
                        app:layout_constraintStart_toEndOf="@id/textViewColorLabel"
                        app:layout_constraintTop_toTopOf="@id/textViewColorLabel"
                        app:layout_constraintBottom_toBottomOf="@id/textViewColorLabel" />
                    
                    <Button
                        android:id="@+id/buttonRed"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        android:text="Red"
                        android:backgroundTint="#F44336"
                        app:layout_constraintStart_toEndOf="@id/buttonBlue"
                        app:layout_constraintTop_toTopOf="@id/buttonBlue"
                        app:layout_constraintBottom_toBottomOf="@id/buttonBlue" />
                    
                    <Button
                        android:id="@+id/buttonGreen"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        android:text="Green"
                        android:backgroundTint="#4CAF50"
                        app:layout_constraintStart_toEndOf="@id/buttonRed"
                        app:layout_constraintTop_toTopOf="@id/buttonRed"
                        app:layout_constraintBottom_toBottomOf="@id/buttonRed" />
                    
                </androidx.constraintlayout.widget.ConstraintLayout>
            """.trimIndent()
            
            File(sampleLayoutDir, "activity_main.xml").writeText(activityMain)
            
            // Create strings.xml for sample app
            val strings = """
                <resources>
                    <string name="app_name">$projectName Sample</string>
                </resources>
            """.trimIndent()
            
            File(sampleValuesDir, "strings.xml").writeText(strings)
            
            // Create proguard-rules.pro for library
            val proguardRules = """
                # Add project specific ProGuard rules here.
                # You can control the set of applied configuration files using the
                # proguardFiles setting in build.gradle.
                #
                # For more details, see
                #   http://developer.android.com/guide/developing/tools/proguard.html
                
                # Keep public classes and methods
                -keep public class $packageName.** {
                    public *;
                    protected *;
                }
            """.trimIndent()
            
            File(libraryDir, "proguard-rules.pro").writeText(proguardRules)
            
            // Create consumer-rules.pro for library
            val consumerRules = """
                # Consumer rules for the library
                # These rules will be applied to apps that use this library
                
                # Keep public classes and methods
                -keep public class $packageName.** {
                    public *;
                    protected *;
                }
            """.trimIndent()
            
            File(libraryDir, "consumer-rules.pro").writeText(consumerRules)
            
            // Create proguard-rules.pro for sample app
            File(sampleDir, "proguard-rules.pro").writeText(proguardRules)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}