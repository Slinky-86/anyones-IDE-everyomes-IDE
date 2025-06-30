package com.anyoneide.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

class SettingsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("ide_settings", Context.MODE_PRIVATE)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<IDESettings> = _settings.asStateFlow()
    
    // Gemini API key management
    private val securePrefs: SharedPreferences = context.getSharedPreferences("secure_settings", Context.MODE_PRIVATE)
    
    private fun loadSettings(): IDESettings {
        return IDESettings(
            // Editor Settings
            fontSize = prefs.getInt("editor_font_size", 14),
            tabSize = prefs.getInt("editor_tab_size", 4),
            wordWrap = prefs.getBoolean("editor_word_wrap", false),
            syntaxHighlighting = prefs.getBoolean("editor_syntax_highlighting", true),
            lineNumbers = prefs.getBoolean("editor_line_numbers", true),
            autoComplete = prefs.getBoolean("editor_auto_complete", true),
            autoIndent = prefs.getBoolean("editor_auto_indent", true),
            
            // Terminal Settings
            terminalFontSize = prefs.getInt("terminal_font_size", 12),
            defaultShell = prefs.getString("terminal_default_shell", "/system/bin/sh") ?: "/system/bin/sh",
            terminalScrollback = prefs.getInt("terminal_scrollback", 1000),
            
            // Appearance Settings
            isDarkTheme = prefs.getBoolean("appearance_dark_theme", true),
            showProjectExplorer = prefs.getBoolean("appearance_show_project_explorer", true),
            showToolWindows = prefs.getBoolean("appearance_show_tool_windows", true),
            showBottomPanel = prefs.getBoolean("appearance_show_bottom_panel", true),
            
            // Build Settings
            gradleDaemon = prefs.getBoolean("build_gradle_daemon", true),
            offlineMode = prefs.getBoolean("build_offline_mode", false),
            parallelBuilds = prefs.getBoolean("build_parallel", true),
            customGradleArgs = prefs.getString("build_custom_gradle_args", "") ?: "",
            selectedBuildType = prefs.getString("build_selected_type", "debug") ?: "debug",
            
            // Code Analysis Settings
            enableTodoWarnings = prefs.getBoolean("analysis_todo_warnings", true),
            enableLongLineWarnings = prefs.getBoolean("analysis_long_line_warnings", true),
            longLineThreshold = prefs.getInt("analysis_long_line_threshold", 120),
            enableUnusedImportWarnings = prefs.getBoolean("analysis_unused_import_warnings", true),
            
            // Advanced Settings
            enableRootFeatures = prefs.getBoolean("advanced_root_features", false),
            enableShizukuIntegration = prefs.getBoolean("advanced_shizuku_integration", false),
            autoSave = prefs.getBoolean("advanced_auto_save", true),
            autoSaveInterval = prefs.getInt("advanced_auto_save_interval", 30),
            maxOpenFiles = prefs.getInt("advanced_max_open_files", 20),
            
            // Project Settings
            lastProjectPath = prefs.getString("project_last_path", null),
            rememberOpenFiles = prefs.getBoolean("project_remember_open_files", true),
            autoSaveOnExit = prefs.getBoolean("project_auto_save_on_exit", true),
            
            // AI Settings
            enableAiFeatures = prefs.getBoolean("ai_enable_features", false),
            aiCompletionEnabled = prefs.getBoolean("ai_completion_enabled", false),
            aiExplanationEnabled = prefs.getBoolean("ai_explanation_enabled", false),
            
            // Terminal Enhancement Settings
            enableCommandBookmarks = prefs.getBoolean("terminal_enable_command_bookmarks", true),
            enableTerminalSearch = prefs.getBoolean("terminal_enable_search", true),
            enableTerminalSaveOutput = prefs.getBoolean("terminal_enable_save_output", true),
            enableNativeTerminal = prefs.getBoolean("terminal_enable_native", false)
        )
    }
    
    fun updateSettings(newSettings: IDESettings) {
        _settings.value = newSettings
        saveSettings(newSettings)
    }
    
    private fun saveSettings(settings: IDESettings) {
        prefs.edit().apply {
            // Editor Settings
            putInt("editor_font_size", settings.fontSize)
            putInt("editor_tab_size", settings.tabSize)
            putBoolean("editor_word_wrap", settings.wordWrap)
            putBoolean("editor_syntax_highlighting", settings.syntaxHighlighting)
            putBoolean("editor_line_numbers", settings.lineNumbers)
            putBoolean("editor_auto_complete", settings.autoComplete)
            putBoolean("editor_auto_indent", settings.autoIndent)
            
            // Terminal Settings
            putInt("terminal_font_size", settings.terminalFontSize)
            putString("terminal_default_shell", settings.defaultShell)
            putInt("terminal_scrollback", settings.terminalScrollback)
            
            // Appearance Settings
            putBoolean("appearance_dark_theme", settings.isDarkTheme)
            putBoolean("appearance_show_project_explorer", settings.showProjectExplorer)
            putBoolean("appearance_show_tool_windows", settings.showToolWindows)
            putBoolean("appearance_show_bottom_panel", settings.showBottomPanel)
            
            // Build Settings
            putBoolean("build_gradle_daemon", settings.gradleDaemon)
            putBoolean("build_offline_mode", settings.offlineMode)
            putBoolean("build_parallel", settings.parallelBuilds)
            putString("build_custom_gradle_args", settings.customGradleArgs)
            putString("build_selected_type", settings.selectedBuildType)
            
            // Code Analysis Settings
            putBoolean("analysis_todo_warnings", settings.enableTodoWarnings)
            putBoolean("analysis_long_line_warnings", settings.enableLongLineWarnings)
            putInt("analysis_long_line_threshold", settings.longLineThreshold)
            putBoolean("analysis_unused_import_warnings", settings.enableUnusedImportWarnings)
            
            // Advanced Settings
            putBoolean("advanced_root_features", settings.enableRootFeatures)
            putBoolean("advanced_shizuku_integration", settings.enableShizukuIntegration)
            putBoolean("advanced_auto_save", settings.autoSave)
            putInt("advanced_auto_save_interval", settings.autoSaveInterval)
            putInt("advanced_max_open_files", settings.maxOpenFiles)
            
            // Project Settings
            putString("project_last_path", settings.lastProjectPath)
            putBoolean("project_remember_open_files", settings.rememberOpenFiles)
            putBoolean("project_auto_save_on_exit", settings.autoSaveOnExit)
            
            // AI Settings
            putBoolean("ai_enable_features", settings.enableAiFeatures)
            putBoolean("ai_completion_enabled", settings.aiCompletionEnabled)
            putBoolean("ai_explanation_enabled", settings.aiExplanationEnabled)
            
            // Terminal Enhancement Settings
            putBoolean("terminal_enable_command_bookmarks", settings.enableCommandBookmarks)
            putBoolean("terminal_enable_search", settings.enableTerminalSearch)
            putBoolean("terminal_enable_save_output", settings.enableTerminalSaveOutput)
            putBoolean("terminal_enable_native", settings.enableNativeTerminal)
            
            apply()
        }
    }
    
    fun saveLastProject(path: String) {
        val currentSettings = _settings.value
        val updatedSettings = currentSettings.copy(lastProjectPath = path)
        updateSettings(updatedSettings)
    }
    
    fun resetToDefaults() {
        val defaultSettings = IDESettings()
        updateSettings(defaultSettings)
        // Don't clear the API key when resetting to defaults
    }
    
    // Gemini API key management
    fun saveGeminiApiKey(apiKey: String) {
        securePrefs.edit { putString("gemini_api_key", apiKey) }
    }
    
    fun getGeminiApiKey(): String? {
        return securePrefs.getString("gemini_api_key", null)
    }
    
    fun clearGeminiApiKey() {
        securePrefs.edit { remove("gemini_api_key") }
    }
}

