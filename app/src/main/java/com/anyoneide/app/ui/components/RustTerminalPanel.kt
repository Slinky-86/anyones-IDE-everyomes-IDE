package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.data.model.BookmarkedCommand
import com.anyoneide.app.model.TerminalOutput
import com.anyoneide.app.model.TerminalSession

@Composable
fun RustTerminalPanel(
    modifier: Modifier = Modifier,
    terminalSession: TerminalSession?,
    terminalOutput: List<TerminalOutput>,
    onCommandExecuted: (String) -> Unit,
    onNewSession: () -> Unit,
    onCloseSession: () -> Unit,
    onStopCommand: () -> Unit = {},
    onSaveOutput: (String) -> Unit = {},
    onShareOutput: () -> Unit = {},
    onBookmarkCommand: (String, String) -> Unit = { _, _ -> },
    bookmarkedCommands: List<BookmarkedCommand> = emptyList(),
    onUseBookmarkedCommand: (String) -> Unit = {},
    onClose: () -> Unit,
    useNativeImplementation: Boolean = false,
    onToggleNativeImplementation: (Boolean) -> Unit = {}
) {
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
                            text = "Rust Native Terminal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "High-performance terminal with native Rust implementation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row {
                        // Native implementation toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Native Mode",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Switch(
                                checked = useNativeImplementation,
                                onCheckedChange = onToggleNativeImplementation
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
            }
            
            // Terminal content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Show experimental badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Experimental",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                // Terminal component
                Terminal(
                    modifier = Modifier.fillMaxSize(),
                    terminalSession = terminalSession,
                    terminalOutput = terminalOutput,
                    onCommandExecuted = onCommandExecuted,
                    onNewSession = onNewSession,
                    onCloseSession = onCloseSession,
                    onStopCommand = onStopCommand,
                    onSaveOutput = onSaveOutput,
                    onShareOutput = onShareOutput,
                    onBookmarkCommand = onBookmarkCommand,
                    bookmarkedCommands = bookmarkedCommands,
                    onUseBookmarkedCommand = onUseBookmarkedCommand
                )
            }
            
            // Footer with status
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status: ${if (useNativeImplementation) "Using native Rust implementation" else "Using standard implementation"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Native mode provides better performance but may be less stable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}