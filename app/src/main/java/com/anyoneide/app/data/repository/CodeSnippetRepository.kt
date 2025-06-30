package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.CodeSnippet
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.CodeSnippetEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for code snippet data
 */
class CodeSnippetRepository(private val roomRepository: RoomRepository) {
    
    private val gson = Gson()
    
    suspend fun getCodeSnippets(userId: String): Result<List<CodeSnippet>> {
        return try {
            val snippets = roomRepository.getAllSnippets().first()
                .map { it.toCodeSnippet(userId) }
            Result.success(snippets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPublicCodeSnippets(): Result<List<CodeSnippet>> {
        return try {
            val snippets = roomRepository.getPublicSnippets()
            Result.success(snippets.map { it.toCodeSnippet("public") })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCodeSnippet(snippetId: String): Result<CodeSnippet?> {
        return try {
            val entity = roomRepository.getSnippet(snippetId)
            Result.success(entity?.toCodeSnippet("owner"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createCodeSnippet(snippet: CodeSnippet): Result<CodeSnippet> {
        return try {
            val entity = snippet.toCodeSnippetEntity()
            roomRepository.saveSnippet(entity)
            Result.success(snippet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateCodeSnippet(snippet: CodeSnippet): Result<CodeSnippet> {
        return try {
            val entity = snippet.toCodeSnippetEntity()
            roomRepository.updateSnippet(entity)
            Result.success(snippet)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteCodeSnippet(snippetId: String): Result<Boolean> {
        return try {
            val entity = roomRepository.getSnippet(snippetId)
            if (entity != null) {
                roomRepository.deleteSnippet(entity)
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun incrementSnippetUsage(snippetId: String): Result<Boolean> {
        return try {
            roomRepository.incrementSnippetUsageCount(snippetId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getCodeSnippetsUpdates(userId: String): Flow<List<CodeSnippet>> {
        return roomRepository.getAllSnippets().map { entities ->
            entities.map { it.toCodeSnippet(userId) }
        }
    }
    
    // Extension functions to convert between domain models and entities
    private fun CodeSnippetEntity.toCodeSnippet(userId: String): CodeSnippet {
        val tagsType = object : TypeToken<List<String>>() {}.type
        
        return CodeSnippet(
            id = this.id,
            userId = userId,
            title = this.title,
            description = this.description,
            language = this.language,
            code = this.code,
            tags = gson.fromJson(this.tags, tagsType),
            isPublic = this.isPublic,
            usageCount = this.usageCount,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun CodeSnippet.toCodeSnippetEntity(): CodeSnippetEntity {
        return CodeSnippetEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            title = this.title,
            description = this.description,
            language = this.language,
            code = this.code,
            tags = gson.toJson(this.tags),
            isPublic = this.isPublic,
            usageCount = this.usageCount,
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}