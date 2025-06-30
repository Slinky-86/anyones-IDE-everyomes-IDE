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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.*

@Composable
fun GradleBuildPanel(
    modifier: Modifier = Modifier,
    projectPath: String?,
    onOptimizeBuild: (String) -> Unit,
    onUpdateDependencies: (String) -> Unit,
    onFixCommonIssues: (String) -> Unit,
    onAddDependency: (String, String, String) -> Unit,
    onRemoveDependency: (String, String) -> Unit,
    onGenerateReport: (String) -> Unit
) {
    var selectedBuildFile by remember { mutableStateOf<String?>(null) }
    var buildAnalysis by remember { mutableStateOf<GradleBuildAnalysis?>(null) }
    var buildReport by remember { mutableStateOf<GradleBuildReport?>(null) }
    var showAddDependencyDialog by remember { mutableStateOf(false) }
    var dependencyToRemove by remember { mutableStateOf<String?>(null) }
    
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
                            text = "Gradle Build Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Optimize and manage your build configuration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row {
                        IconButton(
                            onClick = { 
                                projectPath?.let { onGenerateReport(it) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Generate Report"
                            )
                        }
                        
                        IconButton(
                            onClick = { showAddDependencyDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Dependency"
                            )
                        }
                    }
                }
            }
            
            if (projectPath == null) {
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
                            text = "No project loaded",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Text(
                            text = "Open a project to manage Gradle build files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Build file selection
                    item {
                        BuildFileSelector(
                            projectPath = projectPath,
                            selectedBuildFile = selectedBuildFile,
                            onBuildFileSelected = { buildFile ->
                                selectedBuildFile = buildFile
                            }
                        )
                    }
                    
                    // Quick actions
                    item {
                        QuickActionsCard(
                            selectedBuildFile = selectedBuildFile,
                            onOptimizeBuild = { buildFile ->
                                onOptimizeBuild(buildFile)
                            },
                            onUpdateDependencies = { buildFile ->
                                onUpdateDependencies(buildFile)
                            },
                            onFixCommonIssues = { buildFile ->
                                onFixCommonIssues(buildFile)
                            }
                        )
                    }
                    
                    // Build analysis
                    if (buildAnalysis != null) {
                        item {
                            BuildAnalysisCard(
                                analysis = buildAnalysis!!,
                                onRemoveDependency = { dependency ->
                                    dependencyToRemove = dependency
                                    selectedBuildFile?.let { buildFile ->
                                        onRemoveDependency(buildFile, dependency)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Build report
                    if (buildReport != null) {
                        item {
                            BuildReportCard(
                                report = buildReport!!
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDependencyDialog) {
        AddDependencyDialog(
            onDismiss = { showAddDependencyDialog = false },
            onAddDependency = { dependency, configuration ->
                selectedBuildFile?.let { buildFile ->
                    onAddDependency(buildFile, dependency, configuration)
                }
                showAddDependencyDialog = false
            }
        )
    }
}

@Composable
fun BuildFileSelector(
    projectPath: String,
    selectedBuildFile: String?,
    onBuildFileSelected: (String) -> Unit
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
                text = "Select Build File",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Simulated build files
            val buildFiles = listOf(
                "$projectPath/build.gradle",
                "$projectPath/app/build.gradle",
                "$projectPath/settings.gradle"
            )
            
            buildFiles.forEach { buildFile ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedBuildFile == buildFile,
                        onClick = { onBuildFileSelected(buildFile) }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = buildFile.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = buildFile.substringBeforeLast("/").substringAfterLast("/"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    selectedBuildFile: String?,
    onOptimizeBuild: (String) -> Unit,
    onUpdateDependencies: (String) -> Unit,
    onFixCommonIssues: (String) -> Unit
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
                Button(
                    onClick = { selectedBuildFile?.let { onOptimizeBuild(it) } },
                    modifier = Modifier.weight(1f),
                    enabled = selectedBuildFile != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Optimize")
                }
                
                Button(
                    onClick = { selectedBuildFile?.let { onUpdateDependencies(it) } },
                    modifier = Modifier.weight(1f),
                    enabled = selectedBuildFile != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { selectedBuildFile?.let { onFixCommonIssues(it) } },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedBuildFile != null
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Fix Common Issues")
            }
        }
    }
}

@Composable
fun BuildAnalysisCard(
    analysis: GradleBuildAnalysis,
    onRemoveDependency: (String) -> Unit
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
                text = "Build Analysis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // SDK Versions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SdkInfoItem(
                    title = "Compile SDK",
                    value = analysis.compileSdkVersion ?: "Not set"
                )
                
                SdkInfoItem(
                    title = "Min SDK",
                    value = analysis.minSdkVersion ?: "Not set"
                )
                
                SdkInfoItem(
                    title = "Target SDK",
                    value = analysis.targetSdkVersion ?: "Not set"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Issues
            Text(
                text = "Issues (${analysis.issues.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            if (analysis.issues.isEmpty()) {
                Text(
                    text = "No issues found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(analysis.issues) { issue ->
                        IssueItem(issue = issue)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Dependencies
            Text(
                text = "Dependencies (${analysis.dependencies.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            if (analysis.dependencies.isEmpty()) {
                Text(
                    text = "No dependencies found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(analysis.dependencies) { dependency ->
                        DependencyItem(
                            dependency = dependency,
                            onRemove = {
                                onRemoveDependency("${dependency.group}:${dependency.name}:${dependency.version}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildReportCard(
    report: GradleBuildReport
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
                text = "Project Build Report",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Project info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    title = "Gradle",
                    value = report.gradleVersion ?: "Unknown"
                )
                
                InfoItem(
                    title = "Kotlin",
                    value = report.kotlinVersion ?: "Unknown"
                )
                
                InfoItem(
                    title = "AGP",
                    value = report.agpVersion ?: "Unknown"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Summary
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    title = "Modules",
                    value = report.modules.size.toString(),
                    icon = Icons.Default.Folder
                )
                
                SummaryItem(
                    title = "Dependencies",
                    value = report.totalDependencies.toString(),
                    icon = Icons.Default.Link
                )
                
                SummaryItem(
                    title = "Issues",
                    value = report.totalIssues.toString(),
                    icon = Icons.Default.Warning,
                    valueColor = if (report.totalIssues > 0) Color.Red else Color.Green
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Modules
            Text(
                text = "Modules",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(report.modules) { module ->
                    ModuleItem(module = module)
                }
            }
        }
    }
}

@Composable
fun SdkInfoItem(
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun IssueItem(
    issue: GradleBuildIssue
) {
    val issueColor = when (issue.severity) {
        IssueSeverity.ERROR -> MaterialTheme.colorScheme.error
        IssueSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
        IssueSeverity.INFO -> MaterialTheme.colorScheme.primary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = when (issue.severity) {
                IssueSeverity.ERROR -> Icons.Default.Error
                IssueSeverity.WARNING -> Icons.Default.Warning
                IssueSeverity.INFO -> Icons.Default.Info
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = issueColor
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = issue.suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DependencyItem(
    dependency: GradleDependency,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dependency.configuration,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(120.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${dependency.group}:${dependency.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = dependency.version,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove Dependency",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun InfoItem(
    title: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SummaryItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
fun ModuleItem(
    module: GradleModuleReport
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = module.moduleName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${module.dependencies.size} dependencies, ${module.issues.size} issues",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (module.issues.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDependencyDialog(
    onDismiss: () -> Unit,
    onAddDependency: (String, String) -> Unit
) {
    var dependency by remember { mutableStateOf("") }
    var configuration by remember { mutableStateOf("implementation") }
    
    val configurations = listOf(
        "implementation",
        "api",
        "compileOnly",
        "runtimeOnly",
        "testImplementation",
        "androidTestImplementation"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Dependency") },
        text = {
            Column {
                OutlinedTextField(
                    value = dependency,
                    onValueChange = { dependency = it },
                    label = { Text("Dependency") },
                    placeholder = { Text("group:name:version") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Configuration",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                configurations.forEach { config ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = configuration == config,
                            onClick = { configuration = config }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = config,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddDependency(dependency, configuration) },
                enabled = dependency.isNotEmpty() && dependency.count { it == ':' } == 2
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}