package com.anyoneide.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Entity representing a bookmarked terminal command
 */
@Entity(tableName = "bookmarked_commands")
data class BookmarkedCommandEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "command") val command: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "tags") val tags: String = "[]", // JSON array of tags
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "use_count") val useCount: Int = 0,
    @ColumnInfo(name = "last_used") val lastUsed: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)