package com.anyoneide.app.core

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.util.regex.Pattern

class ProjectManager(context: Context) {
    
    private val projectTemplates = ProjectTemplates(context)
    
    suspend fun openProject(projectPath: String): Result<ProjectInfo> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(projectPath)
            if (!projectDir.exists() || !projectDir.isDirectory) {
                return@withContext Result.failure(Exception("Project directory does not exist: $projectPath"))
            }
            
            val projectInfo = analyzeProject(projectDir)
            Result.success(projectInfo)
        } catch (_: Exception) {
            Result.failure(Exception("Failed to open project"))
        }
    }
    
    suspend fun createProject(
        projectPath: String, 
        projectName: String, 
        projectType: ProjectType,
        packageName: String = "com.example.${projectName.lowercase().replace(" ", "")}"
    ): Result<ProjectInfo> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(projectPath, projectName)
            if (projectDir.exists()) {
                return@withContext Result.failure(Exception("Project directory already exists: ${projectDir.absolutePath}"))
            }
            
            projectDir.mkdirs()
            
            when (projectType) {
                ProjectType.ANDROID_APP -> projectTemplates.createAndroidAppTemplate(projectDir, packageName)
                ProjectType.ANDROID_LIBRARY -> projectTemplates.createAndroidLibraryProject(projectDir, projectName)
                ProjectType.JAVA_LIBRARY -> projectTemplates.createJavaLibraryProject(projectDir, projectName)
                ProjectType.KOTLIN_MULTIPLATFORM -> projectTemplates.createKotlinMultiplatformProject(projectDir, projectName)
                ProjectType.COMPOSE_APP -> projectTemplates.createComposeAppTemplate(projectDir, packageName)
                ProjectType.MVVM_APP -> projectTemplates.createMvvmAppTemplate(projectDir, packageName)
                ProjectType.REST_API_CLIENT -> projectTemplates.createRestApiClientTemplate(projectDir, packageName)
                ProjectType.GAME_2D -> projectTemplates.create2DGameTemplate(projectDir, packageName)
                ProjectType.RUST_ANDROID_LIB -> projectTemplates.createRustAndroidLibraryTemplate(projectDir, packageName)
            }
            
            val projectInfo = analyzeProject(projectDir)
            Result.success(projectInfo)
        } catch (_: Exception) {
            Result.failure(Exception("Failed to create project"))
        }
    }
    
    private fun analyzeProject(projectDir: File): ProjectInfo {
        val projectName = projectDir.name
        val projectType = detectProjectType(projectDir)
        val modules = discoverModules(projectDir)
        val dependencies = parseDependencies(projectDir)
        val fileTree = buildFileTree(projectDir)
        
        return ProjectInfo(
            name = projectName,
            path = projectDir.absolutePath,
            type = projectType,
            modules = modules,
            dependencies = dependencies,
            fileTree = fileTree
        )
    }
    
    private fun detectProjectType(projectDir: File): ProjectType {
        val buildGradle = File(projectDir, "build.gradle")
        val buildGradleKts = File(projectDir, "build.gradle.kts")
        val cargoToml = File(projectDir, "Cargo.toml")
        val rustLibDir = File(projectDir, "rust-lib")
        
        // Check for Rust project first
        if (cargoToml.exists() || (rustLibDir.exists() && File(rustLibDir, "Cargo.toml").exists())) {
            return ProjectType.RUST_ANDROID_LIB
        }
        
        if (buildGradle.exists() || buildGradleKts.exists()) {
            val buildFile = if (buildGradleKts.exists()) buildGradleKts else buildGradle
            val content = buildFile.readText()
            
            return when {
                content.contains("com.android.application") -> {
                    if (content.contains("compose = true") || content.contains("compose true")) {
                        ProjectType.COMPOSE_APP
                    } else {
                        ProjectType.ANDROID_APP
                    }
                }
                content.contains("com.android.library") -> ProjectType.ANDROID_LIBRARY
                content.contains("kotlin-multiplatform") -> ProjectType.KOTLIN_MULTIPLATFORM
                else -> ProjectType.JAVA_LIBRARY
            }
        }
        
        return ProjectType.JAVA_LIBRARY
    }
    
    private fun discoverModules(projectDir: File): List<ModuleInfo> {
        val modules = mutableListOf<ModuleInfo>()
        
        // Check for settings.gradle to find modules
        val settingsFiles = listOf(
            File(projectDir, "settings.gradle"),
            File(projectDir, "settings.gradle.kts")
        )
        
        for (settingsFile in settingsFiles) {
            if (settingsFile.exists()) {
                val content = settingsFile.readText()
                val moduleNames = parseModulesFromSettings(content)
                
                for (moduleName in moduleNames) {
                    val moduleDir = File(projectDir, moduleName)
                    if (moduleDir.exists()) {
                        val moduleInfo = analyzeModule(moduleDir, moduleName)
                        modules.add(moduleInfo)
                    }
                }
                break
            }
        }
        
        // If no settings.gradle, treat as single module
        if (modules.isEmpty()) {
            val moduleInfo = analyzeModule(projectDir, "main")
            modules.add(moduleInfo)
        }
        
        return modules
    }
    
    private fun parseModulesFromSettings(content: String): List<String> {
        val modules = mutableListOf<String>()
        val includePattern = Pattern.compile("include\\s*['\"]([^'\"]+)['\"]")
        val matcher = includePattern.matcher(content)
        
        while (matcher.find()) {
            val moduleName = matcher.group(1)?.removePrefix(":") ?: ""
            if (moduleName.isNotEmpty()) {
                modules.add(moduleName)
            }
        }
        
        return modules
    }
    
    private fun analyzeModule(moduleDir: File, moduleName: String): ModuleInfo {
        val moduleType = when {
            moduleName == "app" -> ModuleType.APP
            moduleName.contains("test") -> ModuleType.TEST
            else -> ModuleType.LIBRARY
        }
        
        val sourceSets = discoverSourceSets(moduleDir)
        
        return ModuleInfo(
            name = moduleName,
            path = moduleDir.absolutePath,
            type = moduleType,
            sourceSets = sourceSets
        )
    }
    
    private fun discoverSourceSets(moduleDir: File): List<SourceSetInfo> {
        val sourceSets = mutableListOf<SourceSetInfo>()
        val srcDir = File(moduleDir, "src")
        
        if (srcDir.exists()) {
            srcDir.listFiles { file -> file.isDirectory }?.forEach { sourceSetDir ->
                val sourceSetName = sourceSetDir.name
                val javaDirs = findSourceDirs(sourceSetDir, "java")
                val kotlinDirs = findSourceDirs(sourceSetDir, "kotlin")
                val resourceDirs = findSourceDirs(sourceSetDir, "res")
                
                sourceSets.add(SourceSetInfo(
                    name = sourceSetName,
                    javaDirs = javaDirs,
                    kotlinDirs = kotlinDirs,
                    resourceDirs = resourceDirs
                ))
            }
        }
        
        return sourceSets
    }
    
    private fun findSourceDirs(baseDir: File, dirName: String): List<String> {
        val dirs = mutableListOf<String>()
        val targetDir = File(baseDir, dirName)
        
        if (targetDir.exists()) {
            dirs.add(targetDir.absolutePath)
        }
        
        return dirs
    }
    
    private fun parseDependencies(projectDir: File): List<ProjectDependencyInfo> {
        val dependencies = mutableListOf<ProjectDependencyInfo>()
        val buildFiles = listOf(
            File(projectDir, "build.gradle"),
            File(projectDir, "build.gradle.kts"),
            File(projectDir, "Cargo.toml")
        )
        
        for (buildFile in buildFiles) {
            if (buildFile.exists()) {
                val content = buildFile.readText()
                val parsedDeps = if (buildFile.name == "Cargo.toml") {
                    parseDependenciesFromCargoToml(content)
                } else {
                    parseDependenciesFromBuildFile(content)
                }
                dependencies.addAll(parsedDeps)
            }
        }
        
        return dependencies
    }
    
    private fun parseDependenciesFromBuildFile(content: String): List<ProjectDependencyInfo> {
        val dependencies = mutableListOf<ProjectDependencyInfo>()
        val dependencyPattern = Pattern.compile("(implementation|api|testImplementation|androidTestImplementation|compileOnly|runtimeOnly)\\s*['\"]([^'\"]+)['\"]")
        val matcher = dependencyPattern.matcher(content)
        
        while (matcher.find()) {
            val configuration = matcher.group(1) ?: ""
            val dependency = matcher.group(2) ?: ""
            val parts = dependency.split(":")
            
            if (parts.size >= 2) {
                val group = parts[0]
                val name = parts[1]
                val version = if (parts.size >= 3) parts[2] else "unknown"
                
                dependencies.add(ProjectDependencyInfo(
                    group = group,
                    name = name,
                    version = version,
                    configuration = configuration
                ))
            }
        }
        
        return dependencies
    }
    
    private fun parseDependenciesFromCargoToml(content: String): List<ProjectDependencyInfo> {
        val dependencies = mutableListOf<ProjectDependencyInfo>()
        var inDependenciesSection = false
        
        content.lines().forEach { line ->
            val trimmedLine = line.trim()
            
            when {
                trimmedLine == "[dependencies]" -> {
                    inDependenciesSection = true
                }
                trimmedLine.startsWith("[") && trimmedLine.endsWith("]") -> {
                    inDependenciesSection = false
                }
                inDependenciesSection && trimmedLine.contains("=") -> {
                    val parts = trimmedLine.split("=", limit = 2)
                    if (parts.size == 2) {
                        val name = parts[0].trim()
                        val versionSpec = parts[1].trim()
                        
                        // Parse version from different formats
                        val version = when {
                            versionSpec.startsWith("\"") && versionSpec.endsWith("\"") -> {
                                versionSpec.removeSurrounding("\"")
                            }
                            versionSpec.startsWith("{") && versionSpec.contains("version") -> {
                                val versionPattern = "version\\s*=\\s*[\"']([^\"']+)[\"']".toRegex()
                                val match = versionPattern.find(versionSpec)
                                match?.groupValues?.get(1) ?: "unknown"
                            }
                            else -> "latest"
                        }
                        
                        dependencies.add(ProjectDependencyInfo(
                            group = "crates.io",
                            name = name,
                            version = version,
                            configuration = "dependency"
                        ))
                    }
                }
            }
        }
        
        return dependencies
    }
    
    private fun buildFileTree(projectDir: File): List<FileNode> {
        return buildFileTreeRecursive(projectDir, 0, 3) // Max depth of 3
    }
    
    private fun buildFileTreeRecursive(dir: File, currentDepth: Int, maxDepth: Int): List<FileNode> {
        if (currentDepth >= maxDepth) return emptyList()
        
        val nodes = mutableListOf<FileNode>()
        
        dir.listFiles()?.forEach { file ->
            // Skip hidden files and common build directories
            if (file.name.startsWith(".") || file.name in listOf("build", ".gradle", ".idea")) {
                return@forEach
            }
            
            val children = if (file.isDirectory) {
                buildFileTreeRecursive(file, currentDepth + 1, maxDepth)
            } else {
                emptyList()
            }
            
            nodes.add(FileNode(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                children = children
            ))
        }
        
        return nodes.sortedWith(compareBy<FileNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ProjectInfo(
    val name: String,
    val path: String,
    val type: ProjectType,
    val modules: List<ModuleInfo>,
    val dependencies: List<ProjectDependencyInfo>,
    val fileTree: List<FileNode>
)

@Serializable
enum class ProjectType {
    ANDROID_APP,
    ANDROID_LIBRARY,
    JAVA_LIBRARY,
    KOTLIN_MULTIPLATFORM,
    COMPOSE_APP,
    MVVM_APP,
    REST_API_CLIENT,
    GAME_2D,
    RUST_ANDROID_LIB
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ModuleInfo(
    val name: String,
    val path: String,
    val type: ModuleType,
    val sourceSets: List<SourceSetInfo>
)

@Serializable
enum class ModuleType {
    APP,
    LIBRARY,
    TEST
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SourceSetInfo(
    val name: String,
    val javaDirs: List<String>,
    val kotlinDirs: List<String>,
    val resourceDirs: List<String>
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ProjectDependencyInfo(
    val group: String,
    val name: String,
    val version: String,
    val configuration: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileNode>
)