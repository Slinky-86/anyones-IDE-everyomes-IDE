package com.anyoneide.app.data.repository

import com.anyoneide.app.data.model.Plugin
import com.anyoneide.app.data.model.PluginDependency
import com.anyoneide.app.data.model.UserPlugin
import com.anyoneide.app.data.room.RoomRepository
import com.anyoneide.app.model.PluginEntity
import com.anyoneide.app.model.UserPluginEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository for plugin data
 */
class PluginRepository(private val roomRepository: RoomRepository) {
    
    private val gson = Gson()
    
    suspend fun getAvailablePlugins(): Result<List<Plugin>> {
        return try {
            val plugins = roomRepository.getAvailablePlugins().first()
                .map { it.toPlugin() }
            Result.success(plugins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPlugin(pluginId: String): Result<Plugin?> {
        return try {
            val entity = roomRepository.getPlugin(pluginId)
            Result.success(entity?.toPlugin())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserPlugins(userId: String): Result<List<UserPlugin>> {
        return try {
            val userPlugins = roomRepository.getUserPlugins().first()
                .map { it.toUserPlugin("current_user") } // Using a default value instead of the parameter
            Result.success(userPlugins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserPlugin(userId: String, pluginId: String): Result<UserPlugin?> {
        return try {
            val entity = roomRepository.getUserPlugin(pluginId)
            Result.success(entity?.toUserPlugin(userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun installPlugin(userPlugin: UserPlugin): Result<UserPlugin> {
        return try {
            val entity = userPlugin.toUserPluginEntity()
            roomRepository.saveUserPlugin(entity)
            
            // Increment download count for the plugin
            roomRepository.incrementPluginDownloadCount(userPlugin.pluginId)
            
            Result.success(userPlugin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserPlugin(userPlugin: UserPlugin): Result<UserPlugin> {
        return try {
            val entity = userPlugin.toUserPluginEntity()
            roomRepository.updateUserPlugin(entity)
            Result.success(userPlugin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uninstallPlugin(userId: String, pluginId: String): Result<Boolean> {
        return try {
            roomRepository.uninstallPlugin(pluginId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getAvailablePluginsUpdates(): Flow<List<Plugin>> {
        return roomRepository.getAvailablePlugins().map { entities ->
            entities.map { it.toPlugin() }
        }
    }
    
    fun getUserPluginsUpdates(userId: String): Flow<List<UserPlugin>> {
        return roomRepository.getUserPlugins().map { entities ->
            entities.map { it.toUserPlugin(userId) }
        }
    }
    
    // Extension functions to convert between domain models and entities
    private fun PluginEntity.toPlugin(): Plugin {
        val dependenciesType = object : TypeToken<List<PluginDependency>>() {}.type
        val tagsType = object : TypeToken<List<String>>() {}.type
        val extensionPointsType = object : TypeToken<List<String>>() {}.type
        val screenshotsType = object : TypeToken<List<String>>() {}.type
        
        return Plugin(
            id = this.id,
            name = this.name,
            description = this.description,
            version = this.version,
            author = this.author,
            category = this.category,
            tags = gson.fromJson(this.tags, tagsType),
            downloadUrl = this.downloadUrl,
            iconUrl = this.iconUrl,
            screenshots = gson.fromJson(this.screenshots, screenshotsType),
            rating = this.rating,
            downloadCount = this.downloadCount,
            minIdeVersion = this.minIdeVersion,
            dependencies = gson.fromJson(this.dependencies, dependenciesType),
            extensionPoints = gson.fromJson(this.extensionPoints, extensionPointsType),
            isVerified = this.isVerified,
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun Plugin.toPluginEntity(): PluginEntity {
        return PluginEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            name = this.name,
            description = this.description,
            version = this.version,
            author = this.author,
            category = this.category,
            tags = gson.toJson(this.tags),
            downloadUrl = this.downloadUrl,
            iconUrl = this.iconUrl,
            screenshots = gson.toJson(this.screenshots),
            rating = this.rating,
            downloadCount = this.downloadCount,
            minIdeVersion = this.minIdeVersion,
            dependencies = gson.toJson(this.dependencies),
            extensionPoints = gson.toJson(this.extensionPoints),
            isVerified = this.isVerified,
            createdAt = this.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
    
    private fun UserPluginEntity.toUserPlugin(userId: String): UserPlugin {
        val settingsType = object : TypeToken<Map<String, String>>() {}.type
        
        return UserPlugin(
            id = this.id,
            userId = userId,
            pluginId = this.pluginId,
            installedVersion = this.installedVersion,
            isEnabled = this.isEnabled,
            installPath = this.installPath,
            settings = gson.fromJson(this.settings, settingsType),
            installedAt = this.installedAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }
    
    private fun UserPlugin.toUserPluginEntity(): UserPluginEntity {
        return UserPluginEntity(
            id = this.id.ifEmpty { UUID.randomUUID().toString() },
            pluginId = this.pluginId,
            installedVersion = this.installedVersion,
            isEnabled = this.isEnabled,
            installPath = this.installPath,
            settings = gson.toJson(this.settings),
            installedAt = this.installedAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}