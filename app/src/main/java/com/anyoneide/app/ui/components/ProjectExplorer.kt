package com.anyoneide.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.FileNode
import com.anyoneide.app.model.ProjectStructure

@Composable
fun ProjectExplorer(
    modifier: Modifier = Modifier,
    projectStructure: ProjectStructure?,
    onFileSelected: (String) -> Unit,
    onProjectOpened: () -> Unit
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Project",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    // New file button
                    IconButton(
                        onClick = { showNewFileDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New File"
                        )
                    }
                    
                    // Open project button
                    IconButton(
                        onClick = onProjectOpened
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Open Project"
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Project content
            if (projectStructure != null) {
                LazyColumn {
                    items(projectStructure.rootFiles) { fileNode ->
                        FileTreeItem(
                            fileNode = fileNode,
                            level = 0,
                            onFileSelected = onFileSelected
                        )
                    }
                }
            } else {
                // No project loaded
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No project opened",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onProjectOpened,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text("Open Project")
                    }
                }
            }
        }
    }
    
    // New file dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create New File") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Name") },
                        placeholder = { Text("Example.kt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Create new file
                        if (newFileName.isNotEmpty()) {
                            // This will be handled by the parent component
                            onFileSelected("new:$newFileName")
                            showNewFileDialog = false
                            newFileName = ""
                        }
                    },
                    enabled = newFileName.isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showNewFileDialog = false
                        newFileName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FileTreeItem(
    fileNode: FileNode,
    level: Int,
    onFileSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (fileNode.isDirectory) {
                        expanded = !expanded
                    } else {
                        onFileSelected(fileNode.path)
                    }
                }
                .padding(
                    start = (level * 16).dp,
                    top = 4.dp,
                    bottom = 4.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fileNode.isDirectory) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.width(20.dp))
                Icon(
                    imageVector = getFileIcon(fileNode.name),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = fileNode.name,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
        
        if (fileNode.isDirectory && expanded) {
            fileNode.children.forEach { child ->
                FileTreeItem(
                    fileNode = child,
                    level = level + 1,
                    onFileSelected = onFileSelected
                )
            }
        }
    }
}

@Composable
private fun getFileIcon(fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        fileName.endsWith(".java") -> Icons.Default.Code
        fileName.endsWith(".kt") -> Icons.Default.Code
        fileName.endsWith(".xml") -> Icons.Default.Code
        fileName.endsWith(".json") -> Icons.Default.DataObject
        fileName.endsWith(".gradle") -> Icons.Default.Build
        fileName.endsWith(".md") -> Icons.Default.Description
        fileName.endsWith(".txt") -> Icons.AutoMirrored.Filled.TextSnippet
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}