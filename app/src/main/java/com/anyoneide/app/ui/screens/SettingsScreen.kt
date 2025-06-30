package com.anyoneide.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.IDESettings
import com.anyoneide.app.core.SettingsManager
import com.anyoneide.app.core.ShizukuManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit,
    onClose: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(SettingsCategory.EDITOR) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Settings Categories
        SettingsCategories(
            modifier = Modifier.width(200.dp).fillMaxHeight(),
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )
        
        VerticalDivider()
        
        // Settings Content
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings - ${selectedCategory.title}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        TextButton(
                            onClick = { onSettingsChanged(IDESettings()) }
                        ) {
                            Text("Reset to Defaults")
                        }
                        
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Settings"
                            )
                        }
                    }
                }
            }
            
            // Settings Content
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedCategory) {
                    SettingsCategory.EDITOR -> {
                        item {
                            EditorSettings(
                                settings = settings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                    SettingsCategory.TERMINAL -> {
                        item {
                            TerminalSettings(
                                settings = settings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                    SettingsCategory.APPEARANCE -> {
                        item {
                            AppearanceSettings(
                                settings = settings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                    SettingsCategory.BUILD -> {
                        item {
                            BuildSettings(
                                settings = settings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                    SettingsCategory.CODE_ANALYSIS -> {
                        item {
                            CodeAnalysisSettings(
                                settings = settings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                    SettingsCategory.PROJECT -> {
                        item {
                            ProjectSettings(
                                settings = settings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                    SettingsCategory.AI_ASSISTANT -> {
                        item {
                            AIAssistantSettings(
                                settings = settings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                    SettingsCategory.ADVANCED -> {
                        item {
                            AdvancedSettings(
                                settings = settings,
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategories(
    modifier: Modifier = Modifier,
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            items(SettingsCategory.entries.toTypedArray()) { category ->
                SettingsCategoryItem(
                    category = category,
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryItem(
    category: SettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun EditorSettings(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit
) {
    SettingsSection(title = "Editor Preferences") {
        // Font Size
        SliderSetting(
            title = "Font Size",
            value = settings.fontSize.toFloat(),
            valueRange = 8f..24f,
            steps = 15,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(fontSize = newValue.toInt()))
            },
            valueText = "${settings.fontSize}sp"
        )
        
        // Tab Size
        SliderSetting(
            title = "Tab Size",
            value = settings.tabSize.toFloat(),
            valueRange = 2f..8f,
            steps = 5,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(tabSize = newValue.toInt()))
            },
            valueText = "${settings.tabSize} spaces"
        )
        
        // Word Wrap
        SwitchSetting(
            title = "Word Wrap",
            description = "Wrap long lines to fit the editor width",
            checked = settings.wordWrap,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(wordWrap = checked))
            }
        )
        
        // Syntax Highlighting
        SwitchSetting(
            title = "Syntax Highlighting",
            description = "Enable syntax highlighting for code files",
            checked = settings.syntaxHighlighting,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(syntaxHighlighting = checked))
            }
        )
        
        // Line Numbers
        SwitchSetting(
            title = "Line Numbers",
            description = "Show line numbers in the editor",
            checked = settings.lineNumbers,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(lineNumbers = checked))
            }
        )
        
        // Auto Complete
        SwitchSetting(
            title = "Auto Complete",
            description = "Enable code completion suggestions",
            checked = settings.autoComplete,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(autoComplete = checked))
            }
        )
        
        // Auto Indent
        SwitchSetting(
            title = "Auto Indent",
            description = "Automatically indent new lines",
            checked = settings.autoIndent,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(autoIndent = checked))
            }
        )
    }
}

@Composable
fun TerminalSettings(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit
) {
    SettingsSection(title = "Terminal Preferences") {
        // Terminal Font Size
        SliderSetting(
            title = "Terminal Font Size",
            value = settings.terminalFontSize.toFloat(),
            valueRange = 8f..20f,
            steps = 11,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(terminalFontSize = newValue.toInt()))
            },
            valueText = "${settings.terminalFontSize}sp"
        )
        
        // Scrollback Buffer
        SliderSetting(
            title = "Scrollback Buffer",
            value = settings.terminalScrollback.toFloat(),
            valueRange = 100f..5000f,
            steps = 48,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(terminalScrollback = newValue.toInt()))
            },
            valueText = "${settings.terminalScrollback} lines"
        )
        
        // Default Shell
        TextFieldSetting(
            title = "Default Shell",
            description = "Path to the default shell executable",
            value = settings.defaultShell,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(defaultShell = newValue))
            }
        )
        
        // Command Bookmarks
        SwitchSetting(
            title = "Command Bookmarks",
            description = "Enable bookmarking of frequently used commands",
            checked = settings.enableCommandBookmarks,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(enableCommandBookmarks = checked))
            }
        )
        
        // Terminal Search
        SwitchSetting(
            title = "Terminal Search",
            description = "Enable searching within terminal output",
            checked = settings.enableTerminalSearch,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(enableTerminalSearch = checked))
            }
        )
        
        // Save Terminal Output
        SwitchSetting(
            title = "Save Terminal Output",
            description = "Enable saving terminal output to a file",
            checked = settings.enableTerminalSaveOutput,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(enableTerminalSaveOutput = checked))
            }
        )
    }
}

@Composable
fun AppearanceSettings(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit
) {
    SettingsSection(title = "Appearance Preferences") {
        // Dark Theme
        SwitchSetting(
            title = "Dark Theme",
            description = "Use dark color scheme",
            checked = settings.isDarkTheme,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(isDarkTheme = checked))
            }
        )
        
        // Show Project Explorer
        SwitchSetting(
            title = "Show Project Explorer",
            description = "Display the project explorer panel by default",
            checked = settings.showProjectExplorer,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(showProjectExplorer = checked))
            }
        )
        
        // Show Tool Windows
        SwitchSetting(
            title = "Show Tool Windows",
            description = "Display the tool windows panel by default",
            checked = settings.showToolWindows,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(showToolWindows = checked))
            }
        )
        
        // Show Bottom Panel
        SwitchSetting(
            title = "Show Bottom Panel",
            description = "Display the bottom panel (terminal/problems) by default",
            checked = settings.showBottomPanel,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(showBottomPanel = checked))
            }
        )
    }
}

