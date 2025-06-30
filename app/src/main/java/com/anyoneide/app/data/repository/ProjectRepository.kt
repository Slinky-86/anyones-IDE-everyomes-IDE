package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.Project
import com.anyoneide.app.data.model.ProjectFile
import com.anyoneide.app.data.model.ProjectSettings
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.ProjectEntity
import com.anyoneide.app.model.ProjectFileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for project data
 */
class ProjectRepository(private val roomRepository: RoomRepository) {
    
    suspend fun getProjects(userId: String): Result<List<Project>> {
        return try {
            val projects = roomRepository.getAllProjects().first()
                .filter { it.userId == userId }
                .map { it.toProject() }
            Result.success(projects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProject(projectId: String): Result<Project?> {
        return try {
            val entity = roomRepository.getProject(projectId)
            Result.success(entity?.toProject())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createProject(project: Project): Result<Project> {
        return try {
            val entity = project.toProjectEntity()
            roomRepository.saveProject(entity)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProject(project: Project): Result<Project> {
        return try {
            val entity = project.toProjectEntity()
            roomRepository.updateProject(entity)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteProject(projectId: String): Result<Boolean> {
        return try {
            roomRepository.deleteProject(projectId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProjectFiles(projectId: String): Result<List<ProjectFile>> {
        return try {
            val files = roomRepository.getProjectFiles(projectId).first()
                .map { it.toProjectFile() }
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProjectFile(fileId: String): Result<ProjectFile?> {
        return try {
            val entity = roomRepository.getFile(fileId)
            Result.success(entity?.toProjectFile())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createProjectFile(file: ProjectFile): Result<ProjectFile> {
        return try {
            val entity = file.toProjectFileEntity()
            roomRepository.saveFile(entity)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProjectFile(file: ProjectFile): Result<ProjectFile> {
        return try {
            val entity = file.toProjectFileEntity()
            roomRepository.updateFile(entity)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteProjectFile(fileId: String): Result<Boolean> {
        return try {
            val entity = roomRepository.getFile(fileId)
            if (entity != null) {
                roomRepository.deleteFile(entity)
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getProjectUpdates(projectId: String): Flow<Project?> {
        // This would require a Flow from Room for a specific project
        // For now, we'll return a flow that emits null
        return roomRepository.getProjectUpdates(projectId).map { it?.toProject() }
    }
    
    fun getProjectFilesUpdates(projectId: String): Flow<List<ProjectFile>> {
        return roomRepository.getProjectFiles(projectId).map { entities ->
            entities.map { it.toProjectFile() }
        }
    }
    
    // Extension functions to convert between domain models and entities
    private fun ProjectEntity.toProject(): Project {
        return Project(
            id = this.id,
            userId = this.userId,
            name = this.name,
            description = this.description,
            path = this.path,
            projectType = this.projectType,
            settings = ProjectSettings(
                buildType = this.buildType,
                gradleArgs = this.gradleArgs,
                autoSave = this.autoSave,
                autoSaveInterval = this.autoSaveInterval
            ),
            isActive = this.isActive,
            lastOpened = this.lastOpened.toString(),
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun Project.toProjectEntity(): ProjectEntity {
        return ProjectEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            userId = this.userId,
            name = this.name,
            description = this.description,
            path = this.path,
            projectType = this.projectType,
            buildType = this.settings?.buildType ?: "debug",
            gradleArgs = this.settings?.gradleArgs ?: "",
            autoSave = this.settings?.autoSave ?: true,
            autoSaveInterval = this.settings?.autoSaveInterval ?: 30,
            isActive = this.isActive,
            lastOpened = this.lastOpened?.toLongOrNull() ?: System.currentTimeMillis(),
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
    
    private fun ProjectFileEntity.toProjectFile(): ProjectFile {
        return ProjectFile(
            id = this.id,
            projectId = this.projectId,
            userId = "", // Not stored in entity
            name = this.name,
            path = this.path,
            relativePath = this.relativePath,
            content = this.content,
            language = this.language,
            isModified = this.isModified,
            isOpen = this.isOpen,
            cursorPosition = this.cursorPosition,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun ProjectFile.toProjectFileEntity(): ProjectFileEntity {
        return ProjectFileEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            projectId = this.projectId,
            name = this.name,
            path = this.path,
            relativePath = this.relativePath,
            content = this.content,
            language = this.language,
            isModified = this.isModified,
            isOpen = this.isOpen,
            cursorPosition = this.cursorPosition,
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}