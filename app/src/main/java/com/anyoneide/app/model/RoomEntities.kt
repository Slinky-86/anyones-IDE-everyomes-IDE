package com.anyoneide.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Entity representing a user profile
 */
@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "full_name") val fullName: String?,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?,
    @ColumnInfo(name = "theme_id") val themeId: String = "dark_default",
    @ColumnInfo(name = "font_size") val fontSize: Int = 14,
    @ColumnInfo(name = "tab_size") val tabSize: Int = 4,
    @ColumnInfo(name = "word_wrap") val wordWrap: Boolean = false,
    @ColumnInfo(name = "syntax_highlighting") val syntaxHighlighting: Boolean = true,
    @ColumnInfo(name = "line_numbers") val lineNumbers: Boolean = true,
    @ColumnInfo(name = "auto_complete") val autoComplete: Boolean = true,
    @ColumnInfo(name = "auto_indent") val autoIndent: Boolean = true,
    @ColumnInfo(name = "terminal_font_size") val terminalFontSize: Int = 12,
    @ColumnInfo(name = "default_shell") val defaultShell: String = "/system/bin/sh",
    @ColumnInfo(name = "enable_root_features") val enableRootFeatures: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a project
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "project_type") val projectType: String,
    @ColumnInfo(name = "build_type") val buildType: String = "debug",
    @ColumnInfo(name = "gradle_args") val gradleArgs: String = "",
    @ColumnInfo(name = "auto_save") val autoSave: Boolean = true,
    @ColumnInfo(name = "auto_save_interval") val autoSaveInterval: Int = 30,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "last_opened") val lastOpened: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a project file
 */
@Entity(
    tableName = "project_files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id"]),
        Index(value = ["project_id", "relative_path"], unique = true)
    ]
)
data class ProjectFileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "relative_path") val relativePath: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "is_modified") val isModified: Boolean = false,
    @ColumnInfo(name = "is_open") val isOpen: Boolean = false,
    @ColumnInfo(name = "cursor_position") val cursorPosition: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a build configuration
 */
@Entity(
    tableName = "build_configurations",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id"])
    ]
)
data class BuildConfigurationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "build_type") val buildType: String,
    @ColumnInfo(name = "gradle_args") val gradleArgs: String = "",
    @ColumnInfo(name = "environment_vars") val environmentVars: String = "{}",
    @ColumnInfo(name = "pre_build_commands") val preBuildCommands: String = "[]",
    @ColumnInfo(name = "post_build_commands") val postBuildCommands: String = "[]",
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a build history record
 */
@Entity(
    tableName = "build_history",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BuildConfigurationEntity::class,
            parentColumns = ["id"],
            childColumns = ["build_config_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["project_id"]),
        Index(value = ["build_config_id"])
    ]
)
data class BuildHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    @ColumnInfo(name = "build_config_id") val buildConfigId: String?,
    @ColumnInfo(name = "status") val status: String, // "success", "failed", "cancelled"
    @ColumnInfo(name = "output") val output: String,
    @ColumnInfo(name = "errors") val errors: String = "[]",
    @ColumnInfo(name = "warnings") val warnings: String = "[]",
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "artifacts") val artifacts: String = "[]",
    @ColumnInfo(name = "started_at") val startedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null
)

/**
 * Entity representing a terminal session
 */
@Entity(tableName = "terminal_sessions")
data class TerminalSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "working_directory") val workingDirectory: String,
    @ColumnInfo(name = "shell_type") val shellType: String = "sh",
    @ColumnInfo(name = "environment_vars") val environmentVars: String = "{}",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a terminal command
 */
@Entity(
    tableName = "terminal_commands",
    foreignKeys = [
        ForeignKey(
            entity = TerminalSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["session_id"])
    ]
)
data class TerminalCommandEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "command") val command: String,
    @ColumnInfo(name = "output") val output: String,
    @ColumnInfo(name = "exit_code") val exitCode: Int,
    @ColumnInfo(name = "executed_at") val executedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a plugin
 */
@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "version") val version: String,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "tags") val tags: String = "[]",
    @ColumnInfo(name = "download_url") val downloadUrl: String,
    @ColumnInfo(name = "icon_url") val iconUrl: String?,
    @ColumnInfo(name = "screenshots") val screenshots: String = "[]",
    @ColumnInfo(name = "rating") val rating: Float = 0.0f,
    @ColumnInfo(name = "download_count") val downloadCount: Long = 0,
    @ColumnInfo(name = "min_ide_version") val minIdeVersion: String,
    @ColumnInfo(name = "dependencies") val dependencies: String = "[]",
    @ColumnInfo(name = "extension_points") val extensionPoints: String = "[]",
    @ColumnInfo(name = "is_verified") val isVerified: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a user's installed plugin
 */
@Entity(
    tableName = "user_plugins",
    foreignKeys = [
        ForeignKey(
            entity = PluginEntity::class,
            parentColumns = ["id"],
            childColumns = ["plugin_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plugin_id"])
    ]
)
data class UserPluginEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "plugin_id") val pluginId: String,
    @ColumnInfo(name = "installed_version") val installedVersion: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "install_path") val installPath: String,
    @ColumnInfo(name = "settings") val settings: String = "{}",
    @ColumnInfo(name = "installed_at") val installedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a theme
 */
@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "is_dark") val isDark: Boolean,
    @ColumnInfo(name = "color_scheme") val colorScheme: String,
    @ColumnInfo(name = "is_built_in") val isBuiltIn: Boolean = false,
    @ColumnInfo(name = "download_url") val downloadUrl: String?,
    @ColumnInfo(name = "rating") val rating: Float = 0.0f,
    @ColumnInfo(name = "download_count") val downloadCount: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a project template
 */
@Entity(tableName = "project_templates")
data class ProjectTemplateEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "tags") val tags: String = "[]",
    @ColumnInfo(name = "project_type") val projectType: String,
    @ColumnInfo(name = "features") val features: String = "[]",
    @ColumnInfo(name = "difficulty") val difficulty: String, // "beginner", "intermediate", "advanced"
    @ColumnInfo(name = "estimated_time") val estimatedTime: String,
    @ColumnInfo(name = "template_data") val templateData: String,
    @ColumnInfo(name = "icon_url") val iconUrl: String?,
    @ColumnInfo(name = "screenshots") val screenshots: String = "[]",
    @ColumnInfo(name = "rating") val rating: Float = 0.0f,
    @ColumnInfo(name = "usage_count") val usageCount: Long = 0,
    @ColumnInfo(name = "is_verified") val isVerified: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a code snippet
 */
@Entity(tableName = "code_snippets")
data class CodeSnippetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "tags") val tags: String = "[]",
    @ColumnInfo(name = "is_public") val isPublic: Boolean = false,
    @ColumnInfo(name = "usage_count") val usageCount: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing an activity log
 */
@Entity(
    tableName = "activity_logs",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["project_id"])
    ]
)
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "details") val details: String = "{}",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)