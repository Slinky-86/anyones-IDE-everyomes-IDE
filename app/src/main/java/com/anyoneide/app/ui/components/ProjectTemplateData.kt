package com.anyoneide.app.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class for project template information
 */
data class ProjectTemplateData(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: String,
    val features: List<String>,
    val difficulty: String,
    val estimatedTime: String,
    val preview: String
)