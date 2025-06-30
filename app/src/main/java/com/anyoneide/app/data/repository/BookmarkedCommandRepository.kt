package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.BookmarkedCommand
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.BookmarkedCommandEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for bookmarked terminal commands
 */
class BookmarkedCommandRepository(private val roomRepository: RoomRepository) {
    
    private val gson = Gson()
    
    /**
     * Get all bookmarked commands
     */
    suspend fun getBookmarkedCommands(): Result<List<BookmarkedCommand>> {
        return try {
            val commands = roomRepository.getAllBookmarkedCommands().first()
                .map { it.toBookmarkedCommand("current_user") }
            Result.success(commands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a bookmarked command by ID
     */
    suspend fun getBookmarkedCommand(commandId: String): Result<BookmarkedCommand?> {
        return try {
            val entity = roomRepository.getBookmarkedCommand(commandId)
            Result.success(entity?.toBookmarkedCommand("current_user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a bookmarked command by command text
     */
    suspend fun getBookmarkedCommandByCommand(command: String): Result<BookmarkedCommand?> {
        return try {
            val entity = roomRepository.getBookmarkedCommandByCommand(command)
            Result.success(entity?.toBookmarkedCommand("current_user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a new bookmarked command
     */
    suspend fun createBookmarkedCommand(command: BookmarkedCommand): Result<BookmarkedCommand> {
        return try {
            val entity = command.toBookmarkedCommandEntity()
            roomRepository.saveBookmarkedCommand(entity)
            Result.success(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing bookmarked command
     */
    suspend fun updateBookmarkedCommand(command: BookmarkedCommand): Result<BookmarkedCommand> {
        return try {
            val entity = command.toBookmarkedCommandEntity()
            roomRepository.updateBookmarkedCommand(entity)
            Result.success(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a bookmarked command
     */
    suspend fun deleteBookmarkedCommand(commandId: String): Result<Boolean> {
        return try {
            val entity = roomRepository.getBookmarkedCommand(commandId)
            if (entity != null) {
                roomRepository.deleteBookmarkedCommand(entity)
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Increment the use count for a bookmarked command
     */
    suspend fun incrementUseCount(commandId: String): Result<Boolean> {
        return try {
            roomRepository.incrementCommandUseCount(commandId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update the favorite status of a bookmarked command
     */
    suspend fun updateFavoriteStatus(commandId: String, isFavorite: Boolean): Result<Boolean> {
        return try {
            roomRepository.updateCommandFavoriteStatus(commandId, isFavorite)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search for bookmarked commands
     */
    suspend fun searchBookmarkedCommands(query: String): Result<List<BookmarkedCommand>> {
        return try {
            val searchQuery = "%$query%"
            val commands = roomRepository.searchBookmarkedCommands(searchQuery)
                .map { it.toBookmarkedCommand("current_user") }
            Result.success(commands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a flow of all bookmarked commands for real-time updates
     */
    fun getBookmarkedCommandsFlow(): Flow<List<BookmarkedCommand>> {
        return roomRepository.getAllBookmarkedCommands().map { entities ->
            entities.map { it.toBookmarkedCommand("current_user") }
        }
    }
    
    // Extension functions to convert between domain models and entities
    private fun BookmarkedCommandEntity.toBookmarkedCommand(userId: String): BookmarkedCommand {
        val tagsType = object : TypeToken<List<String>>() {}.type
        
        return BookmarkedCommand(
            id = this.id,
            userId = userId,
            command = this.command,
            description = this.description,
            tags = gson.fromJson(this.tags, tagsType),
            isFavorite = this.isFavorite,
            useCount = this.useCount,
            lastUsed = this.lastUsed?.toString(),
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun BookmarkedCommand.toBookmarkedCommandEntity(): BookmarkedCommandEntity {
        return BookmarkedCommandEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            command = this.command,
            description = this.description,
            tags = gson.toJson(this.tags),
            isFavorite = this.isFavorite,
            useCount = this.useCount,
            lastUsed = this.lastUsed?.toLongOrNull(),
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}