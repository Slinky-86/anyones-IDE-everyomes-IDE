package com.anyoneide.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager for the native Rust-based Gradle file management
 * This is an experimental feature that uses JNI to call into a Rust library
 */
class RustGradleManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RustGradleManager"
        private val isLibraryLoaded = AtomicBoolean(false)
        
        // Load the native library
        init {
            try {
                System.loadLibrary("rust_native_build")
                isLibraryLoaded.set(true)
                Log.d(TAG, "Native Gradle manager library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                // Library not loaded - will be handled gracefully
                Log.e(TAG, "Failed to load native Gradle manager library", e)
            } catch (e: Exception) {
                // Other exceptions - will be handled gracefully
                Log.e(TAG, "Exception loading native Gradle manager library", e)
            }
        }
        
        // Native method declarations
        @JvmStatic external fun nativeAnalyzeGradleFile(filePath: String): String
        @JvmStatic external fun nativeOptimizeGradleFile(filePath: String): String
        @JvmStatic external fun nativeAddDependency(filePath: String, dependency: String, configuration: String): Boolean
        @JvmStatic external fun nativeRemoveDependency(filePath: String, dependency: String): Boolean
        @JvmStatic external fun nativeUpdateDependencies(filePath: String): String
        @JvmStatic external fun nativeFixCommonIssues(filePath: String): String
        @JvmStatic external fun nativeGenerateBuildReport(projectPath: String): String
    }
    
    private val gradleBuildManager = GradleBuildManager(context)
    
    /**
     * Check if the native Gradle manager is available
     */
    fun isNativeGradleManagerAvailable(): Boolean {
        return isLibraryLoaded.get()
    }
    
    /**
     * Analyze a Gradle build file
     */
    suspend fun analyzeGradleFile(filePath: String): Result<com.anyoneide.app.core.GradleBuildAnalysis> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            return@withContext gradleBuildManager.analyzeBuildFile(filePath)
        }
        
        try {
            val analysisJson = nativeAnalyzeGradleFile(filePath)
            val analysis = parseGradleBuildAnalysis(analysisJson)
            // Convert to the expected type
            val convertedAnalysis = convertToGradleBuildAnalysis(analysis)
            Result.success(convertedAnalysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing Gradle file with native implementation", e)
            // Fall back to Java implementation
            gradleBuildManager.analyzeBuildFile(filePath)
        }
    }
    
    /**
     * Optimize a Gradle build file
     */
    suspend fun optimizeGradleFile(filePath: String): Flow<String> = flow {
        emit("Analyzing Gradle build file: $filePath")
        
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            gradleBuildManager.optimizeBuildFile(filePath).collect { message ->
                emit(message)
            }
            return@flow
        }
        
        try {
            emit("Using native Gradle file optimizer")
            
            val resultJson = nativeOptimizeGradleFile(filePath)
            val result = parseOptimizationResult(resultJson)
            
            if (result.success) {
                emit("Gradle file optimized successfully")
                
                // Report changes
                result.issuesFixed.forEach { issue ->
                    emit("Fixed: $issue")
                }
                
                result.dependenciesUpdated.forEach { update ->
                    emit("Updated: ${update.group}:${update.name} ${update.oldVersion} -> ${update.newVersion}")
                }
            } else {
                emit("ERROR: Failed to optimize Gradle file: ${result.error}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing Gradle file with native implementation", e)
            emit("ERROR: Native implementation failed, falling back to Java implementation")
            
            // Fall back to Java implementation
            gradleBuildManager.optimizeBuildFile(filePath).collect { message ->
                emit(message)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Add a dependency to a Gradle build file
     */
    suspend fun addDependency(filePath: String, dependency: String, configuration: String): Flow<String> = flow {
        emit("Adding dependency: $dependency to $filePath")
        
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            gradleBuildManager.addDependency(filePath, dependency, configuration).collect { message ->
                emit(message)
            }
            return@flow
        }
        
        try {
            val success = nativeAddDependency(filePath, dependency, configuration)
            if (success) {
                emit("Dependency added successfully")
            } else {
                emit("ERROR: Failed to add dependency")
                
                // Fall back to Java implementation
                emit("Falling back to Java implementation")
                gradleBuildManager.addDependency(filePath, dependency, configuration).collect { message ->
                    emit(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding dependency with native implementation", e)
            emit("ERROR: Native implementation failed, falling back to Java implementation")
            
            // Fall back to Java implementation
            gradleBuildManager.addDependency(filePath, dependency, configuration).collect { message ->
                emit(message)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Remove a dependency from a Gradle build file
     */
    suspend fun removeDependency(filePath: String, dependency: String): Flow<String> = flow {
        emit("Removing dependency: $dependency from $filePath")
        
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            gradleBuildManager.removeDependency(filePath, dependency).collect { message ->
                emit(message)
            }
            return@flow
        }
        
        try {
            val success = nativeRemoveDependency(filePath, dependency)
            if (success) {
                emit("Dependency removed successfully")
            } else {
                emit("ERROR: Failed to remove dependency")
                
                // Fall back to Java implementation
                emit("Falling back to Java implementation")
                gradleBuildManager.removeDependency(filePath, dependency).collect { message ->
                    emit(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing dependency with native implementation", e)
            emit("ERROR: Native implementation failed, falling back to Java implementation")
            
            // Fall back to Java implementation
            gradleBuildManager.removeDependency(filePath, dependency).collect { message ->
                emit(message)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Update dependencies in a Gradle build file
     */
    suspend fun updateDependencies(filePath: String): Flow<String> = flow {
        emit("Updating dependencies in: $filePath")
        
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            gradleBuildManager.updateDependencies(filePath).collect { message ->
                emit(message)
            }
            return@flow
        }
        
        try {
            val resultJson = nativeUpdateDependencies(filePath)
            val result = parseOptimizationResult(resultJson)
            
            if (result.success) {
                if (result.dependenciesUpdated.isEmpty()) {
                    emit("All dependencies are already up to date")
                } else {
                    emit("Updated ${result.dependenciesUpdated.size} dependencies successfully")
                    
                    // Report updates
                    result.dependenciesUpdated.forEach { update ->
                        emit("Updated: ${update.group}:${update.name} ${update.oldVersion} -> ${update.newVersion}")
                    }
                }
            } else {
                emit("ERROR: Failed to update dependencies: ${result.error}")
                
                // Fall back to Java implementation
                emit("Falling back to Java implementation")
                gradleBuildManager.updateDependencies(filePath).collect { message ->
                    emit(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating dependencies with native implementation", e)
            emit("ERROR: Native implementation failed, falling back to Java implementation")
            
            // Fall back to Java implementation
            gradleBuildManager.updateDependencies(filePath).collect { message ->
                emit(message)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Fix common issues in a Gradle build file
     */
    suspend fun fixCommonIssues(filePath: String): Flow<String> = flow {
        emit("Scanning for common build issues...")
        
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            gradleBuildManager.fixCommonIssues(filePath).collect { message ->
                emit(message)
            }
            return@flow
        }
        
        try {
            val resultJson = nativeFixCommonIssues(filePath)
            val result = parseOptimizationResult(resultJson)
            
            if (result.success) {
                if (result.issuesFixed.isEmpty()) {
                    emit("No common issues found")
                } else {
                    emit("Fixed ${result.issuesFixed.size} common issues")
                    
                    // Report fixes
                    result.issuesFixed.forEach { issue ->
                        emit("Fixed: $issue")
                    }
                }
            } else {
                emit("ERROR: Failed to fix common issues: ${result.error}")
                
                // Fall back to Java implementation
                emit("Falling back to Java implementation")
                gradleBuildManager.fixCommonIssues(filePath).collect { message ->
                    emit(message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fixing common issues with native implementation", e)
            emit("ERROR: Native implementation failed, falling back to Java implementation")
            
            // Fall back to Java implementation
            gradleBuildManager.fixCommonIssues(filePath).collect { message ->
                emit(message)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Generate a build report for a project
     */
    suspend fun generateBuildReport(projectPath: String): Result<com.anyoneide.app.core.GradleBuildReport> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            return@withContext gradleBuildManager.generateBuildReport(projectPath)
        }
        
        try {
            val reportJson = nativeGenerateBuildReport(projectPath)
            val report = parseGradleBuildReport(reportJson)
            // Convert to the expected type
            val convertedReport = convertToGradleBuildReport(report)
            Result.success(convertedReport)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating build report with native implementation", e)
            // Fall back to Java implementation
            gradleBuildManager.generateBuildReport(projectPath)
        }
    }
    
    /**
     * Parse Gradle build analysis from JSON
     */
    private fun parseGradleBuildAnalysis(json: String): GradleBuildAnalysis {
        val jsonObject = JSONObject(json)
        
        val dependencies = mutableListOf<GradleDependency>()
        val dependenciesArray = jsonObject.getJSONArray("dependencies")
        for (i in 0 until dependenciesArray.length()) {
            val dependency = dependenciesArray.getJSONObject(i)
            dependencies.add(
                GradleDependency(
                    configuration = dependency.getString("configuration"),
                    group = dependency.getString("group"),
                    name = dependency.getString("name"),
                    version = dependency.getString("version")
                )
            )
        }
        
        val plugins = mutableListOf<String>()
        val pluginsArray = jsonObject.getJSONArray("plugins")
        for (i in 0 until pluginsArray.length()) {
            plugins.add(pluginsArray.getString(i))
        }
        
        val issues = mutableListOf<GradleBuildIssue>()
        val issuesArray = jsonObject.getJSONArray("issues")
        for (i in 0 until issuesArray.length()) {
            val issue = issuesArray.getJSONObject(i)
            
            val issueType = when (issue.getString("issue_type")) {
                "MissingRepository" -> GradleBuildIssueType.MISSING_REPOSITORY
                "DeprecatedConfiguration" -> GradleBuildIssueType.DEPRECATED_CONFIGURATION
                "OutdatedDependency" -> GradleBuildIssueType.OUTDATED_DEPENDENCY
                "MissingJavaCompatibility" -> GradleBuildIssueType.MISSING_JAVA_COMPATIBILITY
                "MissingKotlinJvmTarget" -> GradleBuildIssueType.MISSING_KOTLIN_JVM_TARGET
                "MissingProguardConfig" -> GradleBuildIssueType.MISSING_PROGUARD_CONFIG
                "MissingBuildFeatures" -> GradleBuildIssueType.MISSING_BUILD_FEATURES
                else -> GradleBuildIssueType.MISSING_REPOSITORY
            }
            
            val severity = when (issue.getString("severity")) {
                "Error" -> IssueSeverity.ERROR
                "Warning" -> IssueSeverity.WARNING
                "Info" -> IssueSeverity.INFO
                else -> IssueSeverity.INFO
            }
            
            issues.add(
                GradleBuildIssue(
                    type = issueType,
                    description = issue.getString("description"),
                    severity = severity,
                    suggestion = issue.getString("suggestion")
                )
            )
        }
        
        return GradleBuildAnalysis(
            filePath = jsonObject.getString("file_path"),
            dependencies = dependencies,
            plugins = plugins,
            issues = issues,
            compileSdkVersion = if (jsonObject.has("compile_sdk_version") && !jsonObject.isNull("compile_sdk_version")) 
                jsonObject.getString("compile_sdk_version") else null,
            minSdkVersion = if (jsonObject.has("min_sdk_version") && !jsonObject.isNull("min_sdk_version")) 
                jsonObject.getString("min_sdk_version") else null,
            targetSdkVersion = if (jsonObject.has("target_sdk_version") && !jsonObject.isNull("target_sdk_version")) 
                jsonObject.getString("target_sdk_version") else null,
            buildToolsVersion = if (jsonObject.has("build_tools_version") && !jsonObject.isNull("build_tools_version")) 
                jsonObject.getString("build_tools_version") else null
        )
    }
    
    /**
     * Parse optimization result from JSON
     */
    private fun parseOptimizationResult(json: String): OptimizationResult {
        val jsonObject = JSONObject(json)
        
        val issuesFixed = mutableListOf<String>()
        val issuesFixedArray = jsonObject.getJSONArray("issues_fixed")
        for (i in 0 until issuesFixedArray.length()) {
            issuesFixed.add(issuesFixedArray.getString(i))
        }
        
        val dependenciesUpdated = mutableListOf<DependencyUpdate>()
        val dependenciesUpdatedArray = jsonObject.getJSONArray("dependencies_updated")
        for (i in 0 until dependenciesUpdatedArray.length()) {
            val update = dependenciesUpdatedArray.getJSONObject(i)
            dependenciesUpdated.add(
                DependencyUpdate(
                    group = update.getString("group"),
                    name = update.getString("name"),
                    oldVersion = update.getString("old_version"),
                    newVersion = update.getString("new_version")
                )
            )
        }
        
        return OptimizationResult(
            filePath = jsonObject.getString("file_path"),
            issuesFixed = issuesFixed,
            dependenciesUpdated = dependenciesUpdated,
            success = jsonObject.getBoolean("success"),
            error = if (jsonObject.has("error") && !jsonObject.isNull("error")) 
                jsonObject.getString("error") else null
        )
    }
    
    /**
     * Parse Gradle build report from JSON
     */
    private fun parseGradleBuildReport(json: String): GradleBuildReport {
        val jsonObject = JSONObject(json)
        
        val modules = mutableListOf<GradleModuleReport>()
        val modulesArray = jsonObject.getJSONArray("modules")
        for (i in 0 until modulesArray.length()) {
            val module = modulesArray.getJSONObject(i)
            
            val dependencies = mutableListOf<GradleDependency>()
            val dependenciesArray = module.getJSONArray("dependencies")
            for (j in 0 until dependenciesArray.length()) {
                val dependency = dependenciesArray.getJSONObject(j)
                dependencies.add(
                    GradleDependency(
                        configuration = dependency.getString("configuration"),
                        group = dependency.getString("group"),
                        name = dependency.getString("name"),
                        version = dependency.getString("version")
                    )
                )
            }
            
            val issues = mutableListOf<GradleBuildIssue>()
            val issuesArray = module.getJSONArray("issues")
            for (j in 0 until issuesArray.length()) {
                val issue = issuesArray.getJSONObject(j)
                
                val issueType = when (issue.getString("issue_type")) {
                    "MissingRepository" -> GradleBuildIssueType.MISSING_REPOSITORY
                    "DeprecatedConfiguration" -> GradleBuildIssueType.DEPRECATED_CONFIGURATION
                    "OutdatedDependency" -> GradleBuildIssueType.OUTDATED_DEPENDENCY
                    "MissingJavaCompatibility" -> GradleBuildIssueType.MISSING_JAVA_COMPATIBILITY
                    "MissingKotlinJvmTarget" -> GradleBuildIssueType.MISSING_KOTLIN_JVM_TARGET
                    "MissingProguardConfig" -> GradleBuildIssueType.MISSING_PROGUARD_CONFIG
                    "MissingBuildFeatures" -> GradleBuildIssueType.MISSING_BUILD_FEATURES
                    else -> GradleBuildIssueType.MISSING_REPOSITORY
                }
                
                val severity = when (issue.getString("severity")) {
                    "Error" -> IssueSeverity.ERROR
                    "Warning" -> IssueSeverity.WARNING
                    "Info" -> IssueSeverity.INFO
                    else -> IssueSeverity.INFO
                }
                
                issues.add(
                    GradleBuildIssue(
                        type = issueType,
                        description = issue.getString("description"),
                        severity = severity,
                        suggestion = issue.getString("suggestion")
                    )
                )
            }
            
            modules.add(
                GradleModuleReport(
                    moduleName = module.getString("module_name"),
                    buildFilePath = module.getString("build_file_path"),
                    dependencies = dependencies,
                    issues = issues,
                    buildToolsVersion = module.getString("build_tools_version"),
                    compileSdkVersion = module.getString("compile_sdk_version"),
                    minSdkVersion = module.getString("min_sdk_version"),
                    targetSdkVersion = module.getString("target_sdk_version")
                )
            )
        }
        
        return GradleBuildReport(
            projectPath = jsonObject.getString("project_path"),
            modules = modules,
            totalDependencies = jsonObject.getInt("total_dependencies"),
            totalIssues = jsonObject.getInt("total_issues"),
            gradleVersion = if (jsonObject.has("gradle_version") && !jsonObject.isNull("gradle_version")) 
                jsonObject.getString("gradle_version") else null,
            kotlinVersion = if (jsonObject.has("kotlin_version") && !jsonObject.isNull("kotlin_version")) 
                jsonObject.getString("kotlin_version") else null,
            agpVersion = if (jsonObject.has("agp_version") && !jsonObject.isNull("agp_version")) 
                jsonObject.getString("agp_version") else null
        )
    }
    
    /**
     * Convert internal GradleBuildAnalysis to the expected type
     */
    private fun convertToGradleBuildAnalysis(analysis: GradleBuildAnalysis): com.anyoneide.app.core.GradleBuildAnalysis {
        val dependencies = analysis.dependencies.map { dep ->
            com.anyoneide.app.core.GradleDependency(
                configuration = dep.configuration,
                group = dep.group,
                name = dep.name,
                version = dep.version
            )
        }
        
        val issues = analysis.issues.map { issue ->
            val issueType = when (issue.type) {
                GradleBuildIssueType.MISSING_REPOSITORY -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_REPOSITORY
                GradleBuildIssueType.DEPRECATED_CONFIGURATION -> com.anyoneide.app.core.GradleBuildIssueType.DEPRECATED_CONFIGURATION
                GradleBuildIssueType.OUTDATED_DEPENDENCY -> com.anyoneide.app.core.GradleBuildIssueType.OUTDATED_DEPENDENCY
                GradleBuildIssueType.MISSING_JAVA_COMPATIBILITY -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_JAVA_COMPATIBILITY
                GradleBuildIssueType.MISSING_KOTLIN_JVM_TARGET -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_KOTLIN_JVM_TARGET
                GradleBuildIssueType.MISSING_PROGUARD_CONFIG -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_PROGUARD_CONFIG
                GradleBuildIssueType.MISSING_BUILD_FEATURES -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_BUILD_FEATURES
            }
            
            val severity = when (issue.severity) {
                IssueSeverity.ERROR -> com.anyoneide.app.core.IssueSeverity.ERROR
                IssueSeverity.WARNING -> com.anyoneide.app.core.IssueSeverity.WARNING
                IssueSeverity.INFO -> com.anyoneide.app.core.IssueSeverity.INFO
            }
            
            com.anyoneide.app.core.GradleBuildIssue(
                type = issueType,
                description = issue.description,
                severity = severity,
                suggestion = issue.suggestion
            )
        }
        
        return com.anyoneide.app.core.GradleBuildAnalysis(
            filePath = analysis.filePath,
            dependencies = dependencies,
            issues = issues,
            compileSdkVersion = analysis.compileSdkVersion,
            minSdkVersion = analysis.minSdkVersion,
            targetSdkVersion = analysis.targetSdkVersion,
            buildToolsVersion = analysis.buildToolsVersion
        )
    }
    
    /**
     * Convert internal GradleBuildReport to the expected type
     */
    private fun convertToGradleBuildReport(report: GradleBuildReport): com.anyoneide.app.core.GradleBuildReport {
        val modules = report.modules.map { module ->
            val dependencies = module.dependencies.map { dep ->
                com.anyoneide.app.core.GradleDependency(
                    configuration = dep.configuration,
                    group = dep.group,
                    name = dep.name,
                    version = dep.version
                )
            }
            
            val issues = module.issues.map { issue ->
                val issueType = when (issue.type) {
                    GradleBuildIssueType.MISSING_REPOSITORY -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_REPOSITORY
                    GradleBuildIssueType.DEPRECATED_CONFIGURATION -> com.anyoneide.app.core.GradleBuildIssueType.DEPRECATED_CONFIGURATION
                    GradleBuildIssueType.OUTDATED_DEPENDENCY -> com.anyoneide.app.core.GradleBuildIssueType.OUTDATED_DEPENDENCY
                    GradleBuildIssueType.MISSING_JAVA_COMPATIBILITY -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_JAVA_COMPATIBILITY
                    GradleBuildIssueType.MISSING_KOTLIN_JVM_TARGET -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_KOTLIN_JVM_TARGET
                    GradleBuildIssueType.MISSING_PROGUARD_CONFIG -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_PROGUARD_CONFIG
                    GradleBuildIssueType.MISSING_BUILD_FEATURES -> com.anyoneide.app.core.GradleBuildIssueType.MISSING_BUILD_FEATURES
                }
                
                val severity = when (issue.severity) {
                    IssueSeverity.ERROR -> com.anyoneide.app.core.IssueSeverity.ERROR
                    IssueSeverity.WARNING -> com.anyoneide.app.core.IssueSeverity.WARNING
                    IssueSeverity.INFO -> com.anyoneide.app.core.IssueSeverity.INFO
                }
                
                com.anyoneide.app.core.GradleBuildIssue(
                    type = issueType,
                    description = issue.description,
                    severity = severity,
                    suggestion = issue.suggestion
                )
            }
            
            com.anyoneide.app.core.GradleModuleReport(
                moduleName = module.moduleName,
                buildFilePath = module.buildFilePath,
                dependencies = dependencies,
                issues = issues,
                buildToolsVersion = module.buildToolsVersion,
                compileSdkVersion = module.compileSdkVersion,
                minSdkVersion = module.minSdkVersion,
                targetSdkVersion = module.targetSdkVersion
            )
        }
        
        return com.anyoneide.app.core.GradleBuildReport(
            projectPath = report.projectPath,
            modules = modules,
            totalDependencies = report.totalDependencies,
            totalIssues = report.totalIssues,
            gradleVersion = report.gradleVersion,
            kotlinVersion = report.kotlinVersion,
            agpVersion = report.agpVersion
        )
    }
    
    /**
     * Data classes
     */
    data class GradleBuildAnalysis(
        val filePath: String,
        val dependencies: List<GradleDependency>,
        val plugins: List<String>,
        val issues: List<GradleBuildIssue>,
        val compileSdkVersion: String?,
        val minSdkVersion: String?,
        val targetSdkVersion: String?,
        val buildToolsVersion: String?
    )
    
    data class GradleDependency(
        val configuration: String,
        val group: String,
        val name: String,
        val version: String
    )
    
    enum class GradleBuildIssueType {
        MISSING_REPOSITORY,
        DEPRECATED_CONFIGURATION,
        OUTDATED_DEPENDENCY,
        MISSING_JAVA_COMPATIBILITY,
        MISSING_KOTLIN_JVM_TARGET,
        MISSING_PROGUARD_CONFIG,
        MISSING_BUILD_FEATURES
    }
    
    enum class IssueSeverity {
        ERROR,
        WARNING,
        INFO
    }
    
    data class GradleBuildIssue(
        val type: GradleBuildIssueType,
        val description: String,
        val severity: IssueSeverity,
        val suggestion: String
    )
    
    data class OptimizationResult(
        val filePath: String,
        val issuesFixed: List<String>,
        val dependenciesUpdated: List<DependencyUpdate>,
        val success: Boolean,
        val error: String?
    )
    
    data class DependencyUpdate(
        val group: String,
        val name: String,
        val oldVersion: String,
        val newVersion: String
    )
    
    data class GradleBuildReport(
        val projectPath: String,
        val modules: List<GradleModuleReport>,
        val totalDependencies: Int,
        val totalIssues: Int,
        val gradleVersion: String?,
        val kotlinVersion: String?,
        val agpVersion: String?
    )
    
    data class GradleModuleReport(
        val moduleName: String,
        val buildFilePath: String,
        val dependencies: List<GradleDependency>,
        val issues: List<GradleBuildIssue>,
        val buildToolsVersion: String,
        val compileSdkVersion: String,
        val minSdkVersion: String,
        val targetSdkVersion: String
    )
}