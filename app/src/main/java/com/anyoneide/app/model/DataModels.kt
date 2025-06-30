@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.anyoneide.app.model

import com.anyoneide.app.core.FileNode
import com.anyoneide.app.core.ProjectType
import kotlinx.serialization.Serializable

@Serializable
data class ProjectStructure(
    val name: String,
    val path: String,
    val projectType: String,
    val rootFiles: List<FileNode>
) {
    val type: ProjectType? = null
}

@Serializable
data class CompletionItem(
    val label: String,
    val kind: String,
    val detail: String? = null,
    val documentation: String? = null,
    val insertText: String
)

@Serializable
data class SyntaxHighlight(
    val start: Int,
    val end: Int,
    val type: String
)

@Serializable
data class BuildOutput(
    val success: Boolean,
    val output: String,
    val errors: List<String>,
    val warnings: List<String>,
    val durationMs: Long
)

@Serializable
data class DebugSession(
    val sessionId: String,
    val status: String,
    val breakpoints: List<Breakpoint>
)

@Serializable
data class Breakpoint(
    val id: String,
    val filePath: String,
    val line: Int,
    val enabled: Boolean
)

@Serializable
data class Problem(
    val file: String,
    val line: Int,
    val column: Int,
    val message: String,
    val severity: String
)

// Terminal-related data models
@Serializable
data class TerminalSession(
    val sessionId: String,
    val workingDirectory: String,
    val isActive: Boolean
)

@Serializable
data class TerminalOutput(
    val sessionId: String,
    val outputType: String,
    val content: String,
    val timestamp: Long
)

@Serializable
data class PackageInfo(
    val name: String,
    val version: String,
    val description: String,
    val installed: Boolean,
    val size: Long? = null,
    val dependencies: List<String> = emptyList()
)

// Recent Project data model
data class RecentProject(
    val name: String,
    val path: String,
    val lastOpened: Long,
    val projectType: String,
    val description: String = ""
)

// Language and Theme models
@Serializable
data class EditorLanguage(
    val id: String,
    val name: String,
    val extensions: List<String>
)

@Serializable
data class EditorThemeInfo(
    val id: String,
    val name: String,
    val isDark: Boolean
)