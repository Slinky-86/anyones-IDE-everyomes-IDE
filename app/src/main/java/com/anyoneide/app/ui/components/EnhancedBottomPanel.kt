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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.BuildOutputMessage
import com.anyoneide.app.core.BuildOutputType
import com.anyoneide.app.core.GradleTask
import com.anyoneide.app.data.model.BookmarkedCommand
import com.anyoneide.app.model.Problem
import com.anyoneide.app.model.TerminalOutput
import com.anyoneide.app.model.TerminalSession

@Composable
fun EnhancedBottomPanel(
    modifier: Modifier = Modifier,
    terminalSession: TerminalSession?,
    terminalOutput: List<TerminalOutput>,
    problems: List<Problem>,
    gradleTasks: List<GradleTask>,
    onTerminalCommand: (String) -> Unit,
    onNewTerminalSession: () -> Unit,
    onCloseTerminalSession: () -> Unit,
    onTaskExecuted: (String) -> Unit,
    onRefreshTasks: () -> Unit,
    onBuildTypeSelected: (String) -> Unit,
    onCustomArgumentsChanged: (String) -> Unit,
    buildOutputMessages: List<BuildOutputMessage> = emptyList(),
    onAiFixError: (String) -> Unit = {},
    onStopCommand: () -> Unit = {},
    onSaveTerminalOutput: (String) -> Unit = {},
    onShareTerminalOutput: () -> Unit = {},
    onBookmarkCommand: (String, String) -> Unit = { _, _ -> },
    bookmarkedCommands: List<BookmarkedCommand> = emptyList(),
    onUseBookmarkedCommand: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Terminal", "Problems", "Build", "Tasks", "Build Output")
    
    // For AI assistance
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAiFixDialog by remember { mutableStateOf(false) }
    var selectedProblem by remember { mutableStateOf<Problem?>(null) }
    var aiFixSuggestion by remember { mutableStateOf("") }
    var isLoadingAiFix by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                                0 -> Icon(Icons.Default.Terminal, contentDescription = null)
                                1 -> {
                                    BadgedBox(
                                        badge = {
                                            if (problems.isNotEmpty()) {
                                                Badge { Text("${problems.size}") }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null)
                                    }
                                }
                                2 -> Icon(Icons.Default.Build, contentDescription = null)
                                3 -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                4 -> {
                                    BadgedBox(
                                        badge = {
                                            val errorCount = buildOutputMessages.count { it.type == BuildOutputType.ERROR }
                                            if (errorCount > 0) {
                                                Badge { Text("$errorCount") }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Terminal, contentDescription = null)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> {
                    Terminal(
                        modifier = Modifier.fillMaxSize(),
                        terminalSession = terminalSession,
                        terminalOutput = terminalOutput,
                        onCommandExecuted = onTerminalCommand,
                        onNewSession = onNewTerminalSession,
                        onCloseSession = onCloseTerminalSession,
                        onStopCommand = onStopCommand,
                        onSaveOutput = onSaveTerminalOutput,
                        onShareOutput = onShareTerminalOutput,
                        onBookmarkCommand = onBookmarkCommand,
                        bookmarkedCommands = bookmarkedCommands,
                        onUseBookmarkedCommand = onUseBookmarkedCommand
                    )
                }
                1 -> {
                    ProblemsPanel(
                        modifier = Modifier.fillMaxSize(),
                        problems = problems
                    )
                }
                2 -> {
                    BuildConfigurationPanel(
                        modifier = Modifier.fillMaxSize(),
                        gradleTasks = gradleTasks,
                        onTaskExecuted = onTaskExecuted,
                        onRefreshTasks = onRefreshTasks,
                        onBuildTypeSelected = onBuildTypeSelected,
                        onCustomArgumentsChanged = onCustomArgumentsChanged
                    )
                }
                3 -> {
                    TasksPanel(
                        modifier = Modifier.fillMaxSize(),
                        gradleTasks = gradleTasks,
                        onTaskExecuted = onTaskExecuted,
                        onRefreshTasks = onRefreshTasks
                    )
                }
                4 -> {
                    BuildOutputPanel(
                        modifier = Modifier.fillMaxSize(),
                        buildOutputMessages = buildOutputMessages,
                        onAiFixError = onAiFixError
                    )
                }
            }
        }
    }
    
    // AI Fix Dialog
    if (showAiFixDialog) {
        AlertDialog(
            onDismissRequest = { showAiFixDialog = false },
            title = { Text("AI Fix Suggestion") },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (isLoadingAiFix) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analyzing error and generating fix suggestions...")
                        }
                    } else {
                        Text(aiFixSuggestion)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAiFixDialog = false }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun BuildOutputPanel(
    modifier: Modifier = Modifier,
    buildOutputMessages: List<BuildOutputMessage>,
    onAiFixError: (String) -> Unit
) {
    Column(
        modifier = modifier
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
                    text = "Build Output",
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Count errors and warnings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val errorCount = buildOutputMessages.count { it.type == BuildOutputType.ERROR }
                    val warningCount = buildOutputMessages.count { it.type == BuildOutputType.WARNING }
                    
                    if (errorCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Errors",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$errorCount",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    if (warningCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warnings",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$warningCount",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
        
        if (buildOutputMessages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No build output",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Build your project to see output here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(buildOutputMessages.size) { index ->
                    val message = buildOutputMessages[index]
                    BuildOutputMessageItem(
                        message = message,
                        onAiFixError = onAiFixError
                    )
                }
            }
        }
    }
}

@Composable
fun BuildOutputMessageItem(
    message: BuildOutputMessage,
    onAiFixError: (String) -> Unit
) {
    val messageColor = when (message.type) {
        BuildOutputType.ERROR -> MaterialTheme.colorScheme.error
        BuildOutputType.WARNING -> MaterialTheme.colorScheme.tertiary
        BuildOutputType.SUCCESS -> MaterialTheme.colorScheme.primary
        BuildOutputType.TASK -> MaterialTheme.colorScheme.secondary
        BuildOutputType.ARTIFACT -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val icon = when (message.type) {
        BuildOutputType.ERROR -> Icons.Default.Error
        BuildOutputType.WARNING -> Icons.Default.Warning
        BuildOutputType.SUCCESS -> Icons.Default.CheckCircle
        BuildOutputType.TASK -> Icons.Default.PlayArrow
        BuildOutputType.ARTIFACT -> Icons.Default.AttachFile
        else -> null
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = messageColor,
                modifier = Modifier.size(16.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = message.message,
                color = messageColor,
                style = MaterialTheme.typography.bodySmall
            )
            
            // Show errors from the message
            if (message.errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                message.errors.forEach { error ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Show warnings from the message
            if (message.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                message.warnings.forEach { warning ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = warning,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
        // AI Fix button for errors
        if (message.type == BuildOutputType.ERROR) {
            IconButton(
                onClick = { onAiFixError(message.message) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "AI Fix",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TasksPanel(
    modifier: Modifier = Modifier,
    gradleTasks: List<GradleTask>,
    onTaskExecuted: (String) -> Unit,
    onRefreshTasks: () -> Unit
) {
    Column(
        modifier = modifier
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
                    text = "Gradle Tasks (${gradleTasks.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row {
                    IconButton(onClick = onRefreshTasks) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Tasks"
                        )
                    }
                }
            }
        }
        
        if (gradleTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No Gradle tasks found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(onClick = onRefreshTasks) {
                        Text("Refresh Tasks")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val groupedTasks = gradleTasks.groupBy { it.group }
                
                groupedTasks.forEach { (group, tasks) ->
                    item {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                        )
                    }
                    
                    items(tasks) { task ->
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