@Composable
fun BuildSettings(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit
) {
    SettingsSection(title = "Build Preferences") {
        // Gradle Daemon
        SwitchSetting(
            title = "Gradle Daemon",
            description = "Use Gradle daemon for faster builds",
            checked = settings.gradleDaemon,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(gradleDaemon = checked))
            }
        )
        
        // Offline Mode
        SwitchSetting(
            title = "Offline Mode",
            description = "Build without network access",
            checked = settings.offlineMode,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(offlineMode = checked))
            }
        )
        
        // Parallel Builds
        SwitchSetting(
            title = "Parallel Builds",
            description = "Enable parallel execution of Gradle tasks",
            checked = settings.parallelBuilds,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(parallelBuilds = checked))
            }
        )
        
        // Custom Gradle Arguments
        TextFieldSetting(
            title = "Custom Gradle Arguments",
            description = "Additional arguments to pass to Gradle (e.g., --info --stacktrace)",
            value = settings.customGradleArgs,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(customGradleArgs = newValue))
            }
        )
    }
}

@Composable
fun CodeAnalysisSettings(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit
) {
    SettingsSection(title = "Code Analysis Preferences") {
        // TODO Warnings
        SwitchSetting(
            title = "TODO/FIXME Warnings",
            description = "Show warnings for TODO and FIXME comments",
            checked = settings.enableTodoWarnings,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(enableTodoWarnings = checked))
            }
        )
        
        // Long Line Warnings
        SwitchSetting(
            title = "Long Line Warnings",
            description = "Show warnings for lines exceeding the threshold",
            checked = settings.enableLongLineWarnings,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(enableLongLineWarnings = checked))
            }
        )
        
        // Long Line Threshold
        SliderSetting(
            title = "Long Line Threshold",
            value = settings.longLineThreshold.toFloat(),
            valueRange = 80f..200f,
            steps = 23,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(longLineThreshold = newValue.toInt()))
            },
            valueText = "${settings.longLineThreshold} characters"
        )
        
        // Unused Import Warnings
        SwitchSetting(
            title = "Unused Import Warnings",
            description = "Show warnings for unused imports",
            checked = settings.enableUnusedImportWarnings,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(enableUnusedImportWarnings = checked))
            }
        )
    }
}

