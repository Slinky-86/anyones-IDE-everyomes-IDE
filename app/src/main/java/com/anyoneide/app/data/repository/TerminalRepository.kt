package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.TerminalCommand
import com.anyoneide.app.data.model.TerminalSession
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.TerminalCommandEntity
import com.anyoneide.app.model.TerminalSessionEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for terminal data
 */
class TerminalRepository(private val roomRepository: RoomRepository) {
    
    private val gson = Gson()
    
    suspend fun getTerminalSessions(userId: String): Result<List<TerminalSession>> {
        return try {
            val sessions = roomRepository.getActiveSessions().first()
                .map { it.toTerminalSession(userId) }
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTerminalSession(sessionId: String): Result<TerminalSession?> {
        return try {
            val entity = roomRepository.getSession(sessionId)
            Result.success(entity?.toTerminalSession("current_user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createTerminalSession(session: TerminalSession): Result<TerminalSession> {
        return try {
            val entity = session.toTerminalSessionEntity()
            roomRepository.saveSession(entity)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTerminalSession(session: TerminalSession): Result<TerminalSession> {
        return try {
            val entity = session.toTerminalSessionEntity()
            roomRepository.updateSession(entity)
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTerminalSession(sessionId: String): Result<Boolean> {
        return try {
            roomRepository.closeSession(sessionId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTerminalCommands(sessionId: String): Result<List<TerminalCommand>> {
        return try {
            val commands = roomRepository.getSessionCommands(sessionId).first()
                .map { it.toTerminalCommand() }
            Result.success(commands)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveTerminalCommand(command: TerminalCommand): Result<TerminalCommand> {
        return try {
            val entity = command.toTerminalCommandEntity()
            roomRepository.saveCommand(entity)
            Result.success(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTerminalCommand(command: TerminalCommand): Result<TerminalCommand> {
        return try {
            val entity = command.toTerminalCommandEntity()
            roomRepository.saveCommand(entity) // Using saveCommand as it uses REPLACE strategy
            Result.success(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getTerminalSessionsUpdates(userId: String): Flow<List<TerminalSession>> {
        return roomRepository.getActiveSessions().map { entities ->
            entities.map { it.toTerminalSession(userId) }
        }
    }
    
    fun getTerminalCommandsUpdates(sessionId: String): Flow<List<TerminalCommand>> {
        return roomRepository.getSessionCommands(sessionId).map { entities ->
            entities.map { it.toTerminalCommand() }
        }
    }
    
    // Extension functions to convert between domain models and entities
    private fun TerminalSessionEntity.toTerminalSession(userId: String): TerminalSession {
        val environmentVarsType = object : TypeToken<Map<String, String>>() {}.type
        
        return TerminalSession(
            id = this.id,
            userId = userId,
            projectId = this.projectId,
            name = this.name,
            workingDirectory = this.workingDirectory,
            shellType = this.shellType,
            environmentVars = gson.fromJson(this.environmentVars, environmentVarsType),
            isActive = this.isActive,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun TerminalSession.toTerminalSessionEntity(): TerminalSessionEntity {
        return TerminalSessionEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            projectId = this.projectId,
            name = this.name,
            workingDirectory = this.workingDirectory,
            shellType = this.shellType,
            environmentVars = gson.toJson(this.environmentVars),
            isActive = this.isActive,
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
    
    private fun TerminalCommandEntity.toTerminalCommand(): TerminalCommand {
        return TerminalCommand(
            id = this.id,
            sessionId = this.sessionId,
            userId = "current_user", // Using a default value since it's not stored in entity
            command = this.command,
            output = this.output,
            exitCode = this.exitCode,
            executedAt = this.executedAt.toString()
        )
    }
    
    private fun TerminalCommand.toTerminalCommandEntity(): TerminalCommandEntity {
        return TerminalCommandEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            sessionId = this.sessionId,
            command = this.command,
            output = this.output,
            exitCode = this.exitCode,
            executedAt = this.executedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}