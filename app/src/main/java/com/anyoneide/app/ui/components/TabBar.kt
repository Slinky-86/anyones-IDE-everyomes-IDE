package com.anyoneide.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anyoneide.app.model.EditorFile
import com.anyoneide.app.ui.components.ProjectTemplateData

@Composable
fun TabBar(
    openFiles: List<EditorFile>,
    activeFile: EditorFile?,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewProject: (ProjectTemplateData) -> Unit,
    onOpenProject: () -> Unit,
    onImportProject: (String) -> Unit = {},
    onNewFile: (String) -> Unit = {},
    onShowEditor: () -> Unit = {},
    onShowUIDesigner: () -> Unit = {},
    onShowTemplates: () -> Unit = {},
    onShowGradleManager: () -> Unit = {},
    onShowGitPanel: () -> Unit = {},
    onShowRustTerminal: () -> Unit = {}
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu dropdown button
            Box {
                IconButton(
                    onClick = { showDropdownMenu = !showDropdownMenu }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "IDE Menu"
                    )
                }
                
                // Comprehensive IDE Menu
                DropdownMenu(
                    expanded = showDropdownMenu,
                    onDismissRequest = { showDropdownMenu = false },
                    modifier = Modifier.width(280.dp)
                ) {
                    // File Menu Section
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Folder, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("File", fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = { },
                        enabled = false
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.NoteAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("New File")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            showNewFileDialog = true
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("New Project")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onNewProject(ProjectTemplateData(
                                id = "android_basic",
                                name = "Basic Android App",
                                description = "A simple Android application with basic navigation and Material Design components.",
                                icon = Icons.Default.Android,
                                category = "Android",
                                features = listOf("Material Design 3", "Navigation Component", "ViewBinding", "Kotlin"),
                                difficulty = "Beginner",
                                estimatedTime = "30 min",
                                preview = """
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
                                """.trimIndent()
                            ))
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open Project")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onOpenProject()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GetApp, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Import Project")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onImportProject("/storage/emulated/0/AndroidStudioProjects")
                        }
                    )
                    
                    HorizontalDivider()
                    
                    // Editor Section
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Editor", fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = { },
                        enabled = false
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Code Editor")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onShowEditor()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DesignServices, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("UI Designer")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onShowUIDesigner()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Rust Terminal")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onShowRustTerminal()
                        }
                    )
                    
                    HorizontalDivider()
                    
                    // Project Templates Section
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ViewModule, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Project Templates", fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = { },
                        enabled = false
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Android, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Android App")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onNewProject(ProjectTemplateData(
                                id = "android_basic",
                                name = "Android Application",
                                description = "Create a new Android app with modern architecture",
                                icon = Icons.Default.Android,
                                category = "Android",
                                features = listOf("Material Design 3", "Navigation Component", "ViewBinding", "Kotlin"),
                                difficulty = "Beginner",
                                estimatedTime = "30 min",
                                preview = "class MainActivity : AppCompatActivity() { ... }"
                            ))
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Brush, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Compose App")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onNewProject(ProjectTemplateData(
                                id = "compose_app",
                                name = "Jetpack Compose App",
                                description = "Modern Android app built with Jetpack Compose",
                                icon = Icons.Default.Brush,
                                category = "Compose",
                                features = listOf("Jetpack Compose", "Material Design 3", "State Management"),
                                difficulty = "Intermediate",
                                estimatedTime = "45 min",
                                preview = "@Composable fun MainScreen() { ... }"
                            ))
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Android Library")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onNewProject(ProjectTemplateData(
                                id = "android_library",
                                name = "Android Library",
                                description = "Create a reusable Android library module",
                                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                                category = "Android",
                                features = listOf("Library Module", "AAR Publishing", "API Design"),
                                difficulty = "Intermediate",
                                estimatedTime = "35 min",
                                preview = "class MyLibrary { ... }"
                            ))
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Kotlin Library")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onNewProject(ProjectTemplateData(
                                id = "kotlin_library",
                                name = "Kotlin Library",
                                description = "Pure Kotlin library project",
                                icon = Icons.Default.Code,
                                category = "Kotlin",
                                features = listOf("Pure Kotlin", "Unit Tests", "Documentation"),
                                difficulty = "Beginner",
                                estimatedTime = "25 min",
                                preview = "class StringUtils { ... }"
                            ))
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ViewModule, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("All Templates")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onShowTemplates()
                        }
                    )
                    
                    HorizontalDivider()
                    
                    // Tools Section
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tools", fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = { },
                        enabled = false
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Gradle Manager")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onShowGradleManager()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Source, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Git")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            onShowGitPanel()
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Terminal")
                            }
                        },
                        onClick = {
                            showDropdownMenu = false
                            // Terminal will be handled by parent component
                        }
                    )
                }
            }
            
            // File tabs
            if (openFiles.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(openFiles) { file ->
                        FileTab(
                            file = file,
                            isActive = file.path == activeFile?.path,
                            onSelected = { onTabSelected(file.path) },
                            onClosed = { onTabClosed(file.path) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Editor button - Direct access to editor
            IconButton(onClick = onShowEditor) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Editor"
                )
            }
            
            // Rust Terminal button
            IconButton(onClick = onShowRustTerminal) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Rust Terminal"
                )
            }
            
            // New file button
            IconButton(
                onClick = { showNewFileDialog = true }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                    contentDescription = "New File"
                )
            }
        }
    }
    
    // New file dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNewFileDialog = false
                newFileName = ""
            },
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "File will be created in the current project",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotEmpty()) {
                            onNewFile(newFileName)
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
fun FileTab(
    file: EditorFile,
    isActive: Boolean,
    onSelected: () -> Unit,
    onClosed: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onSelected() }
            .padding(vertical = 4.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            
            if (file.isModified) {
                Text(
                    text = "●",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onClosed,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}