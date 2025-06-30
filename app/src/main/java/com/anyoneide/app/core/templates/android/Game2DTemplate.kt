package com.anyoneide.app.core.templates.android

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating a 2D game using Android Canvas
 */
class Game2DTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "game_2d"
    
    override fun getName(): String = "2D Game"
    
    override fun getDescription(): String = "Simple 2D game using Android Canvas and custom views for game development."
    
    override fun getCategory(): String = "Games"
    
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
                        android:theme="@style/Theme.AppCompat.DayNight.NoActionBar">
                        
                        <activity
                            android:name=".MainActivity"
                            android:exported="true"
                            android:screenOrientation="portrait">
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
            
            // Create game directory structure
            val gameDir = File(javaDir, "game")
            val objectsDir = File(gameDir, "objects")
            val utilDir = File(gameDir, "util")
            gameDir.mkdirs()
            objectsDir.mkdirs()
            utilDir.mkdirs()
            
            // Create MainActivity.kt
            val mainActivity = """
                package $packageName
                
                import android.os.Bundle
                import androidx.appcompat.app.AppCompatActivity
                
                class MainActivity : AppCompatActivity() {
                    
                    private lateinit var gameView: GameView
                    
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        
                        // Create and set the game view
                        gameView = GameView(this)
                        setContentView(gameView)
                    }
                    
                    override fun onResume() {
                        super.onResume()
                        gameView.resume()
                    }
                    
                    override fun onPause() {
                        super.onPause()
                        gameView.pause()
                    }
                }
            """.trimIndent()
            
            File(javaDir, "MainActivity.kt").writeText(mainActivity)
            
            // Create GameView.kt
            val gameView = """
                package $packageName
                
                import android.content.Context
                import android.graphics.Canvas
                import android.graphics.Color
                import android.graphics.Paint
                import android.view.MotionEvent
                import android.view.SurfaceHolder
                import android.view.SurfaceView
                import $packageName.game.GameEngine
                import $packageName.game.objects.Player
                
                class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
                    
                    private val gameEngine: GameEngine
                    private val gameThread: GameThread
                    
                    init {
                        // Set up the surface holder
                        holder.addCallback(this)
                        
                        // Create the game engine
                        gameEngine = GameEngine(context)
                        
                        // Create the game thread
                        gameThread = GameThread(holder, gameEngine)
                        
                        // Make the view focusable to handle events
                        isFocusable = true
                    }
                    
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        gameEngine.init(width, height)
                        gameThread.running = true
                        gameThread.start()
                    }
                    
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        gameEngine.resize(width, height)
                    }
                    
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        gameThread.running = false
                        try {
                            gameThread.join()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    
                    override fun onTouchEvent(event: MotionEvent): Boolean {
                        gameEngine.handleTouchEvent(event)
                        return true
                    }
                    
                    fun pause() {
                        gameThread.running = false
                        try {
                            gameThread.join()
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                    
                    fun resume() {
                        if (!gameThread.isAlive) {
                            gameThread.running = true
                            gameThread.start()
                        }
                    }
                    
                    private inner class GameThread(
                        private val surfaceHolder: SurfaceHolder,
                        private val gameEngine: GameEngine
                    ) : Thread() {
                        
                        @Volatile
                        var running = false
                        
                        override fun run() {
                            var canvas: Canvas?
                            
                            while (running) {
                                canvas = null
                                
                                try {
                                    // Update game state
                                    gameEngine.update()
                                    
                                    // Draw the game
                                    canvas = surfaceHolder.lockCanvas()
                                    canvas?.let {
                                        synchronized(surfaceHolder) {
                                            gameEngine.draw(it)
                                        }
                                    }
                                } finally {
                                    canvas?.let {
                                        surfaceHolder.unlockCanvasAndPost(it)
                                    }
                                }
                                
                                // Control the frame rate
                                try {
                                    sleep(16) // ~60 FPS
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
            
            File(javaDir, "GameView.kt").writeText(gameView)
            
            // Create GameEngine.kt
            val gameEngine = """
                package $packageName.game
                
                import android.content.Context
                import android.graphics.Canvas
                import android.graphics.Color
                import android.graphics.Paint
                import android.view.MotionEvent
                import $packageName.game.objects.Enemy
                import $packageName.game.objects.Player
                import $packageName.game.util.CollisionDetector
                import kotlin.random.Random
                
                class GameEngine(private val context: Context) {
                    
                    private lateinit var player: Player
                    private val enemies = mutableListOf<Enemy>()
                    private var screenWidth = 0
                    private var screenHeight = 0
                    private var score = 0
                    private var gameOver = false
                    
                    private val backgroundPaint = Paint().apply {
                        color = Color.BLACK
                    }
                    
                    private val textPaint = Paint().apply {
                        color = Color.WHITE
                        textSize = 50f
                        textAlign = Paint.Align.LEFT
                    }
                    
                    private val gameOverPaint = Paint().apply {
                        color = Color.RED
                        textSize = 100f
                        textAlign = Paint.Align.CENTER
                    }
                    
                    private val collisionDetector = CollisionDetector()
                    private var lastEnemySpawnTime = 0L
                    private val enemySpawnInterval = 2000L // 2 seconds
                    
                    fun init(width: Int, height: Int) {
                        screenWidth = width
                        screenHeight = height
                        
                        // Create player
                        player = Player(
                            x = screenWidth / 2f,
                            y = screenHeight - 200f,
                            radius = 50f,
                            color = Color.BLUE
                        )
                        
                        // Reset game state
                        enemies.clear()
                        score = 0
                        gameOver = false
                        lastEnemySpawnTime = System.currentTimeMillis()
                    }
                    
                    fun resize(width: Int, height: Int) {
                        screenWidth = width
                        screenHeight = height
                    }
                    
                    fun update() {
                        if (gameOver) return
                        
                        // Update player
                        player.update()
                        
                        // Keep player within screen bounds
                        player.x = player.x.coerceIn(player.radius, screenWidth - player.radius)
                        
                        // Spawn enemies
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastEnemySpawnTime > enemySpawnInterval) {
                            spawnEnemy()
                            lastEnemySpawnTime = currentTime
                        }
                        
                        // Update enemies
                        val iterator = enemies.iterator()
                        while (iterator.hasNext()) {
                            val enemy = iterator.next()
                            enemy.update()
                            
                            // Remove enemies that are off-screen
                            if (enemy.y > screenHeight + enemy.radius) {
                                iterator.remove()
                                score++
                            }
                            
                            // Check for collisions with player
                            if (collisionDetector.checkCollision(player, enemy)) {
                                gameOver = true
                                break
                            }
                        }
                    }
                    
                    fun draw(canvas: Canvas) {
                        // Clear the canvas
                        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), backgroundPaint)
                        
                        // Draw player
                        player.draw(canvas)
                        
                        // Draw enemies
                        enemies.forEach { it.draw(canvas) }
                        
                        // Draw score
                        canvas.drawText("Score: $score", 50f, 100f, textPaint)
                        
                        // Draw game over message if game is over
                        if (gameOver) {
                            canvas.drawText("GAME OVER", screenWidth / 2f, screenHeight / 2f, gameOverPaint)
                            canvas.drawText("Final Score: $score", screenWidth / 2f, screenHeight / 2f + 150f, gameOverPaint)
                        }
                    }
                    
                    fun handleTouchEvent(event: MotionEvent): Boolean {
                        if (gameOver) {
                            if (event.action == MotionEvent.ACTION_UP) {
                                // Restart game on tap after game over
                                init(screenWidth, screenHeight)
                                return true
                            }
                        } else {
                            when (event.action) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                    // Move player to touch position (x-axis only)
                                    player.x = event.x
                                    return true
                                }
                            }
                        }
                        return false
                    }
                    
                    private fun spawnEnemy() {
                        val x = Random.nextFloat() * (screenWidth - 100) + 50
                        val enemy = Enemy(
                            x = x,
                            y = -50f,
                            radius = 40f,
                            color = Color.RED,
                            speed = Random.nextFloat() * 5 + 5
                        )
                        enemies.add(enemy)
                    }
                }
            """.trimIndent()
            
            File(gameDir, "GameEngine.kt").writeText(gameEngine)
            
            // Create Player.kt
            val player = """
                package $packageName.game.objects
                
                import android.graphics.Canvas
                import android.graphics.Paint
                
                class Player(
                    var x: Float,
                    var y: Float,
                    val radius: Float,
                    color: Int
                ) : GameObject() {
                    
                    private val paint = Paint().apply {
                        this.color = color
                        isAntiAlias = true
                    }
                    
                    override fun update() {
                        // Player is controlled by touch input
                    }
                    
                    override fun draw(canvas: Canvas) {
                        canvas.drawCircle(x, y, radius, paint)
                    }
                }
            """.trimIndent()
            
            File(objectsDir, "Player.kt").writeText(player)
            
            // Create Enemy.kt
            val enemy = """
                package $packageName.game.objects
                
                import android.graphics.Canvas
                import android.graphics.Paint
                
                class Enemy(
                    var x: Float,
                    var y: Float,
                    val radius: Float,
                    color: Int,
                    private val speed: Float
                ) : GameObject() {
                    
                    private val paint = Paint().apply {
                        this.color = color
                        isAntiAlias = true
                    }
                    
                    override fun update() {
                        // Move enemy down
                        y += speed
                    }
                    
                    override fun draw(canvas: Canvas) {
                        canvas.drawCircle(x, y, radius, paint)
                    }
                }
            """.trimIndent()
            
            File(objectsDir, "Enemy.kt").writeText(enemy)
            
            // Create GameObject.kt
            val gameObject = """
                package $packageName.game.objects
                
                import android.graphics.Canvas
                
                abstract class GameObject {
                    abstract fun update()
                    abstract fun draw(canvas: Canvas)
                }
            """.trimIndent()
            
            File(objectsDir, "GameObject.kt").writeText(gameObject)
            
            // Create CollisionDetector.kt
            val collisionDetector = """
                package $packageName.game.util
                
                import $packageName.game.objects.Enemy
                import $packageName.game.objects.Player
                import kotlin.math.pow
                import kotlin.math.sqrt
                
                class CollisionDetector {
                    
                    fun checkCollision(player: Player, enemy: Enemy): Boolean {
                        // Calculate distance between centers
                        val distance = sqrt(
                            (player.x - enemy.x).pow(2) +
                            (player.y - enemy.y).pow(2)
                        )
                        
                        // Check if distance is less than sum of radii
                        return distance < (player.radius + enemy.radius)
                    }
                }
            """.trimIndent()
            
            File(utilDir, "CollisionDetector.kt").writeText(collisionDetector)
            
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