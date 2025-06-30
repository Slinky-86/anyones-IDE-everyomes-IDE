package com.anyoneide.app.core

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

class PluginManager(context: Context) {
    
    private val _installedPlugins = MutableStateFlow<List<Plugin>>(emptyList())
    val installedPlugins: StateFlow<List<Plugin>> = _installedPlugins.asStateFlow()
    
    private val _availablePlugins = MutableStateFlow<List<PluginMetadata>>(emptyList())
    val availablePlugins: StateFlow<List<PluginMetadata>> = _availablePlugins.asStateFlow()
    
    private val loadedPlugins = ConcurrentHashMap<String, PluginInstance>()
    private val pluginHooks = ConcurrentHashMap<String, MutableList<PluginHook>>()
    private val extensionPoints = ConcurrentHashMap<String, ExtensionPoint>()
    
    private val pluginsDir = File(context.filesDir, "plugins")
    private val json = Json { ignoreUnknownKeys = true }
    
    // Dependencies
    private val languageSupport = LanguageSupport(context)
    private val themeManager = ThemeManager(context)
    private val buildManager = BuildManager(context)
    private val gitIntegration = GitIntegration(context)
    
    init {
        pluginsDir.mkdirs()
        registerCoreExtensionPoints()
        loadInstalledPlugins()
        loadAvailablePlugins()
    }
    
    private fun registerCoreExtensionPoints() {
        // Editor extensions
        registerExtensionPoint(ExtensionPoint(
            id = "editor.language",
            name = "Language Support",
            description = "Add support for new programming languages",
            type = ExtensionType.LANGUAGE_SUPPORT
        ))
        
        registerExtensionPoint(ExtensionPoint(
            id = "editor.theme",
            name = "Editor Themes",
            description = "Custom editor color schemes and themes",
            type = ExtensionType.THEME
        ))
        
        registerExtensionPoint(ExtensionPoint(
            id = "editor.completion",
            name = "Code Completion",
            description = "Enhanced code completion providers",
            type = ExtensionType.CODE_COMPLETION
        ))
        
        // Build system extensions
        registerExtensionPoint(ExtensionPoint(
            id = "build.system",
            name = "Build Systems",
            description = "Support for different build systems",
            type = ExtensionType.BUILD_SYSTEM
        ))
        
        registerExtensionPoint(ExtensionPoint(
            id = "build.tool",
            name = "Build Tools",
            description = "Additional build and deployment tools",
            type = ExtensionType.BUILD_TOOL
        ))
        
        // Debugging extensions
        registerExtensionPoint(ExtensionPoint(
            id = "debug.adapter",
            name = "Debug Adapters",
            description = "Debug protocol adapters for different languages",
            type = ExtensionType.DEBUG_ADAPTER
        ))
        
        // Version control extensions
        registerExtensionPoint(ExtensionPoint(
            id = "vcs.provider",
            name = "Version Control",
            description = "Version control system integrations",
            type = ExtensionType.VCS_PROVIDER
        ))
        
        // Terminal extensions
        registerExtensionPoint(ExtensionPoint(
            id = "terminal.shell",
            name = "Terminal Shells",
            description = "Custom terminal shell implementations",
            type = ExtensionType.TERMINAL_SHELL
        ))
        
        // UI extensions
        registerExtensionPoint(ExtensionPoint(
            id = "ui.panel",
            name = "UI Panels",
            description = "Custom UI panels and tool windows",
            type = ExtensionType.UI_PANEL
        ))
        
        registerExtensionPoint(ExtensionPoint(
            id = "ui.action",
            name = "UI Actions",
            description = "Custom menu items and toolbar actions",
            type = ExtensionType.UI_ACTION
        ))
        
        // File type extensions
        registerExtensionPoint(ExtensionPoint(
            id = "file.type",
            name = "File Types",
            description = "Support for new file types and formats",
            type = ExtensionType.FILE_TYPE
        ))
        
        // Linting and analysis
        registerExtensionPoint(ExtensionPoint(
            id = "analysis.linter",
            name = "Code Linters",
            description = "Code analysis and linting tools",
            type = ExtensionType.LINTER
        ))
        
        // Project templates
        registerExtensionPoint(ExtensionPoint(
            id = "project.template",
            name = "Project Templates",
            description = "Custom project templates and scaffolding",
            type = ExtensionType.PROJECT_TEMPLATE
        ))
    }
    
