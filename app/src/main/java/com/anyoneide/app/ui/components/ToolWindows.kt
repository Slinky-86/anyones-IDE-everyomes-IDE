package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anyoneide.app.model.BuildOutput
import com.anyoneide.app.model.DebugSession

@Composable
fun ToolWindowsPanel(
    modifier: Modifier = Modifier,
    buildOutput: BuildOutput?,
    debugSession: DebugSession?,
    onBuildProject: (String) -> Unit,
    onRunProject: () -> Unit = {},
    onDebugProject: () -> Unit = {},
    gitPanel: @Composable () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Build", "Debug", "Terminal", "Git")
    
    Column(modifier = modifier) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> {
                    BuildPanelContent(
                        modifier = Modifier.fillMaxSize(),
                        buildOutput = buildOutput,
                        onBuildProject = onBuildProject
                    )
                }
                1 -> {
                    DebugPanelContent(
                        modifier = Modifier.fillMaxSize(),
                        debugSession = debugSession
                    )
                }
                2 -> {
                    TerminalPanelContent(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                3 -> {
                    gitPanel()
                }
            }
        }
    }
}

@Composable
fun BuildPanelContent(
    modifier: Modifier = Modifier,
    buildOutput: BuildOutput?,
    onBuildProject: (String) -> Unit
) {
    Box(
        modifier = modifier,
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
                text = if (buildOutput != null) {
                    if (buildOutput.success) "Build Successful" else "Build Failed"
                } else {
                    "Build panel"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { onBuildProject("debug") }
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Build Project")
            }
        }
    }
}

@Composable
fun DebugPanelContent(
    modifier: Modifier = Modifier,
    debugSession: DebugSession?
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (debugSession != null) {
                    "Debug Session: ${debugSession.sessionId}"
                } else {
                    "Debug panel"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TerminalPanelContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
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
                text = "Terminal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Terminal functionality coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}