data class IDESettings(
    // Editor Settings
    val fontSize: Int = 14,
    val tabSize: Int = 4,
    val wordWrap: Boolean = false,
    val syntaxHighlighting: Boolean = true,
    val lineNumbers: Boolean = true,
    val autoComplete: Boolean = true,
    val autoIndent: Boolean = true,
    
    // Terminal Settings
    val terminalFontSize: Int = 12,
    val defaultShell: String = "/system/bin/sh",
    val terminalScrollback: Int = 1000,
    
    // Appearance Settings
    val isDarkTheme: Boolean = true,
    val showProjectExplorer: Boolean = true,
    val showToolWindows: Boolean = true,
    val showBottomPanel: Boolean = true,
    
    // Build Settings
    val gradleDaemon: Boolean = true,
    val offlineMode: Boolean = false,
    val parallelBuilds: Boolean = true,
    val customGradleArgs: String = "",
    val selectedBuildType: String = "debug",
    
    // Code Analysis Settings
    val enableTodoWarnings: Boolean = true,
    val enableLongLineWarnings: Boolean = true,
    val longLineThreshold: Int = 120,
    val enableUnusedImportWarnings: Boolean = true,
    
    // Advanced Settings
    val enableRootFeatures: Boolean = false,
    val enableShizukuIntegration: Boolean = false,
    val autoSave: Boolean = true,
    val autoSaveInterval: Int = 30, // seconds
    val maxOpenFiles: Int = 20,
    
    // Project Settings
    val lastProjectPath: String? = null,
    val rememberOpenFiles: Boolean = true,
    val autoSaveOnExit: Boolean = true,
    
    // AI Settings
    val enableAiFeatures: Boolean = false,
    val aiCompletionEnabled: Boolean = false,
    val aiExplanationEnabled: Boolean = false,
    
    // Terminal Enhancement Settings
    val enableCommandBookmarks: Boolean = true,
    val enableTerminalSearch: Boolean = true,
    val enableTerminalSaveOutput: Boolean = true,
    val enableNativeTerminal: Boolean = false
)