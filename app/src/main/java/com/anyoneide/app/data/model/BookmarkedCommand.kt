package com.anyoneide.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Data class for bookmarked terminal commands
 */
@Serializable
data class BookmarkedCommand(
    val id: String,
    @SerialName("user_id") val userId: String,
    val command: String,
    val description: String,
    val tags: List<String> = emptyList(),
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("use_count") val useCount: Int = 0,
    @SerialName("last_used") val lastUsed: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)