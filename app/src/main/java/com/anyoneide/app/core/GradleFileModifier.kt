package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

/**
 * A utility class for modifying Gradle build files to fix common issues
 * and ensure successful builds.
 */
class GradleFileModifier(context: Context) {
    
    private val fileManager = FileManager(context)
    
    /**
     * Fixes common Gradle build issues in the specified file.
     */
    suspend fun fixBuildFile(buildFilePath: String): Flow<String> = flow {
        emit("Analyzing Gradle build file: $buildFilePath")
        
        try {
            val result = fileManager.readFile(buildFilePath)
            result.onSuccess { content ->
                var modified = false
                var updatedContent = content
                
                // Create backup
                val backupFile = File("$buildFilePath.bak")
                if (!backupFile.exists()) {
                    fileManager.copyFile(buildFilePath, backupFile.absolutePath)
                    emit("Created backup at ${backupFile.absolutePath}")
                }
                
                // Fix 1: Add missing repositories
                if (!content.contains("google()") || !content.contains("mavenCentral()")) {
                    updatedContent = addMissingRepositories(updatedContent)
                    emit("Fixed: Added missing repositories")
                    modified = true
                }
                
                // Fix 2: Update deprecated configurations
                if (content.contains("compile ") || content.contains("provided ")) {
                    updatedContent = updateDeprecatedConfigurations(updatedContent)
                    emit("Fixed: Updated deprecated configurations")
                    modified = true
                }
                
                // Fix 3: Add Java compatibility if missing
                if (!content.contains("sourceCompatibility") && !content.contains("targetCompatibility")) {
                    updatedContent = addJavaCompatibility(updatedContent)
                    emit("Fixed: Added Java compatibility settings")
                    modified = true
                }
                
                // Fix 4: Add Kotlin JVM target if missing
                if (content.contains("kotlin") && !content.contains("jvmTarget")) {
                    updatedContent = addKotlinJvmTarget(updatedContent)
                    emit("Fixed: Added Kotlin JVM target")
                    modified = true
                }
                
                // Fix 5: Add missing namespace for Android projects
                if (content.contains("com.android.") && !content.contains("namespace")) {
                    updatedContent = addNamespace(updatedContent, buildFilePath)
                    emit("Fixed: Added namespace declaration")
                    modified = true
                }
                
                // Fix 6: Add missing build features for Compose
                if (content.contains("compose") && !content.contains("buildFeatures")) {
                    updatedContent = addBuildFeatures(updatedContent)
                    emit("Fixed: Added build features for Compose")
                    modified = true
                }
                
                // Fix 7: Add missing packaging options
                if (content.contains("android {") && !content.contains("packaging {")) {
                    updatedContent = addPackagingOptions(updatedContent)
                    emit("Fixed: Added packaging options")
                    modified = true
                }
                
                // Fix 8: Update outdated dependencies
                val (dependenciesUpdated, updatedWithDeps) = updateOutdatedDependencies(updatedContent)
                if (dependenciesUpdated) {
                    updatedContent = updatedWithDeps
                    emit("Fixed: Updated outdated dependencies")
                    modified = true
                }
                
                // Save changes if modified
                if (modified) {
                    fileManager.writeFile(buildFilePath, updatedContent)
                    emit("Successfully updated Gradle build file")
                } else {
                    emit("No issues found in Gradle build file")
                }
            }
            result.onFailure { error ->
                emit("ERROR: Failed to read build file: ${error.message}")
            }
        } catch (_: Exception) {
            emit("ERROR: Failed to fix build file")
        }
    }
    
    /**
     * Adds a new dependency to the build file.
     */
    suspend fun addDependency(
        buildFilePath: String,
        dependency: String,
        configuration: String = "implementation"
    ): Flow<String> = flow {
        emit("Adding dependency: $dependency to $buildFilePath")
        
        try {
            val result = fileManager.readFile(buildFilePath)
            result.onSuccess { content ->
                // Check if dependency already exists
                if (content.contains(dependency)) {
                    emit("Dependency already exists in build file")
                    return@onSuccess
                }
                
                val updatedContent = if (content.contains("dependencies {")) {
                    // Add to existing dependencies block
                    val dependencyLine = "    $configuration \"$dependency\""
                    val pattern = Pattern.compile("dependencies\\s*\\{")
                    val matcher = pattern.matcher(content)
                    
                    if (matcher.find()) {
                        val position = matcher.end()
                        content.substring(0, position) + "\n" + dependencyLine + content.substring(position)
                    } else {
                        content
                    }
                } else {
                    // Create new dependencies block
                    val dependenciesBlock = """
                        |
                        |dependencies {
                        |    $configuration "$dependency"
                        |}
                    """.trimMargin()
                    
                    content + dependenciesBlock
                }
                
                fileManager.writeFile(buildFilePath, updatedContent)
                emit("Successfully added dependency: $dependency")
            }
            result.onFailure { error ->
                emit("ERROR: Failed to read build file: ${error.message}")
            }
        } catch (_: Exception) {
            emit("ERROR: Failed to add dependency")
        }
    }
    
