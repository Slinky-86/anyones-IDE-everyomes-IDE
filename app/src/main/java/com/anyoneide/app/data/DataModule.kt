package com.anyoneide.app.data

import android.content.Context
import com.anyoneide.app.data.repository.*
import com.anyoneide.app.data.room.AppDatabase
import com.anyoneide.app.data.room.RoomRepository

/**
 * Dependency injection module for data repositories
 */
object DataModule {
    
    // Repositories
    private var userRepository: UserRepository? = null
    private var projectRepository: ProjectRepository? = null
    private var pluginRepository: PluginRepository? = null
    private var terminalRepository: TerminalRepository? = null
    private var themeRepository: ThemeRepository? = null
    private var templateRepository: TemplateRepository? = null
    private var codeSnippetRepository: CodeSnippetRepository? = null
    private var activityLogRepository: ActivityLogRepository? = null
    private var storageRepository: StorageRepository? = null
    private var roomRepository: RoomRepository? = null
    private var bookmarkedCommandRepository: BookmarkedCommandRepository? = null
    private var initialized = false
    
    fun initialize(context: Context) {
        if (initialized) return
        
        // Initialize Room database
        val database = AppDatabase.getInstance(context)
        roomRepository = RoomRepository(context)
        
        // Mark as initialized
        initialized = true
    }
    
    fun provideRoomRepository(context: Context): RoomRepository {
        if (roomRepository == null) {
            initialize(context)
        }
        return roomRepository ?: RoomRepository(context)
    }
    
    fun provideUserRepository(context: Context): UserRepository {
        if (userRepository == null) {
            userRepository = UserRepository(provideRoomRepository(context))
        }
        return userRepository!!
    }
    
    fun provideProjectRepository(context: Context): ProjectRepository {
        if (projectRepository == null) {
            projectRepository = ProjectRepository(provideRoomRepository(context))
        }
        return projectRepository!!
    }
    
    fun providePluginRepository(context: Context): PluginRepository {
        if (pluginRepository == null) {
            pluginRepository = PluginRepository(provideRoomRepository(context))
        }
        return pluginRepository!!
    }
    
    fun provideTerminalRepository(context: Context): TerminalRepository {
        if (terminalRepository == null) {
            terminalRepository = TerminalRepository(provideRoomRepository(context))
        }
        return terminalRepository!!
    }
    
    fun provideThemeRepository(context: Context): ThemeRepository {
        if (themeRepository == null) {
            themeRepository = ThemeRepository(provideRoomRepository(context))
        }
        return themeRepository!!
    }
    
    fun provideTemplateRepository(context: Context): TemplateRepository {
        if (templateRepository == null) {
            templateRepository = TemplateRepository(provideRoomRepository(context))
        }
        return templateRepository!!
    }
    
    fun provideCodeSnippetRepository(context: Context): CodeSnippetRepository {
        if (codeSnippetRepository == null) {
            codeSnippetRepository = CodeSnippetRepository(provideRoomRepository(context))
        }
        return codeSnippetRepository!!
    }
    
    fun provideActivityLogRepository(context: Context): ActivityLogRepository {
        if (activityLogRepository == null) {
            activityLogRepository = ActivityLogRepository(provideRoomRepository(context))
        }
        return activityLogRepository!!
    }
    
    fun provideStorageRepository(context: Context): StorageRepository {
        if (storageRepository == null) {
            storageRepository = StorageRepository(context)
        }
        return storageRepository!!
    }
    
    fun provideBookmarkedCommandRepository(context: Context): BookmarkedCommandRepository {
        if (bookmarkedCommandRepository == null) {
            bookmarkedCommandRepository = BookmarkedCommandRepository(provideRoomRepository(context))
        }
        return bookmarkedCommandRepository!!
    }
}