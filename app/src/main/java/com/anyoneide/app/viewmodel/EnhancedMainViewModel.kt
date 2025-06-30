package com.anyoneide.app.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anyoneide.app.AnyoneIDEApplication
import com.anyoneide.app.core.*
import com.anyoneide.app.data.DataModule
import com.anyoneide.app.data.model.BookmarkedCommand
import com.anyoneide.app.data.repository.ActivityLogRepository
import com.anyoneide.app.data.repository.BookmarkedCommandRepository
import com.anyoneide.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EnhancedMainViewModel(application: AnyoneIDEApplication) : AndroidViewModel(application) {

    // Core managers
    private val fileManager = FileManager(application)
    private val projectManager = ProjectManager(application)
    private val buildManager = BuildManager(application)
    private val editorManager = EditorManager(application)
    private val gitIntegration = GitIntegration(application)
    private val terminalManager = TerminalManager(application)
    private val settingsManager = SettingsManager(application)
    private val languageSupport = LanguageSupport(application)
    private val themeManager = ThemeManager(application)
    private val geminiApiService = GeminiApiService(application)
    private val rustBuildManager = RustBuildManager(application)
    private val rustNativeBuildManager = RustNativeBuildManager(application)
    private val rustTerminalManager = RustTerminalManager(application)
    private val shizukuManager = ShizukuManager(application)
    private val gradleBuildManager = GradleBuildManager(application)
    private val gradleFileModifier = GradleFileModifier(application)
    private val pluginManager = PluginManager(application)
    
    // Repositories
    private val activityLogRepository = DataModule.provideActivityLogRepository(application)
    private val bookmarkedCommandRepository = DataModule.provideBookmarkedCommandRepository(application)
    
    // UI State
    private val _uiState = MutableStateFlow(IDEUiState())
    val uiState: StateFlow<IDEUiState> = _uiState.asStateFlow()
    
    // Root access flag
    var isRootAccessRequested = false
        private set
    
    init {
        // Load settings
        _uiState.update { it.copy(settings = settingsManager.settings.value) }
        
        // Load themes
        _uiState.update { it.copy(
            availableThemes = themeManager.availableThemes.value,
            currentTheme = themeManager.currentTheme.value
        ) }
        
        // Load languages
        _uiState.update { it.copy(
            availableLanguages = languageSupport.getAllLanguages()
        ) }
        
        // Load bookmarked commands
        viewModelScope.launch {
            bookmarkedCommandRepository.getBookmarkedCommands().onSuccess { commands ->
                _uiState.update { it.copy(bookmarkedCommands = commands) }
            }
        }
        
        // Check if native build system is available
        viewModelScope.launch {
            val isAvailable = rustNativeBuildManager.isNativeBuildSystemAvailable()
            val rustVersion = rustNativeBuildManager.getRustVersion()
            val buildSystemStatus = rustNativeBuildManager.getBuildSystemStatus()
            
            _uiState.update { it.copy(
                isNativeBuildSystemAvailable = isAvailable,
                rustVersion = rustVersion,
                nativeBuildSystemStatus = buildSystemStatus
            ) }
        }
        
        // Apply theme based on settings
        viewModelScope.launch {
            settingsManager.settings.collect { settings ->
                _uiState.update { it.copy(
                    settings = settings,
                    isDarkTheme = settings.isDarkTheme,
                    showLeftPanel = settings.showProjectExplorer,
                    showRightPanel = settings.showToolWindows,
                    showBottomPanel = settings.showBottomPanel
                ) }
            }
        }
    }
    
    // File Operations
    fun openFile(filePath: String) {
        viewModelScope.launch {
            fileManager.readFile(filePath).onSuccess { content ->
                val fileName = File(filePath).name
                val extension = fileManager.getFileExtension(filePath)
                val language = fileManager.getLanguageFromExtension(extension)
                
                val file = EditorFile(
                    path = filePath,
                    name = fileName,
                    content = content,
                    language = language,
                    isModified = false,
                    lineCount = content.lines().size
                )
                
                // Check if file is already open
                val openFiles = _uiState.value.openFiles.toMutableList()
                val existingIndex = openFiles.indexOfFirst { it.path == filePath }
                
                if (existingIndex >= 0) {
                    // Update existing file
                    openFiles[existingIndex] = file
                } else {
                    // Add new file
                    openFiles.add(file)
                }
                
                _uiState.update { it.copy(
                    openFiles = openFiles,
                    activeFile = file
                ) }
                
                // Highlight syntax
                highlightSyntax(file)
            }
        }
    }
    
    fun updateFileContent(filePath: String, content: String) {
        val openFiles = _uiState.value.openFiles.toMutableList()
        val index = openFiles.indexOfFirst { it.path == filePath }
        
        if (index >= 0) {
            val file = openFiles[index]
            val updatedFile = file.copy(
                content = content,
                isModified = true,
                lineCount = content.lines().size
            )
            openFiles[index] = updatedFile
            
            _uiState.update { it.copy(
                openFiles = openFiles,
                activeFile = if (it.activeFile?.path == filePath) updatedFile else it.activeFile
            ) }
            
            // Auto-save if enabled
            if (_uiState.value.settings.autoSave) {
                viewModelScope.launch {
                    saveFile(filePath)
                }
            }
            
            // Highlight syntax
            highlightSyntax(updatedFile)
        }
    }
    
    fun saveFile(filePath: String? = null) {
        val fileToSave = filePath?.let { path ->
            _uiState.value.openFiles.find { it.path == path }
        } ?: _uiState.value.activeFile
        
        if (fileToSave != null && fileToSave.isModified) {
            viewModelScope.launch {
                fileManager.writeFile(fileToSave.path, fileToSave.content).onSuccess {
                    val openFiles = _uiState.value.openFiles.toMutableList()
                    val index = openFiles.indexOfFirst { it.path == fileToSave.path }
                    
                    if (index >= 0) {
                        val updatedFile = fileToSave.copy(isModified = false)
                        openFiles[index] = updatedFile
                        
                        _uiState.update { it.copy(
                            openFiles = openFiles,
                            activeFile = if (it.activeFile?.path == fileToSave.path) updatedFile else it.activeFile
                        ) }
                    }
                }
            }
        }
    }
    
    fun closeFile(filePath: String) {
        val openFiles = _uiState.value.openFiles.toMutableList()
        val fileToClose = openFiles.find { it.path == filePath }
        
        if (fileToClose != null) {
            // Auto-save if modified
            if (fileToClose.isModified && _uiState.value.settings.autoSaveOnExit) {
                viewModelScope.launch {
                    saveFile(filePath)
                    
                    // Remove file after saving
                    val updatedOpenFiles = _uiState.value.openFiles.filter { it.path != filePath }
                    
                    // Update active file if needed
                    val updatedActiveFile = if (_uiState.value.activeFile?.path == filePath) {
                        updatedOpenFiles.lastOrNull()
                    } else {
                        _uiState.value.activeFile
                    }
                    
                    _uiState.update { it.copy(
                        openFiles = updatedOpenFiles,
                        activeFile = updatedActiveFile
                    ) }
                }
            } else {
                // Remove file without saving
                openFiles.removeAll { it.path == filePath }
                
                // Update active file if needed
                val updatedActiveFile = if (_uiState.value.activeFile?.path == filePath) {
                    openFiles.lastOrNull()
                } else {
                    _uiState.value.activeFile
                }
                
                _uiState.update { it.copy(
                    openFiles = openFiles,
                    activeFile = updatedActiveFile
                ) }
            }
        }
    }
    
    fun selectFile(filePath: String) {
        val file = _uiState.value.openFiles.find { it.path == filePath }
        if (file != null) {
            _uiState.update { it.copy(activeFile = file) }
            
            // Highlight syntax
            highlightSyntax(file)
        }
    }
    
    fun createNewFile(fileName: String, content: String = "") {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path
            if (projectPath != null) {
                val filePath = "$projectPath/$fileName"
                fileManager.createFile(filePath, content).onSuccess {
                    // Open the new file
                    openFile(filePath)
                    
                    // Refresh project structure
                    refreshProjectStructure()
                }
            }
        }
    }
    
    fun changeFileLanguage(filePath: String, language: String) {
        val openFiles = _uiState.value.openFiles.toMutableList()
        val index = openFiles.indexOfFirst { it.path == filePath }
        
        if (index >= 0) {
            val file = openFiles[index]
            val updatedFile = file.copy(language = language)
            openFiles[index] = updatedFile
            
            _uiState.update { it.copy(
                openFiles = openFiles,
                activeFile = if (it.activeFile?.path == filePath) updatedFile else it.activeFile
            ) }
            
            // Highlight syntax with new language
            highlightSyntax(updatedFile)
        }
    }
    
    // Project Operations
    fun openProject() {
        // In a real implementation, this would show a file picker
        // For now, we'll use a hardcoded path
        val projectPath = "/storage/emulated/0/AndroidProjects/MyApp"
        
        viewModelScope.launch {
            projectManager.openProject(projectPath).onSuccess { projectInfo ->
                val projectStructure = ProjectStructure(
                    name = projectInfo.name,
                    path = projectInfo.path,
                    projectType = projectInfo.type.toString(),
                    rootFiles = projectInfo.fileTree
                )
                
                _uiState.update { it.copy(
                    projectStructure = projectStructure
                ) }
                
                // Check if Git is initialized
                checkGitStatus(projectPath)
                
                // Load Gradle tasks
                refreshGradleTasks()
                
                // Load Rust crate info if applicable
                if (projectInfo.type == ProjectType.RUST_ANDROID_LIB) {
                    loadRustCrateInfo(projectPath)
                }
            }
        }
    }
    
    fun closeProject() {
        // Save all open files
        autoSaveOnExit()
        
        // Clear project state
        _uiState.update { it.copy(
            projectStructure = null,
            openFiles = emptyList(),
            activeFile = null,
            gitStatus = emptyList(),
            gitBranches = emptyList(),
            gitCommits = emptyList(),
            isGitInitialized = false,
            currentBranch = "main",
            gradleTasks = emptyList(),
            buildOutput = null,
            buildOutputMessages = emptyList(),
            rustBuildOutput = emptyList(),
            rustCrateInfo = null
        ) }
    }
    
    fun createNewProject(template: ProjectTemplateData) {
        // In a real implementation, this would show a dialog for project details
        // For now, we'll use hardcoded values
        val projectName = template.name.replace(" ", "")
        val projectPath = "/storage/emulated/0/AndroidProjects"
        val projectType = when (template.id) {
            "android_basic" -> ProjectType.ANDROID_APP
            "compose_app" -> ProjectType.COMPOSE_APP
            "mvvm_app" -> ProjectType.MVVM_APP
            "game_2d" -> ProjectType.GAME_2D
            "rest_api" -> ProjectType.REST_API_CLIENT
            "kotlin_library" -> ProjectType.KOTLIN_MULTIPLATFORM
            "rust_android_lib" -> ProjectType.RUST_ANDROID_LIB
            else -> ProjectType.ANDROID_APP
        }
        
        viewModelScope.launch {
            projectManager.createProject(projectPath, projectName, projectType).onSuccess { projectInfo ->
                val projectStructure = ProjectStructure(
                    name = projectInfo.name,
                    path = projectInfo.path,
                    projectType = projectInfo.type.toString(),
                    rootFiles = projectInfo.fileTree
                )
                
                _uiState.update { it.copy(
                    projectStructure = projectStructure
                ) }
                
                // Initialize Git
                gitInit()
                
                // Load Gradle tasks
                refreshGradleTasks()
            }
        }
    }
    
    fun createProjectFromTemplate(template: ProjectTemplateData) {
        createNewProject(template)
    }
    
    fun importProject(path: String) {
        viewModelScope.launch {
            projectManager.openProject(path).onSuccess { projectInfo ->
                val projectStructure = ProjectStructure(
                    name = projectInfo.name,
                    path = projectInfo.path,
                    projectType = projectInfo.type.toString(),
                    rootFiles = projectInfo.fileTree
                )
                
                _uiState.update { it.copy(
                    projectStructure = projectStructure
                ) }
                
                // Check if Git is initialized
                checkGitStatus(path)
                
                // Load Gradle tasks
                refreshGradleTasks()
                
                // Load Rust crate info if applicable
                if (projectInfo.type == ProjectType.RUST_ANDROID_LIB) {
                    loadRustCrateInfo(path)
                }
            }
        }
    }
    
    private fun refreshProjectStructure() {
        val projectPath = _uiState.value.projectStructure?.path ?: return
        
        viewModelScope.launch {
            projectManager.openProject(projectPath).onSuccess { projectInfo ->
                val projectStructure = ProjectStructure(
                    name = projectInfo.name,
                    path = projectInfo.path,
                    projectType = projectInfo.type.toString(),
                    rootFiles = projectInfo.fileTree
                )
                
                _uiState.update { it.copy(
                    projectStructure = projectStructure
                ) }
            }
        }
    }
    
    // Build Operations
    fun buildProject(buildType: String = "debug") {
        _uiState.update { it.copy(isBuilding = true) }
        
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            // Save all files before building
            _uiState.value.openFiles.filter { it.isModified }.forEach { file ->
                saveFile(file.path)
            }
            
            buildManager.buildProject(projectPath, buildType).collect { output ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + output,
                        isBuilding = output.type != BuildOutputType.SUCCESS && output.type != BuildOutputType.ERROR
                    )
                }
            }
        }
    }
    
    fun runProject() {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            // First build the project
            buildManager.buildProject(projectPath, "debug").collect { output ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + output,
                        isBuilding = output.type != BuildOutputType.SUCCESS && output.type != BuildOutputType.ERROR
                    )
                }
                
                // If build is successful, install and run the app
                if (output.type == BuildOutputType.SUCCESS) {
                    buildManager.installDebugApk(projectPath).collect { installOutput ->
                        _uiState.update { state ->
                            state.copy(
                                buildOutputMessages = state.buildOutputMessages + installOutput
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun debugProject() {
        // Similar to runProject but with debug configuration
        runProject()
    }
    
    fun executeGradleTask(taskName: String) {
        _uiState.update { it.copy(isBuilding = true) }
        
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            // Save all files before running task
            _uiState.value.openFiles.filter { it.isModified }.forEach { file ->
                saveFile(file.path)
            }
            
            buildManager.buildProject(projectPath, taskName).collect { output ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + output,
                        isBuilding = output.type != BuildOutputType.SUCCESS && output.type != BuildOutputType.ERROR
                    )
                }
            }
        }
    }
    
    fun refreshGradleTasks() {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            buildManager.getGradleTasks(projectPath).onSuccess { tasks ->
                _uiState.update { it.copy(gradleTasks = tasks) }
            }
        }
    }
    
    // Rust Build Operations
    fun buildRustProject(projectPath: String, buildType: String, release: Boolean) {
        _uiState.update { it.copy(isBuilding = true) }
        
        viewModelScope.launch {
            // Save all files before building
            _uiState.value.openFiles.filter { it.isModified }.forEach { file ->
                saveFile(file.path)
            }
            
            rustBuildManager.buildRustProject(projectPath, buildType, release).collect { output ->
                _uiState.update { state ->
                    state.copy(
                        rustBuildOutput = state.rustBuildOutput + output,
                        isBuilding = output.type != RustBuildOutput.Type.SUCCESS && output.type != RustBuildOutput.Type.ERROR
                    )
                }
            }
        }
    }
    
    fun cleanRustProject(projectPath: String) {
        viewModelScope.launch {
            rustBuildManager.cleanRustProject(projectPath).collect { output ->
                _uiState.update { state ->
                    state.copy(rustBuildOutput = state.rustBuildOutput + output)
                }
            }
        }
    }
    
    fun testRustProject(projectPath: String, release: Boolean) {
        viewModelScope.launch {
            rustBuildManager.testRustProject(projectPath, release).collect { output ->
                _uiState.update { state ->
                    state.copy(rustBuildOutput = state.rustBuildOutput + output)
                }
            }
        }
    }
    
    fun addRustDependency(projectPath: String, name: String, version: String) {
        viewModelScope.launch {
            rustBuildManager.addRustDependency(projectPath, name, version).collect { output ->
                _uiState.update { state ->
                    state.copy(rustBuildOutput = state.rustBuildOutput + output)
                }
            }
            
            // Refresh crate info
            loadRustCrateInfo(projectPath)
        }
    }
    
    fun removeRustDependency(projectPath: String, name: String) {
        viewModelScope.launch {
            rustBuildManager.removeRustDependency(projectPath, name).collect { output ->
                _uiState.update { state ->
                    state.copy(rustBuildOutput = state.rustBuildOutput + output)
                }
            }
            
            // Refresh crate info
            loadRustCrateInfo(projectPath)
        }
    }
    
    fun createRustProject(name: String, path: String, template: String, isAndroidLib: Boolean) {
        viewModelScope.launch {
            createRustProject(rustBuildManager, name, path, template, isAndroidLib).collect { output ->
                _uiState.update { state ->
                    state.copy(rustBuildOutput = state.rustBuildOutput + output)
                }
            }
            
            // Open the project after creation
            val projectPath = "$path/$name"
            projectManager.openProject(projectPath).onSuccess { projectInfo ->
                val projectStructure = ProjectStructure(
                    name = projectInfo.name,
                    path = projectInfo.path,
                    projectType = projectInfo.type.toString(),
                    rootFiles = projectInfo.fileTree
                )
                
                _uiState.update { it.copy(
                    projectStructure = projectStructure,
                    selectedBuildSystem = if (isAndroidLib) BuildSystemType.HYBRID else BuildSystemType.RUST
                ) }
                
                // Initialize Git
                gitInit()
                
                // Load Rust crate info
                loadRustCrateInfo(projectPath)
            }
        }
    }
    
    fun generateRustAndroidBindings(projectPath: String) {
        viewModelScope.launch {
            rustBuildManager.generateRustAndroidBindings(projectPath).collect { output ->
                _uiState.update { state ->
                    state.copy(rustBuildOutput = state.rustBuildOutput + output)
                }
            }
        }
    }
    
    fun buildRustForAndroidTarget(projectPath: String, target: String, release: Boolean) {
        _uiState.update { it.copy(isBuilding = true) }
        
        viewModelScope.launch {
            rustBuildManager.buildRustForAndroidTarget(projectPath, target, release).collect { output ->
                _uiState.update { state ->
                    state.copy(
                        rustBuildOutput = state.rustBuildOutput + output,
                        isBuilding = output.type != RustBuildOutput.Type.SUCCESS && output.type != RustBuildOutput.Type.ERROR
                    )
                }
            }
        }
    }
    
    private fun loadRustCrateInfo(projectPath: String) {
        viewModelScope.launch {
            rustBuildManager.getCrateInfo(projectPath).onSuccess { crateInfo ->
                _uiState.update { it.copy(rustCrateInfo = crateInfo) }
            }
        }
    }
    
    // Rust Native Build Operations
    fun buildProjectWithNative(projectPath: String, buildType: String) {
        _uiState.update { it.copy(isBuilding = true) }
        
        viewModelScope.launch {
            // Save all files before building
            _uiState.value.openFiles.filter { it.isModified }.forEach { file ->
                saveFile(file.path)
            }
            
            rustNativeBuildManager.buildProject(projectPath, buildType).collect { output ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + output,
                        isBuilding = output.type != BuildOutputType.SUCCESS && output.type != BuildOutputType.ERROR
                    )
                }
            }
        }
    }
    
    fun cleanProjectWithNative(projectPath: String) {
        viewModelScope.launch {
            rustNativeBuildManager.cleanProject(projectPath).collect { output ->
                _uiState.update { state ->
                    state.copy(buildOutputMessages = state.buildOutputMessages + output)
                }
            }
        }
    }
    
    fun testProjectWithNative(projectPath: String, release: Boolean) {
        viewModelScope.launch {
            rustNativeBuildManager.testProject(projectPath, release).collect { output ->
                _uiState.update { state ->
                    state.copy(buildOutputMessages = state.buildOutputMessages + output)
                }
            }
        }
    }
    
    // Gradle Build Management
    fun optimizeBuildFile(buildFilePath: String) {
        viewModelScope.launch {
            gradleBuildManager.optimizeBuildFile(buildFilePath).collect { message ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + BuildOutputMessage(
                            type = BuildOutputType.INFO,
                            message = message
                        )
                    )
                }
            }
        }
    }
    
    fun updateDependencies(buildFilePath: String) {
        viewModelScope.launch {
            gradleBuildManager.updateDependencies(buildFilePath).collect { message ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + BuildOutputMessage(
                            type = BuildOutputType.INFO,
                            message = message
                        )
                    )
                }
            }
        }
    }
    
    fun fixCommonIssues(buildFilePath: String) {
        viewModelScope.launch {
            gradleFileModifier.fixBuildFile(buildFilePath).collect { message ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + BuildOutputMessage(
                            type = BuildOutputType.INFO,
                            message = message
                        )
                    )
                }
            }
        }
    }
    
    fun addGradleDependency(buildFilePath: String, dependency: String, configuration: String) {
        viewModelScope.launch {
            gradleFileModifier.addDependency(buildFilePath, dependency, configuration).collect { message ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + BuildOutputMessage(
                            type = BuildOutputType.INFO,
                            message = message
                        )
                    )
                }
            }
        }
    }
    
    fun removeGradleDependency(buildFilePath: String, dependency: String) {
        viewModelScope.launch {
            gradleFileModifier.removeDependency(buildFilePath, dependency).collect { message ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + BuildOutputMessage(
                            type = BuildOutputType.INFO,
                            message = message
                        )
                    )
                }
            }
        }
    }
    
    fun generateBuildReport(projectPath: String) {
        viewModelScope.launch {
            gradleBuildManager.generateBuildReport(projectPath).onSuccess { report ->
                _uiState.update { state ->
                    state.copy(
                        buildOutputMessages = state.buildOutputMessages + BuildOutputMessage(
                            type = BuildOutputType.SUCCESS,
                            message = "Build report generated successfully"
                        )
                    )
                }
            }
        }
    }
    
    // Git Operations
    fun gitInit() {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.initRepository(projectPath).collect { result ->
                when (result) {
                    is GitResult.Success -> {
                        _uiState.update { it.copy(isGitInitialized = true) }
                        getGitStatus()
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun getGitStatus() {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.getStatus(projectPath).collect { result ->
                when (result) {
                    is GitResult.StatusResult -> {
                        _uiState.update { it.copy(
                            gitStatus = result.status,
                            isGitInitialized = true
                        ) }
                    }
                    is GitResult.Error -> {
                        _uiState.update { it.copy(isGitInitialized = false) }
                    }
                    else -> {}
                }
            }
            
            // Also get branches
            gitIntegration.getBranches(projectPath).collect { result ->
                when (result) {
                    is GitResult.BranchResult -> {
                        _uiState.update { it.copy(
                            gitBranches = result.branches,
                            currentBranch = result.branches.find { branch -> branch.isCurrent }?.name ?: "main"
                        ) }
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun gitAdd(file: String) {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.addFiles(projectPath, listOf(file)).collect { result ->
                when (result) {
                    is GitResult.Success -> {
                        getGitStatus()
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun gitCommit(message: String) {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.commit(projectPath, message).collect { result ->
                when (result) {
                    is GitResult.Success -> {
                        getGitStatus()
                        gitLog()
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun gitPush() {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.push(projectPath).collect { result ->
                when (result) {
                    is GitResult.Success -> {
                        getGitStatus()
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun gitPull() {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.pull(projectPath).collect { result ->
                when (result) {
                    is GitResult.Success -> {
                        getGitStatus()
                        gitLog()
                        refreshProjectStructure()
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun gitCreateBranch(branchName: String) {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.createBranch(projectPath, branchName).collect { result ->
                when (result) {
                    is GitResult.Success -> {
                        getGitStatus()
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun gitCheckoutBranch(branchName: String) {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.switchBranch(projectPath, branchName).collect { result ->
                when (result) {
                    is GitResult.Success -> {
                        getGitStatus()
                        gitLog()
                        refreshProjectStructure()
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun gitLog() {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path ?: return@launch
            
            gitIntegration.getLog(projectPath).collect { result ->
                when (result) {
                    is GitResult.LogResult -> {
                        _uiState.update { it.copy(gitCommits = result.commits) }
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun checkGitStatus(projectPath: String) {
        viewModelScope.launch {
            gitIntegration.getStatus(projectPath).collect { result ->
                when (result) {
                    is GitResult.StatusResult -> {
                        _uiState.update { it.copy(
                            gitStatus = result.status,
                            isGitInitialized = true
                        ) }
                    }
                    is GitResult.Error -> {
                        _uiState.update { it.copy(isGitInitialized = false) }
                    }
                    else -> {}
                }
            }
        }
    }
    
    // Terminal Operations
    fun createTerminalSession() {
        viewModelScope.launch {
            val projectPath = _uiState.value.projectStructure?.path
            val workingDirectory = projectPath ?: getApplication<AnyoneIDEApplication>().filesDir.absolutePath
            
            terminalManager.createSession(workingDirectory).onSuccess { sessionId ->
                val session = TerminalSession(
                    sessionId = sessionId,
                    workingDirectory = workingDirectory,
                    isActive = true
                )
                
                _uiState.update { it.copy(
                    terminalSession = session,
                    terminalOutput = emptyList()
                ) }
            }
        }
    }
    
    fun executeTerminalCommand(command: String) {
        viewModelScope.launch {
            val sessionId = _uiState.value.terminalSession?.sessionId ?: return@launch
            
            // Add command to output
            _uiState.update { state ->
                state.copy(
                    terminalOutput = state.terminalOutput + TerminalOutput(
                        sessionId = sessionId,
                        outputType = "Command",
                        content = command,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
            
            // Execute command
            if (_uiState.value.settings.enableNativeTerminal && rustTerminalManager.isNativeTerminalAvailable()) {
                // Use Rust terminal implementation
                rustTerminalManager.executeCommand(sessionId, command).collect { output ->
                    _uiState.update { state ->
                        // Handle clear command
                        if (output.type == TerminalOutputType.CLEAR) {
                            state.copy(terminalOutput = emptyList())
                        } else {
                            state.copy(
                                terminalOutput = state.terminalOutput + TerminalOutput(
                                    sessionId = sessionId,
                                    outputType = output.type.toString(),
                                    content = output.content,
                                    timestamp = output.timestamp
                                )
                            )
                        }
                    }
                }
            } else {
                // Use standard terminal implementation
                terminalManager.executeCommand(sessionId, command).collect { output ->
                    _uiState.update { state ->
                        // Handle clear command
                        if (output.type == TerminalOutputType.CLEAR) {
                            state.copy(terminalOutput = emptyList())
                        } else {
                            state.copy(
                                terminalOutput = state.terminalOutput + TerminalOutput(
                                    sessionId = sessionId,
                                    outputType = output.type.toString(),
                                    content = output.content,
                                    timestamp = output.timestamp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun closeTerminalSession() {
        viewModelScope.launch {
            val sessionId = _uiState.value.terminalSession?.sessionId ?: return@launch
            
            if (_uiState.value.settings.enableNativeTerminal && rustTerminalManager.isNativeTerminalAvailable()) {
                rustTerminalManager.closeSession(sessionId)
            } else {
                terminalManager.closeSession(sessionId)
            }
            
            _uiState.update { it.copy(
                terminalSession = null,
                terminalOutput = emptyList()
            ) }
        }
    }
    
    fun stopTerminalCommand() {
        viewModelScope.launch {
            val sessionId = _uiState.value.terminalSession?.sessionId ?: return@launch
            
            if (_uiState.value.settings.enableNativeTerminal && rustTerminalManager.isNativeTerminalAvailable()) {
                rustTerminalManager.stopCommand(sessionId)
            } else {
                terminalManager.stopCommand(sessionId)
            }
            
            _uiState.update { state ->
                state.copy(
                    terminalOutput = state.terminalOutput + TerminalOutput(
                        sessionId = sessionId,
                        outputType = "System",
                        content = "Command terminated by user",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    fun saveTerminalOutput(fileName: String) {
        viewModelScope.launch {
            val sessionId = _uiState.value.terminalSession?.sessionId ?: return@launch
            
            // Create output content
            val outputContent = _uiState.value.terminalOutput
                .joinToString("\n") { "${it.outputType}: ${it.content}" }
            
            // Save to file
            val context = getApplication<AnyoneIDEApplication>()
            val outputDir = context.getExternalFilesDir("terminal_logs")
            outputDir?.mkdirs()
            
            val outputFile = File(outputDir, fileName)
            outputFile.writeText(outputContent)
            
            // Add system message
            _uiState.update { state ->
                state.copy(
                    terminalOutput = state.terminalOutput + TerminalOutput(
                        sessionId = sessionId,
                        outputType = "System",
                        content = "Terminal output saved to: ${outputFile.absolutePath}",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    fun shareTerminalOutput() {
        viewModelScope.launch {
            val sessionId = _uiState.value.terminalSession?.sessionId ?: return@launch
            
            // Create output content
            val outputContent = _uiState.value.terminalOutput
                .joinToString("\n") { "${it.outputType}: ${it.content}" }
            
            // Save to temporary file
            val context = getApplication<AnyoneIDEApplication>()
            val outputDir = context.cacheDir
            val outputFile = File(outputDir, "terminal_output_${System.currentTimeMillis()}.txt")
            outputFile.writeText(outputContent)
            
            // Share the file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Create chooser intent
            val chooserIntent = Intent.createChooser(shareIntent, "Share Terminal Output")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(chooserIntent)
            
            // Add system message
            _uiState.update { state ->
                state.copy(
                    terminalOutput = state.terminalOutput + TerminalOutput(
                        sessionId = sessionId,
                        outputType = "System",
                        content = "Terminal output shared",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
    
    fun bookmarkCommand(command: String, description: String) {
        viewModelScope.launch {
            val bookmarkedCommand = BookmarkedCommand(
                id = UUID.randomUUID().toString(),
                userId = "current_user",
                command = command,
                description = description,
                tags = emptyList(),
                isFavorite = false,
                useCount = 0,
                lastUsed = null,
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            
            bookmarkedCommandRepository.createBookmarkedCommand(bookmarkedCommand).onSuccess {
                // Refresh bookmarked commands
                bookmarkedCommandRepository.getBookmarkedCommands().onSuccess { commands ->
                    _uiState.update { it.copy(bookmarkedCommands = commands) }
                }
                
                // Add system message
                val sessionId = _uiState.value.terminalSession?.sessionId ?: return@onSuccess
                _uiState.update { state ->
                    state.copy(
                        terminalOutput = state.terminalOutput + TerminalOutput(
                            sessionId = sessionId,
                            outputType = "System",
                            content = "Command bookmarked: $command",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
    
    fun useBookmarkedCommand(command: String) {
        viewModelScope.launch {
            // Find the command
            val bookmarkedCommand = _uiState.value.bookmarkedCommands.find { it.command == command }
            
            // Increment use count if found
            if (bookmarkedCommand != null) {
                bookmarkedCommandRepository.incrementUseCount(bookmarkedCommand.id)
            }
            
            // Execute the command
            executeTerminalCommand(command)
        }
    }
    
    // Settings Operations
    fun updateSettings(settings: IDESettings) {
        settingsManager.updateSettings(settings)
    }
    
    fun showSettings() {
        _uiState.update { it.copy(showSettingsScreen = true) }
    }
    
    fun hideSettings() {
        _uiState.update { it.copy(showSettingsScreen = false) }
    }
    
    fun toggleTheme() {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(isDarkTheme = !currentSettings.isDarkTheme)
        updateSettings(updatedSettings)
    }
    
    fun toggleLeftPanel() {
        _uiState.update { it.copy(showLeftPanel = !it.showLeftPanel) }
    }
    
    fun toggleRightPanel() {
        _uiState.update { it.copy(showRightPanel = !it.showRightPanel) }
    }
    
    fun toggleBottomPanel() {
        _uiState.update { it.copy(showBottomPanel = !it.showBottomPanel) }
    }
    
    fun setBuildType(buildType: String) {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(selectedBuildType = buildType)
        updateSettings(updatedSettings)
    }
    
    fun setCustomGradleArguments(args: String) {
        val currentSettings = _uiState.value.settings
        val updatedSettings = currentSettings.copy(customGradleArgs = args)
        updateSettings(updatedSettings)
    }
    
    fun setBuildSystem(buildSystem: BuildSystemType) {
        _uiState.update { it.copy(selectedBuildSystem = buildSystem) }
    }
    
    fun setEditorLanguage(language: String) {
        val activeFile = _uiState.value.activeFile ?: return
        changeFileLanguage(activeFile.path, language.lowercase())
    }
    
    fun setEditorTheme(themeId: String) {
        themeManager.setTheme(themeId)
        
        _uiState.update { it.copy(
            currentTheme = themeManager.currentTheme.value
        ) }
    }
    
    // Plugin Operations
    fun showPluginManager() {
        _uiState.update { it.copy(showPluginManager = true) }
    }
    
    fun hidePluginManager() {
        _uiState.update { it.copy(showPluginManager = false) }
    }
    
    fun installPlugin(plugin: PluginMetadata) {
        viewModelScope.launch {
            // In a real implementation, this would download and install the plugin
            // For now, we'll just add it to the installed plugins list
            val installedPlugin = Plugin(
                metadata = plugin,
                isEnabled = true,
                installPath = "/data/data/com.anyoneide.app/plugins/${plugin.id}"
            )
            
            _uiState.update { state ->
                state.copy(
                    installedPlugins = state.installedPlugins + installedPlugin
                )
            }
        }
    }
    
    fun uninstallPlugin(pluginId: String) {
        _uiState.update { state ->
            state.copy(
                installedPlugins = state.installedPlugins.filter { it.metadata.id != pluginId }
            )
        }
    }
    
    fun enablePlugin(pluginId: String) {
        _uiState.update { state ->
            val updatedPlugins = state.installedPlugins.map { plugin ->
                if (plugin.metadata.id == pluginId) {
                    plugin.copy(isEnabled = true)
                } else {
                    plugin
                }
            }
            
            state.copy(installedPlugins = updatedPlugins)
        }
    }
    
    fun disablePlugin(pluginId: String) {
        _uiState.update { state ->
            val updatedPlugins = state.installedPlugins.map { plugin ->
                if (plugin.metadata.id == pluginId) {
                    plugin.copy(isEnabled = false)
                } else {
                    plugin
                }
            }
            
            state.copy(installedPlugins = updatedPlugins)
        }
    }
    
    fun refreshPlugins() {
        pluginManager.refreshPlugins()
        
        _uiState.update { state ->
            state.copy(
                availablePlugins = pluginManager.availablePlugins.value
            )
        }
    }
    
    // AI Assistant Operations
    fun showAiAssistant() {
        _uiState.update { it.copy(showAiAssistant = true) }
    }
    
    fun hideAiAssistant() {
        _uiState.update { it.copy(showAiAssistant = false) }
    }
    
    fun generateCode(prompt: String, language: String): Flow<GeminiResponse> {
        return geminiApiService.generateCode(prompt, language)
    }
    
    fun aiExplainCode(code: String) {
        val activeFile = _uiState.value.activeFile ?: return
        
        viewModelScope.launch {
            geminiApiService.explainCode(code, activeFile.language).collect { response ->
                when (response) {
                    is GeminiResponse.Success -> {
                        _uiState.update { it.copy(aiExplanation = response.content) }
                    }
                    is GeminiResponse.Error -> {
                        _uiState.update { it.copy(aiExplanation = "Error: ${response.message}") }
                    }
                    is GeminiResponse.Loading -> {
                        _uiState.update { it.copy(aiExplanation = "Loading...") }
                    }
                }
            }
        }
    }
    
    fun aiFixCode(code: String, error: String) {
        val activeFile = _uiState.value.activeFile ?: return
        
        viewModelScope.launch {
            geminiApiService.fixCodeIssues(code, error, activeFile.language).collect { response ->
                when (response) {
                    is GeminiResponse.Success -> {
                        // Update file content with fixed code
                        updateFileContent(activeFile.path, response.content)
                    }
                    is GeminiResponse.Error -> {
                        _uiState.update { it.copy(aiExplanation = "Error: ${response.message}") }
                    }
                    is GeminiResponse.Loading -> {
                        _uiState.update { it.copy(aiExplanation = "Loading...") }
                    }
                }
            }
        }
    }
    
    fun aiFixError(errorMessage: String) {
        val activeFile = _uiState.value.activeFile ?: return
        
        viewModelScope.launch {
            geminiApiService.fixCodeIssues(activeFile.content, errorMessage, activeFile.language).collect { response ->
                when (response) {
                    is GeminiResponse.Success -> {
                        // Update file content with fixed code
                        updateFileContent(activeFile.path, response.content)
                    }
                    is GeminiResponse.Error -> {
                        _uiState.update { it.copy(aiExplanation = "Error: ${response.message}") }
                    }
                    is GeminiResponse.Loading -> {
                        _uiState.update { it.copy(aiExplanation = "Loading...") }
                    }
                }
            }
        }
    }
    
    // Root Operations
    fun requestRootAccess() {
        viewModelScope.launch {
            val rootManager = RootManager(getApplication())
            rootManager.requestRootAccess().onSuccess { hasRoot ->
                _uiState.update { it.copy(hasRootAccess = hasRoot) }
                isRootAccessRequested = true
            }
        }
    }
    
    // Editor State Management
    fun updateEditorState(filePath: String, scrollX: Int, scrollY: Int, selectionStart: Int, selectionEnd: Int) {
        _uiState.update { state ->
            state.copy(
                editorScrollPosition = Pair(scrollY, scrollX),
                editorSelectionPosition = Pair(selectionStart, selectionEnd)
            )
        }
    }
    
    // Auto-save all modified files
    fun autoSaveOnExit() {
        viewModelScope.launch {
            _uiState.value.openFiles.filter { it.isModified }.forEach { file ->
                fileManager.writeFile(file.path, file.content)
            }
        }
    }
    
    // Syntax Highlighting
    private fun highlightSyntax(file: EditorFile) {
        viewModelScope.launch {
            val languageConfig = languageSupport.getLanguageConfigById(file.language)
                ?: languageSupport.getLanguageConfigByExtension(fileManager.getFileExtension(file.path))
            
            if (languageConfig != null) {
                val syntaxHighlighter = SyntaxHighlighter()
                val highlights = syntaxHighlighter.highlightCode(file.content, languageConfig.id)
                
                _uiState.update { it.copy(syntaxHighlighting = highlights) }
            } else {
                _uiState.update { it.copy(syntaxHighlighting = emptyList()) }
            }
        }
    }
    
    // Activity Logging
    fun logActivity(action: String, details: Map<String, String> = emptyMap()) {
        viewModelScope.launch {
            val projectId = _uiState.value.projectStructure?.path
            
            val log = com.anyoneide.app.data.model.ActivityLog(
                id = UUID.randomUUID().toString(),
                userId = "current_user",
                projectId = projectId,
                action = action,
                details = details,
                createdAt = System.currentTimeMillis().toString()
            )
            
            activityLogRepository.logActivity(log)
        }
    }
}

// UI State
data class IDEUiState(
    // Project
    val projectStructure: ProjectStructure? = null,
    
    // Files
    val openFiles: List<EditorFile> = emptyList(),
    val activeFile: EditorFile? = null,
    val editorScrollPosition: Pair<Int, Int>? = null,
    val editorSelectionPosition: Pair<Int, Int>? = null,
    
    // Editor
    val syntaxHighlighting: List<SyntaxHighlight> = emptyList(),
    val availableLanguages: List<LanguageConfig> = emptyList(),
    val availableThemes: List<EditorTheme> = emptyList(),
    val currentTheme: EditorTheme = EditorTheme(
        id = "dark_default",
        name = "Dark Default",
        description = "Default dark theme",
        isCustom = false,
        colors = EditorColors(
            background = "#1E1E1E",
            foreground = "#D4D4D4",
            selection = "#264F78",
            lineNumber = "#858585",
            currentLine = "#2A2D2E",
            cursor = "#FFFFFF",
            keyword = "#569CD6",
            string = "#CE9178",
            comment = "#6A9955",
            number = "#B5CEA8",
            function = "#DCDCAA",
            type = "#4EC9B0",
            variable = "#9CDCFE",
            operator = "#D4D4D4",
            bracket = "#FFD700",
            error = "#F44747",
            warning = "#FF8C00",
            info = "#3794FF"
        )
    ),
    
    // UI State
    val isDarkTheme: Boolean = true,
    val showLeftPanel: Boolean = true,
    val showRightPanel: Boolean = true,
    val showBottomPanel: Boolean = true,
    val showSettingsScreen: Boolean = false,
    val showPluginManager: Boolean = false,
    val showAiAssistant: Boolean = false,
    
    // Build
    val isBuilding: Boolean = false,
    val isAnalyzing: Boolean = false,
    val buildOutput: BuildOutput? = null,
    val buildOutputMessages: List<BuildOutputMessage> = emptyList(),
    val gradleTasks: List<GradleTask> = emptyList(),
    
    // Rust Build
    val rustBuildOutput: List<RustBuildOutput> = emptyList(),
    val rustCrateInfo: RustCrateInfo? = null,
    val selectedBuildSystem: BuildSystemType = BuildSystemType.GRADLE,
    
    // Rust Native Build
    val isNativeBuildSystemAvailable: Boolean = false,
    val rustVersion: String = "Unknown",
    val nativeBuildSystemStatus: Map<String, Any> = emptyMap(),
    
    // Debug
    val debugSession: DebugSession? = null,
    val problems: List<Problem> = emptyList(),
    
    // Git
    val gitStatus: List<GitFileStatus> = emptyList(),
    val gitBranches: List<GitBranch> = emptyList(),
    val gitCommits: List<GitCommit> = emptyList(),
    val isGitInitialized: Boolean = false,
    val currentBranch: String = "main",
    
    // Terminal
    val terminalSession: TerminalSession? = null,
    val terminalOutput: List<TerminalOutput> = emptyList(),
    
    // Bookmarked Commands
    val bookmarkedCommands: List<BookmarkedCommand> = emptyList(),
    
    // Settings
    val settings: IDESettings = IDESettings(),
    
    // Plugins
    val installedPlugins: List<Plugin> = emptyList(),
    val availablePlugins: List<PluginMetadata> = emptyList(),
    
    // AI
    val aiExplanation: String? = null,
    
    // Root
    val hasRootAccess: Boolean = false
)