    /**
     * Removes a dependency from the build file.
     */
    suspend fun removeDependency(buildFilePath: String, dependency: String): Flow<String> = flow {
        emit("Removing dependency: $dependency from $buildFilePath")
        
        try {
            val result = fileManager.readFile(buildFilePath)
            result.onSuccess { content ->
                // Find and remove the dependency line
                val lines = content.lines().toMutableList()
                var found = false
                
                val iterator = lines.listIterator()
                while (iterator.hasNext()) {
                    val line = iterator.next()
                    if (line.contains(dependency) && 
                        (line.contains("implementation") || 
                         line.contains("api") || 
                         line.contains("testImplementation") || 
                         line.contains("androidTestImplementation") ||
                         line.contains("compileOnly") ||
                         line.contains("runtimeOnly"))) {
                        iterator.remove()
                        found = true
                        break
                    }
                }
                
                if (found) {
                    val updatedContent = lines.joinToString("\n")
                    fileManager.writeFile(buildFilePath, updatedContent)
                    emit("Successfully removed dependency: $dependency")
                } else {
                    emit("Dependency not found in build file")
                }
            }
            result.onFailure { error ->
                emit("ERROR: Failed to read build file: ${error.message}")
            }
        } catch (_: Exception) {
            emit("ERROR: Failed to remove dependency")
        }
    }
    
    /**
     * Updates all dependencies to their latest versions.
     */
    suspend fun updateAllDependencies(buildFilePath: String): Flow<String> = flow {
        emit("Updating all dependencies in $buildFilePath")
        
        try {
            val result = fileManager.readFile(buildFilePath)
            result.onSuccess { content ->
                val (updated, updatedContent) = updateOutdatedDependencies(content)
                
                if (updated) {
                    fileManager.writeFile(buildFilePath, updatedContent)
                    emit("Successfully updated dependencies")
                } else {
                    emit("All dependencies are already up to date")
                }
            }
            result.onFailure { error ->
                emit("ERROR: Failed to read build file: ${error.message}")
            }
        } catch (_: Exception) {
            emit("ERROR: Failed to update dependencies")
        }
    }
    
    /**
     * Adds missing repositories to the build file.
     */
    private fun addMissingRepositories(content: String): String {
        // Check if repositories block exists
        if (content.contains("repositories {")) {
            var updatedContent = content
            
            // Add Google repository if missing
            if (!content.contains("google()")) {
                updatedContent = updatedContent.replace(
                    "repositories {",
                    "repositories {\n        google()"
                )
            }
            
            // Add Maven Central if missing
            if (!content.contains("mavenCentral()")) {
                updatedContent = updatedContent.replace(
                    "repositories {",
                    "repositories {\n        mavenCentral()"
                )
            }
            
            return updatedContent
        } else {
            // Create repositories block
            val repositoriesBlock = """
                |repositories {
                |    google()
                |    mavenCentral()
                |}
            """.trimMargin()
            
            // Add after plugins block if it exists
            return if (content.contains("plugins {")) {
                content.replace(
                    "plugins {",
                    "plugins {\n}\n\n$repositoriesBlock\n"
                )
            } else {
                // Add at the beginning
                "$repositoriesBlock\n\n$content"
            }
        }
    }
    
    /**
     * Updates deprecated Gradle configurations.
     */
    private fun updateDeprecatedConfigurations(content: String): String {
        return content
            .replace("compile ", "implementation ")
            .replace("provided ", "compileOnly ")
            .replace("compile(", "implementation(")
            .replace("provided(", "compileOnly(")
            .replace("testCompile ", "testImplementation ")
            .replace("testCompile(", "testImplementation(")
            .replace("androidTestCompile ", "androidTestImplementation ")
            .replace("androidTestCompile(", "androidTestImplementation(")
    }
    
    /**
     * Adds Java compatibility settings if missing.
     */
    private fun addJavaCompatibility(content: String): String {
        val compileOptionsBlock = """
            |compileOptions {
            |    sourceCompatibility JavaVersion.VERSION_1_8
            |    targetCompatibility JavaVersion.VERSION_1_8
            |}
        """.trimMargin()
        
        return if (content.contains("android {")) {
            // Add inside android block
            content.replace(
                "android {",
                "android {\n    $compileOptionsBlock"
            )
        } else {
            // Add at the end
            "$content\n\n$compileOptionsBlock"
        }
    }
    
