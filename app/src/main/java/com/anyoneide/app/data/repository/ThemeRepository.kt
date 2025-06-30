package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.Theme
import com.anyoneide.app.data.model.ThemeColorScheme
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.ThemeEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for theme data
 */
class ThemeRepository(private val roomRepository: RoomRepository) {
    
    private val gson = Gson()
    
    suspend fun getThemes(): Result<List<Theme>> {
        return try {
            val themes = roomRepository.getAllThemes().first()
                .map { it.toTheme() }
            Result.success(themes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTheme(themeId: String): Result<Theme?> {
        return try {
            val entity = roomRepository.getTheme(themeId)
            Result.success(entity?.toTheme())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createTheme(theme: Theme): Result<Theme> {
        return try {
            val entity = theme.toThemeEntity()
            roomRepository.saveTheme(entity)
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTheme(theme: Theme): Result<Theme> {
        return try {
            val entity = theme.toThemeEntity()
            roomRepository.updateTheme(entity)
            Result.success(theme)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTheme(themeId: String): Result<Boolean> {
        return try {
            val entity = roomRepository.getTheme(themeId)
            if (entity != null) {
                roomRepository.deleteTheme(entity)
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getThemesUpdates(): Flow<List<Theme>> {
        return roomRepository.getAllThemes().map { entities ->
            entities.map { it.toTheme() }
        }
    }
    
    // Extension functions to convert between domain models and entities
    private fun ThemeEntity.toTheme(): Theme {
        val colorSchemeType = object : TypeToken<ThemeColorScheme>() {}.type
        
        return Theme(
            id = this.id,
            name = this.name,
            description = this.description,
            author = this.author,
            isDark = this.isDark,
            colorScheme = gson.fromJson(this.colorScheme, colorSchemeType),
            isBuiltIn = this.isBuiltIn,
            downloadUrl = this.downloadUrl,
            rating = this.rating,
            downloadCount = this.downloadCount,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun Theme.toThemeEntity(): ThemeEntity {
        return ThemeEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            name = this.name,
            description = this.description,
            author = this.author,
            isDark = this.isDark,
            colorScheme = gson.toJson(this.colorScheme),
            isBuiltIn = this.isBuiltIn,
            downloadUrl = this.downloadUrl,
            rating = this.rating,
            downloadCount = this.downloadCount,
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}