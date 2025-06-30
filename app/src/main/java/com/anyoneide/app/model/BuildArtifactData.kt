package com.anyoneide.app.model

/**
 * Data class for build artifacts
 */
data class BuildArtifactData(
    val name: String,
    val path: String,
    val type: String, // "apk", "aab", "jar", "aar"
    val size: Long
)