package com.anyoneide.app.data.room

import androidx.room.*
import com.anyoneide.app.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for UserProfileEntity
 */
@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getUserProfile(id: String): UserProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileEntity)
    
    @Update
    suspend fun updateUserProfile(userProfile: UserProfileEntity)
    
    @Delete
    suspend fun deleteUserProfile(userProfile: UserProfileEntity)
}

/**
 * Data Access Object for ProjectEntity
 */
@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE is_active = 1 ORDER BY last_opened DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: String): ProjectEntity?
    
    @Query("SELECT * FROM projects WHERE path = :path")
    suspend fun getProjectByPath(path: String): ProjectEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)
    
    @Update
    suspend fun updateProject(project: ProjectEntity)
    
    @Query("UPDATE projects SET is_active = 0 WHERE id = :id")
    suspend fun deleteProject(id: String)
    
    @Query("UPDATE projects SET last_opened = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: String, timestamp: Long = System.currentTimeMillis())
}

/**
 * Data Access Object for ProjectFileEntity
 */
@Dao
interface ProjectFileDao {
    @Query("SELECT * FROM project_files WHERE project_id = :projectId")
    fun getProjectFiles(projectId: String): Flow<List<ProjectFileEntity>>
    
    @Query("SELECT * FROM project_files WHERE project_id = :projectId AND is_open = 1")
    suspend fun getOpenFiles(projectId: String): List<ProjectFileEntity>
    
    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getFile(id: String): ProjectFileEntity?
    
    @Query("SELECT * FROM project_files WHERE project_id = :projectId AND relative_path = :relativePath")
    suspend fun getFileByPath(projectId: String, relativePath: String): ProjectFileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)
    
    @Update
    suspend fun updateFile(file: ProjectFileEntity)
    
    @Delete
    suspend fun deleteFile(file: ProjectFileEntity)
    
    @Query("UPDATE project_files SET is_open = :isOpen WHERE id = :id")
    suspend fun updateFileOpenStatus(id: String, isOpen: Boolean)
    
    @Query("UPDATE project_files SET cursor_position = :position WHERE id = :id")
    suspend fun updateCursorPosition(id: String, position: Int)
    
    @Query("UPDATE project_files SET content = :content, is_modified = :isModified, updated_at = :timestamp WHERE id = :id")
    suspend fun updateFileContent(id: String, content: String, isModified: Boolean = true, timestamp: Long = System.currentTimeMillis())
}

/**
 * Data Access Object for BuildConfigurationEntity
 */
@Dao
interface BuildConfigurationDao {
    @Query("SELECT * FROM build_configurations WHERE project_id = :projectId ORDER BY is_default DESC")
    fun getBuildConfigurations(projectId: String): Flow<List<BuildConfigurationEntity>>
    
    @Query("SELECT * FROM build_configurations WHERE id = :id")
    suspend fun getBuildConfiguration(id: String): BuildConfigurationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildConfiguration(buildConfiguration: BuildConfigurationEntity)
    
    @Update
    suspend fun updateBuildConfiguration(buildConfiguration: BuildConfigurationEntity)
    
    @Delete
    suspend fun deleteBuildConfiguration(buildConfiguration: BuildConfigurationEntity)
}

/**
 * Data Access Object for BuildHistoryEntity
 */
@Dao
interface BuildHistoryDao {
    @Query("SELECT * FROM build_history WHERE project_id = :projectId ORDER BY started_at DESC LIMIT :limit")
    fun getBuildHistory(projectId: String, limit: Int = 50): Flow<List<BuildHistoryEntity>>
    
    @Query("SELECT * FROM build_history WHERE id = :id")
    suspend fun getBuildRecord(id: String): BuildHistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildRecord(buildRecord: BuildHistoryEntity)
    
    @Update
    suspend fun updateBuildRecord(buildRecord: BuildHistoryEntity)
    
    @Delete
    suspend fun deleteBuildRecord(buildRecord: BuildHistoryEntity)
}

/**
 * Data Access Object for TerminalSessionEntity
 */
