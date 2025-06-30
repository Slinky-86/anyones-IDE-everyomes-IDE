package com.anyoneide.app.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.anyoneide.app.model.*

/**
 * Main database for the application
 */
@Database(
    entities = [
        UserProfileEntity::class,
        ProjectEntity::class,
        ProjectFileEntity::class,
        BuildConfigurationEntity::class,
        BuildHistoryEntity::class,
        TerminalSessionEntity::class,
        TerminalCommandEntity::class,
        PluginEntity::class,
        UserPluginEntity::class,
        ThemeEntity::class,
        ProjectTemplateEntity::class,
        CodeSnippetEntity::class,
        ActivityLogEntity::class,
        BookmarkedCommandEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun userProfileDao(): UserProfileDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileDao(): ProjectFileDao
    abstract fun buildConfigurationDao(): BuildConfigurationDao
    abstract fun buildHistoryDao(): BuildHistoryDao
    abstract fun terminalSessionDao(): TerminalSessionDao
    abstract fun terminalCommandDao(): TerminalCommandDao
    abstract fun pluginDao(): PluginDao
    abstract fun userPluginDao(): UserPluginDao
    abstract fun themeDao(): ThemeDao
    abstract fun projectTemplateDao(): ProjectTemplateDao
    abstract fun codeSnippetDao(): CodeSnippetDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun bookmarkedCommandDao(): BookmarkedCommandDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anyone_ide_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}