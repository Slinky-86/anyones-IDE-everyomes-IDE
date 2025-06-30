package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.InstallationProgress
import com.anyoneide.app.core.SdkComponent
import com.anyoneide.app.core.SdkComponentType
import com.anyoneide.app.core.SdkManagerStatus

@Composable
fun SdkManagerPanel(
    modifier: Modifier = Modifier,
    sdkStatus: SdkManagerStatus,
    availableComponents: List<SdkComponent>,
    installedComponents: List<SdkComponent>,
    onInstallComponent: (String) -> Unit,
    onUninstallComponent: (String) -> Unit,
    onRefreshStatus: () -> Unit,
    installationProgress: Map<String, InstallationProgress> = emptyMap(),
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Installed", "Available", "Status")
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                    Column {
                        Text(
                            text = "SDK Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage Android SDK, JDK, and other development tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row {
                        IconButton(onClick = onRefreshStatus) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Status"
                            )
                        }
                        
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
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
                                0 -> Icon(Icons.Default.CheckCircle, contentDescription = null)
                                1 -> Icon(Icons.Default.GetApp, contentDescription = null)
                                2 -> Icon(Icons.Default.Info, contentDescription = null)
                            }
                        }
                    )
                }
            }
            
            // Tab content
            when (selectedTab) {
                0 -> {
                    // Installed components tab
                    InstalledComponentsTab(
                        installedComponents = installedComponents,
                        onUninstallComponent = onUninstallComponent,
                        installationProgress = installationProgress
                    )
                }
                1 -> {
                    // Available components tab
                    AvailableComponentsTab(
                        availableComponents = availableComponents,
                        installedComponents = installedComponents,
                        onInstallComponent = onInstallComponent,
                        installationProgress = installationProgress
                    )
                }
                2 -> {
                    // Status tab
                    StatusTab(
                        sdkStatus = sdkStatus
                    )
                }
            }
        }
    }
}

@Composable
fun InstalledComponentsTab(
    installedComponents: List<SdkComponent>,
    onUninstallComponent: (String) -> Unit,
    installationProgress: Map<String, InstallationProgress>
) {
    if (installedComponents.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "No components installed",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Install components from the Available tab",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(installedComponents) { component ->
                ComponentItem(
                    component = component,
                    isInstalled = true,
                    onAction = { onUninstallComponent(component.id) },
                    actionText = "Uninstall",
                    actionIcon = Icons.Default.Delete,
                    progress = installationProgress[component.id]
                )
            }
        }
    }
}

@Composable
fun AvailableComponentsTab(
    availableComponents: List<SdkComponent>,
    installedComponents: List<SdkComponent>,
    onInstallComponent: (String) -> Unit,
    installationProgress: Map<String, InstallationProgress>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableComponents) { component ->
            val isInstalled = installedComponents.any { it.id == component.id }
            
            ComponentItem(
                component = component,
                isInstalled = isInstalled,
                onAction = { onInstallComponent(component.id) },
                actionText = if (isInstalled) "Installed" else "Install",
                actionIcon = if (isInstalled) Icons.Default.CheckCircle else Icons.Default.GetApp,
                progress = installationProgress[component.id]
            )
        }
    }
}

