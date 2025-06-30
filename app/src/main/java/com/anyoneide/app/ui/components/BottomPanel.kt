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
import androidx.compose.ui.unit.dp
import com.anyoneide.app.model.Problem
import com.anyoneide.app.model.TerminalOutput
import com.anyoneide.app.model.TerminalSession

@Composable
fun BottomPanel(
    modifier: Modifier = Modifier,
    terminalSession: TerminalSession?,
    terminalOutput: List<TerminalOutput>,
    problems: List<Problem>,
    onTerminalCommand: (String) -> Unit,
    onNewTerminalSession: () -> Unit,
    onCloseTerminalSession: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Terminal", "Problems")

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
                                1 -> Icon(Icons.Default.Warning, contentDescription = null)
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
                        onCloseSession = onCloseTerminalSession
                    )
                }
                1 -> {
                    ProblemsPanel(
                        modifier = Modifier.fillMaxSize(),
                        problems = problems
                    )
                }
            }
        }
    }
}

@Composable
fun ProblemsPanel(
    modifier: Modifier = Modifier,
    problems: List<Problem>
) {
    if (problems.isEmpty()) {
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
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "No problems found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(16.dp)
        ) {
            items(problems) { problem ->
                ProblemItem(problem = problem)
            }
        }
    }
}

@Composable
fun ProblemItem(
    problem: Problem
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (problem.severity) {
                    "error" -> Icons.Default.Error
                    "warning" -> Icons.Default.Warning
                    else -> Icons.Default.Info
                },
                contentDescription = problem.severity,
                tint = when (problem.severity) {
                    "error" -> MaterialTheme.colorScheme.error
                    "warning" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = problem.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${problem.file}:${problem.line}:${problem.column}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}