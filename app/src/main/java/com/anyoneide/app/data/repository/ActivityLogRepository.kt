package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.ActivityLog
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.ActivityLogEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Repository for activity log data
 */
class ActivityLogRepository(private val roomRepository: RoomRepository) {
    
    private val gson = Gson()
    
    suspend fun getActivityLogs(userId: String, limit: Int = 100): Result<List<ActivityLog>> {
        return try {
            // Get all logs for the user, regardless of project
            val logs = roomRepository.getProjectLogs(null, limit).first()
                .map { it.toActivityLog(userId) }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProjectActivityLogs(projectId: String, limit: Int = 100): Result<List<ActivityLog>> {
        return try {
            val logs = roomRepository.getProjectLogs(projectId, limit).first()
                .map { it.toActivityLog("current_user") }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logActivity(log: ActivityLog): Result<ActivityLog> {
        return try {
            val entity = log.toActivityLogEntity()
            roomRepository.saveLog(entity)
            Result.success(log)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun clearOldLogs(olderThanDays: Int = 30): Result<Int> {
        return try {
            val timestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(olderThanDays.toLong())
            roomRepository.deleteOldLogs(timestamp)
            Result.success(olderThanDays)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getActivityLogsUpdates(userId: String): Flow<List<ActivityLog>> {
        // Get all logs for the user, regardless of project
        return roomRepository.getProjectLogs(null).map { entities ->
            entities.map { it.toActivityLog(userId) }
        }
    }
    
    fun getProjectActivityLogsUpdates(projectId: String): Flow<List<ActivityLog>> {
        return roomRepository.getProjectLogs(projectId).map { entities ->
            entities.map { it.toActivityLog("current_user") }
        }
    }
    
    // Extension functions to convert between domain models and entities
    private fun ActivityLogEntity.toActivityLog(userId: String): ActivityLog {
        val detailsType = object : TypeToken<Map<String, String>>() {}.type
        
        return ActivityLog(
            id = this.id,
            userId = userId,
            projectId = this.projectId,
            action = this.action,
            details = gson.fromJson(this.details, detailsType),
            createdAt = this.createdAt.toString()
        )
    }
    
    private fun ActivityLog.toActivityLogEntity(): ActivityLogEntity {
        return ActivityLogEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            projectId = this.projectId,
            action = this.action,
            details = gson.toJson(this.details),
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}