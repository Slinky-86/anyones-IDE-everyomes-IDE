package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.BuildSystemType
import com.anyoneide.app.core.ProjectType
import com.anyoneide.app.core.SettingsManager
import com.anyoneide.app.core.EditorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainToolbar(
    isBuilding: Boolean = false,
    isAnalyzing: Boolean = false,
    onSaveFile: () -> Unit,
    onBuildProject: () -> Unit,
    onRunProject: () -> Unit,
    onShowEditor: () -> Unit,
    onShowUIDesigner: () -> Unit,
    onShowTemplates: () -> Unit,
    onShowGradleManager: () -> Unit,
    onShowRustManager: () -> Unit,
    onShowRustTerminal: () -> Unit = {},
    onToggleLeftPanel: () -> Unit,
    onToggleRightPanel: () -> Unit,
    onToggleBottomPanel: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    onShowAiAssistant: () -> Unit,
    onBuildSystemSelected: (BuildSystemType) -> Unit,
    onLanguageSelected: (String) -> Unit = {},
    onThemeSelected: (String) -> Unit = {},
    isDarkTheme: Boolean = true,
    showLeftPanel: Boolean = true,
    showRightPanel: Boolean = true,
    showBottomPanel: Boolean = true,
    aiEnabled: Boolean = false,
    selectedBuildSystem: BuildSystemType = BuildSystemType.GRADLE,
    projectType: ProjectType? = null,
    availableLanguages: List<String> = listOf("Kotlin", "Java", "XML", "Gradle", "JSON"),
    availableThemes: List<EditorTheme> = emptyList(),
    currentLanguage: String = "Kotlin",
    currentTheme: String = "Dark Default",
    showRustTerminalOption: Boolean = false
) {
    val context = LocalContext.current
    val hasGeminiApiKey = remember { 
        SettingsManager(context).getGeminiApiKey()?.isNotEmpty() == true 
    }
    
    var showBuildSystemMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Anyone IDE")
                
                if (isAnalyzing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Analyzing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isBuilding) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Building...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            // Language Selector
            Box {
                IconButton(onClick = { showLanguageMenu = true }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = currentLanguage,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Language"
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false }
                ) {
                    availableLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language) },
                            onClick = { 
                                onLanguageSelected(language)
                                showLanguageMenu = false
                            },
                            leadingIcon = {
                                if (language == currentLanguage) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }
            }
            
            // Theme Selector
            Box {
                IconButton(onClick = { showThemeMenu = true }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Select Theme"
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
                    availableThemes.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.name) },
                            onClick = { 
                                onThemeSelected(theme.id)
                                showThemeMenu = false
                            },
                            leadingIcon = {
                                if (theme.name == currentTheme) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }
            }
            
            // Build System Selector (only show if project supports multiple build systems)
            if (projectType == ProjectType.RUST_ANDROID_LIB) {
                Box {
                    IconButton(onClick = { showBuildSystemMenu = true }) {
                        when (selectedBuildSystem) {
                            BuildSystemType.GRADLE -> Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Gradle Build System"
                            )
                            BuildSystemType.RUST -> Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Rust Build System"
                            )
                            BuildSystemType.HYBRID -> Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Hybrid Build System"
                            )
                            BuildSystemType.RUST_NATIVE_TEST -> Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Rust Native Build System"
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showBuildSystemMenu,
                        onDismissRequest = { showBuildSystemMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Gradle Build") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null
                                )
                            },
                            onClick = { 
                                onBuildSystemSelected(BuildSystemType.GRADLE)
                                showBuildSystemMenu = false
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Rust Build") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null
                                )
                            },
                            onClick = { 
                                onBuildSystemSelected(BuildSystemType.RUST)
                                showBuildSystemMenu = false
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Hybrid Build") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null
                                )
                            },
                            onClick = { 
                                onBuildSystemSelected(BuildSystemType.HYBRID)
                                showBuildSystemMenu = false
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Rust Native Build") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = null
                                )
                            },
                            onClick = { 
                                onBuildSystemSelected(BuildSystemType.RUST_NATIVE_TEST)
                                showBuildSystemMenu = false
                            }
                        )
                    }
                }
            }
            
            // Editor Button - Direct access to editor
            IconButton(onClick = onShowEditor) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Editor"
                )
            }
            
            // Save Button
            IconButton(onClick = onSaveFile) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save File"
                )
            }
            
            // Build Button
            IconButton(onClick = onBuildProject) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Build Project"
                )
            }
            
            // Run Button
            IconButton(onClick = onRunProject) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run Project"
                )
            }
            
            // Gradle Manager Button
            IconButton(onClick = onShowGradleManager) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Gradle Manager"
                )
            }
            
            // Rust Manager Button (if project is Rust or Hybrid)
            if (projectType == ProjectType.RUST_ANDROID_LIB) {
                IconButton(onClick = onShowRustManager) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Rust Manager"
                    )
                }
            }
            
            // Rust Terminal Button (if enabled)
            if (showRustTerminalOption) {
                IconButton(onClick = onShowRustTerminal) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Rust Terminal"
                    )
                }
            }
            
            // Panel toggle buttons
            IconButton(onClick = onToggleLeftPanel) {
                Icon(
                    imageVector = if (showLeftPanel) Icons.AutoMirrored.Filled.ViewSidebar else Icons.Default.Menu,
                    contentDescription = "Toggle Project Explorer"
                )
            }
            
            IconButton(onClick = onToggleRightPanel) {
                Icon(
                    imageVector = if (showRightPanel) Icons.AutoMirrored.Filled.ViewSidebar else Icons.Default.Menu,
                    contentDescription = "Toggle Tools Panel"
                )
            }
            
            IconButton(onClick = onToggleBottomPanel) {
                Icon(
                    imageVector = if (showBottomPanel) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Terminal Panel"
                )
            }
            
            // UI Designer Button
            IconButton(onClick = onShowUIDesigner) {
                Icon(
                    imageVector = Icons.Default.DesignServices,
                    contentDescription = "UI Designer"
                )
            }
            
            // Template Preview Button
            IconButton(onClick = onShowTemplates) {
                Icon(
                    imageVector = Icons.Default.ViewModule,
                    contentDescription = "Project Templates"
                )
            }
            
            // Git button
            IconButton(onClick = { /* Show Git panel */ }) {
                Icon(
                    imageVector = Icons.Default.Source,
                    contentDescription = "Git"
                )
            }
            
            // Settings Button
            IconButton(onClick = onShowSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
            
            // Theme Toggle
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDarkTheme) {
                        Icons.Default.LightMode
                    } else {
                        Icons.Default.DarkMode
                    },
                    contentDescription = "Toggle theme"
                )
            }
            
            // AI Assistant Button (only if API key is set)
            if (hasGeminiApiKey || aiEnabled) {
                IconButton(onClick = onShowAiAssistant) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Assistant"
                    )
                }
            }
        }
    )
}