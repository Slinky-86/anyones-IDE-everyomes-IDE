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
import com.anyoneide.app.core.GradleTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildConfigurationPanel(
    modifier: Modifier = Modifier,
    gradleTasks: List<GradleTask> = emptyList(),
    onTaskExecuted: (String) -> Unit,
    onRefreshTasks: () -> Unit,
    onBuildTypeSelected: (String) -> Unit,
    onCustomArgumentsChanged: (String) -> Unit
) {
    var selectedBuildType by remember { mutableStateOf("debug") }
    var customArguments by remember { mutableStateOf("") }
    var showTaskDetails by remember { mutableStateOf(false) }
    
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
                    Text(
                        text = "Build Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        IconButton(onClick = onRefreshTasks) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Tasks"
                            )
                        }
                        
                        IconButton(onClick = { showTaskDetails = !showTaskDetails }) {
                            Icon(
                                imageVector = if (showTaskDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Task Details"
                            )
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Build Type Selection
                item {
                    BuildTypeSelector(
                        selectedBuildType = selectedBuildType,
                        onBuildTypeSelected = { buildType ->
                            selectedBuildType = buildType
                            onBuildTypeSelected(buildType)
                        }
                    )
                }
                
                // Custom Arguments
                item {
                    CustomArgumentsSection(
                        customArguments = customArguments,
                        onArgumentsChanged = { args ->
                            customArguments = args
                            onCustomArgumentsChanged(args)
                        }
                    )
                }
                
                // Quick Actions
                item {
                    QuickActionsSection(
                        selectedBuildType = selectedBuildType,
                        onTaskExecuted = onTaskExecuted
                    )
                }
                
                // Gradle Tasks
                if (showTaskDetails) {
                    item {
                        Text(
                            text = "Available Gradle Tasks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    items(gradleTasks.groupBy { it.group }.toList()) { (group, tasks) ->
                        TaskGroupSection(
                            groupName = group,
                            tasks = tasks,
                            onTaskExecuted = onTaskExecuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildTypeSelector(
    selectedBuildType: String,
    onBuildTypeSelected: (String) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Build Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val buildTypes = listOf(
                "debug" to "Debug Build",
                "release" to "Release Build",
                "test" to "Test Build"
            )
            
            buildTypes.forEach { (type, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedBuildType == type,
                        onClick = { onBuildTypeSelected(type) }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomArgumentsSection(
    customArguments: String,
    onArgumentsChanged: (String) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Custom Gradle Arguments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Additional arguments to pass to Gradle (e.g., --info --stacktrace --offline)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            OutlinedTextField(
                value = customArguments,
                onValueChange = onArgumentsChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("--info --stacktrace") },
                singleLine = true
            )
            
            // Common argument suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val commonArgs = listOf("--info", "--stacktrace", "--offline", "--parallel")
                
                commonArgs.forEach { arg ->
                    FilterChip(
                        onClick = {
                            val current = customArguments.split(" ").filter { it.isNotBlank() }
                            val updated = if (current.contains(arg)) {
                                current - arg
                            } else {
                                current + arg
                            }
                            onArgumentsChanged(updated.joinToString(" "))
                        },
                        label = { Text(arg) },
                        selected = customArguments.contains(arg)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    selectedBuildType: String,
    onTaskExecuted: (String) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Build
                Button(
                    onClick = { onTaskExecuted("assemble${selectedBuildType.replaceFirstChar { it.uppercase() }}") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Build")
                }
                
                // Clean
                OutlinedButton(
                    onClick = { onTaskExecuted("clean") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clean")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test
                OutlinedButton(
                    onClick = { onTaskExecuted("test") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test")
                }
                
                // Install
                OutlinedButton(
                    onClick = { onTaskExecuted("install${selectedBuildType.replaceFirstChar { it.uppercase() }}") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Install")
                }
            }
        }
    }
}

@Composable
fun TaskGroupSection(
    groupName: String,
    tasks: List<GradleTask>,
    onTaskExecuted: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Group Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = { expanded = !expanded },
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
                        text = "$groupName (${tasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // Tasks List
            if (expanded) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    tasks.forEach { task ->
                        TaskItem(
                            task = task,
                            onTaskExecuted = onTaskExecuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: GradleTask,
    onTaskExecuted: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { onTaskExecuted(task.name) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Execute Task",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}