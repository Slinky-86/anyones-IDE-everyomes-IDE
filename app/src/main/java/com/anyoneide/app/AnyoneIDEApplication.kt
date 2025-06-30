package com.anyoneide.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.anyoneide.app.data.DataModule
import com.anyoneide.app.data.room.AppDatabase
import com.anyoneide.app.viewmodel.EnhancedMainViewModel
import com.anyoneide.app.core.ShizukuManager

class AnyoneIDEApplication : Application(), ViewModelStoreOwner {
    
    private val _viewModelStore = ViewModelStore()

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: Context
            private set
    }
    
    // Keep a reference to the main ViewModel for global access
    lateinit var mainViewModel: EnhancedMainViewModel
    
    // Database instance
    lateinit var database: AppDatabase
    
    // Shizuku manager
    lateinit var shizukuManager: ShizukuManager
    
    override fun onCreate() {
        super.onCreate()

        // Set instance for global access
        instance = this
        
        // Initialize Room database
        database = AppDatabase.getInstance(this)
        
        // Initialize data module
        DataModule.initialize(this)
        
        // Initialize Shizuku manager
        shizukuManager = ShizukuManager(this)
        
        // Initialize the main ViewModel
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        val provider = ViewModelProvider(this, factory)
        mainViewModel = provider[EnhancedMainViewModel::class.java]
    }
    
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore
        
    override fun onTerminate() {
        super.onTerminate()
        
        // Clean up Shizuku resources
        shizukuManager.cleanup()
    }
}