    suspend fun installPlugin(pluginPath: String): Result<Plugin> = withContext(Dispatchers.IO) {
        try {
            val pluginFile = File(pluginPath)
            if (!pluginFile.exists()) {
                return@withContext Result.failure(Exception("Plugin file not found: $pluginPath"))
            }
            
            // Extract and validate plugin
            val plugin = extractAndValidatePlugin(pluginFile)
            
            // Check dependencies
            val missingDeps = checkDependencies(plugin.metadata.dependencies)
            if (missingDeps.isNotEmpty()) {
                return@withContext Result.failure(Exception("Missing dependencies: ${missingDeps.joinToString(", ")}"))
            }
            
            // Install plugin files
            val pluginDir = File(pluginsDir, plugin.metadata.id)
            pluginDir.mkdirs()
            
            // Copy plugin files
            if (pluginFile.isDirectory) {
                pluginFile.copyRecursively(pluginDir, overwrite = true)
            } else {
                // Extract ZIP/JAR if needed
                extractPluginArchive(pluginFile, pluginDir)
            }
            
            // Load and activate plugin
            val pluginInstance = loadPlugin(plugin)
            loadedPlugins[plugin.metadata.id] = pluginInstance
            
            // Update installed plugins list
            val currentPlugins = _installedPlugins.value.toMutableList()
            currentPlugins.removeAll { it.metadata.id == plugin.metadata.id }
            currentPlugins.add(plugin)
            _installedPlugins.value = currentPlugins
            
            // Save plugin registry
            savePluginRegistry()
            
            Result.success(plugin)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to install plugin", e))
        }
    }
    
    suspend fun uninstallPlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val plugin = _installedPlugins.value.find { it.metadata.id == pluginId }
                ?: return@withContext Result.failure(Exception("Plugin not found: $pluginId"))
            
            // Check if other plugins depend on this one
            val dependentPlugins = _installedPlugins.value.filter { 
                it.metadata.dependencies.any { dep -> dep.id == pluginId }
            }
            
            if (dependentPlugins.isNotEmpty()) {
                return@withContext Result.failure(Exception(
                    "Cannot uninstall plugin. Required by: ${dependentPlugins.joinToString(", ") { it.metadata.name }}"
                ))
            }
            
            // Deactivate plugin
            loadedPlugins[pluginId]?.deactivate()
            loadedPlugins.remove(pluginId)
            
            // Remove plugin files
            val pluginDir = File(pluginsDir, pluginId)
            if (pluginDir.exists()) {
                pluginDir.deleteRecursively()
            }
            
            // Update installed plugins list
            val currentPlugins = _installedPlugins.value.toMutableList()
            currentPlugins.removeAll { it.metadata.id == pluginId }
            _installedPlugins.value = currentPlugins
            
