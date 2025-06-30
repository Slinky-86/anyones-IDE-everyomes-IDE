package com.anyoneide.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("user_id") val userId: String,
    val username: String,
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val preferences: UserPreferences? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class UserPreferences(
    @SerialName("theme_id") val themeId: String = "dark_default",
    @SerialName("font_size") val fontSize: Int = 14,
    @SerialName("tab_size") val tabSize: Int = 4,
    @SerialName("word_wrap") val wordWrap: Boolean = false,
    @SerialName("syntax_highlighting") val syntaxHighlighting: Boolean = true,
    @SerialName("line_numbers") val lineNumbers: Boolean = true,
    @SerialName("auto_complete") val autoComplete: Boolean = true,
    @SerialName("auto_indent") val autoIndent: Boolean = true,
    @SerialName("terminal_font_size") val terminalFontSize: Int = 12,
    @SerialName("default_shell") val defaultShell: String = "/system/bin/sh",
    @SerialName("enable_root_features") val enableRootFeatures: Boolean = false
)

@Serializable
data class Project(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String? = null,
    val path: String,
    @SerialName("project_type") val projectType: String,
    val settings: ProjectSettings? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_opened") val lastOpened: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ProjectSettings(
    @SerialName("build_type") val buildType: String = "debug",
    @SerialName("gradle_args") val gradleArgs: String = "",
    @SerialName("auto_save") val autoSave: Boolean = true,
    @SerialName("auto_save_interval") val autoSaveInterval: Int = 30
)

@Serializable
data class ProjectFile(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val path: String,
    @SerialName("relative_path") val relativePath: String,
    val content: String,
    val language: String,
    @SerialName("is_modified") val isModified: Boolean = false,
    @SerialName("is_open") val isOpen: Boolean = false,
    @SerialName("cursor_position") val cursorPosition: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class BuildConfiguration(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("build_type") val buildType: String,
    val configuration: BuildConfigData,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class BuildConfigData(
    @SerialName("gradle_args") val gradleArgs: List<String> = emptyList(),
    @SerialName("environment_vars") val environmentVars: Map<String, String> = emptyMap(),
    @SerialName("pre_build_commands") val preBuildCommands: List<String> = emptyList(),
    @SerialName("post_build_commands") val postBuildCommands: List<String> = emptyList()
)

@Serializable
data class BuildHistory(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("build_config_id") val buildConfigId: String,
    val status: String, // "success", "failed", "cancelled"
    val output: String,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("artifacts") val artifacts: List<BuildArtifactData> = emptyList(),
    @SerialName("started_at") val startedAt: String,
    @SerialName("completed_at") val completedAt: String? = null
)

@Serializable
data class BuildArtifactData(
    val name: String,
    val path: String,
    val type: String, // "apk", "aab", "jar", "aar"
    val size: Long
)

@Serializable
data class TerminalSession(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("project_id") val projectId: String? = null,
    val name: String,
    @SerialName("working_directory") val workingDirectory: String,
    @SerialName("shell_type") val shellType: String = "sh",
    @SerialName("environment_vars") val environmentVars: Map<String, String> = emptyMap(),
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class TerminalCommand(
    val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String,
    val command: String,
    val output: String,
    @SerialName("exit_code") val exitCode: Int,
    @SerialName("executed_at") val executedAt: String
)

@Serializable
data class Plugin(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val category: String,
    val tags: List<String> = emptyList(),
    @SerialName("download_url") val downloadUrl: String,
    @SerialName("icon_url") val iconUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val rating: Float = 0.0f,
    @SerialName("download_count") val downloadCount: Long = 0,
    @SerialName("min_ide_version") val minIdeVersion: String,
    val dependencies: List<PluginDependency> = emptyList(),
    @SerialName("extension_points") val extensionPoints: List<String> = emptyList(),
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class PluginDependency(
    val id: String,
    @SerialName("min_version") val minVersion: String,
    val optional: Boolean = false
)

@Serializable
data class UserPlugin(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("plugin_id") val pluginId: String,
    @SerialName("installed_version") val installedVersion: String,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("install_path") val installPath: String,
    val settings: Map<String, String> = emptyMap(),
    @SerialName("installed_at") val installedAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class Theme(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    @SerialName("is_dark") val isDark: Boolean,
    @SerialName("color_scheme") val colorScheme: ThemeColorScheme,
    @SerialName("is_built_in") val isBuiltIn: Boolean = false,
    @SerialName("download_url") val downloadUrl: String? = null,
    val rating: Float = 0.0f,
    @SerialName("download_count") val downloadCount: Long = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ThemeColorScheme(
    val background: String,
    val foreground: String,
    val selection: String,
    @SerialName("line_number") val lineNumber: String,
    @SerialName("current_line") val currentLine: String,
    val cursor: String,
    val keyword: String,
    val string: String,
    val comment: String,
    val number: String,
    val function: String,
    val type: String,
    val variable: String,
    val operator: String,
    val bracket: String,
    val error: String,
    val warning: String,
    val info: String
)

@Serializable
data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val tags: List<String> = emptyList(),
    @SerialName("project_type") val projectType: String,
    val features: List<String> = emptyList(),
    val difficulty: String, // "beginner", "intermediate", "advanced"
    @SerialName("estimated_time") val estimatedTime: String,
    @SerialName("template_data") val templateData: TemplateData,
    @SerialName("icon_url") val iconUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val rating: Float = 0.0f,
    @SerialName("usage_count") val usageCount: Long = 0,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class TemplateData(
    val files: List<TemplateFile> = emptyList(),
    val dependencies: List<String> = emptyList(),
    @SerialName("gradle_config") val gradleConfig: Map<String, String> = emptyMap(),
    @SerialName("manifest_config") val manifestConfig: Map<String, String> = emptyMap(),
    val variables: Map<String, String> = emptyMap()
)

@Serializable
data class TemplateFile(
    val path: String,
    val content: String,
    @SerialName("is_template") val isTemplate: Boolean = false
)

@Serializable
data class CodeSnippet(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String? = null,
    val language: String,
    val code: String,
    val tags: List<String> = emptyList(),
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("usage_count") val usageCount: Long = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

data class ActivityLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("project_id") val projectId: String? = null,
    val action: String,
    val details: Map<String, String> = emptyMap(),
    @SerialName("created_at") val createdAt: String
)