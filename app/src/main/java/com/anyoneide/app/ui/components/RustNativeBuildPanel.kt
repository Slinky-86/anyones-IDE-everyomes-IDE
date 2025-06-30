package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.R
import com.anyoneide.app.core.BuildOutputMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RustNativeBuildPanel(
    modifier: Modifier = Modifier,
    projectPath: String?,
    isNativeBuildSystemAvailable: Boolean,
    rustVersion: String,
    buildSystemStatus: Map<String, Any>,
    onBuildProject: (String, String) -> Unit,
    onCleanProject: (String) -> Unit,
    onTestProject: (String, Boolean) -> Unit,
    buildOutput: List<BuildOutputMessage> = emptyList(),
    isBuilding: Boolean = false
) {
    var selectedBuildType by remember { mutableStateOf("debug") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Build", "Output", "Status")
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with experimental warning
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = stringResource(R.string.rust_native_build_system),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = stringResource(R.string.rust_native_test_mode),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                        
                        Text(
                            text = stringResource(R.string.rust_native_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Tab bar
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Build, contentDescription = null)
                                1 -> Icon(Icons.Default.Terminal, contentDescription = null)
                                2 -> Icon(Icons.Default.Info, contentDescription = null)
                            }
                        }
                    )
                }
            }
            
            // Main content
            when (selectedTab) {
                0 -> {
                    // Build tab
                    if (!isNativeBuildSystemAvailable) {
                        // Native build system not available
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Rust Native Build System Not Available",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "The Rust native build system requires the Rust toolchain to be installed.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { /* Install Rust */ }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Install Rust Toolchain")
                                }
                            }
                        }
                    } else if (projectPath == null) {
                        // No project loaded
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = stringResource(R.string.no_rust_project),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = stringResource(R.string.open_rust_project),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Project loaded - build controls
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Build configuration
                            item {
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.build_configuration_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        
                                        // Build type selection
                                        Text(
                                            text = stringResource(R.string.build_type),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val buildTypes = listOf("debug", "release")
                                            
                                            buildTypes.forEach { buildType ->
                                                FilterChip(
                                                    selected = selectedBuildType == buildType,
                                                    onClick = { selectedBuildType = buildType },
                                                    label = { Text(buildType.replaceFirstChar { it.uppercase() }) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Quick actions
                            item {
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.quick_actions),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Build button
                                            Button(
                                                onClick = { onBuildProject(projectPath, selectedBuildType) },
                                                modifier = Modifier.weight(1f),
                                                enabled = !isBuilding
                                            ) {
                                                if (isBuilding) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Build,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(stringResource(R.string.build))
                                            }
                                            
                                            // Clean button
                                            OutlinedButton(
                                                onClick = { onCleanProject(projectPath) },
                                                modifier = Modifier.weight(1f),
                                                enabled = !isBuilding
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CleaningServices,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(stringResource(R.string.clean))
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Test button
                                            OutlinedButton(
                                                onClick = { onTestProject(projectPath, selectedBuildType == "release") },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = !isBuilding
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BugReport,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(stringResource(R.string.test))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Project information
                            item {
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Project Information",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        
                                        Text(
                                            text = "Path: $projectPath",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "Build Type: ${selectedBuildType.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "Rust Version: ${rustVersion.split(" ").firstOrNull() ?: "Unknown"}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Output tab
                    if (buildOutput.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "No Build Output",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Build output will appear here",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(buildOutput) { output ->
                                BuildOutputMessageItem(
                                    message = output,
                                    onAiFixError = { /* Not implemented for native build */ }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Status tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Build system status
                        item {
                            Card(
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Rust Native Build System Status",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Status",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (isNativeBuildSystemAvailable) "Available" else "Not Available",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Rust Version",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = rustVersion.split(" ").firstOrNull() ?: "Unknown",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = buildSystemStatus["description"]?.toString() ?: "Native Rust build system for Android",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Features
                                    Text(
                                        text = "Features",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    @Suppress("UNCHECKED_CAST")
                                    val features = buildSystemStatus["features"] as? List<String> ?: listOf(
                                        "Build",
                                        "Clean",
                                        "Test"
                                    )
                                    
                                    features.forEach { feature ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            Text(
                                                text = feature,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // System information
                                    Text(
                                        text = "System Information",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "OS: ${buildSystemStatus["os_info"] ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    
                                    Text(
                                        text = "NDK Installed: ${buildSystemStatus["ndk_installed"] ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    
                                    // Build configuration
                                    @Suppress("UNCHECKED_CAST")
                                    val buildConfig = buildSystemStatus["build_config"] as? Map<String, Any>
                                    
                                    if (buildConfig != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Text(
                                            text = "Build Configuration",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "Timeout: ${buildConfig["timeout_seconds"] ?: "300"} seconds",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        
                                        Text(
                                            text = "Max Output Lines: ${buildConfig["max_output_lines"] ?: "10000"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        
                                        Text(
                                            text = "Verbose Output: ${buildConfig["enable_verbose_output"] ?: "true"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        
                                        Text(
                                            text = "Error Recovery: ${buildConfig["enable_error_recovery"] ?: "true"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Health checks
                        item {
                            Card(
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "System Health Checks",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    // Run health checks button
                                    Button(
                                        onClick = { /* Run health checks */ },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HealthAndSafety,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Run Health Checks")
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Health check results
                                    Text(
                                        text = "Overall Status: ${if (isNativeBuildSystemAvailable) "Healthy" else "Unhealthy"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isNativeBuildSystemAvailable) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Basic checks
                                    HealthCheckItem(
                                        name = "Rust Installation",
                                        status = if (isNativeBuildSystemAvailable) "passed" else "failed",
                                        message = if (isNativeBuildSystemAvailable) 
                                            "Rust is installed" 
                                        else 
                                            "Rust is not installed"
                                    )
                                    
                                    HealthCheckItem(
                                        name = "Library Loading",
                                        status = if (isLibraryLoaded()) "passed" else "failed",
                                        message = if (isLibraryLoaded()) 
                                            "Native library loaded successfully" 
                                        else 
                                            "Failed to load native library"
                                    )
                                    
                                    HealthCheckItem(
                                        name = "Project Validation",
                                        status = if (projectPath != null) "passed" else "warning",
                                        message = if (projectPath != null) 
                                            "Project path is valid" 
                                        else 
                                            "No project loaded"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthCheckItem(
    name: String,
    status: String,
    message: String
) {
    val statusColor = when (status) {
        "passed" -> MaterialTheme.colorScheme.primary
        "warning" -> MaterialTheme.colorScheme.tertiary
        "failed" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val statusIcon = when (status) {
        "passed" -> Icons.Default.CheckCircle
        "warning" -> Icons.Default.Warning
        "failed" -> Icons.Default.Error
        else -> Icons.Default.Info
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper function to check if the library is loaded
private fun isLibraryLoaded(): Boolean {
    return try {
        // Try to access a static method from the companion object
        com.anyoneide.app.core.RustNativeBuildManager.Companion::class.java.getDeclaredMethod("nativeGetRustVersion")
        true
    } catch (e: Exception) {
        false
    }
}