@Dao
interface TerminalSessionDao {
    @Query("SELECT * FROM terminal_sessions WHERE is_active = 1 ORDER BY created_at DESC")
    fun getActiveSessions(): Flow<List<TerminalSessionEntity>>
    
    @Query("SELECT * FROM terminal_sessions WHERE id = :id")
    suspend fun getSession(id: String): TerminalSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TerminalSessionEntity)
    
    @Update
    suspend fun updateSession(session: TerminalSessionEntity)
    
    @Query("UPDATE terminal_sessions SET is_active = 0 WHERE id = :id")
    suspend fun closeSession(id: String)
    
    @Delete
    suspend fun deleteSession(session: TerminalSessionEntity)
}

/**
 * Data Access Object for TerminalCommandEntity
 */
@Dao
interface TerminalCommandDao {
    @Query("SELECT * FROM terminal_commands WHERE session_id = :sessionId ORDER BY executed_at ASC")
    fun getSessionCommands(sessionId: String): Flow<List<TerminalCommandEntity>>
    
    @Query("SELECT * FROM terminal_commands ORDER BY executed_at DESC LIMIT :limit")
    suspend fun getCommandHistory(limit: Int = 50): List<TerminalCommandEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: TerminalCommandEntity)
    
    @Query("DELETE FROM terminal_commands WHERE session_id = :sessionId")
    suspend fun deleteSessionCommands(sessionId: String)
    
    @Query("DELETE FROM terminal_commands WHERE session_id = :sessionId AND id NOT IN (SELECT id FROM terminal_commands WHERE session_id = :sessionId ORDER BY executed_at DESC LIMIT :keepCount)")
    suspend fun cleanupOldCommands(sessionId: String, keepCount: Int = 1000)
}

/**
 * Data Access Object for PluginEntity
 */
@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins WHERE is_verified = 1 ORDER BY download_count DESC LIMIT :limit")
    fun getAvailablePlugins(limit: Int = 50): Flow<List<PluginEntity>>
    
    @Query("SELECT * FROM plugins WHERE category = :category AND is_verified = 1 ORDER BY rating DESC")
    suspend fun getPluginsByCategory(category: String): List<PluginEntity>
    
    @Query("SELECT * FROM plugins WHERE id = :id")
    suspend fun getPlugin(id: String): PluginEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: PluginEntity)
    
    @Update
    suspend fun updatePlugin(plugin: PluginEntity)
    
    @Delete
    suspend fun deletePlugin(plugin: PluginEntity)
    
    @Query("UPDATE plugins SET download_count = download_count + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: String)
}

/**
 * Data Access Object for UserPluginEntity
 */
@Dao
interface UserPluginDao {
    @Query("SELECT * FROM user_plugins ORDER BY installed_at DESC")
    fun getUserPlugins(): Flow<List<UserPluginEntity>>
    
    @Query("SELECT * FROM user_plugins WHERE plugin_id = :pluginId")
    suspend fun getUserPlugin(pluginId: String): UserPluginEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPlugin(userPlugin: UserPluginEntity)
    
    @Update
    suspend fun updateUserPlugin(userPlugin: UserPluginEntity)
    
    @Query("UPDATE user_plugins SET is_enabled = :isEnabled WHERE plugin_id = :pluginId")
    suspend fun updatePluginStatus(pluginId: String, isEnabled: Boolean)
    
    @Delete
    suspend fun deleteUserPlugin(userPlugin: UserPluginEntity)
    
    @Query("DELETE FROM user_plugins WHERE plugin_id = :pluginId")
    suspend fun uninstallPlugin(pluginId: String)
}

/**
 * Data Access Object for ThemeEntity
 */
@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes ORDER BY is_built_in DESC, rating DESC")
    fun getAllThemes(): Flow<List<ThemeEntity>>
    
    @Query("SELECT * FROM themes WHERE is_built_in = 1")
    suspend fun getBuiltInThemes(): List<ThemeEntity>
    
    @Query("SELECT * FROM themes WHERE id = :id")
    suspend fun getTheme(id: String): ThemeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeEntity)
    
    @Update
    suspend fun updateTheme(theme: ThemeEntity)
    
    @Delete
    suspend fun deleteTheme(theme: ThemeEntity)
    
    @Query("UPDATE themes SET download_count = download_count + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: String)
}

