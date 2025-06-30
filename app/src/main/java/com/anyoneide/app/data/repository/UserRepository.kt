package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.UserProfile
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for user profile data
 */
/**
 * Repository for user profile data
 */
class UserRepository(private val roomRepository: RoomRepository) {
    
    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return try {
            val entity = roomRepository.getUserProfile(userId)
            Result.success(entity?.toUserProfile())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            val entity = userProfile.toUserProfileEntity()
            roomRepository.saveUserProfile(entity)
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            val entity = userProfile.toUserProfileEntity()
            roomRepository.updateUserProfile(entity)
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteUserProfile(userId: String): Result<Boolean> {
        return try {
            val entity = roomRepository.getUserProfile(userId)
            if (entity != null) {
                roomRepository.deleteUserProfile(entity)
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getUserProfileUpdates(userId: String): Flow<UserProfile?> {
        // This would require a Flow from Room, which isn't implemented yet
        // For now, we'll return a flow that emits null
        return roomRepository.getUserProfileUpdates(userId).map { it?.toUserProfile() }
    }
    
    // Extension functions to convert between domain models and entities
    private fun UserProfileEntity.toUserProfile(): UserProfile {
        val preferences = if (this.themeId.isNotEmpty()) {
            com.anyoneide.app.data.model.UserPreferences(
                themeId = this.themeId,
                fontSize = this.fontSize,
                tabSize = this.tabSize,
                wordWrap = this.wordWrap,
                syntaxHighlighting = this.syntaxHighlighting,
                lineNumbers = this.lineNumbers,
                autoComplete = this.autoComplete,
                autoIndent = this.autoIndent,
                terminalFontSize = this.terminalFontSize,
                defaultShell = this.defaultShell,
                enableRootFeatures = this.enableRootFeatures
            )
        } else {
            null
        }
        
        return UserProfile(
            id = this.id,
            userId = this.id, // Using same ID for both fields
            username = this.username,
            email = this.email,
            fullName = this.fullName,
            avatarUrl = this.avatarUrl,
            preferences = preferences,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun UserProfile.toUserProfileEntity(): UserProfileEntity {
        return UserProfileEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            username = this.username,
            email = this.email,
            fullName = this.fullName,
            avatarUrl = this.avatarUrl,
            themeId = this.preferences?.themeId ?: "dark_default",
            fontSize = this.preferences?.fontSize ?: 14,
            tabSize = this.preferences?.tabSize ?: 4,
            wordWrap = this.preferences?.wordWrap ?: false,
            syntaxHighlighting = this.preferences?.syntaxHighlighting ?: true,
            lineNumbers = this.preferences?.lineNumbers ?: true,
            autoComplete = this.preferences?.autoComplete ?: true,
            autoIndent = this.preferences?.autoIndent ?: true,
            terminalFontSize = this.preferences?.terminalFontSize ?: 12,
            defaultShell = this.preferences?.defaultShell ?: "/system/bin/sh",
            enableRootFeatures = this.preferences?.enableRootFeatures ?: false,
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}