    /**
     * Adds Kotlin JVM target if missing.
     */
    private fun addKotlinJvmTarget(content: String): String {
        val kotlinOptionsBlock = """
            |kotlinOptions {
            |    jvmTarget = "1.8"
            |}
        """.trimMargin()
        
        return if (content.contains("android {")) {
            // Add inside android block
            content.replace(
                "android {",
                "android {\n    $kotlinOptionsBlock"
            )
        } else {
            // Add at the end
            "$content\n\n$kotlinOptionsBlock"
        }
    }
    
    /**
     * Adds namespace declaration for Android projects.
     */
    private fun addNamespace(content: String, buildFilePath: String): String {
        // Extract package name from directory structure
        val packageName = try {
            val buildFile = File(buildFilePath)
            val projectDir = buildFile.parentFile
            val srcDir = File(projectDir, "src/main/java")
            
            if (srcDir.exists()) {
                var packageDir = srcDir
                val packageParts = mutableListOf<String>()
                
                // Find the deepest directory with a single subdirectory
                var foundJavaFiles = false
                while (!foundJavaFiles) {
                    val subdirs = packageDir.listFiles { subdir -> subdir.isDirectory }
                    
                    if (subdirs == null || subdirs.isEmpty()) {
                        break
                    } else if (subdirs.size == 1) {
                        packageDir = subdirs[0]
                        packageParts.add(packageDir.name)
                    } else {
                        break
                    }
                    
                    // Check if this directory contains Java/Kotlin files
                    if (packageDir.listFiles { sourceFile -> 
                        sourceFile.isFile && (sourceFile.name.endsWith(".java") || sourceFile.name.endsWith(".kt"))
                    }?.isNotEmpty() == true) {
                        foundJavaFiles = true
                    }
                }
                
                if (packageParts.isNotEmpty()) {
                    packageParts.joinToString(".")
                } else {
                    "com.example.app"
                }
            } else {
                "com.example.app"
            }
        } catch (_: Exception) {
            "com.example.app"
        }
        
        return if (content.contains("android {")) {
            // Add namespace inside android block
            content.replace(
                "android {",
                "android {\n    namespace = \"$packageName\""
            )
        } else {
            content
        }
    }
    
    /**
     * Adds build features for Compose if missing.
     */
    private fun addBuildFeatures(content: String): String {
        val buildFeaturesBlock = """
            |buildFeatures {
            |    compose = true
            |}
        """.trimMargin()
        
        return if (content.contains("android {")) {
            // Add inside android block
            content.replace(
                "android {",
                "android {\n    $buildFeaturesBlock"
            )
        } else {
            content
        }
    }
    
    /**
     * Adds packaging options to fix common conflicts.
     */
    private fun addPackagingOptions(content: String): String {
        val packagingBlock = """
            |packaging {
            |    resources {
            |        excludes += "/META-INF/{AL2.0,LGPL2.1}"
            |        excludes += "/META-INF/INDEX.LIST"
            |        excludes += "/META-INF/DEPENDENCIES"
            |    }
            |}
        """.trimMargin()
        
        return if (content.contains("android {")) {
            // Add inside android block
            content.replace(
                "android {",
                "android {\n    $packagingBlock"
            )
        } else {
            content
        }
    }
    
    /**
     * Updates outdated dependencies to their latest versions.
     * Returns a pair of (wasUpdated, updatedContent)
     */
    private fun updateOutdatedDependencies(content: String): Pair<Boolean, String> {
        var updated = false
        var updatedContent = content
        
        // Latest versions of common dependencies
        val latestVersions = mapOf(
            "androidx.core:core-ktx" to "1.12.0",
            "androidx.appcompat:appcompat" to "1.6.1",
            "androidx.activity:activity-compose" to "1.8.2",
            "androidx.compose:compose-bom" to "2024.02.00",
            "androidx.compose.material3:material3" to "1.2.0",
            "androidx.lifecycle:lifecycle-runtime-ktx" to "2.7.0",
            "androidx.lifecycle:lifecycle-viewmodel-compose" to "2.7.0",
            "androidx.navigation:navigation-compose" to "2.7.5",
            "androidx.room:room-runtime" to "2.6.1",
            "androidx.room:room-ktx" to "2.6.1",
            "com.google.android.material:material" to "1.11.0",
            "org.jetbrains.kotlinx:kotlinx-coroutines-android" to "1.7.3",
            "com.squareup.retrofit2:retrofit" to "2.9.0",
            "com.squareup.okhttp3:okhttp" to "4.12.0",
            "io.coil-kt:coil-compose" to "2.5.0",
            "junit:junit" to "4.13.2",
            "androidx.test.ext:junit" to "1.1.5",
            "androidx.test.espresso:espresso-core" to "3.5.1"
        )
        
        // Find and update dependencies
        latestVersions.forEach { (dependency, latestVersion) ->
            val regex = "$dependency:(['\"])([^'\"]+)(['\"])".toRegex()
            val result = regex.find(updatedContent)
            
            if (result != null) {
                val currentVersion = result.groupValues[2]
                if (currentVersion != latestVersion) {
                    updatedContent = updatedContent.replace(
                        "$dependency:${result.groupValues[1]}$currentVersion${result.groupValues[3]}",
                        "$dependency:${result.groupValues[1]}$latestVersion${result.groupValues[3]}"
                    )
                    updated = true
                }
            }
        }
        
        return Pair(updated, updatedContent)
    }
    