/**
 * Data Access Object for ProjectTemplateEntity
 */
@Dao
interface ProjectTemplateDao {
    @Query("SELECT * FROM project_templates WHERE is_verified = 1 ORDER BY usage_count DESC")
    fun getAllTemplates(): Flow<List<ProjectTemplateEntity>>
    
    @Query("SELECT * FROM project_templates WHERE category = :category AND is_verified = 1 ORDER BY rating DESC")
    suspend fun getTemplatesByCategory(category: String): List<ProjectTemplateEntity>
    
    @Query("SELECT * FROM project_templates WHERE id = :id")
    suspend fun getTemplate(id: String): ProjectTemplateEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ProjectTemplateEntity)
    
    @Update
    suspend fun updateTemplate(template: ProjectTemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: ProjectTemplateEntity)
    
    @Query("UPDATE project_templates SET usage_count = usage_count + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)
}

/**
 * Data Access Object for CodeSnippetEntity
 */
@Dao
interface CodeSnippetDao {
    @Query("SELECT * FROM code_snippets ORDER BY created_at DESC")
    fun getAllSnippets(): Flow<List<CodeSnippetEntity>>
    
    @Query("SELECT * FROM code_snippets WHERE is_public = 1 ORDER BY usage_count DESC LIMIT :limit")
    suspend fun getPublicSnippets(limit: Int = 50): List<CodeSnippetEntity>
    
    @Query("SELECT * FROM code_snippets WHERE id = :id")
    suspend fun getSnippet(id: String): CodeSnippetEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: CodeSnippetEntity)
    
    @Update
    suspend fun updateSnippet(snippet: CodeSnippetEntity)
    
    @Delete
    suspend fun deleteSnippet(snippet: CodeSnippetEntity)
    
    @Query("UPDATE code_snippets SET usage_count = usage_count + 1 WHERE id = :id")
    suspend fun incrementUsageCount(id: String)
}

/**
 * Data Access Object for ActivityLogEntity
 */
@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE project_id = :projectId ORDER BY created_at DESC LIMIT :limit")
    fun getProjectLogs(projectId: String?, limit: Int = 100): Flow<List<ActivityLogEntity>>
    
    @Query("SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT :limit")
    fun getAllLogs(limit: Int = 100): Flow<List<ActivityLogEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)
    
    @Query("DELETE FROM activity_logs WHERE created_at < :timestamp")
    suspend fun deleteOldLogs(timestamp: Long)
}

/**
 * Data Access Object for BookmarkedCommandEntity
 */
@Dao
interface BookmarkedCommandDao {
    @Query("SELECT * FROM bookmarked_commands ORDER BY is_favorite DESC, use_count DESC")
    fun getAllBookmarkedCommands(): Flow<List<BookmarkedCommandEntity>>
    
    @Query("SELECT * FROM bookmarked_commands WHERE id = :id")
    suspend fun getBookmarkedCommand(id: String): BookmarkedCommandEntity?
    
    @Query("SELECT * FROM bookmarked_commands WHERE command = :command")
    suspend fun getBookmarkedCommandByCommand(command: String): BookmarkedCommandEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarkedCommand(command: BookmarkedCommandEntity)
    
    @Update
    suspend fun updateBookmarkedCommand(command: BookmarkedCommandEntity)
    
    @Delete
    suspend fun deleteBookmarkedCommand(command: BookmarkedCommandEntity)
    
    @Query("UPDATE bookmarked_commands SET use_count = use_count + 1, last_used = :timestamp, updated_at = :timestamp WHERE id = :id")
    suspend fun incrementUseCount(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE bookmarked_commands SET is_favorite = :isFavorite, updated_at = :timestamp WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM bookmarked_commands WHERE command LIKE :searchQuery OR description LIKE :searchQuery ORDER BY is_favorite DESC, use_count DESC")
    suspend fun searchBookmarkedCommands(searchQuery: String): List<BookmarkedCommandEntity>
}