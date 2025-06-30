package com.anyoneide.app.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EditorFile(
    val path: String,
    val name: String,
    val content: String,
    val language: String,
    val isModified: Boolean = false,
    val lineCount: Int = 0
)