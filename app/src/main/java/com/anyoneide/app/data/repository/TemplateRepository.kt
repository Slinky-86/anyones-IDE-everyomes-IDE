package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.ProjectTemplate
import com.anyoneide.app.data.model.TemplateData
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.ProjectTemplateEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for project template data
 */
class TemplateRepository(private val roomRepository: RoomRepository) {
    
    private val gson = Gson()
    
    suspend fun getTemplates(): Result<List<ProjectTemplate>> {
        return try {
            val templates = roomRepository.getAllTemplates().first()
                .map { it.toProjectTemplate() }
            Result.success(templates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTemplate(templateId: String): Result<ProjectTemplate?> {
        return try {
            val entity = roomRepository.getTemplate(templateId)
            Result.success(entity?.toProjectTemplate())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTemplatesByCategory(category: String): Result<List<ProjectTemplate>> {
        return try {
            val templates = roomRepository.getTemplatesByCategory(category)
            Result.success(templates.map { it.toProjectTemplate() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun incrementTemplateUsage(templateId: String): Result<Boolean> {
        return try {
            roomRepository.incrementTemplateUsageCount(templateId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getTemplatesUpdates(): Flow<List<ProjectTemplate>> {
        return roomRepository.getAllTemplates().map { entities ->
            entities.map { it.toProjectTemplate() }
        }
    }
    
    // Extension functions to convert between domain models and entities
    private fun ProjectTemplateEntity.toProjectTemplate(): ProjectTemplate {
        val tagsType = object : TypeToken<List<String>>() {}.type
        val featuresType = object : TypeToken<List<String>>() {}.type
        val screenshotsType = object : TypeToken<List<String>>() {}.type
        val templateDataType = object : TypeToken<TemplateData>() {}.type
        
        return ProjectTemplate(
            id = this.id,
            name = this.name,
            description = this.description,
            category = this.category,
            tags = gson.fromJson(this.tags, tagsType),
            projectType = this.projectType,
            features = gson.fromJson(this.features, featuresType),
            difficulty = this.difficulty,
            estimatedTime = this.estimatedTime,
            templateData = gson.fromJson(this.templateData, templateDataType),
            iconUrl = this.iconUrl,
            screenshots = gson.fromJson(this.screenshots, screenshotsType),
            rating = this.rating,
            usageCount = this.usageCount,
            isVerified = this.isVerified,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun ProjectTemplate.toProjectTemplateEntity(): ProjectTemplateEntity {
        return ProjectTemplateEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            name = this.name,
            description = this.description,
            category = this.category,
            tags = gson.toJson(this.tags),
            projectType = this.projectType,
            features = gson.toJson(this.features),
            difficulty = this.difficulty,
            estimatedTime = this.estimatedTime,
            templateData = gson.toJson(this.templateData),
            iconUrl = this.iconUrl,
            screenshots = gson.toJson(this.screenshots),
            rating = this.rating,
            usageCount = this.usageCount,
            isVerified = this.isVerified,
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}