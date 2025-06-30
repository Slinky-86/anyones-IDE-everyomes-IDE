package com.anyoneide.app.data.room

import android.content.Context
import com.anyoneide.app.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for accessing Room database
 */
class RoomRepository(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val gson = Gson()
    
    // DAOs
    private val userProfileDao by lazy { database?.userProfileDao() }
    private val projectDao by lazy { database?.projectDao() }
    private val projectFileDao by lazy { database?.projectFileDao() }
    private val buildConfigurationDao by lazy { database?.buildConfigurationDao() }
    private val buildHistoryDao by lazy { database?.buildHistoryDao() }
    private val terminalSessionDao by lazy { database?.terminalSessionDao() }
    private val terminalCommandDao by lazy { database?.terminalCommandDao() }
    private val pluginDao by lazy { database?.pluginDao() }
    private val userPluginDao by lazy { database?.userPluginDao() }
    private val themeDao by lazy { database?.themeDao() }
    private val projectTemplateDao by lazy { database?.projectTemplateDao() }
    private val codeSnippetDao by lazy { database?.codeSnippetDao() }
    private val activityLogDao by lazy { database?.activityLogDao() }
    private val bookmarkedCommandDao by lazy { database?.bookmarkedCommandDao() }
    
    // User Profile
    suspend fun getUserProfile(id: String): UserProfileEntity? {
        return userProfileDao?.getUserProfile(id)
    }
    
    suspend fun saveUserProfile(userProfile: UserProfileEntity) {
        userProfileDao?.insertUserProfile(userProfile)
    }
    
    suspend fun updateUserProfile(userProfile: UserProfileEntity) {
        userProfileDao?.updateUserProfile(userProfile)
    }
    
    suspend fun deleteUserProfile(userProfile: UserProfileEntity) {
        userProfileDao?.deleteUserProfile(userProfile)
    }
    
    fun getUserProfileUpdates(userId: String): Flow<UserProfileEntity?> {
        // Room doesn't provide a direct way to observe a single entity
        // In a real implementation, we would create a DAO method for this
        return emptyFlow() // userId is used in the method signature for API consistency
    }
    
    // Projects
    fun getAllProjects(): Flow<List<ProjectEntity>> {
        return projectDao?.getAllProjects() ?: emptyFlow()
    }
    
    suspend fun getProject(id: String): ProjectEntity? {
        return projectDao?.getProject(id)
    }
    
    suspend fun getProjectByPath(path: String): ProjectEntity? {
        return projectDao?.getProjectByPath(path)
    }
    
    suspend fun saveProject(project: ProjectEntity) {
        projectDao?.insertProject(project)
    }
    
    suspend fun updateProject(project: ProjectEntity) {
        projectDao?.updateProject(project)
    }
    
    suspend fun deleteProject(id: String) {
        projectDao?.deleteProject(id)
    }
    
    suspend fun updateLastOpened(id: String) {
        projectDao?.updateLastOpened(id)
    }
    
    fun getProjectUpdates(projectId: String): Flow<ProjectEntity?> {
        // Room doesn't provide a direct way to observe a single entity
        // In a real implementation, we would create a DAO method for this
        return emptyFlow() // projectId is used in the method signature for API consistency
    }
    
    // Project Files
    fun getProjectFiles(projectId: String): Flow<List<ProjectFileEntity>> {
        return projectFileDao?.getProjectFiles(projectId) ?: emptyFlow()
    }
    
    suspend fun getOpenFiles(projectId: String): List<ProjectFileEntity> {
        return projectFileDao?.getOpenFiles(projectId) ?: emptyList()
    }
    
    suspend fun getFile(id: String): ProjectFileEntity? {
        return projectFileDao?.getFile(id)
    }
    
    suspend fun getFileByPath(projectId: String, relativePath: String): ProjectFileEntity? {
        return projectFileDao?.getFileByPath(projectId, relativePath)
    }
    
    suspend fun saveFile(file: ProjectFileEntity) {
        projectFileDao?.insertFile(file)
    }
    
    suspend fun updateFile(file: ProjectFileEntity) {
        projectFileDao?.updateFile(file)
    }
    
    suspend fun updateFileContent(id: String, content: String, isModified: Boolean = true) {
        projectFileDao?.updateFileContent(id, content, isModified)
    }
    
    suspend fun updateFileOpenStatus(id: String, isOpen: Boolean) {
        projectFileDao?.updateFileOpenStatus(id, isOpen)
    }
    
    suspend fun updateCursorPosition(id: String, position: Int) {
        projectFileDao?.updateCursorPosition(id, position)
    }
    
    suspend fun deleteFile(file: ProjectFileEntity) {
        projectFileDao?.deleteFile(file)
    }
    
    // Build Configurations
    fun getBuildConfigurations(projectId: String): Flow<List<BuildConfigurationEntity>> {
        return buildConfigurationDao?.getBuildConfigurations(projectId) ?: emptyFlow()
    }
    
    suspend fun getBuildConfiguration(id: String): BuildConfigurationEntity? {
        return buildConfigurationDao?.getBuildConfiguration(id)
    }
    
    suspend fun saveBuildConfiguration(buildConfiguration: BuildConfigurationEntity) {
        buildConfigurationDao?.insertBuildConfiguration(buildConfiguration)
    }
    
    suspend fun updateBuildConfiguration(buildConfiguration: BuildConfigurationEntity) {
        buildConfigurationDao?.updateBuildConfiguration(buildConfiguration)
    }
    
    suspend fun deleteBuildConfiguration(buildConfiguration: BuildConfigurationEntity) {
        buildConfigurationDao?.deleteBuildConfiguration(buildConfiguration)
    }
    
    // Build History
    fun getBuildHistory(projectId: String, limit: Int = 50): Flow<List<BuildHistoryEntity>> {
        return buildHistoryDao?.getBuildHistory(projectId, limit) ?: emptyFlow()
    }
    
    suspend fun getBuildRecord(id: String): BuildHistoryEntity? {
        return buildHistoryDao?.getBuildRecord(id)
    }
    
    suspend fun saveBuildRecord(buildRecord: BuildHistoryEntity) {
        buildHistoryDao?.insertBuildRecord(buildRecord)
    }
    
    suspend fun updateBuildRecord(buildRecord: BuildHistoryEntity) {
        buildHistoryDao?.updateBuildRecord(buildRecord)
    }
    
    suspend fun deleteBuildRecord(buildRecord: BuildHistoryEntity) {
        buildHistoryDao?.deleteBuildRecord(buildRecord)
    }
    
    // Terminal Sessions
    fun getActiveSessions(): Flow<List<TerminalSessionEntity>> {
        return terminalSessionDao?.getActiveSessions() ?: emptyFlow()
    }
    
    suspend fun getSession(id: String): TerminalSessionEntity? {
        return terminalSessionDao?.getSession(id)
    }
    
    suspend fun saveSession(session: TerminalSessionEntity) {
        terminalSessionDao?.insertSession(session)
    }
    
    suspend fun updateSession(session: TerminalSessionEntity) {
        terminalSessionDao?.updateSession(session)
    }
    
    suspend fun closeSession(id: String) {
        terminalSessionDao?.closeSession(id)
    }
    
    suspend fun deleteSession(session: TerminalSessionEntity) {
        terminalSessionDao?.deleteSession(session)
    }
    
    // Terminal Commands
    fun getSessionCommands(sessionId: String): Flow<List<TerminalCommandEntity>> {
        return terminalCommandDao?.getSessionCommands(sessionId) ?: emptyFlow()
    }
    
    suspend fun getCommandHistory(limit: Int = 50): List<TerminalCommandEntity> {
        return terminalCommandDao?.getCommandHistory(limit) ?: emptyList()
    }
    
    suspend fun saveCommand(command: TerminalCommandEntity) {
        terminalCommandDao?.insertCommand(command)
    }
    
    suspend fun deleteSessionCommands(sessionId: String) {
        terminalCommandDao?.deleteSessionCommands(sessionId)
    }
    
    suspend fun cleanupOldCommands(sessionId: String, keepCount: Int = 1000) {
        terminalCommandDao?.cleanupOldCommands(sessionId, keepCount)
    }
    
    // Plugins
    fun getAvailablePlugins(limit: Int = 50): Flow<List<PluginEntity>> {
        return pluginDao?.getAvailablePlugins(limit) ?: emptyFlow()
    }
    
    suspend fun getPluginsByCategory(category: String): List<PluginEntity> {
        return pluginDao?.getPluginsByCategory(category) ?: emptyList()
    }
    
    suspend fun getPlugin(id: String): PluginEntity? {
        return pluginDao?.getPlugin(id)
    }
    
    suspend fun savePlugin(plugin: PluginEntity) {
        pluginDao?.insertPlugin(plugin)
    }
    
    suspend fun updatePlugin(plugin: PluginEntity) {
        pluginDao?.updatePlugin(plugin)
    }
    
    suspend fun deletePlugin(plugin: PluginEntity) {
        pluginDao?.deletePlugin(plugin)
    }
    
    suspend fun incrementPluginDownloadCount(id: String) {
        pluginDao?.incrementDownloadCount(id)
    }
    
    // User Plugins
    fun getUserPlugins(): Flow<List<UserPluginEntity>> {
        return userPluginDao?.getUserPlugins() ?: emptyFlow()
    }
    
    suspend fun getUserPlugin(pluginId: String): UserPluginEntity? {
        return userPluginDao?.getUserPlugin(pluginId)
    }
    
    suspend fun saveUserPlugin(userPlugin: UserPluginEntity) {
        userPluginDao?.insertUserPlugin(userPlugin)
    }
    
    suspend fun updateUserPlugin(userPlugin: UserPluginEntity) {
        userPluginDao?.updateUserPlugin(userPlugin)
    }
    
    suspend fun updatePluginStatus(pluginId: String, isEnabled: Boolean) {
        userPluginDao?.updatePluginStatus(pluginId, isEnabled)
    }
    
    suspend fun uninstallPlugin(pluginId: String) {
        userPluginDao?.uninstallPlugin(pluginId)
    }
    
    // Themes
    fun getAllThemes(): Flow<List<ThemeEntity>> {
        return themeDao?.getAllThemes() ?: emptyFlow()
    }
    
    suspend fun getBuiltInThemes(): List<ThemeEntity> {
        return themeDao?.getBuiltInThemes() ?: emptyList()
    }
    
    suspend fun getTheme(id: String): ThemeEntity? {
        return themeDao?.getTheme(id)
    }
    
    suspend fun saveTheme(theme: ThemeEntity) {
        themeDao?.insertTheme(theme)
    }
    
    suspend fun updateTheme(theme: ThemeEntity) {
        themeDao?.updateTheme(theme)
    }
    
    suspend fun deleteTheme(theme: ThemeEntity) {
        themeDao?.deleteTheme(theme)
    }
    
    suspend fun incrementThemeDownloadCount(id: String) {
        themeDao?.incrementDownloadCount(id)
    }
    
    // Project Templates
    fun getAllTemplates(): Flow<List<ProjectTemplateEntity>> {
        return projectTemplateDao?.getAllTemplates() ?: emptyFlow()
    }
    
    suspend fun getTemplatesByCategory(category: String): List<ProjectTemplateEntity> {
        return projectTemplateDao?.getTemplatesByCategory(category) ?: emptyList()
    }
    
    suspend fun getTemplate(id: String): ProjectTemplateEntity? {
        return projectTemplateDao?.getTemplate(id)
    }
    
    suspend fun saveTemplate(template: ProjectTemplateEntity) {
        projectTemplateDao?.insertTemplate(template)
    }
    
    suspend fun updateTemplate(template: ProjectTemplateEntity) {
        projectTemplateDao?.updateTemplate(template)
    }
    
    suspend fun deleteTemplate(template: ProjectTemplateEntity) {
        projectTemplateDao?.deleteTemplate(template)
    }
    
    suspend fun incrementTemplateUsageCount(id: String) {
        projectTemplateDao?.incrementUsageCount(id)
    }
    
    // Code Snippets
    fun getAllSnippets(): Flow<List<CodeSnippetEntity>> {
        return codeSnippetDao?.getAllSnippets() ?: emptyFlow()
    }
    
    suspend fun getPublicSnippets(limit: Int = 50): List<CodeSnippetEntity> {
        return codeSnippetDao?.getPublicSnippets(limit) ?: emptyList()
    }
    
    suspend fun getSnippet(id: String): CodeSnippetEntity? {
        return codeSnippetDao?.getSnippet(id)
    }
    
    suspend fun saveSnippet(snippet: CodeSnippetEntity) {
        codeSnippetDao?.insertSnippet(snippet)
    }
    
    suspend fun updateSnippet(snippet: CodeSnippetEntity) {
        codeSnippetDao?.updateSnippet(snippet)
    }
    
    suspend fun deleteSnippet(snippet: CodeSnippetEntity) {
        codeSnippetDao?.deleteSnippet(snippet)
    }
    
    suspend fun incrementSnippetUsageCount(id: String) {
        codeSnippetDao?.incrementUsageCount(id)
    }
    
    // Activity Logs
    fun getProjectLogs(projectId: String?, limit: Int = 100): Flow<List<ActivityLogEntity>> {
        return if (projectId != null) {
            activityLogDao?.getProjectLogs(projectId, limit) ?: emptyFlow()
        } else {
            // For user logs, we'll return all logs regardless of project
            activityLogDao?.getAllLogs(limit) ?: emptyFlow()
        }
    }
    
    suspend fun saveLog(log: ActivityLogEntity) {
        activityLogDao?.insertLog(log)
    }
    
    suspend fun deleteOldLogs(timestamp: Long) {
        activityLogDao?.deleteOldLogs(timestamp)
    }
    
    // Bookmarked Commands
    fun getAllBookmarkedCommands(): Flow<List<BookmarkedCommandEntity>> {
        return bookmarkedCommandDao?.getAllBookmarkedCommands() ?: emptyFlow()
    }
    
    suspend fun getBookmarkedCommand(id: String): BookmarkedCommandEntity? {
        return bookmarkedCommandDao?.getBookmarkedCommand(id)
    }
    
    suspend fun getBookmarkedCommandByCommand(command: String): BookmarkedCommandEntity? {
        return bookmarkedCommandDao?.getBookmarkedCommandByCommand(command)
    }
    
    suspend fun saveBookmarkedCommand(command: BookmarkedCommandEntity) {
        bookmarkedCommandDao?.insertBookmarkedCommand(command)
    }
    
    suspend fun updateBookmarkedCommand(command: BookmarkedCommandEntity) {
        bookmarkedCommandDao?.updateBookmarkedCommand(command)
    }
    
    suspend fun deleteBookmarkedCommand(command: BookmarkedCommandEntity) {
        bookmarkedCommandDao?.deleteBookmarkedCommand(command)
    }
    
    suspend fun incrementCommandUseCount(id: String) {
        bookmarkedCommandDao?.incrementUseCount(id)
    }
    
    suspend fun updateCommandFavoriteStatus(id: String, isFavorite: Boolean) {
        bookmarkedCommandDao?.updateFavoriteStatus(id, isFavorite)
    }
    
    suspend fun searchBookmarkedCommands(searchQuery: String): List<BookmarkedCommandEntity> {
        return bookmarkedCommandDao?.searchBookmarkedCommands(searchQuery) ?: emptyList()
    }
    
    // Helper methods for JSON conversion
    fun stringListToJson(list: List<String>): String {
        return gson.toJson(list)
    }
    
    fun jsonToStringList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun mapToJson(map: Map<String, String>): String {
        return gson.toJson(map)
    }
    
    fun jsonToMap(json: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
    
    // Generate unique ID
    fun generateId(): String {
        return UUID.randomUUID().toString()
    }
}