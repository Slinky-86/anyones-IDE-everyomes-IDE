@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class GradleBuildManager(context: Context) {
    
    private val fileManager = FileManager(context)
    private val mavenCentralUrl = "https://repo1.maven.org/maven2"
    private val googleMavenUrl = "https://maven.google.com/web/index.html"
    
    suspend fun analyzeBuildFile(buildFilePath: String): Result<GradleBuildAnalysis> = withContext(Dispatchers.IO) {
        try {
            val result = fileManager.readFile(buildFilePath)
            result.fold(
                onSuccess = { content ->
                    val analysis = performBuildAnalysis(content, buildFilePath)
                    Result.success(analysis)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to analyze build file", e))
        }
    }
    
    suspend fun optimizeBuildFile(buildFilePath: String): Flow<String> = flow {
        emit("Analyzing build file: $buildFilePath")
        
        try {
            val analysisResult = analyzeBuildFile(buildFilePath)
            analysisResult.onSuccess { analysis ->
                emit("Found ${analysis.issues.size} potential issues")
                
                if (analysis.issues.isNotEmpty()) {
                    emit("Applying optimizations...")
                    val optimizedContent = applyOptimizations(analysis)
                    
                    // Backup original file
                    val backupPath = "$buildFilePath.backup"
                    fileManager.copyFile(buildFilePath, backupPath)
                    emit("Created backup: $backupPath")
                    
                    // Write optimized content
                    fileManager.writeFile(buildFilePath, optimizedContent)
                    emit("Build file optimized successfully")
                    
                    // Report changes
                    analysis.issues.forEach { issue ->
                        emit("Fixed: ${issue.description}")
                    }
                } else {
                    emit("Build file is already optimized")
                }
            }
            analysisResult.onFailure { error ->
                emit("ERROR: Failed to analyze build file: ${error.message}")
            }
            
        } catch (e: Exception) {
            emit("ERROR: Failed to optimize build file: ${e.message}")
        }
    }
    
    suspend fun updateDependencies(buildFilePath: String): Flow<String> = flow {
        emit("Updating dependencies in: $buildFilePath")
        
        val dependencyUpdates = mutableMapOf<String, Pair<String, String>>()
        
        try {
            val result = fileManager.readFile(buildFilePath)
            result.onSuccess { content ->
                // Extract current dependencies
                val dependencies = extractDependencies(content)
                emit("Found ${dependencies.size} dependencies to check")
                
                // Check for updates for each dependency
                dependencies.forEach { dependency ->
                    val dependencyKey = "${dependency.group}:${dependency.name}"
                    emit("Checking updates for $dependencyKey:${dependency.version}")
                    
                    try {
                        val latestVersion = getLatestVersion(dependency.group, dependency.name)
                        if (latestVersion != null && latestVersion != dependency.version) {
                            dependencyUpdates[dependencyKey] = Pair(dependency.version, latestVersion)
                            emit("Update available: $dependencyKey ${dependency.version} -> $latestVersion")
                        } else {
                            emit("$dependencyKey is already up to date")
                        }
                    } catch (e: Exception) {
                        emit("Failed to check updates for $dependencyKey: ${e.message}")
                    }
                }
                
                // Apply updates
                val updatedContent = if (dependencyUpdates.isNotEmpty()) {
                    applyDependencyUpdates(content, dependencyUpdates)
                } else {
                    content
                }
                
                if (updatedContent != content) {
                    fileManager.writeFile(buildFilePath, updatedContent)
                    emit("Updated ${dependencyUpdates.size} dependencies successfully")
                } else {
                    emit("All dependencies are already up to date")
                }
            }
            result.onFailure { error ->
                emit("ERROR: Failed to read build file: ${error.message}")
            }
            
        } catch (e: Exception) {
            emit("ERROR: Failed to update dependencies: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
    
    private suspend fun getLatestVersion(group: String, name: String): String? = withContext(Dispatchers.IO) {
        try {
            // Try Google Maven first for Android dependencies
            if (group.startsWith("androidx") || group.startsWith("com.google.android") || group.startsWith("com.android")) {
                val googleVersion = getLatestVersionFromGoogle(group, name)
                if (googleVersion != null) {
                    return@withContext googleVersion
                }
            }
            
            // Fall back to Maven Central
            val groupPath = group.replace('.', '/')
            val metadataUrl = "$mavenCentralUrl/$groupPath/$name/maven-metadata.xml"
            
            val connection = URL(metadataUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val metadataContent = connection.inputStream.bufferedReader().use { it.readText() }
                val versionPattern = "<version>(.*?)</version>"
                val versionRegex = Pattern.compile(versionPattern)
                val matcher = versionRegex.matcher(metadataContent)
                
                val versions = mutableListOf<String>()
                while (matcher.find()) {
                    versions.add(matcher.group(1) ?: "")
                }
                
                // Return the latest version (assuming versions are sorted in the metadata)
                return@withContext versions.lastOrNull()
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun getLatestVersionFromGoogle(group: String, name: String): String? = withContext(Dispatchers.IO) {
        try {
            // This is a simplified approach - in a real implementation, you would parse the Google Maven repository properly
            // For now, we'll use our known latest versions for common Android libraries
            val androidLatestVersions = mapOf(
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
                "com.google.android.material:material" to "1.11.0"
            )
            
            return@withContext androidLatestVersions["$group:$name"]
        } catch (e: Exception) {
            null
        }
    }
    
    private fun applyDependencyUpdates(content: String, updates: Map<String, Pair<String, String>>): String {
        var updatedContent = content
        
        updates.forEach { (dependency, versionPair) ->
            val (oldVersion, newVersion) = versionPair
            val parts = dependency.split(":")
            if (parts.size == 2) {
                val group = parts[0]
                val name = parts[1]
                
                // Handle different formats of dependency declarations
                val patterns = listOf(
                    "$group:$name:\"$oldVersion\"",
                    "$group:$name:'$oldVersion'",
                    "$group:$name:$oldVersion"
                )
                
                patterns.forEach { pattern ->
                    if (updatedContent.contains(pattern)) {
                        val replacement = when {
                            pattern.contains("\"") -> "$group:$name:\"$newVersion\""
                            pattern.contains("'") -> "$group:$name:'$newVersion'"
                            else -> "$group:$name:$newVersion"
                        }
                        updatedContent = updatedContent.replace(pattern, replacement)
                    }
                }
            }
        }
        
        return updatedContent
    }
    
    suspend fun addDependency(
        buildFilePath: String,
        dependency: String,
        configuration: String = "implementation"
    ): Flow<String> = flow {
        emit("Adding dependency: $dependency")
        
        try {
            val result = fileManager.readFile(buildFilePath)
            result.onSuccess { content ->
                val updatedContent = insertDependency(content, dependency, configuration)
                fileManager.writeFile(buildFilePath, updatedContent)
                emit("Dependency added successfully")
            }
            result.onFailure { error ->
                emit("ERROR: Failed to read build file: ${error.message}")
            }
            
        } catch (e: Exception) {
            emit("ERROR: Failed to add dependency: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun removeDependency(buildFilePath: String, dependency: String): Flow<String> = flow {
        emit("Removing dependency: $dependency")
        
        try {
            val result = fileManager.readFile(buildFilePath)
            result.onSuccess { content ->
                val updatedContent = removeDependencyFromContent(content, dependency)
                
                if (updatedContent != content) {
                    fileManager.writeFile(buildFilePath, updatedContent)
                    emit("Dependency removed successfully")
                } else {
                    emit("Dependency not found in build file")
                }
            }
            result.onFailure { error ->
                emit("ERROR: Failed to read build file: ${error.message}")
            }
            
        } catch (e: Exception) {
            emit("ERROR: Failed to remove dependency: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun fixCommonIssues(buildFilePath: String): Flow<String> = flow {
        emit("Scanning for common build issues...")
        
        try {
            val result = fileManager.readFile(buildFilePath)
            result.onSuccess { content ->
                var fixedContent = content
                var issuesFixed = 0
                
                // Fix missing repositories
                if (!content.contains("google()") || !content.contains("mavenCentral()")) {
                    fixedContent = ensureRepositories(fixedContent)
                    emit("Fixed: Added missing repositories")
                    issuesFixed++
                }
                
                // Fix deprecated configurations
                if (content.contains("compile ") || content.contains("testCompile ")) {
                    fixedContent = fixDeprecatedConfigurations(fixedContent)
                    emit("Fixed: Updated deprecated dependency configurations")
                    issuesFixed++
                }
                
                // Fix missing Java compatibility
                if (!content.contains("sourceCompatibility") || !content.contains("targetCompatibility")) {
                    fixedContent = ensureJavaCompatibility(fixedContent)
                    emit("Fixed: Added Java compatibility settings")
                    issuesFixed++
                }
                
                // Fix missing Kotlin JVM target
                if (content.contains("kotlin") && !content.contains("jvmTarget")) {
                    fixedContent = ensureKotlinJvmTarget(fixedContent)
                    emit("Fixed: Added Kotlin JVM target")
                    issuesFixed++
                }
                
                // Fix missing ProGuard configuration
                if (content.contains("minifyEnabled true") && !content.contains("proguardFiles")) {
                    fixedContent = ensureProguardConfig(fixedContent)
                    emit("Fixed: Added ProGuard configuration")
                    issuesFixed++
                }
                
                // Fix missing build features
                if (content.contains("compose") && !content.contains("buildFeatures")) {
                    fixedContent = ensureBuildFeatures(fixedContent)
                    emit("Fixed: Added missing build features")
                    issuesFixed++
                }
                
                if (issuesFixed > 0) {
                    fileManager.writeFile(buildFilePath, fixedContent)
                    emit("Fixed $issuesFixed common issues")
                } else {
                    emit("No common issues found")
                }
            }
            result.onFailure { error ->
                emit("ERROR: Failed to read build file: ${error.message}")
            }
            
        } catch (e: Exception) {
            emit("ERROR: Failed to fix common issues: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun generateBuildReport(projectPath: String): Result<GradleBuildReport> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(projectPath)
            val buildFiles = findBuildFiles(projectDir)
            
            val moduleReports = buildFiles.mapNotNull { buildFile ->
                val analysisResult = analyzeBuildFile(buildFile.absolutePath)
                analysisResult.getOrNull()?.let { analysis ->
                    GradleModuleReport(
                        moduleName = getModuleName(buildFile),
                        buildFilePath = buildFile.absolutePath,
                        dependencies = analysis.dependencies,
                        issues = analysis.issues,
                        buildToolsVersion = analysis.buildToolsVersion ?: "Unknown",
                        compileSdkVersion = analysis.compileSdkVersion ?: "Unknown",
                        minSdkVersion = analysis.minSdkVersion ?: "Unknown",
                        targetSdkVersion = analysis.targetSdkVersion ?: "Unknown"
                    )
                }
            }
            
            val report = GradleBuildReport(
                projectPath = projectPath,
                modules = moduleReports,
                totalDependencies = moduleReports.sumOf { it.dependencies.size },
                totalIssues = moduleReports.sumOf { it.issues.size },
                gradleVersion = detectGradleVersion(projectDir),
                kotlinVersion = detectKotlinVersion(moduleReports),
                agpVersion = detectAGPVersion(moduleReports)
            )
            
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate build report", e))
        }
    }
    
    private fun performBuildAnalysis(content: String, filePath: String): GradleBuildAnalysis {
        val issues = mutableListOf<GradleBuildIssue>()
        val dependencies = extractDependencies(content)
        
        // Check for common issues
        if (!content.contains("google()")) {
            issues.add(GradleBuildIssue(
                type = GradleBuildIssueType.MISSING_REPOSITORY,
                description = "Missing Google repository",
                severity = IssueSeverity.WARNING,
                suggestion = "Add google() to repositories block"
            ))
        }
        
        if (!content.contains("mavenCentral()")) {
            issues.add(GradleBuildIssue(
                type = GradleBuildIssueType.MISSING_REPOSITORY,
                description = "Missing Maven Central repository",
                severity = IssueSeverity.WARNING,
                suggestion = "Add mavenCentral() to repositories block"
            ))
        }
        
        if (content.contains("compile ")) {
            issues.add(GradleBuildIssue(
                type = GradleBuildIssueType.DEPRECATED_CONFIGURATION,
                description = "Using deprecated 'compile' configuration",
                severity = IssueSeverity.ERROR,
                suggestion = "Replace 'compile' with 'implementation'"
            ))
        }
        
        if (content.contains("testCompile ")) {
            issues.add(GradleBuildIssue(
                type = GradleBuildIssueType.DEPRECATED_CONFIGURATION,
                description = "Using deprecated 'testCompile' configuration",
                severity = IssueSeverity.ERROR,
                suggestion = "Replace 'testCompile' with 'testImplementation'"
            ))
        }
        
        // Check for outdated dependencies
        dependencies.forEach { dep ->
            if (isOutdatedDependency(dep)) {
                issues.add(GradleBuildIssue(
                    type = GradleBuildIssueType.OUTDATED_DEPENDENCY,
                    description = "Outdated dependency: ${dep.group}:${dep.name}:${dep.version}",
                    severity = IssueSeverity.INFO,
                    suggestion = "Consider updating to latest version"
                ))
            }
        }
        
        // Extract version information
        val compileSdkVersion = extractCompileSdkVersion(content)
        val minSdkVersion = extractMinSdkVersion(content)
        val targetSdkVersion = extractTargetSdkVersion(content)
        val buildToolsVersion = extractBuildToolsVersion(content)
        
        return GradleBuildAnalysis(
            filePath = filePath,
            dependencies = dependencies,
            issues = issues,
            compileSdkVersion = compileSdkVersion,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion,
            buildToolsVersion = buildToolsVersion
        )
    }
    
    private suspend fun applyOptimizations(analysis: GradleBuildAnalysis): String = withContext(Dispatchers.IO) {
        val result = fileManager.readFile(analysis.filePath)
        var content = result.getOrElse { return@withContext "" }
        
        analysis.issues.forEach { issue ->
            content = when (issue.type) {
                GradleBuildIssueType.MISSING_REPOSITORY -> ensureRepositories(content)
                GradleBuildIssueType.DEPRECATED_CONFIGURATION -> fixDeprecatedConfigurations(content)
                GradleBuildIssueType.OUTDATED_DEPENDENCY -> updateDependencyVersions(content)
                GradleBuildIssueType.MISSING_JAVA_COMPATIBILITY -> ensureJavaCompatibility(content)
                GradleBuildIssueType.MISSING_KOTLIN_JVM_TARGET -> ensureKotlinJvmTarget(content)
                GradleBuildIssueType.MISSING_PROGUARD_CONFIG -> ensureProguardConfig(content)
                GradleBuildIssueType.MISSING_BUILD_FEATURES -> ensureBuildFeatures(content)
            }
        }
        
        return@withContext content
    }
    
    private fun ensureRepositories(content: String): String {
        if (content.contains("repositories {")) {
            // Add missing repositories to existing block
            var updatedContent = content
            
            if (!content.contains("google()")) {
                updatedContent = updatedContent.replace(
                    "repositories {",
                    "repositories {\n        google()"
                )
            }
            
            if (!content.contains("mavenCentral()")) {
                updatedContent = updatedContent.replace(
                    "repositories {",
                    "repositories {\n        mavenCentral()"
                )
            }
            
            return updatedContent
        } else {
            // Add repositories block
            val repositoriesBlock = """
                repositories {
                    google()
                    mavenCentral()
                }
                
            """.trimIndent()
            
            return if (content.contains("android {")) {
                content.replace("android {", "$repositoriesBlock\nandroid {")
            } else {
                "$repositoriesBlock\n$content"
            }
        }
    }
    
    private fun fixDeprecatedConfigurations(content: String): String {
        return content
            .replace(Regex("\\bcompile\\s+"), "implementation ")
            .replace(Regex("\\btestCompile\\s+"), "testImplementation ")
            .replace(Regex("\\bandroidTestCompile\\s+"), "androidTestImplementation ")
    }
    
    private fun ensureJavaCompatibility(content: String): String {
        if (content.contains("compileOptions {")) {
            return content
        }
        
        val javaCompatibilityBlock = """
            compileOptions {
                sourceCompatibility JavaVersion.VERSION_1_8
                targetCompatibility JavaVersion.VERSION_1_8
            }
            
        """.trimIndent()
        
        return if (content.contains("android {")) {
            content.replace(
                "android {",
                "android {\n    $javaCompatibilityBlock"
            )
        } else {
            content
        }
    }
    
    private fun ensureKotlinJvmTarget(content: String): String {
        if (content.contains("kotlinOptions {")) {
            return content
        }
        
        val kotlinOptionsBlock = """
            kotlinOptions {
                jvmTarget = '1.8'
            }
            
        """.trimIndent()
        
        return if (content.contains("compileOptions {")) {
            content.replace(
                "compileOptions {",
                "$kotlinOptionsBlock\n    compileOptions {"
            )
        } else if (content.contains("android {")) {
            content.replace(
                "android {",
                "android {\n    $kotlinOptionsBlock"
            )
        } else {
            content
        }
    }
    
    private fun ensureProguardConfig(content: String): String {
        if (content.contains("proguardFiles")) {
            return content
        }
        
        return content.replace(
            "minifyEnabled true",
            """minifyEnabled true
                proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'"""
        )
    }
    
    private fun ensureBuildFeatures(content: String): String {
        if (content.contains("buildFeatures {")) {
            return content
        }
        
        val buildFeaturesBlock = if (content.contains("compose")) {
            """
                buildFeatures {
                    compose true
                }
                
            """.trimIndent()
        } else {
            """
                buildFeatures {
                    viewBinding true
                }
                
            """.trimIndent()
        }
        
        return if (content.contains("compileOptions {")) {
            content.replace(
                "compileOptions {",
                "$buildFeaturesBlock\n    compileOptions {"
            )
        } else if (content.contains("android {")) {
            content.replace(
                "android {",
                "android {\n    $buildFeaturesBlock"
        )
        } else {
            content
        }
    }
    
    private fun updateDependencyVersions(content: String): String {
        var updatedContent = content
        var updated = false
        
        // Extract all dependencies
        val dependencies = extractDependencies(content)
        
        // For each dependency, try to find a newer version
        dependencies.forEach { dependency ->
            val dependencyKey = "${dependency.group}:${dependency.name}"
            
            // Check if we have a known latest version
            val knownLatestVersions = mapOf(
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
            
            val latestVersion = knownLatestVersions[dependencyKey]
            if (latestVersion != null && dependency.version != latestVersion) {
                // Different formats of dependency declarations
                val patterns = listOf(
                    "$dependencyKey:\"${dependency.version}\"",
                    "$dependencyKey:'${dependency.version}'",
                    "$dependencyKey:${dependency.version}"
                )
                
                patterns.forEach { pattern ->
                    if (updatedContent.contains(pattern)) {
                        val replacement = when {
                            pattern.contains("\"") -> "$dependencyKey:\"$latestVersion\""
                            pattern.contains("'") -> "$dependencyKey:'$latestVersion'"
                            else -> "$dependencyKey:$latestVersion"
                        }
                        updatedContent = updatedContent.replace(pattern, replacement)
                        updated = true
                    }
                }
            }
        }
        return if (updated) updatedContent else content
    }
    
    private fun insertDependency(content: String, dependency: String, configuration: String): String {
        val dependenciesPattern = Pattern.compile("dependencies\\s*\\{")
        val matcher = dependenciesPattern.matcher(content)
        
        return if (matcher.find()) {
            val insertPosition = matcher.end()
            val beforeDependencies = content.substring(0, insertPosition)
            val afterDependencies = content.substring(insertPosition)
            
            "$beforeDependencies\n    $configuration '$dependency'$afterDependencies"
        } else {
            // Add dependencies block if it doesn't exist
            val dependenciesBlock = """
                dependencies {
                    $configuration '$dependency'
                }
                
            """.trimIndent()
            
            "$content\n$dependenciesBlock"
        }
    }
    
    private fun removeDependencyFromContent(content: String, dependency: String): String {
        val lines = content.lines().toMutableList()
        val iterator = lines.iterator()
        
        while (iterator.hasNext()) {
            val line = iterator.next()
            if (line.contains(dependency)) {
                iterator.remove()
            }
        }
        
        return lines.joinToString("\n")
    }
    
    private fun extractDependencies(content: String): List<GradleDependency> {
        val dependencies = mutableListOf<GradleDependency>()
        val dependencyPattern = Pattern.compile("(\\w+)\\s+['\"]([^:'\"]+):([^:'\"]+):([^'\"]+)['\"]")
        val matcher = dependencyPattern.matcher(content)
        
        while (matcher.find()) {
            val configuration = matcher.group(1)
            val group = matcher.group(2)
            val name = matcher.group(3)
            val version = matcher.group(4)
            
            dependencies.add(GradleDependency(
                configuration = configuration,
                group = group,
                name = name,
                version = version
            ))
        }
        
        return dependencies
    }
    
    private fun extractCompileSdkVersion(content: String): String? {
        val pattern = Pattern.compile("compileSdk\\s+(\\d+)")
        val matcher = pattern.matcher(content)
        return if (matcher.find()) matcher.group(1) ?: "" else null
    }
    
    private fun extractMinSdkVersion(content: String): String? {
        val pattern = Pattern.compile("minSdk\\s+(\\d+)")
        val matcher = pattern.matcher(content)
        return if (matcher.find()) matcher.group(1) ?: "" else null
    }
    
    private fun extractTargetSdkVersion(content: String): String? {
        val pattern = Pattern.compile("targetSdk\\s+(\\d+)")
        val matcher = pattern.matcher(content)
        return if (matcher.find()) matcher.group(1) ?: "" else null
    }
    
    private fun extractBuildToolsVersion(content: String): String? {
        val pattern = Pattern.compile("buildToolsVersion\\s+['\"]([^'\"]+)['\"]")
        val matcher = pattern.matcher(content)
        return if (matcher.find()) matcher.group(1) ?: "" else null
    }
    
    private fun isOutdatedDependency(dependency: GradleDependency): Boolean {
        // Simple version comparison - in production, use proper semantic versioning
        val knownLatestVersions = mapOf(
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
            "io.coil-kt:coil-compose" to "2.5.0"
        )
        
        val dependencyKey = "${dependency.group}:${dependency.name}"
        val latestVersion = knownLatestVersions[dependencyKey]
        
        return latestVersion != null && dependency.version < latestVersion
    }
    
    private fun findBuildFiles(projectDir: File): List<File> {
        val buildFiles = mutableListOf<File>()
        
        projectDir.walkTopDown().forEach { file ->
            if (file.name == "build.gradle" || file.name == "build.gradle.kts") {
                buildFiles.add(file)
            }
        }
        
        return buildFiles
    }
    
    private fun getModuleName(buildFile: File): String {
        return buildFile.parentFile?.name ?: "root"
    }
    
    private fun detectGradleVersion(projectDir: File): String? {
        val gradleWrapperProps = File(projectDir, "gradle/wrapper/gradle-wrapper.properties")
        if (gradleWrapperProps.exists()) {
            try {
                val content = gradleWrapperProps.readText()
                val pattern = Pattern.compile("gradle-([\\d.]+)-")
                val matcher = pattern.matcher(content)
                if (matcher.find()) {
                    return matcher.group(1)
                }
            } catch (e: Exception) {
                // Ignore and return null
            }
        }
        return null
    }
    
    private fun detectKotlinVersion(modules: List<GradleModuleReport>): String? {
        modules.forEach { module ->
            module.dependencies.forEach { dep ->
                if (dep.group == "org.jetbrains.kotlin" && dep.name.startsWith("kotlin-")) {
                    return dep.version
                }
            }
        }
        return null
    }
    
    private fun detectAGPVersion(modules: List<GradleModuleReport>): String? {
        modules.forEach { module ->
            module.dependencies.forEach { dep ->
                if (dep.group == "com.android.tools.build" && dep.name == "gradle") {
                    return dep.version
                }
            }
        }
        return null
    }
}

// Extension function to compare version strings
private fun String.isNewerThan(other: String): Boolean {
    val thisParts = this.split(".")
    val otherParts = other.split(".")
    
    val maxLength = maxOf(thisParts.size, otherParts.size)
    
    for (i in 0 until maxLength) {
        val thisPart = if (i < thisParts.size) thisParts[i].toIntOrNull() ?: 0 else 0
        val otherPart = if (i < otherParts.size) otherParts[i].toIntOrNull() ?: 0 else 0
        
        if (thisPart > otherPart) return true
        if (thisPart < otherPart) return false
    }
    
    return false // Versions are equal
}

// Data classes for Gradle analysis
data class GradleBuildAnalysis(
    val filePath: String,
    val dependencies: List<GradleDependency>,
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

data class GradleBuildIssue(
    val type: GradleBuildIssueType,
    val description: String,
    val severity: IssueSeverity,
    val suggestion: String
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