@file:Suppress("USELESS_CAST", "UNNECESSARY_NOT_NULL_ASSERTION")

package com.anyoneide.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anyoneide.app.core.BuildSystemType
import com.anyoneide.app.model.EditorFile
import com.anyoneide.app.model.ProjectStructure
import com.anyoneide.app.ui.components.*
import com.anyoneide.app.viewmodel.EnhancedMainViewModel
import com.anyoneide.app.viewmodel.SdkManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedMainScreen(
    viewModel: EnhancedMainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUIDesigner by remember { mutableStateOf(false) }
    var showTemplatePreview by remember { mutableStateOf(false) }
    var showGradleManager by remember { mutableStateOf(false) }
    var showRustManager by remember { mutableStateOf(false) }
    var showRustNativeManager by remember { mutableStateOf(false) }
    var showRustTerminal by remember { mutableStateOf(false) }
    var showRustEditor by remember { mutableStateOf(false) }
    var showSdkManager by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // Create SDK Manager ViewModel
    val context = LocalContext.current
    val sdkManagerViewModel = remember { SdkManagerViewModel(context) }
    val sdkStatus by sdkManagerViewModel.sdkStatus.collectAsState()
    val availableComponents by sdkManagerViewModel.availableComponents.collectAsState()
    val installedComponents by sdkManagerViewModel.installedComponents.collectAsState()
    val installationProgress by sdkManagerViewModel.installationProgress.collectAsState()

    // Auto-save on app exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.autoSaveOnExit()
        }
    }

    // Centralized back button handling
    BackHandler {
        when {
            // If any overlay screens are open, close them first
            uiState.showSettingsScreen -> viewModel.hideSettings()
            uiState.showPluginManager -> viewModel.hidePluginManager()
            showGradleManager -> showGradleManager = false
            showRustManager -> showRustManager = false
            showRustNativeManager -> showRustNativeManager = false
            showRustTerminal -> showRustTerminal = false
            showRustEditor -> showRustEditor = false
            showUIDesigner -> showUIDesigner = false
            showTemplatePreview -> showTemplatePreview = false
            showSdkManager -> showSdkManager = false
            
            // If bottom panel is open, close it
            uiState.showBottomPanel -> viewModel.toggleBottomPanel()
            
            // If right panel is open, close it
            uiState.showRightPanel -> viewModel.toggleRightPanel()
            
            // If left panel is open, close it
            uiState.showLeftPanel -> viewModel.toggleLeftPanel()
            
            // If files are open, close the active file
            uiState.activeFile != null && uiState.openFiles.size > 1 -> {
                uiState.activeFile?.let { file ->
                    viewModel.closeFile(file.path)
                }
            }
            
            // If project is open but no files, close project
            uiState.projectStructure != null -> {
                viewModel.closeProject()
            }
            
            // Last resort - show exit dialog
            else -> showExitDialog = true
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Anyone IDE") },
            text = { 
                Text("Are you sure you want to exit? All files will be auto-saved.") 
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.autoSaveOnExit()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settings Screen Overlay
    if (uiState.showSettingsScreen) {
        BackHandler {
            viewModel.hideSettings()
        }
        SettingsScreen(
            settings = uiState.settings,
            onSettingsChanged = viewModel::updateSettings,
            onClose = viewModel::hideSettings
        )
        return
    }

    // Plugin Manager Overlay
    if (uiState.showPluginManager) {
        BackHandler {
            viewModel.hidePluginManager()
        }
        PluginManagerScreen(
            modifier = Modifier.fillMaxSize(),
            installedPlugins = uiState.installedPlugins,
            availablePlugins = uiState.availablePlugins,
            onInstallPlugin = viewModel::installPlugin,
            onUninstallPlugin = viewModel::uninstallPlugin,
            onEnablePlugin = viewModel::enablePlugin,
            onDisablePlugin = viewModel::disablePlugin,
            onRefreshPlugins = viewModel::refreshPlugins,
            onClose = viewModel::hidePluginManager
        )
        return
    }
    
    // SDK Manager Overlay
    if (showSdkManager) {
        BackHandler {
            showSdkManager = false
        }
        SdkManagerPanel(
            modifier = Modifier.fillMaxSize(),
            sdkStatus = sdkStatus ?: com.anyoneide.app.core.SdkManagerStatus(
                androidSdkInstalled = false,
                jdkInstalled = false,
                kotlinInstalled = false,
                gradleInstalled = false,
                ndkInstalled = false,
                rustInstalled = false,
                androidSdkPath = null,
                jdkPath = null,
                kotlinPath = null,
                gradlePath = null,
                ndkPath = null,
                rustPath = null,
                availableComponents = emptyList(),
                installedComponents = emptyList()
            ),
            availableComponents = availableComponents,
            installedComponents = installedComponents,
            onInstallComponent = sdkManagerViewModel::installComponent,
            onUninstallComponent = sdkManagerViewModel::uninstallComponent,
            onRefreshStatus = sdkManagerViewModel::refreshSdkStatus,
            installationProgress = installationProgress,
            onClose = { showSdkManager = false }
        )
        return
    }
    
    // Rust Editor Overlay
    if (showRustEditor) {
        BackHandler {
            showRustEditor = false
        }
        RustEditorPanel(
            modifier = Modifier.fillMaxSize(),
            file = uiState.activeFile,
            onContentChanged = viewModel::updateFileContent,
            syntaxHighlighting = uiState.syntaxHighlighting,
            availableLanguages = uiState.availableLanguages,
            onLanguageChanged = viewModel::changeFileLanguage,
            fontSize = uiState.settings.fontSize,
            editorScaleFactor = 1.0f,
            lineNumbersScaleFactor = 0.9f,
            onEditorStateChanged = viewModel::updateEditorState,
            savedScrollPosition = uiState.editorScrollPosition,
            savedSelectionPosition = uiState.editorSelectionPosition,
            onAiExplainCode = viewModel::aiExplainCode,
            onAiFixCode = viewModel::aiFixCode,
            onClose = { showRustEditor = false }
        )
        return
    }
    
    // Gradle Manager Overlay
    if (showGradleManager) {
        BackHandler {
            showGradleManager = false
        }
        GradleBuildPanel(
            modifier = Modifier.fillMaxSize(),
            projectPath = uiState.projectStructure?.path,
            onOptimizeBuild = viewModel::optimizeBuildFile,
            onUpdateDependencies = viewModel::updateDependencies,
            onFixCommonIssues = viewModel::fixCommonIssues,
            onAddDependency = viewModel::addGradleDependency,
            onRemoveDependency = viewModel::removeGradleDependency,
            onGenerateReport = viewModel::generateBuildReport
        )
        return
    }
    
    // Rust Manager Overlay
    if (showRustManager) {
        BackHandler {
            showRustManager = false
        }
        RustBuildPanel(
            modifier = Modifier.fillMaxSize(),
            projectPath = uiState.projectStructure?.path,
            onBuildProject = viewModel::buildRustProject,
            onCleanProject = viewModel::cleanRustProject,
            onTestProject = viewModel::testRustProject,
            onAddDependency = viewModel::addRustDependency,
            onRemoveDependency = viewModel::removeRustDependency,
            onCreateProject = viewModel::createRustProject,
            onGenerateBindings = viewModel::generateRustAndroidBindings,
            onBuildForAndroidTarget = viewModel::buildRustForAndroidTarget,
            buildOutput = uiState.rustBuildOutput,
            crateInfo = uiState.rustCrateInfo,
            isBuilding = uiState.isBuilding
        )
        return
    }
    
    // Rust Native Build Manager Overlay
    if (showRustNativeManager) {
        BackHandler {
            showRustNativeManager = false
        }
        RustNativeBuildPanel(
            modifier = Modifier.fillMaxSize(),
            projectPath = uiState.projectStructure?.path,
            isNativeBuildSystemAvailable = uiState.isNativeBuildSystemAvailable,
            rustVersion = uiState.rustVersion,
            buildSystemStatus = uiState.nativeBuildSystemStatus,
            onBuildProject = viewModel::buildProjectWithNative,
            onCleanProject = viewModel::cleanProjectWithNative,
            onTestProject = viewModel::testProjectWithNative,
            buildOutput = uiState.buildOutputMessages,
            isBuilding = uiState.isBuilding
        )
        return
    }
    
    // Rust Terminal Overlay
    if (showRustTerminal) {
        BackHandler {
            showRustTerminal = false
        }
        RustTerminalPanel(
            modifier = Modifier.fillMaxSize(),
            terminalSession = uiState.terminalSession,
            terminalOutput = uiState.terminalOutput,
            onCommandExecuted = viewModel::executeTerminalCommand,
            onNewSession = viewModel::createTerminalSession,
            onCloseSession = viewModel::closeTerminalSession,
            onStopCommand = viewModel::stopTerminalCommand,
            onSaveOutput = viewModel::saveTerminalOutput,
            onShareOutput = viewModel::shareTerminalOutput,
            onBookmarkCommand = viewModel::bookmarkCommand,
            bookmarkedCommands = uiState.bookmarkedCommands,
            onUseBookmarkedCommand = viewModel::useBookmarkedCommand,
            onClose = { showRustTerminal = false },
            useNativeImplementation = uiState.settings.enableNativeTerminal,
            onToggleNativeImplementation = { useNative ->
                viewModel.updateSettings(uiState.settings.copy(enableNativeTerminal = useNative))
            }
        )
        return
    }

    // UI Designer
    if (showUIDesigner) {
        BackHandler {
            showUIDesigner = false
        }
        
        var showFileNameDialog by remember { mutableStateOf(false) }
        var generatedXmlCode by remember { mutableStateOf("") }
        var fileName by remember { mutableStateOf("activity_main.xml") }
        
        UIDesigner(
            modifier = Modifier.fillMaxSize(),
            onCodeGenerated = { code ->
                generatedXmlCode = code
                showFileNameDialog = true
            }
        )
        
        // File name dialog for XML generation
        if (showFileNameDialog) {
            AlertDialog(
                onDismissRequest = { showFileNameDialog = false },
                title = { Text("Save XML Layout") },
                text = {
                    Column {
                        Text("Enter a filename for your XML layout:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            label = { Text("Filename") },
                            placeholder = { Text("activity_main.xml") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val finalFileName = if (fileName.endsWith(".xml")) fileName else "$fileName.xml"
                            viewModel.createNewFile(finalFileName, generatedXmlCode)
                            showFileNameDialog = false
                            showUIDesigner = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showFileNameDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        return
    }

    // Template Preview
    if (showTemplatePreview) {
        BackHandler {
            showTemplatePreview = false
        }
        ProjectTemplatePreview(
            modifier = Modifier.fillMaxSize(),
            onTemplateSelected = { _ -> },
            onCreateProject = { template ->
                viewModel.createProjectFromTemplate(template)
                showTemplatePreview = false
            }
        )
        return
    }

    // Main IDE Interface
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        MainToolbar(
            isBuilding = uiState.isBuilding,
            isAnalyzing = uiState.isAnalyzing,
            onSaveFile = { viewModel.saveFile() },
            onBuildProject = { 
                when (uiState.selectedBuildSystem) {
                    BuildSystemType.RUST -> {
                        viewModel.buildRustProject(
                            uiState.projectStructure?.path ?: "",
                            uiState.settings.selectedBuildType,
                            uiState.settings.selectedBuildType == "release"
                        )
                    }
                    BuildSystemType.RUST_NATIVE_TEST -> {
                        viewModel.buildProjectWithNative(
                            uiState.projectStructure?.path ?: "",
                            uiState.settings.selectedBuildType
                        )
                    }
                    else -> {
                        viewModel.buildProject(uiState.settings.selectedBuildType)
                    }
                }
            },
            onRunProject = { viewModel.runProject() },
            onShowEditor = { 
                showRustEditor = false
                showUIDesigner = false
                showTemplatePreview = false
                showGradleManager = false
                showRustManager = false
                showRustNativeManager = false
                showRustTerminal = false
                showSdkManager = false
            },
            onShowUIDesigner = { showUIDesigner = !showUIDesigner },
            onShowTemplates = { showTemplatePreview = !showTemplatePreview },
            onShowGradleManager = { showGradleManager = !showGradleManager },
            onShowRustManager = { 
                if (uiState.selectedBuildSystem == BuildSystemType.RUST_NATIVE_TEST) {
                    showRustNativeManager = !showRustNativeManager
                } else {
                    showRustManager = !showRustManager
                }
            },
            onShowRustTerminal = { showRustTerminal = !showRustTerminal },
            onToggleLeftPanel = viewModel::toggleLeftPanel,
            onToggleRightPanel = viewModel::toggleRightPanel,
            onToggleBottomPanel = viewModel::toggleBottomPanel,
            onShowSettings = viewModel::showSettings,
            onToggleTheme = viewModel::toggleTheme,
            onShowAiAssistant = { viewModel.showAiAssistant() },
            onBuildSystemSelected = viewModel::setBuildSystem,
            onLanguageSelected = viewModel::setEditorLanguage,
            onThemeSelected = viewModel::setEditorTheme,
            isDarkTheme = uiState.isDarkTheme,
            showLeftPanel = uiState.showLeftPanel,
            showRightPanel = uiState.showRightPanel,
            showBottomPanel = uiState.showBottomPanel,
            aiEnabled = uiState.settings.enableAiFeatures,
            selectedBuildSystem = uiState.selectedBuildSystem,
            projectType = uiState.projectStructure?.type,
            availableLanguages = uiState.availableLanguages.map { it.name },
            availableThemes = uiState.availableThemes,
            currentLanguage = uiState.activeFile?.language?.capitalize() ?: "Kotlin",
            currentTheme = uiState.currentTheme.name,
            showRustTerminalOption = true
        )

        // Main content area
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Project Explorer (Left Panel) - Toggleable
            if (uiState.showLeftPanel) {
                ProjectExplorer(
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight(),
                    projectStructure = uiState.projectStructure as ProjectStructure?,
                    onFileSelected = { filePath ->
                        if (filePath.startsWith("new:")) {
                            // Create new file
                            val fileName = filePath.substringAfter("new:")
                            viewModel.createNewFile(fileName)
                        } else {
                            // Open existing file
                            viewModel.openFile(filePath)
                        }
                    },
                    onProjectOpened = { viewModel.openProject() }
                )

                VerticalDivider()
            }

            // Editor Area (Center) - Always visible
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Tab Bar with comprehensive menu
                TabBar(
                    openFiles = uiState.openFiles,
                    activeFile = uiState.activeFile,
                    onTabSelected = viewModel::selectFile,
                    onTabClosed = viewModel::closeFile,
                    onNewProject = viewModel::createNewProject,
                    onOpenProject = { viewModel.openProject() },
                    onImportProject = viewModel::importProject,
                    onNewFile = viewModel::createNewFile,
                    onShowEditor = { 
                        showRustEditor = false
                        showUIDesigner = false
                        showTemplatePreview = false
                        showGradleManager = false
                        showRustManager = false
                        showRustNativeManager = false
                        showRustTerminal = false
                        showSdkManager = false
                    },
                    onShowUIDesigner = { showUIDesigner = true },
                    onShowTemplates = { showTemplatePreview = true },
                    onShowGradleManager = { showGradleManager = true },
                    onShowGitPanel = { /* Show Git panel */ },
                    onShowRustTerminal = { showRustTerminal = true }
                )

                // Build system selector (only for Rust Android projects)
                if (uiState.projectStructure?.type == com.anyoneide.app.core.ProjectType.RUST_ANDROID_LIB) {
                    BuildSystemSelector(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        selectedBuildSystem = uiState.selectedBuildSystem,
                        projectType = uiState.projectStructure!!.type!!,
                        onBuildSystemSelected = viewModel::setBuildSystem
                    )
                }
                
                // SDK Manager button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { showSdkManager = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SDK Manager")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Rust Editor button
                    if (uiState.activeFile != null) {
                        Button(
                            onClick = { showRustEditor = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open in Rust Editor")
                        }
                    }
                }

                // Editor - always show editor, no welcome screen
                CodeEditor(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    file = uiState.activeFile ?: EditorFile(
                        path = "untitled.kt",
                        name = "untitled.kt",
                        content = "",
                        language = "kotlin",
                        isModified = false,
                        lineCount = 0
                    ),
                    onContentChanged = viewModel::updateFileContent,
                    syntaxHighlighting = uiState.syntaxHighlighting,
                    availableLanguages = uiState.availableLanguages,
                    onLanguageChanged = viewModel::changeFileLanguage,
                    fontSize = uiState.settings.fontSize,
                    editorScaleFactor = 1.0f,
                    lineNumbersScaleFactor = 0.9f,
                    editorScrollSynchronized = true,
                    onEditorStateChanged = viewModel::updateEditorState,
                    savedScrollPosition = uiState.editorScrollPosition,
                    savedSelectionPosition = uiState.editorSelectionPosition,
                    onAiExplainCode = viewModel::aiExplainCode,
                    onAiFixCode = viewModel::aiFixCode,
                    viewModel = viewModel,
                    selectedTheme = uiState.currentTheme.id
                )
            }

            // Tool Windows (Right Panel) - Toggleable
            if (uiState.showRightPanel) {
                VerticalDivider()
                
                ToolWindowsPanel(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    buildOutput = uiState.buildOutput,
                    debugSession = uiState.debugSession,
                    onBuildProject = viewModel::buildProject,
                    onRunProject = viewModel::runProject,
                    onDebugProject = viewModel::debugProject,
                    gitPanel = {
                        GitPanel(
                            modifier = Modifier.fillMaxSize(),
                            gitStatus = uiState.gitStatus,
                            gitBranches = uiState.gitBranches,
                            gitCommits = uiState.gitCommits,
                            currentBranch = uiState.currentBranch,
                            isGitInitialized = uiState.isGitInitialized,
                            onGitInit = { viewModel.gitInit() },
                            onGitStatus = { viewModel.getGitStatus() },
                            onGitAdd = { viewModel.gitAdd(it) },
                            onGitCommit = { viewModel.gitCommit(it) },
                            onGitPush = { viewModel.gitPush() },
                            onGitPull = { viewModel.gitPull() },
                            onGitCreateBranch = { viewModel.gitCreateBranch(it) },
                            onGitCheckout = { viewModel.gitCheckoutBranch(it) },
                            onGitLog = { viewModel.gitLog() }
                        )
                    }
                )
            }
        }

        // Enhanced Bottom Panel - Toggleable
        if (uiState.showBottomPanel) {
            HorizontalDivider()
            
            EnhancedBottomPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                terminalSession = uiState.terminalSession,
                terminalOutput = uiState.terminalOutput,
                problems = uiState.problems,
                gradleTasks = uiState.gradleTasks,
                onTerminalCommand = viewModel::executeTerminalCommand,
                onNewTerminalSession = viewModel::createTerminalSession,
                onCloseTerminalSession = viewModel::closeTerminalSession,
                onTaskExecuted = viewModel::executeGradleTask,
                onRefreshTasks = viewModel::refreshGradleTasks,
                onBuildTypeSelected = viewModel::setBuildType,
                onCustomArgumentsChanged = viewModel::setCustomGradleArguments,
                buildOutputMessages = uiState.buildOutputMessages,
                onAiFixError = viewModel::aiFixError,
                onStopCommand = viewModel::stopTerminalCommand,
                onSaveTerminalOutput = viewModel::saveTerminalOutput,
                onShareTerminalOutput = viewModel::shareTerminalOutput,
                onBookmarkCommand = viewModel::bookmarkCommand,
                bookmarkedCommands = uiState.bookmarkedCommands,
                onUseBookmarkedCommand = viewModel::useBookmarkedCommand
            )
        }
    }
}

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}