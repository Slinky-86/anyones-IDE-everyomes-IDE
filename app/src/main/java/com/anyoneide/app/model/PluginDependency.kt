package com.anyoneide.app.model

/**
 * Data class for plugin dependencies
 */
data class PluginDependency(
    val id: String,
    val minVersion: String,
    val optional: Boolean = false
)