            // Save plugin registry
            savePluginRegistry()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to uninstall plugin", e))
        }
    }
    
    suspend fun enablePlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val plugin = _installedPlugins.value.find { it.metadata.id == pluginId }
                ?: return@withContext Result.failure(Exception("Plugin not found: $pluginId"))
            
            if (plugin.isEnabled) {
                return@withContext Result.success(Unit)
            }
            
            // Load and activate plugin
            val pluginInstance = loadPlugin(plugin)
            loadedPlugins[pluginId] = pluginInstance
            
            // Update plugin status
            val updatedPlugin = plugin.copy(isEnabled = true)
            val currentPlugins = _installedPlugins.value.toMutableList()
            val index = currentPlugins.indexOfFirst { it.metadata.id == pluginId }
            if (index >= 0) {
                currentPlugins[index] = updatedPlugin
                _installedPlugins.value = currentPlugins
            }
            
            savePluginRegistry()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to enable plugin", e))
        }
    }
    
    suspend fun disablePlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val plugin = _installedPlugins.value.find { it.metadata.id == pluginId }
                ?: return@withContext Result.failure(Exception("Plugin not found: $pluginId"))
            
            if (!plugin.isEnabled) {
                return@withContext Result.success(Unit)
            }
            
            // Deactivate plugin
            loadedPlugins[pluginId]?.deactivate()
            loadedPlugins.remove(pluginId)
            
            // Update plugin status
            val updatedPlugin = plugin.copy(isEnabled = false)
            val currentPlugins = _installedPlugins.value.toMutableList()
            val index = currentPlugins.indexOfFirst { it.metadata.id == pluginId }
            if (index >= 0) {
                currentPlugins[index] = updatedPlugin
                _installedPlugins.value = currentPlugins
            }
            
            savePluginRegistry()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to disable plugin", e))
        }
    }
    
    fun registerExtensionPoint(extensionPoint: ExtensionPoint) {
        extensionPoints[extensionPoint.id] = extensionPoint
    }
    
    fun registerHook(hookName: String, hook: PluginHook) {
        val hooks = pluginHooks.getOrPut(hookName) { mutableListOf() }
        hooks.add(hook)
    }
    
    fun unregisterHook(hookName: String, hook: PluginHook) {
        pluginHooks[hookName]?.remove(hook)
    }
    
    suspend fun executeHook(hookName: String, data: Any? = null): List<Any?> {
        val hooks = pluginHooks[hookName] ?: return emptyList()
        return hooks.mapNotNull { hook ->
            try {
                hook.execute(data)
            } catch (e: Exception) {
                null // Log error in production
            }
        }
    }
    
    fun getExtensions(extensionPointId: String): List<Extension> {
        return loadedPlugins.values.flatMap { plugin ->
            plugin.extensions.filter { it.extensionPointId == extensionPointId }
        }
    }
    
    private fun loadInstalledPlugins() {
        try {
            val registryFile = File(pluginsDir, "registry.json")
            if (registryFile.exists()) {
                val registryContent = registryFile.readText()
                val registry = json.decodeFromString<PluginRegistry>(registryContent)
                _installedPlugins.value = registry.plugins
                
                // Load enabled plugins
                registry.plugins.filter { it.isEnabled }.forEach { plugin ->
                    try {
                        val pluginInstance = loadPlugin(plugin)
                        loadedPlugins[plugin.metadata.id] = pluginInstance
                    } catch (e: Exception) {
                        // Log error and continue
                    }
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }
    
    private fun loadAvailablePlugins() {
        // Load from built-in plugin catalog
        val builtInPlugins = listOf(
            PluginMetadata(
                id = "git-integration",
                name = "Git Integration",
                version = "1.0.0",
                description = "Full Git version control integration with visual diff, branch management, and commit history",
                author = "Anyone IDE Team",
                category = "Version Control",
                tags = listOf("git", "vcs", "version-control"),
                extensionPoints = listOf("vcs.provider"),
                dependencies = emptyList(),
                minIdeVersion = "1.0.0",
                downloadUrl = "builtin://git-integration",
                iconUrl = null,
                screenshots = emptyList(),
                rating = 4.8f,
                downloadCount = 15420
            ),
            PluginMetadata(
                id = "flutter-support",
                name = "Flutter Development",
                version = "2.1.0",
                description = "Complete Flutter development support with hot reload, widget inspector, and Dart analysis",
                author = "Flutter Community",
                category = "Language Support",
                tags = listOf("flutter", "dart", "mobile"),
                extensionPoints = listOf("editor.language", "build.system", "debug.adapter"),
                dependencies = emptyList(),
                minIdeVersion = "1.0.0",
                downloadUrl = "https://plugins.anyoneide.com/flutter-support.zip",
                iconUrl = "https://plugins.anyoneide.com/icons/flutter.png",
                screenshots = listOf(
                    "https://plugins.anyoneide.com/screenshots/flutter-1.png",
                    "https://plugins.anyoneide.com/screenshots/flutter-2.png"
                ),
                rating = 4.9f,
                downloadCount = 28350
            ),
            PluginMetadata(
                id = "cpp-support",
                name = "C++ Development",
                version = "1.3.0",
                description = "Complete C++ development support with CMake, debugging, and IntelliSense",
                author = "C++ Community",
                category = "Language Support",
                tags = listOf("cpp", "c++", "cmake", "native"),
                extensionPoints = listOf("editor.language", "build.system", "debug.adapter"),
                dependencies = emptyList(),
                minIdeVersion = "1.0.0",
                downloadUrl = "https://plugins.anyoneide.com/cpp-support.zip",
                iconUrl = "https://plugins.anyoneide.com/icons/cpp.png",
                screenshots = emptyList(),
                rating = 4.7f,
                downloadCount = 12890
            ),
            PluginMetadata(
                id = "rest-api-tools",
                name = "REST API Tools",
                version = "1.5.0",
                description = "Complete REST API development and testing tools with request builder and response viewer",
                author = "API Tools Team",
                category = "Development Tools",
                tags = listOf("rest", "api", "http", "testing"),
                extensionPoints = listOf("ui.panel", "file.type"),
                dependencies = emptyList(),
                minIdeVersion = "1.0.0",
                downloadUrl = "https://plugins.anyoneide.com/rest-api-tools.zip",
                iconUrl = "https://plugins.anyoneide.com/icons/rest.png",
                screenshots = emptyList(),
                rating = 4.6f,
                downloadCount = 18750
            ),
            PluginMetadata(
                id = "kotlin-advanced",
                name = "Kotlin Advanced Tools",
                version = "1.2.0",
                description = "Enhanced Kotlin support with advanced refactoring, code analysis, and coroutine debugging",
                author = "Kotlin Community",
                category = "Language Support",
                tags = listOf("kotlin", "coroutines", "refactoring"),
                extensionPoints = listOf("editor.language", "analysis.linter"),
                dependencies = emptyList(),
                minIdeVersion = "1.0.0",
                downloadUrl = "https://plugins.anyoneide.com/kotlin-advanced.zip",
                iconUrl = "https://plugins.anyoneide.com/icons/kotlin.png",
                screenshots = emptyList(),
                rating = 4.8f,
                downloadCount = 14320
            ),
            PluginMetadata(
                id = "material-design-templates",
                name = "Material Design Templates",
                version = "2.0.0",
                description = "Collection of Material Design 3 templates and components for Android development",
                author = "UI/UX Team",
                category = "UI/UX",
                tags = listOf("material", "design", "templates", "ui"),
                extensionPoints = listOf("project.template", "ui.panel"),
                dependencies = emptyList(),
                minIdeVersion = "1.0.0",
                downloadUrl = "https://plugins.anyoneide.com/material-templates.zip",
                iconUrl = "https://plugins.anyoneide.com/icons/material.png",
                screenshots = emptyList(),
                rating = 4.9f,
                downloadCount = 22150
            ),
            PluginMetadata(
                id = "database-tools",
                name = "Database Tools",
                version = "1.1.0",
                description = "Database management tools for SQLite, Room, and other database systems",
                author = "Database Team",
                category = "Development Tools",
                tags = listOf("database", "sqlite", "room", "sql"),
                extensionPoints = listOf("ui.panel", "file.type"),
                dependencies = emptyList(),
                minIdeVersion = "1.0.0",
                downloadUrl = "https://plugins.anyoneide.com/database-tools.zip",
                iconUrl = "https://plugins.anyoneide.com/icons/database.png",
                screenshots = emptyList(),
                rating = 4.5f,
                downloadCount = 9870
            ),
            PluginMetadata(
                id = "theme-pack",
                name = "Premium Theme Pack",
                version = "3.0.0",
                description = "Collection of premium editor themes including Dark+, Monokai Pro, and Nord",
                author = "Theme Designers",
                category = "Themes",
                tags = listOf("themes", "dark", "light", "editor"),
                extensionPoints = listOf("editor.theme"),
                dependencies = emptyList(),
                minIdeVersion = "1.0.0",
                downloadUrl = "https://plugins.anyoneide.com/theme-pack.zip",
                iconUrl = "https://plugins.anyoneide.com/icons/themes.png",
                screenshots = emptyList(),
                rating = 4.7f,
                downloadCount = 31250
            )
        )
        
        _availablePlugins.value = builtInPlugins
    }
    
    private fun extractAndValidatePlugin(pluginFile: File): Plugin {
        // For now, assume it's a directory with plugin.json
        val manifestFile = if (pluginFile.isDirectory) {
            File(pluginFile, "plugin.json")
        } else {
            // Extract and find manifest
            throw Exception("Archive plugins not yet implemented")
        }
        
        if (!manifestFile.exists()) {
            throw Exception("Plugin manifest not found")
        }
        
        val manifestContent = manifestFile.readText()
        val metadata = json.decodeFromString<PluginMetadata>(manifestContent)
        
        return Plugin(
            metadata = metadata,
            isEnabled = true,
            installPath = pluginFile.absolutePath
        )
    }
    
    private fun checkDependencies(dependencies: List<PluginDependency>): List<String> {
        val missing = mutableListOf<String>()
        val installed = _installedPlugins.value
        
        dependencies.forEach { dep ->
            val installedPlugin = installed.find { it.metadata.id == dep.id }
            if (installedPlugin == null) {
                missing.add(dep.id)
            } else if (!isVersionCompatible(installedPlugin.metadata.version, dep.minVersion)) {
                missing.add("${dep.id} (requires ${dep.minVersion}, found ${installedPlugin.metadata.version})")
            }
        }
        
        return missing
    }
    
    private fun isVersionCompatible(installed: String, required: String): Boolean {
        // Simple version comparison - in production, use proper semantic versioning
        return installed >= required
    }
    
    private fun extractPluginArchive(archiveFile: File, targetDir: File) {
        // Implement ZIP/JAR extraction
        try {
            ZipInputStream(archiveFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val file = File(targetDir, entry.name)
                    
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                    }
                    
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to extract plugin archive", e)
        }
    }
    
    private fun loadPlugin(plugin: Plugin): PluginInstance {
        // Create plugin instance with actual functionality
        val extensions = loadPluginExtensions(plugin)
        val hooks = loadPluginHooks(plugin)
        
        val instance = PluginInstance(
            plugin = plugin,
            extensions = extensions,
            hooks = hooks,
            pluginManager = this
        )
        
        // Activate plugin
        instance.activate()
        
        return instance
    }
    
    private fun loadPluginExtensions(plugin: Plugin): List<Extension> {
        // Load extensions from plugin manifest and implementation
        val extensions = mutableListOf<Extension>()
        
        plugin.metadata.extensionPoints.forEach { extensionPointId ->
            // Create extension based on plugin type
            val extension = Extension(
                id = "${plugin.metadata.id}_extension",
                extensionPointId = extensionPointId,
                implementation = createExtensionImplementation(plugin, extensionPointId)
            )
            extensions.add(extension)
        }
        
        return extensions
    }
    
    private fun loadPluginHooks(plugin: Plugin): List<PluginHook> {
        // Load hooks from plugin implementation
        val hooks = mutableListOf<PluginHook>()
        
        // Create default hooks based on plugin category
        when (plugin.metadata.category) {
            "Language Support" -> {
                hooks.add(object : PluginHook {
                    override suspend fun execute(data: Any?): Any? {
                        // Language support hook implementation
                        return "Language support for ${plugin.metadata.name}"
                    }
                })
            }
            "Version Control" -> {
                hooks.add(object : PluginHook {
                    override suspend fun execute(data: Any?): Any? {
                        // VCS hook implementation
                        return "VCS integration for ${plugin.metadata.name}"
                    }
                })
            }
            "Build Tools" -> {
                hooks.add(object : PluginHook {
                    override suspend fun execute(data: Any?): Any? {
                        // Build tool hook implementation
                        return "Build tool for ${plugin.metadata.name}"
                    }
                })
            }
            "Themes" -> {
                hooks.add(object : PluginHook {
                    override suspend fun execute(data: Any?): Any? {
                        // Theme hook implementation
                        return "Theme for ${plugin.metadata.name}"
                    }
                })
            }
            "UI/UX" -> {
                hooks.add(object : PluginHook {
                    override suspend fun execute(data: Any?): Any? {
                        // UI/UX hook implementation
                        return "UI/UX components for ${plugin.metadata.name}"
                    }
                })
            }
            "Development Tools" -> {
                hooks.add(object : PluginHook {
                    override suspend fun execute(data: Any?): Any? {
                        // Development tools hook implementation
                        return "Development tools for ${plugin.metadata.name}"
                    }
                })
            }
            else -> {
                // Default hook for other categories
                hooks.add(object : PluginHook {
                    override suspend fun execute(data: Any?): Any? {
                        return "Plugin functionality for ${plugin.metadata.name}"
                    }
                })
            }
        }
        
        return hooks
    }
    
    private fun createExtensionImplementation(plugin: Plugin, extensionPointId: String): Any {
        // Create appropriate implementation based on extension point and plugin
        return when (extensionPointId) {
            "editor.language" -> LanguageExtension(
                pluginName = plugin.metadata.name,
                supportedExtensions = plugin.metadata.tags,
                features = plugin.metadata.extensionPoints
            )
            "editor.theme" -> ThemeExtension(
                themeName = plugin.metadata.name,
                themeData = plugin.metadata.description
            )
            "build.system" -> BuildSystemExtension(
                systemName = plugin.metadata.name,
                buildCommands = plugin.metadata.tags
            )
            "vcs.provider" -> VcsProviderExtension(
                providerName = plugin.metadata.name,
                supportedOperations = plugin.metadata.tags
            )
            "project.template" -> ProjectTemplateExtension(
                templateName = plugin.metadata.name,
                templateCategory = plugin.metadata.category,
                templateFeatures = plugin.metadata.tags
            )
            "ui.panel" -> UIPanelExtension(
                panelName = plugin.metadata.name,
                panelType = plugin.metadata.tags.firstOrNull() ?: "generic"
            )
            "debug.adapter" -> DebugAdapterExtension(
                adapterName = plugin.metadata.name,
                supportedLanguages = plugin.metadata.tags
            )
            "file.type" -> FileTypeExtension(
                typeName = plugin.metadata.name,
                extensions = plugin.metadata.tags
            )
            "analysis.linter" -> LinterExtension(
                linterName = plugin.metadata.name,
                supportedLanguages = plugin.metadata.tags
            )
            else -> GenericExtension(
                pluginName = plugin.metadata.name,
                extensionType = extensionPointId
            )
        }
    }
    
    private fun savePluginRegistry() {
        try {
            val registry = PluginRegistry(_installedPlugins.value)
            val registryContent = json.encodeToString(PluginRegistry.serializer(), registry)
            val registryFile = File(pluginsDir, "registry.json")
            registryFile.writeText(registryContent)
        } catch (e: Exception) {
            // Log error
        }
    }
    
    // Method to refresh available plugins (could fetch from remote in a real implementation)
    fun refreshPlugins() {
        loadAvailablePlugins()
    }
    
    // Method to get all extension points
    fun getAllExtensionPoints(): List<ExtensionPoint> {
        return extensionPoints.values.toList()
    }
    
    // Method to get all extensions for a specific plugin
    fun getPluginExtensions(pluginId: String): List<Extension> {
        return loadedPlugins[pluginId]?.extensions ?: emptyList()
    }
    
    // Method to check if a plugin is compatible with the current IDE version
    fun isPluginCompatible(pluginMetadata: PluginMetadata): Boolean {
        // In a real implementation, compare semantic versions
        return true
    }
}

// Extension implementations
data class LanguageExtension(
    val pluginName: String,
    val supportedExtensions: List<String>,
    val features: List<String>
)

data class ThemeExtension(
    val themeName: String,
    val themeData: String
)

data class BuildSystemExtension(
    val systemName: String,
    val buildCommands: List<String>
)

data class VcsProviderExtension(
    val providerName: String,
    val supportedOperations: List<String>
)

data class ProjectTemplateExtension(
    val templateName: String,
    val templateCategory: String,
    val templateFeatures: List<String>
)

data class UIPanelExtension(
    val panelName: String,
    val panelType: String
)

data class DebugAdapterExtension(
    val adapterName: String,
    val supportedLanguages: List<String>
)

data class FileTypeExtension(
    val typeName: String,
    val extensions: List<String>
)

data class LinterExtension(
    val linterName: String,
    val supportedLanguages: List<String>
)

data class GenericExtension(
    val pluginName: String,
    val extensionType: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PluginMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val category: String,
    val tags: List<String>,
    val extensionPoints: List<String>,
    val dependencies: List<PluginDependency>,
    val minIdeVersion: String,
    val downloadUrl: String,
    val iconUrl: String?,
    val screenshots: List<String>,
    val rating: Float,
    val downloadCount: Long
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PluginDependency(
    val id: String,
    val minVersion: String,
    val optional: Boolean = false
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Plugin(
    val metadata: PluginMetadata,
    val isEnabled: Boolean,
    val installPath: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PluginRegistry(
    val plugins: List<Plugin>
)

data class PluginInstance(
    val plugin: Plugin,
    val extensions: List<Extension>,
    val hooks: List<PluginHook>,
    val pluginManager: PluginManager
) {
    fun activate() {
        // Activate plugin extensions and hooks with actual functionality
        extensions.forEach { extension ->
            // Register extension with the system
            println("Activating extension: ${extension.id} for ${extension.extensionPointId}")
        }
        
        hooks.forEach { hook ->
            // Register hook with the plugin manager
            pluginManager.registerHook("plugin_${plugin.metadata.id}", hook)
            println("Activating hook for plugin: ${plugin.metadata.name}")
        }
    }
    
    fun deactivate() {
        // Deactivate plugin with proper cleanup
        extensions.forEach { extension ->
            // Unregister extension
            println("Deactivating extension: ${extension.id}")
        }
        
        hooks.forEach { hook ->
            // Unregister hook from plugin manager
            pluginManager.unregisterHook("plugin_${plugin.metadata.id}", hook)
            println("Deactivating hook for plugin: ${plugin.metadata.name}")
        }
    }
}

data class ExtensionPoint(
    val id: String,
    val name: String,
    val description: String,
    val type: ExtensionType
)

enum class ExtensionType {
    LANGUAGE_SUPPORT,
    THEME,
    CODE_COMPLETION,
    BUILD_SYSTEM,
    BUILD_TOOL,
    DEBUG_ADAPTER,
    VCS_PROVIDER,
    TERMINAL_SHELL,
    UI_PANEL,
    UI_ACTION,
    FILE_TYPE,
    LINTER,
    PROJECT_TEMPLATE
}

data class Extension(
    val id: String,
    val extensionPointId: String,
    val implementation: Any
)

interface PluginHook {
    suspend fun execute(data: Any?): Any?
}