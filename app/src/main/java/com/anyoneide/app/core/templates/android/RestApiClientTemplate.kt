package com.anyoneide.app.core.templates.android

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating a REST API client application
 */
class RestApiClientTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "rest_api_client"
    
    override fun getName(): String = "REST API Client"
    
    override fun getDescription(): String = "Android app that consumes REST APIs with Retrofit and displays data in RecyclerView."
    
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
                    
                    // Retrofit for API calls
                    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
                    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
                    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
                    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
                    
                    // Coroutines
                    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
                    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
                    
                    // ViewModel and LiveData
                    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
                    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
                    implementation 'androidx.activity:activity-ktx:1.8.2'
                    
                    // Glide for image loading
                    implementation 'com.github.bumptech.glide:glide:4.16.0'
                    kapt 'com.github.bumptech.glide:compiler:4.16.0'
                    
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
                    
                    <uses-permission android:name="android.permission.INTERNET" />
                    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
                    
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
                        
                        <activity
                            android:name=".ui.DetailActivity"
                            android:exported="false" />
                        
                    </application>
                    
                </manifest>
            """.trimIndent()
            
            File(mainDir, "AndroidManifest.xml").writeText(manifest)
            
            // Create package structure
            val packagePath = packageName.replace(".", "/")
            val javaDir = File(mainDir, "java/$packagePath")
            
            // Create directories for app structure
            val dataDir = File(javaDir, "data")
            val apiDir = File(dataDir, "api")
            val modelDir = File(dataDir, "model")
            val repositoryDir = File(dataDir, "repository")
            val uiDir = File(javaDir, "ui")
            val adapterDir = File(uiDir, "adapter")
            val viewmodelDir = File(javaDir, "viewmodel")
            val utilDir = File(javaDir, "util")
            
            dataDir.mkdirs()
            apiDir.mkdirs()
            modelDir.mkdirs()
            repositoryDir.mkdirs()
            uiDir.mkdirs()
            adapterDir.mkdirs()
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
                
                import com.google.gson.annotations.SerializedName
                
                data class User(
                    @SerializedName("id") val id: Int,
                    @SerializedName("name") val name: String,
                    @SerializedName("username") val username: String,
                    @SerializedName("email") val email: String,
                    @SerializedName("phone") val phone: String,
                    @SerializedName("website") val website: String
                )
            """.trimIndent()
            
            File(modelDir, "User.kt").writeText(userKt)
            
            // Create Post model
            val postKt = """
                package $packageName.data.model
                
                import com.google.gson.annotations.SerializedName
                
                data class Post(
                    @SerializedName("id") val id: Int,
                    @SerializedName("userId") val userId: Int,
                    @SerializedName("title") val title: String,
                    @SerializedName("body") val body: String
                )
            """.trimIndent()
            
            File(modelDir, "Post.kt").writeText(postKt)
            
            // Create ApiService
            val apiServiceKt = """
                package $packageName.data.api
                
                import $packageName.data.model.Post
                import $packageName.data.model.User
                import retrofit2.http.GET
                import retrofit2.http.Path
                
                interface ApiService {
                    
                    @GET("users")
                    suspend fun getUsers(): List<User>
                    
                    @GET("users/{userId}")
                    suspend fun getUser(@Path("userId") userId: Int): User
                    
                    @GET("posts")
                    suspend fun getPosts(): List<Post>
                    
                    @GET("users/{userId}/posts")
                    suspend fun getUserPosts(@Path("userId") userId: Int): List<Post>
                }
            """.trimIndent()
            
            File(apiDir, "ApiService.kt").writeText(apiServiceKt)
            
            // Create RetrofitClient
            val retrofitClientKt = """
                package $packageName.data.api
                
                import okhttp3.OkHttpClient
                import okhttp3.logging.HttpLoggingInterceptor
                import retrofit2.Retrofit
                import retrofit2.converter.gson.GsonConverterFactory
                import java.util.concurrent.TimeUnit
                
                object RetrofitClient {
                    
                    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"
                    
                    private val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build()
                    
                    private val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    
                    val apiService: ApiService = retrofit.create(ApiService::class.java)
                }
            """.trimIndent()
            
            File(apiDir, "RetrofitClient.kt").writeText(retrofitClientKt)
            
            // Create UserRepository
            val userRepositoryKt = """
                package $packageName.data.repository
                
                import $packageName.data.api.RetrofitClient
                import $packageName.data.model.User
                
                class UserRepository {
                    
                    private val apiService = RetrofitClient.apiService
                    
                    suspend fun getUsers(): List<User> {
                        return apiService.getUsers()
                    }
                    
                    suspend fun getUser(userId: Int): User {
                        return apiService.getUser(userId)
                    }
                }
            """.trimIndent()
            
            File(repositoryDir, "UserRepository.kt").writeText(userRepositoryKt)
            
            // Create PostRepository
            val postRepositoryKt = """
                package $packageName.data.repository
                
                import $packageName.data.api.RetrofitClient
                import $packageName.data.model.Post
                
                class PostRepository {
                    
                    private val apiService = RetrofitClient.apiService
                    
                    suspend fun getPosts(): List<Post> {
                        return apiService.getPosts()
                    }
                    
                    suspend fun getUserPosts(userId: Int): List<Post> {
                        return apiService.getUserPosts(userId)
                    }
                }
            """.trimIndent()
            
            File(repositoryDir, "PostRepository.kt").writeText(postRepositoryKt)
            
            // Create UserViewModel
            val userViewModelKt = """
                package $packageName.viewmodel
                
                import androidx.lifecycle.LiveData
                import androidx.lifecycle.MutableLiveData
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.viewModelScope
                import $packageName.data.model.User
                import $packageName.data.repository.UserRepository
                import kotlinx.coroutines.launch
                
                class UserViewModel : ViewModel() {
                    
                    private val repository = UserRepository()
                    
                    private val _users = MutableLiveData<List<User>>()
                    val users: LiveData<List<User>> = _users
                    
                    private val _loading = MutableLiveData<Boolean>()
                    val loading: LiveData<Boolean> = _loading
                    
                    private val _error = MutableLiveData<String>()
                    val error: LiveData<String> = _error
                    
                    init {
                        loadUsers()
                    }
                    
                    fun loadUsers() {
                        viewModelScope.launch {
                            try {
                                _loading.value = true
                                _users.value = repository.getUsers()
                                _loading.value = false
                            } catch (e: Exception) {
                                _loading.value = false
                                _error.value = e.message ?: "Unknown error"
                            }
                        }
                    }
                    
                    fun getUser(userId: Int, callback: (User) -> Unit) {
                        viewModelScope.launch {
                            try {
                                val user = repository.getUser(userId)
                                callback(user)
                            } catch (e: Exception) {
                                _error.value = e.message ?: "Unknown error"
                            }
                        }
                    }
                }
            """.trimIndent()
            
            File(viewmodelDir, "UserViewModel.kt").writeText(userViewModelKt)
            
            // Create UserAdapter
            val userAdapterKt = """
                package $packageName.ui.adapter
                
                import android.view.LayoutInflater
                import android.view.ViewGroup
                import androidx.recyclerview.widget.DiffUtil
                import androidx.recyclerview.widget.ListAdapter
                import androidx.recyclerview.widget.RecyclerView
                import $packageName.data.model.User
                import $packageName.databinding.ItemUserBinding
                
                class UserAdapter(private val onUserClicked: (User) -> Unit) : 
                    ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {
                    
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
                        val binding = ItemUserBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        )
                        return UserViewHolder(binding)
                    }
                    
                    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
                        holder.bind(getItem(position))
                    }
                    
                    inner class UserViewHolder(private val binding: ItemUserBinding) : 
                        RecyclerView.ViewHolder(binding.root) {
                        
                        init {
                            binding.root.setOnClickListener {
                                val position = bindingAdapterPosition
                                if (position != RecyclerView.NO_POSITION) {
                                    onUserClicked(getItem(position))
                                }
                            }
                        }
                        
                        fun bind(user: User) {
                            binding.textViewName.text = user.name
                            binding.textViewEmail.text = user.email
                            binding.textViewWebsite.text = user.website
                        }
                    }
                    
                    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
                        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                            return oldItem.id == newItem.id
                        }
                        
                        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                            return oldItem == newItem
                        }
                    }
                }
            """.trimIndent()
            
            File(adapterDir, "UserAdapter.kt").writeText(userAdapterKt)
            
            // Create MainActivity
            val mainActivityKt = """
                package $packageName.ui
                
                import android.content.Intent
                import android.os.Bundle
                import android.view.View
                import android.widget.Toast
                import androidx.activity.viewModels
                import androidx.appcompat.app.AppCompatActivity
                import androidx.lifecycle.Observer
                import androidx.recyclerview.widget.LinearLayoutManager
                import $packageName.databinding.ActivityMainBinding
                import $packageName.ui.adapter.UserAdapter
                import $packageName.viewmodel.UserViewModel
                
                class MainActivity : AppCompatActivity() {
                    
                    private lateinit var binding: ActivityMainBinding
                    private val viewModel: UserViewModel by viewModels()
                    private lateinit var adapter: UserAdapter
                    
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        binding = ActivityMainBinding.inflate(layoutInflater)
                        setContentView(binding.root)
                        
                        setupRecyclerView()
                        observeViewModel()
                        
                        binding.swipeRefreshLayout.setOnRefreshListener {
                            viewModel.loadUsers()
                        }
                    }
                    
                    private fun setupRecyclerView() {
                        adapter = UserAdapter { user ->
                            // Navigate to detail screen
                            val intent = Intent(this, DetailActivity::class.java)
                            intent.putExtra(DetailActivity.EXTRA_USER_ID, user.id)
                            startActivity(intent)
                        }
                        
                        binding.recyclerView.adapter = adapter
                        binding.recyclerView.layoutManager = LinearLayoutManager(this)
                    }
                    
                    private fun observeViewModel() {
                        viewModel.users.observe(this, Observer { users ->
                            adapter.submitList(users)
                            binding.textViewEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                        })
                        
                        viewModel.loading.observe(this, Observer { isLoading ->
                            binding.swipeRefreshLayout.isRefreshing = isLoading
                        })
                        
                        viewModel.error.observe(this, Observer { errorMessage ->
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        })
                    }
                }
            """.trimIndent()
            
            File(uiDir, "MainActivity.kt").writeText(mainActivityKt)
            
            // Create DetailActivity
            val detailActivityKt = """
                package $packageName.ui
                
                import android.os.Bundle
                import android.view.MenuItem
                import android.widget.Toast
                import androidx.activity.viewModels
                import androidx.appcompat.app.AppCompatActivity
                import $packageName.data.model.User
                import $packageName.databinding.ActivityDetailBinding
                import $packageName.viewmodel.UserViewModel
                
                class DetailActivity : AppCompatActivity() {
                    
                    private lateinit var binding: ActivityDetailBinding
                    private val viewModel: UserViewModel by viewModels()
                    
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        binding = ActivityDetailBinding.inflate(layoutInflater)
                        setContentView(binding.root)
                        
                        supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        
                        val userId = intent.getIntExtra(EXTRA_USER_ID, -1)
                        if (userId != -1) {
                            loadUserDetails(userId)
                        } else {
                            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    
                    private fun loadUserDetails(userId: Int) {
                        viewModel.getUser(userId) { user ->
                            displayUserDetails(user)
                        }
                    }
                    
                    private fun displayUserDetails(user: User) {
                        binding.textViewName.text = user.name
                        binding.textViewUsername.text = user.username
                        binding.textViewEmail.text = user.email
                        binding.textViewPhone.text = user.phone
                        binding.textViewWebsite.text = user.website
                        
                        supportActionBar?.title = user.name
                    }
                    
                    override fun onOptionsItemSelected(item: MenuItem): Boolean {
                        if (item.itemId == android.R.id.home) {
                            onBackPressed()
                            return true
                        }
                        return super.onOptionsItemSelected(item)
                    }
                    
                    companion object {
                        const val EXTRA_USER_ID = "extra_user_id"
                    }
                }
            """.trimIndent()
            
            File(uiDir, "DetailActivity.kt").writeText(detailActivityKt)
            
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
                    tools:context=".ui.MainActivity">
                    
                    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
                        android:id="@+id/swipeRefreshLayout"
                        android:layout_width="0dp"
                        android:layout_height="0dp"
                        app:layout_constraintBottom_toBottomOf="parent"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toTopOf="parent">
                        
                        <androidx.recyclerview.widget.RecyclerView
                            android:id="@+id/recyclerView"
                            android:layout_width="match_parent"
                            android:layout_height="match_parent"
                            android:clipToPadding="false"
                            android:padding="8dp"
                            tools:listitem="@layout/item_user" />
                            
                    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
                    
                    <TextView
                        android:id="@+id/textViewEmpty"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="No users found"
                        android:visibility="gone"
                        app:layout_constraintBottom_toBottomOf="parent"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toTopOf="parent" />
                    
                </androidx.constraintlayout.widget.ConstraintLayout>
            """.trimIndent()
            
            File(layoutDir, "activity_main.xml").writeText(activityMain)
            
            // Create activity_detail.xml
            val activityDetail = """
                <?xml version="1.0" encoding="utf-8"?>
                <androidx.constraintlayout.widget.ConstraintLayout 
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:app="http://schemas.android.com/apk/res-auto"
                    xmlns:tools="http://schemas.android.com/tools"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:padding="16dp"
                    tools:context=".ui.DetailActivity">
                    
                    <TextView
                        android:id="@+id/textViewNameLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Name:"
                        android:textStyle="bold"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toTopOf="parent" />
                    
                    <TextView
                        android:id="@+id/textViewName"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toEndOf="@id/textViewNameLabel"
                        app:layout_constraintTop_toTopOf="@id/textViewNameLabel"
                        tools:text="John Doe" />
                    
                    <TextView
                        android:id="@+id/textViewUsernameLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="16dp"
                        android:text="Username:"
                        android:textStyle="bold"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toBottomOf="@id/textViewNameLabel" />
                    
                    <TextView
                        android:id="@+id/textViewUsername"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toEndOf="@id/textViewUsernameLabel"
                        app:layout_constraintTop_toTopOf="@id/textViewUsernameLabel"
                        tools:text="johndoe" />
                    
                    <TextView
                        android:id="@+id/textViewEmailLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="16dp"
                        android:text="Email:"
                        android:textStyle="bold"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toBottomOf="@id/textViewUsernameLabel" />
                    
                    <TextView
                        android:id="@+id/textViewEmail"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toEndOf="@id/textViewEmailLabel"
                        app:layout_constraintTop_toTopOf="@id/textViewEmailLabel"
                        tools:text="john.doe@example.com" />
                    
                    <TextView
                        android:id="@+id/textViewPhoneLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="16dp"
                        android:text="Phone:"
                        android:textStyle="bold"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toBottomOf="@id/textViewEmailLabel" />
                    
                    <TextView
                        android:id="@+id/textViewPhone"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toEndOf="@id/textViewPhoneLabel"
                        app:layout_constraintTop_toTopOf="@id/textViewPhoneLabel"
                        tools:text="123-456-7890" />
                    
                    <TextView
                        android:id="@+id/textViewWebsiteLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="16dp"
                        android:text="Website:"
                        android:textStyle="bold"
                        app:layout_constraintStart_toStartOf="parent"
                        app:layout_constraintTop_toBottomOf="@id/textViewPhoneLabel" />
                    
                    <TextView
                        android:id="@+id/textViewWebsite"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_marginStart="8dp"
                        app:layout_constraintEnd_toEndOf="parent"
                        app:layout_constraintStart_toEndOf="@id/textViewWebsiteLabel"
                        app:layout_constraintTop_toTopOf="@id/textViewWebsiteLabel"
                        tools:text="www.example.com" />
                    
                </androidx.constraintlayout.widget.ConstraintLayout>
            """.trimIndent()
            
            File(layoutDir, "activity_detail.xml").writeText(activityDetail)
            
            // Create item_user.xml
            val itemUser = """
                <?xml version="1.0" encoding="utf-8"?>
                <androidx.cardview.widget.CardView 
                    xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:app="http://schemas.android.com/apk/res-auto"
                    xmlns:tools="http://schemas.android.com/tools"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_margin="4dp"
                    app:cardCornerRadius="8dp"
                    app:cardElevation="4dp">
                    
                    <androidx.constraintlayout.widget.ConstraintLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:padding="16dp">
                        
                        <TextView
                            android:id="@+id/textViewName"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:textSize="18sp"
                            android:textStyle="bold"
                            app:layout_constraintEnd_toEndOf="parent"
                            app:layout_constraintStart_toStartOf="parent"
                            app:layout_constraintTop_toTopOf="parent"
                            tools:text="John Doe" />
                        
                        <TextView
                            android:id="@+id/textViewEmail"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            app:layout_constraintEnd_toEndOf="parent"
                            app:layout_constraintStart_toStartOf="parent"
                            app:layout_constraintTop_toBottomOf="@id/textViewName"
                            tools:text="john.doe@example.com" />
                        
                        <TextView
                            android:id="@+id/textViewWebsite"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="4dp"
                            android:textColor="@android:color/holo_blue_dark"
                            app:layout_constraintEnd_toEndOf="parent"
                            app:layout_constraintStart_toStartOf="parent"
                            app:layout_constraintTop_toBottomOf="@id/textViewEmail"
                            tools:text="www.example.com" />
                        
                    </androidx.constraintlayout.widget.ConstraintLayout>
                    
                </androidx.cardview.widget.CardView>
            """.trimIndent()
            
            File(layoutDir, "item_user.xml").writeText(itemUser)
            
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
                
                # Retrofit
                -keepattributes Signature, InnerClasses, EnclosingMethod
                -keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
                -keepclassmembers,allowshrinking,allowobfuscation interface * {
                    @retrofit2.http.* <methods>;
                }
                -dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
                -dontwarn javax.annotation.**
                -dontwarn kotlin.Unit
                -dontwarn retrofit2.KotlinExtensions
                -dontwarn retrofit2.KotlinExtensions$*
                -if interface * { @retrofit2.http.* <methods>; }
                -keep,allowobfuscation interface <1>
                
                # OkHttp
                -dontwarn okhttp3.**
                -dontwarn okio.**
                -dontwarn javax.annotation.**
                -dontwarn org.conscrypt.**
                
                # Gson
                -keepattributes Signature
                -keepattributes *Annotation*
                -dontwarn sun.misc.**
                -keep class com.google.gson.examples.android.model.** { <fields>; }
                -keep class * implements com.google.gson.TypeAdapter
                -keep class * implements com.google.gson.TypeAdapterFactory
                -keep class * implements com.google.gson.JsonSerializer
                -keep class * implements com.google.gson.JsonDeserializer
                -keepclassmembers,allowobfuscation class * {
                  @com.google.gson.annotations.SerializedName <fields>;
                }
                
                # Glide
                -keep public class * implements com.bumptech.glide.module.GlideModule
                -keep class * extends com.bumptech.glide.module.AppGlideModule {
                 <init>(...);
                }
                -keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
                  **[] $VALUES;
                  public *;
                }
                -keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
                  *** rewind();
                }
            """.trimIndent()
            
            File(appDir, "proguard-rules.pro").writeText(proguardRules)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}