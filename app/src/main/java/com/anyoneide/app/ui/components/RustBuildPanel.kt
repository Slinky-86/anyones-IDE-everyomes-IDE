package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.RustBuildOutput
import com.anyoneide.app.core.RustCrateInfo
import com.anyoneide.app.core.RustDependency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RustBuildPanel(
    modifier: Modifier = Modifier,
    projectPath: String?,
    onBuildProject: (String, String, Boolean) -> Unit,
    onCleanProject: (String) -> Unit,
    onTestProject: (String, Boolean) -> Unit,
    onAddDependency: (String, String, String) -> Unit,
    onRemoveDependency: (String, String) -> Unit,
    onCreateProject: (String, String, String, Boolean) -> Unit,
    onGenerateBindings: (String) -> Unit,
    onBuildForAndroidTarget: (String, String, Boolean) -> Unit,
    buildOutput: List<RustBuildOutput> = emptyList(),
    crateInfo: RustCrateInfo? = null,
    isBuilding: Boolean = false
) {
    var selectedBuildProfile by remember { mutableStateOf("debug") }
    var selectedTarget by remember { mutableStateOf("default") }
    var showAddDependencyDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showGenerateBindingsDialog by remember { mutableStateOf(false) }
    
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
                            text = "Rust Build Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Build and manage Rust projects",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row {
                        IconButton(
                            onClick = { showCreateProjectDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Rust Project"
                            )
                        }
                        
                        IconButton(
                            onClick = { showAddDependencyDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Add Dependency"
                            )
                        }
                    }
                }
            }
            
            if (projectPath == null) {
                // No project loaded state
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
                            text = "No Rust project loaded",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Open a Rust project or create a new one",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showCreateProjectDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Rust Project")
                        }
                    }
                }
            } else {
                // Project loaded state
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Build configuration section
                    item {
                        BuildConfigSection(
                            selectedBuildProfile = selectedBuildProfile,
                            selectedTarget = selectedTarget,
                            onBuildProfileSelected = { selectedBuildProfile = it },
                            onTargetSelected = { selectedTarget = it }
                        )
                    }
                    
                    // Quick actions section
                    item {
                        QuickActionsSection(
                            projectPath = projectPath,
                            selectedBuildProfile = selectedBuildProfile,
                            selectedTarget = selectedTarget,
                            isBuilding = isBuilding,
                            onBuildProject = { path, profile, release ->
                                if (selectedTarget == "default") {
                                    onBuildProject(path, profile, release)
                                } else {
                                    onBuildForAndroidTarget(path, selectedTarget, release)
                                }
                            },
                            onCleanProject = onCleanProject,
                            onTestProject = onTestProject,
                            onGenerateBindings = { showGenerateBindingsDialog = true }
                        )
                    }
                    
                    // Android targets section
                    item {
                        AndroidTargetsSection(
                            selectedTarget = selectedTarget,
                            onTargetSelected = { selectedTarget = it },
                            onBuildForTarget = { target ->
                                onBuildForAndroidTarget(projectPath, target, selectedBuildProfile == "release")
                            }
                        )
                    }
                    
                    // Crate info section
                    if (crateInfo != null) {
                        item {
                            CrateInfoSection(crateInfo = crateInfo)
                        }
                    }
                    
                    // Dependencies section
                    if (crateInfo != null && crateInfo.dependencies.isNotEmpty()) {
                        item {
                            DependenciesSection(
                                dependencies = crateInfo.dependencies,
                                onRemoveDependency = { dependency ->
                                    onRemoveDependency(projectPath, dependency.name)
                                }
                            )
                        }
                    }
                    
                    // Build output section
                    if (buildOutput.isNotEmpty()) {
                        item {
                            BuildOutputSection(buildOutput = buildOutput)
                        }
                    }
                }
            }
        }
    }
    
    // Add dependency dialog
    if (showAddDependencyDialog) {
        AddRustDependencyDialog(
            onDismiss = { showAddDependencyDialog = false },
            onAddDependency = { name, version, features ->
                if (projectPath != null) {
                    onAddDependency(projectPath, name, "$version $features".trim())
                    showAddDependencyDialog = false
                }
            }
        )
    }
    
    // Create project dialog
    if (showCreateProjectDialog) {
        CreateRustProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onCreateProject = { name, path, template, isAndroidLib ->
                onCreateProject(name, path, template, isAndroidLib)
                showCreateProjectDialog = false
            }
        )
    }
    
    // Generate bindings dialog
    if (showGenerateBindingsDialog) {
        GenerateBindingsDialog(
            onDismiss = { showGenerateBindingsDialog = false },
            onGenerateBindings = {
                if (projectPath != null) {
                    onGenerateBindings(projectPath)
                    showGenerateBindingsDialog = false
                }
            }
        )
    }
}

