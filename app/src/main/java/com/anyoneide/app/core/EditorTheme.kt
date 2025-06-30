package com.anyoneide.app.core

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Theme(
    val id: String,
    val name: String,
    val description: String,
    val isCustom: Boolean = false,
    val colors: EditorColors
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Colors(
    val background: String,
    val foreground: String,
    val selection: String,
    val lineNumber: String,
    val currentLine: String,
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

