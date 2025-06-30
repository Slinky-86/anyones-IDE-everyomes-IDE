package com.anyoneide.app.core.templates.android

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating an Android app with MVVM architecture
 */
class MvvmAppTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "mvvm_app"
    
    override fun getName(): String = "MVVM Architecture"
    
    override fun getDescription(): String = "Android app following MVVM pattern with Repository, ViewModel, and LiveData."
    
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
                    id 'kotlin-kapt'
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
                    
                    buildFeatures {
                        viewBinding true
                    }
                }
                
                dependencies {
                    implementation 'androidx.core:core-ktx:1.12.0'
                    implementation 'androidx.appcompat:appcompat:1.6.1'
                    implementation 'com.google.android.material:material:1.11.0'
                    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
                    
                    // ViewModel and LiveData
                    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
                    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
                    implementation 'androidx.activity:activity-ktx:1.8.2'
                    implementation 'androidx.fragment:fragment-ktx:1.6.2'
                    
                    // Room
                    implementation 'androidx.room:room-runtime:2.6.1'
                    implementation 'androidx.room:room-ktx:2.6.1'
                    kapt 'androidx.room:room-compiler:2.6.1'
                    
                    // Coroutines
                    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
                    
                    // Testing
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
                        android:name=".App"
                        android:allowBackup="true"
                        android:icon="@mipmap/ic_launcher"
                        android:label="@string/app_name"
                        android:roundIcon="@mipmap/ic_launcher_round"
                        android:supportsRtl="true"
                        android:theme="@style/Theme.AppCompat.DayNight">
                        
                        <activity
                            android:name=".ui.MainActivity"
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
            
            // Create package structure
            val packagePath = packageName.replace(".", "/")
            val javaDir = File(mainDir, "java/$packagePath")
            
            // Create directories for MVVM structure
            val dataDir = File(javaDir, "data")
            val repositoryDir = File(dataDir, "repository")
            val localDir = File(dataDir, "local")
            val remoteDir = File(dataDir, "remote")
            val modelDir = File(dataDir, "model")
            val uiDir = File(javaDir, "ui")
            val viewmodelDir = File(javaDir, "viewmodel")
            val utilDir = File(javaDir, "util")
            
            dataDir.mkdirs()
            repositoryDir.mkdirs()
            localDir.mkdirs()
            remoteDir.mkdirs()
            modelDir.mkdirs()
            uiDir.mkdirs()
            viewmodelDir.mkdirs()
            utilDir.mkdirs()
            
            // Create App.kt
            val appKt = """
                package $packageName
                
                import android.app.Application
                
                class App : Application() {
                    
                    override fun onCreate() {
                        super.onCreate()
                        // Initialize dependencies here
                    }
                }
            """.trimIndent()
            
            File(javaDir, "App.kt").writeText(appKt)
            
            // Create User model
            val userKt = """
                package $packageName.data.model
                
                import androidx.room.Entity
                import androidx.room.PrimaryKey
                
                @Entity(tableName = "users")
                data class User(
                    @PrimaryKey val id: String,
                    val name: String,
                    val email: String,
                    val avatarUrl: String? = null
                )
            """.trimIndent()
            
            File(modelDir, "User.kt").writeText(userKt)
            
            // Create UserDao
            val userDaoKt = """
                package $packageName.data.local
                
                import androidx.lifecycle.LiveData
                import androidx.room.Dao
                import androidx.room.Insert
                import androidx.room.OnConflictStrategy
                import androidx.room.Query
                import $packageName.data.model.User
                
                @Dao
                interface UserDao {
                    
                    @Query("SELECT * FROM users")
                    fun getAllUsers(): LiveData<List<User>>
                    
                    @Query("SELECT * FROM users WHERE id = :userId")
                    suspend fun getUserById(userId: String): User?
                    
                    @Insert(onConflict = OnConflictStrategy.REPLACE)
                    suspend fun insertUser(user: User)
                    
                    @Query("DELETE FROM users WHERE id = :userId")
                    suspend fun deleteUser(userId: String)
                }
            """.trimIndent()
            
            File(localDir, "UserDao.kt").writeText(userDaoKt)
            
            // Create AppDatabase
            val appDatabaseKt = """
                package $packageName.data.local
                
                import android.content.Context
                import androidx.room.Database
                import androidx.room.Room
                import androidx.room.RoomDatabase
                import $packageName.data.model.User
                
                @Database(entities = [User::class], version = 1, exportSchema = false)
                abstract class AppDatabase : RoomDatabase() {
                    
                    abstract fun userDao(): UserDao
                    
                    companion object {
                        @Volatile
                        private var INSTANCE: AppDatabase? = null
                        
                        fun getInstance(context: Context): AppDatabase {
                            return INSTANCE ?: synchronized(this) {
                                val instance = Room.databaseBuilder(
                                    context.applicationContext,
                                    AppDatabase::class.java,
                                    "app_database"
                                ).build()
                                INSTANCE = instance
                                instance
                            }
                        }
                    }
                }
            """.trimIndent()
            
            File(localDir, "AppDatabase.kt").writeText(appDatabaseKt)
            
            // Create UserRepository
            val userRepositoryKt = """
                package $packageName.data.repository
                
                import androidx.lifecycle.LiveData
                import $packageName.data.local.UserDao
                import $packageName.data.model.User
                
                class UserRepository(private val userDao: UserDao) {
                    
                    val allUsers: LiveData<List<User>> = userDao.getAllUsers()
                    
                    suspend fun getUserById(userId: String): User? {
                        return userDao.getUserById(userId)
                    }
                    
                    suspend fun insertUser(user: User) {
                        userDao.insertUser(user)
                    }
                    
                    suspend fun deleteUser(userId: String) {
                        userDao.deleteUser(userId)
                    }
                }
            """.trimIndent()
            
            File(repositoryDir, "UserRepository.kt").writeText(userRepositoryKt)
            
            // Create UserViewModel
            val userViewModelKt = """
                package $packageName.viewmodel
                
                import android.app.Application
                import androidx.lifecycle.AndroidViewModel
                import androidx.lifecycle.LiveData
                import androidx.lifecycle.viewModelScope
                import $packageName.data.local.AppDatabase
                import $packageName.data.model.User
                import $packageName.data.repository.UserRepository
                import kotlinx.coroutines.launch
                
                class UserViewModel(application: Application) : AndroidViewModel(application) {
                    
                    private val repository: UserRepository
                    val allUsers: LiveData<List<User>>
                    
                    init {
                        val userDao = AppDatabase.getInstance(application).userDao()
                        repository = UserRepository(userDao)
                        allUsers = repository.allUsers
                    }
                    
                    fun insertUser(user: User) = viewModelScope.launch {
                        repository.insertUser(user)
                    }
                    
                    fun deleteUser(userId: String) = viewModelScope.launch {
                        repository.deleteUser(userId)
                    }
                }
            """.trimIndent()
            
            File(viewmodelDir, "UserViewModel.kt").writeText(userViewModelKt)
            
            // Create MainActivity
            val mainActivityKt = """
                package $packageName.ui
                
                import android.os.Bundle
                import androidx.activity.viewModels
                import androidx.appcompat.app.AppCompatActivity
                import androidx.lifecycle.Observer
                import $packageName.data.model.User
                import $packageName.databinding.ActivityMainBinding
                import $packageName.viewmodel.UserViewModel
                import java.util.UUID
                
                class MainActivity : AppCompatActivity() {
                    
                    private lateinit var binding: ActivityMainBinding
                    private val userViewModel: UserViewModel by viewModels()
                    
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        binding = ActivityMainBinding.inflate(layoutInflater)
                        setContentView(binding.root)
                        
                        setupUI()
                        observeViewModel()
                    }
                    
                    private fun setupUI() {
                        binding.buttonAddUser.setOnClickListener {
                            val name = binding.editTextName.text.toString()
                            val email = binding.editTextEmail.text.toString()
                            
                            if (name.isNotEmpty() && email.isNotEmpty()) {
                                val user = User(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    email = email
                                )
                                userViewModel.insertUser(user)
                                
                                // Clear input fields
                                binding.editTextName.text.clear()
                                binding.editTextEmail.text.clear()
                            }
                        }
                    }
                    
                    private fun observeViewModel() {
                        userViewModel.allUsers.observe(this, Observer { users ->
                            // Update UI with users
                            val userText = users.joinToString("\\n") { 
                                "Name: \${it.name}, Email: \${it.email}" 
                            }
                            binding.textViewUsers.text = userText
                        })
                    }
                }
            """.trimIndent()
            
            File(uiDir, "MainActivity.kt").writeText(mainActivityKt)
            
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
                    android:padding="16dp"
                    tools:context=".ui.MainActivity">
                    
                    <TextView
                        android:id="@+id/textViewTitle"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="MVVM Demo"
                        android:textSize="24sp"
                        android:textStyle="bold"
                        app:layout_constraintTop_toTopOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />
                    
                    <EditText
                        android:id="@+id/editTextName"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="24dp"
                        android:hint="Enter name"
                        android:inputType="textPersonName"
                        app:layout_constraintTop_toBottomOf="@id/textViewTitle"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />
                    
                    <EditText
                        android:id="@+id/editTextEmail"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:hint="Enter email"
                        android:inputType="textEmailAddress"
                        app:layout_constraintTop_toBottomOf="@id/editTextName"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />
                    
                    <Button
                        android:id="@+id/buttonAddUser"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="16dp"
                        android:text="Add User"
                        app:layout_constraintTop_toBottomOf="@id/editTextEmail"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent" />
                    
                    <TextView
                        android:id="@+id/textViewUsersTitle"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="32dp"
                        android:text="Users:"
                        android:textSize="18sp"
                        android:textStyle="bold"
                        app:layout_constraintTop_toBottomOf="@id/buttonAddUser"
                        app:layout_constraintStart_toStartOf="parent" />
                    
                    <TextView
                        android:id="@+id/textViewUsers"
                        android:layout_width="0dp"
                        android:layout_height="0dp"
                        android:layout_marginTop="8dp"
                        app:layout_constraintTop_toBottomOf="@id/textViewUsersTitle"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintBottom_toBottomOf="parent" />
                    
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