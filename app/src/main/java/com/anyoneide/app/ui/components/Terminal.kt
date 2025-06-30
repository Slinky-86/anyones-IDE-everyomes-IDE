package com.anyoneide.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyoneide.app.data.model.BookmarkedCommand
import com.anyoneide.app.model.TerminalOutput
import com.anyoneide.app.model.TerminalSession
import com.anyoneide.app.core.TerminalOutputType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Terminal(
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
    onUseBookmarkedCommand: (String) -> Unit = {}
) {
    var currentCommand by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<String>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var bookmarkDescription by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showBookmarksPanel by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveFileName by remember { mutableStateOf("terminal_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt") }
    
    // Auto-scroll to bottom when new output arrives
    LaunchedEffect(terminalOutput.size) {
        if (terminalOutput.isNotEmpty()) {
            listState.animateScrollToItem(terminalOutput.size - 1)
        }
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0C0C0C) // True terminal black
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Terminal header with enhanced info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E1E1E)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal",
                                tint = Color(0xFF00FF00),
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Column {
                                Text(
                                    text = if (terminalSession != null) {
                                        "Terminal - ${terminalSession.workingDirectory.substringAfterLast("/")}"
                                    } else {
                                        "Terminal"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                
                                if (terminalSession != null) {
                                    Text(
                                        text = "Session: ${terminalSession.sessionId.take(8)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }
                        }
                        
                        Row {
                            // Search button
                            IconButton(
                                onClick = { showSearchBar = !showSearchBar },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Terminal",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            // Save output button
                            IconButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save Terminal Output",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            // Share output button
                            IconButton(
                                onClick = { onShareOutput() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Terminal Output",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            // Bookmarks button
                            IconButton(
                                onClick = { showBookmarksPanel = !showBookmarksPanel },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = "Command Bookmarks",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            // Clear terminal button
                            IconButton(
                                onClick = { onCommandExecuted("clear") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClearAll,
                                    contentDescription = "Clear Terminal",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            // Stop command button (only visible when a command is running)
                            if (terminalSession?.isActive == true) {
                                IconButton(
                                    onClick = { onStopCommand() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Command",
                                        tint = Color(0xFFFF6B6B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = onNewSession,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Session",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            if (terminalSession != null) {
                                IconButton(
                                    onClick = onCloseSession,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Session",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Search bar (only visible when search is active)
                    AnimatedVisibility(visible = showSearchBar) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search in terminal output", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FF00),
                                    unfocusedBorderColor = Color.Gray
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear search",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                },
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Previous match button
                            IconButton(onClick = { /* Navigate to previous match */ }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous match",
                                    tint = Color.White
                                )
                            }
                            
                            // Next match button
                            IconButton(onClick = { /* Navigate to next match */ }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next match",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (terminalSession == null) {
                // No session state with enhanced welcome
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF00FF00)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Anyone IDE Terminal",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Enhanced Linux-like terminal environment",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF888888)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onNewSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF00),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Terminal Session")
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Feature highlights
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Features:",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF00FF00)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val features = listOf(
                                "• Real shell command execution",
                                "• APT package management simulation",
                                "• Git version control support",
                                "• Gradle build tool integration",
                                "• ADB Android debugging",
                                "• File system operations",
                                "• Command history and completion",
                                "• SDK component installation"
                            )
                            
                            features.forEach { feature ->
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCCCCCC),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Main content area with terminal output and bookmarks panel
                Row(modifier = Modifier.weight(1f)) {
                    // Terminal content
                    Box(
                        modifier = Modifier
                            .weight(if (showBookmarksPanel) 0.7f else 1f)
                            .fillMaxHeight()
                    ) {
                        // Output area with enhanced styling
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            state = listState
                        ) {
                            // Welcome message for new sessions
                            if (terminalOutput.isEmpty()) {
                                item {
                                    Column {
                                        Text(
                                            text = "Welcome to Anyone IDE Terminal",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp,
                                                color = Color(0xFF00FF00)
                                            ),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        
                                        Text(
                                            text = "Type 'help' for available commands or try:",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = Color(0xFF888888)
                                            ),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                        
                                        val suggestions = listOf(
                                            "ls -la", "pwd", "apt update", "git status", 
                                            "gradle tasks", "adb devices", "ps", "uname -a",
                                            "sdk-install android-sdk", "sdk-install openjdk-11"
                                        )
                                        
                                        suggestions.forEach { cmd ->
                                            Text(
                                                text = "  $cmd",
                                                style = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF4ECDC4)
                                                ),
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            
                            items(terminalOutput) { output ->
                                TerminalOutputLine(
                                    output = output,
                                    searchQuery = if (showSearchBar) searchQuery else ""
                                )
                            }
                        }
                    }
                    
                    // Bookmarks panel (conditionally visible)
                    if (showBookmarksPanel) {
                        Surface(
                            modifier = Modifier
                                .weight(0.3f)
                                .fillMaxHeight(),
                            color = Color(0xFF1A1A1A)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                // Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Command Bookmarks",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White
                                    )
                                    
                                    IconButton(
                                        onClick = { showBookmarksPanel = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Bookmarks",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                // Search field
                                OutlinedTextField(
                                    value = "",
                                    onValueChange = { },
                                    placeholder = { Text("Search bookmarks", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color.White,
                                        focusedBorderColor = Color(0xFF00FF00),
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    },
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Bookmarks list
                                LazyColumn(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(bookmarkedCommands) { bookmark ->
                                        BookmarkedCommandItem(
                                            bookmark = bookmark,
                                            onClick = { onUseBookmarkedCommand(bookmark.command) }
                                        )
                                    }
                                    
                                    if (bookmarkedCommands.isEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No bookmarked commands yet.\nBookmark frequently used commands for quick access.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Command input area with enhanced features
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1E1E)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Enhanced prompt with working directory
                        val workingDir = terminalSession.workingDirectory.substringAfterLast("/")
                        Text(
                            text = "anyoneide@terminal:~/$workingDir$ ",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = Color(0xFF00FF00)
                            )
                        )
                        
                        BasicTextField(
                            value = currentCommand,
                            onValueChange = { 
                                currentCommand = it
                                historyIndex = -1
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (currentCommand.isNotBlank()) {
                                        commandHistory = commandHistory + currentCommand
                                        onCommandExecuted(currentCommand)
                                        currentCommand = ""
                                        historyIndex = -1
                                    }
                                }
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (currentCommand.isEmpty()) {
                                        Text(
                                            text = "Enter command... (↑/↓ for history)",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp,
                                                color = Color(0xFF555555)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        // Bookmark button
                        IconButton(
                            onClick = { 
                                if (currentCommand.isNotBlank()) {
                                    bookmarkDescription = ""
                                    showBookmarkDialog = true
                                }
                            },
                            modifier = Modifier.size(32.dp),
                            enabled = currentCommand.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "Bookmark Command",
                                tint = if (currentCommand.isNotBlank()) Color(0xFFFFD700) else Color(0xFF555555),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        // Command history navigation
                        IconButton(
                            onClick = {
                                if (commandHistory.isNotEmpty()) {
                                    historyIndex = if (historyIndex == -1) {
                                        commandHistory.size - 1
                                    } else {
                                        (historyIndex - 1).coerceAtLeast(0)
                                    }
                                    currentCommand = commandHistory[historyIndex]
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Previous Command",
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                if (historyIndex < commandHistory.size - 1) {
                                    historyIndex++
                                    currentCommand = commandHistory[historyIndex]
                                } else {
                                    historyIndex = -1
                                    currentCommand = ""
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Next Command",
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = {
                                if (currentCommand.isNotBlank()) {
                                    commandHistory = commandHistory + currentCommand
                                    onCommandExecuted(currentCommand)
                                    currentCommand = ""
                                    historyIndex = -1
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Execute Command",
                                tint = Color(0xFF00FF00),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Bookmark dialog
    if (showBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("Bookmark Command") },
            text = {
                Column {
                    Text("Command: $currentCommand")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bookmarkDescription,
                        onValueChange = { bookmarkDescription = it },
                        label = { Text("Description") },
                        placeholder = { Text("Enter a description for this command") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBookmarkCommand(currentCommand, bookmarkDescription)
                        showBookmarkDialog = false
                    }
                ) {
                    Text("Bookmark")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBookmarkDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Save dialog
    if (showSaveDialog) {
        val context = LocalContext.current
        
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Terminal Output") },
            text = {
                Column {
                    Text("Save terminal output to a file:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saveFileName,
                        onValueChange = { saveFileName = it },
                        label = { Text("Filename") },
                        placeholder = { Text("terminal_output.txt") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File will be saved to: ${context.getExternalFilesDir(null)?.absolutePath ?: "External storage"}/terminal_logs/",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveOutput(saveFileName)
                        showSaveDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TerminalOutputLine(
    output: TerminalOutput,
    searchQuery: String = ""
) {
    val (textColor, backgroundColor) = when (output.outputType) {
        "Stdout" -> Color(0xFFCCCCCC) to Color.Transparent
        "Stderr" -> Color(0xFFFF6B6B) to Color.Transparent
        "Command" -> Color(0xFF4ECDC4) to Color.Transparent
        "System" -> Color(0xFFFFE66D) to Color.Transparent
        "Error" -> Color(0xFFFF6B6B) to Color(0x22FF6B6B)
        "Clear" -> Color.Transparent to Color.Transparent
        else -> Color(0xFFCCCCCC) to Color.Transparent
    }

    if (output.outputType == "Clear") {
        // Handle clear command - this would be handled by the parent
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 1.dp, horizontal = if (backgroundColor != Color.Transparent) 4.dp else 0.dp)
    ) {
        // Add timestamp for system messages
        if (output.outputType == "System" || output.outputType == "Error") {
            Text(
                text = "[${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(output.timestamp))}] ",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
            )
        }
        
        SelectionContainer {
            if (searchQuery.isNotEmpty() && output.content.contains(searchQuery, ignoreCase = true)) {
                // Highlighted text with search results
                Text(
                    text = buildHighlightedText(output.content, searchQuery),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Regular text
                Text(
                    text = output.content,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BookmarkedCommandItem(
    bookmark: BookmarkedCommand,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorite star
            Icon(
                imageVector = if (bookmark.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = if (bookmark.isFavorite) "Favorite" else "Not favorite",
                tint = if (bookmark.isFavorite) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bookmark.command,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF4ECDC4)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (bookmark.description.isNotEmpty()) {
                    Text(
                        text = bookmark.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (bookmark.useCount > 0) {
                Text(
                    text = "${bookmark.useCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// Build highlighted text for search results
private fun buildHighlightedText(text: String, query: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        
        val startIndexes = mutableListOf<Int>()
        var startIndex = text.indexOf(query, ignoreCase = true)
        while (startIndex >= 0) {
            startIndexes.add(startIndex)
            startIndex = text.indexOf(query, startIndex + 1, ignoreCase = true)
        }
        
        startIndexes.forEach { index ->
            addStyle(
                style = SpanStyle(
                    background = Color(0xFF3B3B00),
                    color = Color.Yellow
                ),
                start = index,
                end = index + query.length
            )
        }
    }
}