@Composable
fun ProjectSettings(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit
) {
    SettingsSection(title = "Project Preferences") {
        // Remember Open Files
        SwitchSetting(
            title = "Remember Open Files",
            description = "Reopen previously open files when loading a project",
            checked = settings.rememberOpenFiles,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(rememberOpenFiles = checked))
            }
        )
        
        // Auto-save on Exit
        SwitchSetting(
            title = "Auto-save on Exit",
            description = "Automatically save all modified files when exiting",
            checked = settings.autoSaveOnExit,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(autoSaveOnExit = checked))
            }
        )
        
        // Project Storage Location
        TextFieldSetting(
            title = "Project Storage Location",
            description = "Default location for storing projects",
            value = settings.lastProjectPath ?: "Default",
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(lastProjectPath = newValue))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantSettings(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(SettingsManager(context).getGeminiApiKey() ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    
    // Load API key when the composable is first created
    LaunchedEffect(Unit) {
        apiKey = SettingsManager(context).getGeminiApiKey() ?: ""
    }
    
    SettingsSection(title = "AI Assistant Preferences") {
        // Enable AI Features
        SwitchSetting(
            title = "Enable AI Features",
            description = "Use Gemini AI to enhance your coding experience",
            checked = settings.enableAiFeatures,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(enableAiFeatures = checked))
            }
        )
        
        // API Key
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Gemini API Key",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "Enter your Gemini API key to enable AI features",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = apiKey,
                onValueChange = { 
                    apiKey = it
                    SettingsManager(context).saveGeminiApiKey(it)
                },
                placeholder = { Text("Enter your Gemini API key") },
                visualTransformation = if (showApiKey) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Get your API key from https://ai.google.dev/",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // AI Code Completion
        SwitchSetting(
            title = "AI Code Completion",
            description = "Use AI to provide intelligent code completions",
            checked = settings.aiCompletionEnabled,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(aiCompletionEnabled = checked))
            }
        )
        
        // AI Code Explanation
        SwitchSetting(
            title = "AI Code Explanation",
            description = "Use AI to explain selected code",
            checked = settings.aiExplanationEnabled,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(aiExplanationEnabled = checked))
            }
        )
    }
}

@Composable
fun AdvancedSettings(
    settings: IDESettings,
    onSettingsChanged: (IDESettings) -> Unit
) {
    val context = LocalContext.current
    val shizukuManager = remember { ShizukuManager(context) }
    
    SettingsSection(title = "Advanced Preferences") {
        // Root Features
        SwitchSetting(
            title = "Enable Root Features",
            description = "Enable advanced features that require root access",
            checked = settings.enableRootFeatures,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(enableRootFeatures = checked))
            }
        )
        
        // Shizuku Integration
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Shizuku Integration",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Use Shizuku for enhanced command execution",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Show Shizuku status
                    val shizukuStatus = when {
                        !shizukuManager.isShizukuInstalled() -> "Not installed"
                        !shizukuManager.isShizukuAvailable() -> "Not running"
                        !shizukuManager.isShizukuPermissionGranted() -> "Permission required"
                        else -> "Available"
                    }
                    
                    Text(
                        text = "Status: $shizukuStatus",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (shizukuStatus) {
                            "Available" -> MaterialTheme.colorScheme.primary
                            "Not installed" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
                
                Switch(
                    checked = settings.enableShizukuIntegration,
                    onCheckedChange = { checked ->
                        onSettingsChanged(settings.copy(enableShizukuIntegration = checked))
                        if (checked && !shizukuManager.isShizukuPermissionGranted()) {
                            shizukuManager.requestPermission()
                        }
                    }
                )
            }
            
            // Show request permission button if Shizuku is available but permission not granted
            if (settings.enableShizukuIntegration && 
                shizukuManager.isShizukuInstalled() && 
                shizukuManager.isShizukuAvailable() && 
                !shizukuManager.isShizukuPermissionGranted()) {
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { shizukuManager.requestPermission() }
                ) {
                    Text("Request Shizuku Permission")
                }
            }
        }
        
        // Auto Save
        SwitchSetting(
            title = "Auto Save",
            description = "Automatically save files after changes",
            checked = settings.autoSave,
            onCheckedChange = { checked ->
                onSettingsChanged(settings.copy(autoSave = checked))
            }
        )
        
        // Auto Save Interval
        SliderSetting(
            title = "Auto Save Interval",
            value = settings.autoSaveInterval.toFloat(),
            valueRange = 5f..120f,
            steps = 22,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(autoSaveInterval = newValue.toInt()))
            },
            valueText = "${settings.autoSaveInterval} seconds"
        )
        
        // Max Open Files
        SliderSetting(
            title = "Maximum Open Files",
            value = settings.maxOpenFiles.toFloat(),
            valueRange = 5f..50f,
            steps = 44,
            onValueChange = { newValue ->
                onSettingsChanged(settings.copy(maxOpenFiles = newValue.toInt()))
            },
            valueText = "${settings.maxOpenFiles} files"
        )
    }
}

// Helper Composables
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            content()
        }
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldSetting(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

enum class SettingsCategory(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    EDITOR("Editor", Icons.Default.Edit),
    TERMINAL("Terminal", Icons.Default.Terminal),
    APPEARANCE("Appearance", Icons.Default.Palette),
    BUILD("Build", Icons.Default.Build),
    CODE_ANALYSIS("Code Analysis", Icons.Default.Analytics),
    PROJECT("Project", Icons.Default.Folder),
    AI_ASSISTANT("AI Assistant", Icons.Default.Psychology),
    ADVANCED("Advanced", Icons.Default.Settings)
}