@Composable
fun StatusTab(
    sdkStatus: SdkManagerStatus
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SDK Status
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
                        text = "SDK Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    StatusItem(
                        name = "Android SDK",
                        installed = sdkStatus.androidSdkInstalled,
                        path = sdkStatus.androidSdkPath
                    )
                    
                    StatusItem(
                        name = "JDK",
                        installed = sdkStatus.jdkInstalled,
                        path = sdkStatus.jdkPath
                    )
                    
                    StatusItem(
                        name = "Kotlin",
                        installed = sdkStatus.kotlinInstalled,
                        path = sdkStatus.kotlinPath
                    )
                    
                    StatusItem(
                        name = "Gradle",
                        installed = sdkStatus.gradleInstalled,
                        path = sdkStatus.gradlePath
                    )
                    
                    StatusItem(
                        name = "NDK",
                        installed = sdkStatus.ndkInstalled,
                        path = sdkStatus.ndkPath
                    )
                    
                    StatusItem(
                        name = "Rust",
                        installed = sdkStatus.rustInstalled,
                        path = sdkStatus.rustPath
                    )
                }
            }
        }
        
        // Component counts
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
                        text = "Component Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${sdkStatus.availableComponents.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Installed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${sdkStatus.installedComponents.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Space Used",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${calculateTotalSize(sdkStatus.installedComponents)} MB",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        // Component types
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
                        text = "Component Types",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    val componentTypes = sdkStatus.installedComponents.groupBy { it.componentType }
                    
                    componentTypes.forEach { (type, components) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getIconForComponentType(type),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = getNameForComponentType(type),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Text(
                                text = "${components.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentItem(
    component: SdkComponent,
    isInstalled: Boolean,
    onAction: () -> Unit,
    actionText: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    progress: InstallationProgress? = null
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Version: ${component.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Size: ${component.sizeMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (component.dependencies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Dependencies: ${component.dependencies.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Action button or progress
                if (progress != null) {
                    // Show progress
                    when (progress) {
                        is InstallationProgress.Downloading -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    progress = progress.progress / 100f,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "${progress.progress}%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        is InstallationProgress.Started,
                        is InstallationProgress.Extracting,
                        is InstallationProgress.Installing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        is InstallationProgress.Completed -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        is InstallationProgress.Failed -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    // Show action button
                    Button(
                        onClick = onAction,
                        enabled = !isInstalled || actionText == "Uninstall"
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(actionText)
                    }
                }
            }
            
            // Show progress message if available
            if (progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                val message = when (progress) {
                    is InstallationProgress.Started -> progress.message
                    is InstallationProgress.Downloading -> "Downloading: ${progress.progress}%"
                    is InstallationProgress.Extracting -> progress.message
                    is InstallationProgress.Installing -> progress.message
                    is InstallationProgress.Completed -> progress.message
                    is InstallationProgress.Failed -> progress.message
                }
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (progress) {
                        is InstallationProgress.Failed -> MaterialTheme.colorScheme.error
                        is InstallationProgress.Completed -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
fun StatusItem(
    name: String,
    installed: Boolean,
    path: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            if (path != null) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (installed) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Installed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Not Installed",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Helper functions

private fun calculateTotalSize(components: List<SdkComponent>): String {
    val totalMb = components.sumOf { it.sizeMb }
    return if (totalMb >= 1024) {
        String.format("%.1f GB", totalMb / 1024)
    } else {
        String.format("%.0f MB", totalMb)
    }
}

private fun getIconForComponentType(type: SdkComponentType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        is SdkComponentType.AndroidSdk -> Icons.Default.Android
        is SdkComponentType.BuildTools -> Icons.Default.Build
        is SdkComponentType.PlatformTools -> Icons.Default.Handyman
        is SdkComponentType.Platform -> Icons.Default.Layers
        is SdkComponentType.SystemImages -> Icons.Default.Image
        is SdkComponentType.Emulator -> Icons.Default.PhoneAndroid
        is SdkComponentType.Jdk -> Icons.Default.Coffee
        is SdkComponentType.Kotlin -> Icons.Default.Code
        is SdkComponentType.Gradle -> Icons.Default.Architecture
        is SdkComponentType.Ndk -> Icons.Default.Memory
        is SdkComponentType.Cmake -> Icons.Default.SettingsApplications
        is SdkComponentType.Rust -> Icons.Default.Settings
        is SdkComponentType.Cargo -> Icons.Default.Inventory
        is SdkComponentType.Other -> Icons.Default.Extension
    }
}

private fun getNameForComponentType(type: SdkComponentType): String {
    return when (type) {
        is SdkComponentType.AndroidSdk -> "Android SDK"
        is SdkComponentType.BuildTools -> "Build Tools"
        is SdkComponentType.PlatformTools -> "Platform Tools"
        is SdkComponentType.Platform -> "Platform"
        is SdkComponentType.SystemImages -> "System Images"
        is SdkComponentType.Emulator -> "Emulator"
        is SdkComponentType.Jdk -> "JDK"
        is SdkComponentType.Kotlin -> "Kotlin"
        is SdkComponentType.Gradle -> "Gradle"
        is SdkComponentType.Ndk -> "NDK"
        is SdkComponentType.Cmake -> "CMake"
        is SdkComponentType.Rust -> "Rust"
        is SdkComponentType.Cargo -> "Cargo"
        is SdkComponentType.Other -> type.type
    }
}