@Composable
fun BuildConfigSection(
    selectedBuildProfile: String,
    selectedTarget: String,
    onBuildProfileSelected: (String) -> Unit,
    onTargetSelected: (String) -> Unit
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
                text = "Build Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Build profile selection
            Text(
                text = "Build Profile",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val profiles = listOf("debug", "release", "test")
                
                profiles.forEach { profile ->
                    FilterChip(
                        selected = selectedBuildProfile == profile,
                        onClick = { onBuildProfileSelected(profile) },
                        label = { Text(profile.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    projectPath: String,
    selectedBuildProfile: String,
    selectedTarget: String,
    isBuilding: Boolean,
    onBuildProject: (String, String, Boolean) -> Unit,
    onCleanProject: (String) -> Unit,
    onTestProject: (String, Boolean) -> Unit,
    onGenerateBindings: () -> Unit
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
                // Build button
                Button(
                    onClick = { 
                        onBuildProject(
                            projectPath, 
                            selectedBuildProfile,
                            selectedBuildProfile == "release"
                        ) 
                    },
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
                    Text("Build")
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
                    Text("Clean")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test button
                OutlinedButton(
                    onClick = { onTestProject(projectPath, selectedBuildProfile == "release") },
                    modifier = Modifier.weight(1f),
                    enabled = !isBuilding
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test")
                }
                
                // Generate bindings button
                OutlinedButton(
                    onClick = onGenerateBindings,
                    modifier = Modifier.weight(1f),
                    enabled = !isBuilding
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bindings")
                }
            }
        }
    }
}

@Composable
fun AndroidTargetsSection(
    selectedTarget: String,
    onTargetSelected: (String) -> Unit,
    onBuildForTarget: (String) -> Unit
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
                text = "Android Targets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = "Select a target architecture to build for Android",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Target selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val targets = listOf(
                    "default" to "Default",
                    "aarch64-linux-android" to "ARM64",
                    "armv7-linux-androideabi" to "ARM32",
                    "i686-linux-android" to "x86",
                    "x86_64-linux-android" to "x86_64"
                )
                
                targets.forEach { (target, label) ->
                    FilterChip(
                        selected = selectedTarget == target,
                        onClick = { onTargetSelected(target) },
                        label = { Text(label) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Build for specific target button
            if (selectedTarget != "default") {
                Button(
                    onClick = { onBuildForTarget(selectedTarget) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Build for ${selectedTarget.split("-").first().uppercase()}")
                }
            }
        }
    }
}

@Composable
fun CrateInfoSection(
    crateInfo: RustCrateInfo
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
                text = "Crate Information",
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
                        text = "Name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = crateInfo.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = crateInfo.version,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = crateInfo.crateType,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Edition",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = crateInfo.edition,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (crateInfo.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = crateInfo.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (crateInfo.authors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Authors",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = crateInfo.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (crateInfo.features.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    crateInfo.features.forEach { feature ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = feature,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DependenciesSection(
    dependencies: List<RustDependency>,
    onRemoveDependency: (RustDependency) -> Unit
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
                text = "Dependencies (${dependencies.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(dependencies) { dependency ->
                    DependencyItem(
                        dependency = dependency,
                        onRemove = { onRemoveDependency(dependency) }
                    )
                }
            }
        }
    }
}

@Composable
fun DependencyItem(
    dependency: RustDependency,
    onRemove: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dependency.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (dependency.version.isNotEmpty()) {
                    Text(
                        text = "Version: ${dependency.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (dependency.features.isNotEmpty()) {
                    Text(
                        text = "Features: ${dependency.features.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
}

@Composable
fun BuildOutputSection(
    buildOutput: List<RustBuildOutput>
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
                text = "Build Output",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(buildOutput) { output ->
                    BuildOutputItem(output = output)
                }
            }
        }
    }
}

@Composable
fun BuildOutputItem(
    output: RustBuildOutput
) {
    val messageColor = when (output.type) {
        RustBuildOutput.Type.ERROR -> MaterialTheme.colorScheme.error
        RustBuildOutput.Type.WARNING -> MaterialTheme.colorScheme.tertiary
        RustBuildOutput.Type.INFO -> MaterialTheme.colorScheme.onSurface
        RustBuildOutput.Type.SUCCESS -> MaterialTheme.colorScheme.primary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        when (output.type) {
            RustBuildOutput.Type.ERROR -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = messageColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
            }
            RustBuildOutput.Type.WARNING -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = messageColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
            }
            RustBuildOutput.Type.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = messageColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = messageColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = output.message,
            style = MaterialTheme.typography.bodySmall,
            color = messageColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRustDependencyDialog(
    onDismiss: () -> Unit,
    onAddDependency: (String, String, String) -> Unit
) {
    var dependencyName by remember { mutableStateOf("") }
    var dependencyVersion by remember { mutableStateOf("") }
    var dependencyFeatures by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Rust Dependency") },
        text = {
            Column {
                OutlinedTextField(
                    value = dependencyName,
                    onValueChange = { dependencyName = it },
                    label = { Text("Dependency Name") },
                    placeholder = { Text("e.g., serde") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = dependencyVersion,
                    onValueChange = { dependencyVersion = it },
                    label = { Text("Version (optional)") },
                    placeholder = { Text("e.g., 1.0.0 or ^1.0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = dependencyFeatures,
                    onValueChange = { dependencyFeatures = it },
                    label = { Text("Features (optional)") },
                    placeholder = { Text("e.g., derive, std") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Common dependencies:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { dependencyName = "serde" },
                        label = { Text("serde") }
                    )
                    
                    SuggestionChip(
                        onClick = { dependencyName = "tokio" },
                        label = { Text("tokio") }
                    )
                    
                    SuggestionChip(
                        onClick = { dependencyName = "jni" },
                        label = { Text("jni") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onAddDependency(
                        dependencyName, 
                        dependencyVersion, 
                        dependencyFeatures
                    ) 
                },
                enabled = dependencyName.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRustProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (String, String, String, Boolean) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var projectPath by remember { mutableStateOf("/storage/emulated/0/RustProjects") }
    var selectedTemplate by remember { mutableStateOf("bin") }
    var isAndroidLibrary by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Rust Project") },
        text = {
            Column {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    placeholder = { Text("my_rust_project") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = projectPath,
                    onValueChange = { projectPath = it },
                    label = { Text("Project Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Project Template",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val templates = listOf(
                        "bin" to "Binary",
                        "lib" to "Library",
                        "staticlib" to "Static Library",
                        "cdylib" to "C Dynamic Library"
                    )
                    
                    templates.forEach { (template, label) ->
                        FilterChip(
                            selected = selectedTemplate == template,
                            onClick = { selectedTemplate = template },
                            label = { Text(label) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAndroidLibrary,
                        onCheckedChange = { isAndroidLibrary = it }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = "Android Library",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Create as Android-compatible library",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onCreateProject(
                        projectName, 
                        projectPath, 
                        selectedTemplate,
                        isAndroidLibrary
                    ) 
                },
                enabled = projectName.isNotEmpty() && projectPath.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GenerateBindingsDialog(
    onDismiss: () -> Unit,
    onGenerateBindings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Android Bindings") },
        text = {
            Column {
                Text(
                    text = "This will generate JNI bindings for your Rust library to be used in Android projects.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Requirements:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• Rust project must be a library (lib, staticlib, or cdylib)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• JNI dependency must be added to Cargo.toml",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Android targets must be installed",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "This will create:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• JNI binding code in src/lib.rs",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Java wrapper classes in android/src",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Build scripts for Android integration",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGenerateBindings
            ) {
                Text("Generate Bindings")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}