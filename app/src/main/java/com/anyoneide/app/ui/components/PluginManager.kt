package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.Plugin
import com.anyoneide.app.core.PluginMetadata

@Composable
fun PluginManagerScreen(
    modifier: Modifier = Modifier,
    installedPlugins: List<Plugin>,
    availablePlugins: List<PluginMetadata>,
    onInstallPlugin: (PluginMetadata) -> Unit,
    onUninstallPlugin: (String) -> Unit,
    onEnablePlugin: (String) -> Unit,
    onDisablePlugin: (String) -> Unit,
    onRefreshPlugins: () -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    val tabs = listOf("Installed", "Available", "Updates")
    val categories = listOf("All", "Language Support", "Themes", "Version Control", "Build Tools", "DevOps", "Cloud Services")

    Column(modifier = modifier.fillMaxSize()) {
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
                        text = "Plugin Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Extend Anyone IDE with powerful plugins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = onRefreshPlugins) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Plugins"
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
                    text = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (index) {
                                0 -> Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                1 -> Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                                2 -> Icon(Icons.Default.Update, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title)
                            
                            // Show count badges
                            when (index) {
                                0 -> {
                                    if (installedPlugins.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge { Text("${installedPlugins.size}") }
                                    }
                                }
                                1 -> {
                                    if (availablePlugins.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge { Text("${availablePlugins.size}") }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        // Search and filters
        if (selectedTab == 1) { // Available plugins tab
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search plugins...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Category filters
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                selected = selectedCategory == category
                            )
                        }
                    }
                }
            }
        }

        // Content
        when (selectedTab) {
            0 -> InstalledPluginsTab(
                modifier = Modifier.fillMaxSize(),
                plugins = installedPlugins,
                onEnablePlugin = onEnablePlugin,
                onDisablePlugin = onDisablePlugin,
                onUninstallPlugin = onUninstallPlugin
            )
            1 -> AvailablePluginsTab(
                modifier = Modifier.fillMaxSize(),
                plugins = availablePlugins,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                installedPlugins = installedPlugins,
                onInstallPlugin = onInstallPlugin
            )
            2 -> UpdatesTab(
                modifier = Modifier.fillMaxSize(),
                installedPlugins = installedPlugins,
                availablePlugins = availablePlugins
            )
        }
    }
}

@Composable
fun InstalledPluginsTab(
    modifier: Modifier = Modifier,
    plugins: List<Plugin>,
    onEnablePlugin: (String) -> Unit,
    onDisablePlugin: (String) -> Unit,
    onUninstallPlugin: (String) -> Unit
) {
    if (plugins.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "No plugins installed",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "Browse available plugins to extend your IDE",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(plugins) { plugin ->
                InstalledPluginCard(
                    plugin = plugin,
                    onEnablePlugin = onEnablePlugin,
                    onDisablePlugin = onDisablePlugin,
                    onUninstallPlugin = onUninstallPlugin
                )
            }
        }
    }
}

@Composable
fun AvailablePluginsTab(
    modifier: Modifier = Modifier,
    plugins: List<PluginMetadata>,
    searchQuery: String,
    selectedCategory: String,
    installedPlugins: List<Plugin>,
    onInstallPlugin: (PluginMetadata) -> Unit
) {
    val filteredPlugins = plugins.filter { plugin ->
        val matchesSearch = searchQuery.isEmpty() || 
            plugin.name.contains(searchQuery, ignoreCase = true) ||
            plugin.description.contains(searchQuery, ignoreCase = true) ||
            plugin.tags.any { it.contains(searchQuery, ignoreCase = true) }
        
        val matchesCategory = selectedCategory == "All" || plugin.category == selectedCategory
        
        matchesSearch && matchesCategory
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(filteredPlugins) { plugin ->
            val isInstalled = installedPlugins.any { it.metadata.id == plugin.id }
            
            AvailablePluginCard(
                plugin = plugin,
                isInstalled = isInstalled,
                onInstallPlugin = onInstallPlugin
            )
        }
    }
}

@Composable
fun UpdatesTab(
    modifier: Modifier = Modifier,
    installedPlugins: List<Plugin>,
    availablePlugins: List<PluginMetadata>
) {
    val updatablePlugins = installedPlugins.mapNotNull { installed ->
        val available = availablePlugins.find { it.id == installed.metadata.id }
        if (available != null && available.version > installed.metadata.version) {
            installed to available
        } else {
            null
        }
    }

    if (updatablePlugins.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "All plugins are up to date",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = "Check back later for updates",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(updatablePlugins) { (installed, available) ->
                UpdateablePluginCard(
                    installedPlugin = installed,
                    availablePlugin = available,
                    onUpdatePlugin = { /* TODO: Implement update */ }
                )
            }
        }
    }
}

@Composable
fun InstalledPluginCard(
    plugin: Plugin,
    onEnablePlugin: (String) -> Unit,
    onDisablePlugin: (String) -> Unit,
    onUninstallPlugin: (String) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plugin.metadata.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            color = if (plugin.isEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (plugin.isEnabled) "Enabled" else "Disabled",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (plugin.isEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "v${plugin.metadata.version} • ${plugin.metadata.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = plugin.metadata.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Column {
                    Switch(
                        checked = plugin.isEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                onEnablePlugin(plugin.metadata.id)
                            } else {
                                onDisablePlugin(plugin.metadata.id)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tags
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(plugin.metadata.tags) { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onUninstallPlugin(plugin.metadata.id) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Uninstall")
                }
            }
        }
    }
}

@Composable
fun AvailablePluginCard(
    plugin: PluginMetadata,
    isInstalled: Boolean,
    onInstallPlugin: (PluginMetadata) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${plugin.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFFFB000)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "%.1f".format(plugin.rating),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "${plugin.downloadCount} downloads",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = plugin.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "by ${plugin.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tags
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(plugin.tags.take(3)) { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Install button
            Button(
                onClick = { onInstallPlugin(plugin) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isInstalled
            ) {
                if (isInstalled) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Installed")
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install")
                }
            }
        }
    }
}

@Composable
fun UpdateablePluginCard(
    installedPlugin: Plugin,
    availablePlugin: PluginMetadata,
    onUpdatePlugin: (String) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = installedPlugin.metadata.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "v${installedPlugin.metadata.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(horizontal = 4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "v${availablePlugin.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Update Available",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = availablePlugin.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { onUpdatePlugin(installedPlugin.metadata.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Update")
            }
        }
    }
}