    /**
     * Analyzes a Gradle build file and returns information about it.
     */
    suspend fun analyzeBuildFile(buildFilePath: String): Result<BuildFileInfo> = withContext(Dispatchers.IO) {
        try {
            val result = fileManager.readFile(buildFilePath)
            result.fold(
                onSuccess = { content ->
                    val info = BuildFileInfo(
                        path = buildFilePath,
                        dependencies = extractDependencies(content),
                        plugins = extractPlugins(content),
                        compileSdk = extractCompileSdk(content),
                        minSdk = extractMinSdk(content),
                        targetSdk = extractTargetSdk(content),
                        kotlinVersion = extractKotlinVersion(content),
                        agpVersion = extractAGPVersion(content),
                        hasCompose = content.contains("compose"),
                        hasViewBinding = content.contains("viewBinding"),
                        hasDataBinding = content.contains("dataBinding"),
                        hasKotlinAndroid = content.contains("kotlin-android"),
                        hasKapt = content.contains("kotlin-kapt")
                    )
                    Result.success(info)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (_: Exception) {
            Result.failure(Exception("Failed to analyze build file"))
        }
    }
    
    /**
     * Extracts dependencies from build file content.
     */
    private fun extractDependencies(content: String): List<BuildDependencyInfo> {
        val dependencies = mutableListOf<BuildDependencyInfo>()
        val regex = "(\\w+)\\s+['\"]([^:]+):([^:]+):([^'\"]+)['\"]".toRegex()
        
        regex.findAll(content).forEach { matchResult ->
            val (configuration, group, name, version) = matchResult.destructured
            dependencies.add(
                BuildDependencyInfo(
                    configuration = configuration,
                    group = group,
                    name = name,
                    version = version
                )
            )
        }
        
        return dependencies
    }
    
    /**
     * Extracts plugins from build file content.
     */
    private fun extractPlugins(content: String): List<String> {
        val plugins = mutableListOf<String>()
        val regex = "id\\s+['\"]([^'\"]+)['\"]".toRegex()
        
        regex.findAll(content).forEach { matchResult ->
            plugins.add(matchResult.groupValues[1])
        }
        
        return plugins
    }
    
    /**
     * Extracts compileSdk from build file content.
     */
    private fun extractCompileSdk(content: String): Int? {
        val regex = "compileSdk\\s*=?\\s*(\\d+)".toRegex()
        val result = regex.find(content)
        return result?.groupValues?.get(1)?.toIntOrNull()
    }
    
    /**
     * Extracts minSdk from build file content.
     */
    private fun extractMinSdk(content: String): Int? {
        val regex = "minSdk\\s*=?\\s*(\\d+)".toRegex()
        val result = regex.find(content)
        return result?.groupValues?.get(1)?.toIntOrNull()
    }
    
    /**
     * Extracts targetSdk from build file content.
     */
    private fun extractTargetSdk(content: String): Int? {
        val regex = "targetSdk\\s*=?\\s*(\\d+)".toRegex()
        val result = regex.find(content)
        return result?.groupValues?.get(1)?.toIntOrNull()
    }
    
    /**
     * Extracts Kotlin version from build file content.
     */
    private fun extractKotlinVersion(content: String): String? {
        val regex = "kotlin[^\"']*[\"']([\\d.]+)[\"']".toRegex()
        val result = regex.find(content)
        return result?.groupValues?.get(1)
    }
    
    /**
     * Extracts Android Gradle Plugin version from build file content.
     */
    private fun extractAGPVersion(content: String): String? {
        val regex = "com\\.android\\.application[^\"']*[\"']([\\d.]+)[\"']".toRegex()
        val result = regex.find(content)
        return result?.groupValues?.get(1)
    }
}

/**
 * Information about a Gradle build file.
 */
data class BuildFileInfo(
    val path: String,
    val dependencies: List<BuildDependencyInfo>,
    val plugins: List<String>,
    val compileSdk: Int?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val kotlinVersion: String?,
    val agpVersion: String?,
    val hasCompose: Boolean,
    val hasViewBinding: Boolean,
    val hasDataBinding: Boolean,
    val hasKotlinAndroid: Boolean,
    val hasKapt: Boolean
)

/**
 * Information about a dependency.
 */
data class BuildDependencyInfo(
    val configuration: String,
    val group: String,
    val name: String,
    val version: String
)