package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.GeminiApiService
import com.anyoneide.app.core.GeminiResponse
import kotlinx.coroutines.launch
import com.anyoneide.app.viewmodel.EnhancedMainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantPanel(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onInsertCode: (String) -> Unit,
    viewModel: EnhancedMainViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var prompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var conversation by remember { mutableStateOf(listOf<ChatMessage>()) }
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(conversation.size) {
        if (conversation.isNotEmpty()) {
            listState.animateScrollToItem(conversation.size - 1)
        }
    }
    
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = "AI Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Powered by Google Gemini",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            }
            
            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Welcome message if no conversation yet
                if (conversation.isEmpty()) {
                    item {
                        WelcomeMessage()
                    }
                }
                
                // Conversation messages
                items(conversation) { message ->
                    ChatMessageItem(
                        message = message,
                        onInsertCode = onInsertCode
                    )
                }
                
                // Loading indicator
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Thinking...")
                                }
                            }
                        }
                    }
                }
            }
            
            // Quick prompts
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Quick Prompts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { prompt = "Generate a RecyclerView adapter for a list of users" },
                            label = { Text("RecyclerView adapter") }
                        )
                        
                        SuggestionChip(
                            onClick = { prompt = "Create a Room database entity for a Todo item" },
                            label = { Text("Room entity") }
                        )
                        
                        SuggestionChip(
                            onClick = { prompt = "Write a Kotlin extension function to format dates" },
                            label = { Text("Date formatter") }
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { prompt = "Create a Compose UI for a login screen" },
                            label = { Text("Login UI") }
                        )
                        
                        SuggestionChip(
                            onClick = { prompt = "Write a ViewModel for user authentication" },
                            label = { Text("Auth ViewModel") }
                        )
                        
                        SuggestionChip(
                            onClick = { prompt = "Generate a Retrofit API interface" },
                            label = { Text("Retrofit API") }
                        )
                    }
                }
            }
            
            // Input area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Ask for code, explanations, or help with errors...") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (prompt.isNotEmpty()) {
                                // Add user message to conversation
                                conversation = conversation + ChatMessage(
                                    content = prompt,
                                    isUser = true
                                )
                                
                                // Get response from Gemini
                                coroutineScope.launch {
                                    isLoading = true

                                    // Use viewModel if available, otherwise use direct service
                                    val responseFlow = viewModel?.generateCode(prompt, "kotlin") 
                                        ?: GeminiApiService(context).generateCode(prompt, "kotlin")
                                        
                                    responseFlow.collect { response ->
                                        when (response) {
                                            is GeminiResponse.Success -> {
                                                conversation = conversation + ChatMessage(
                                                    content = response.content,
                                                    isUser = false
                                                )
                                                isLoading = false
                                            }
                                            is GeminiResponse.Error -> {
                                                conversation = conversation + ChatMessage(
                                                    content = "Error: ${response.message}",
                                                    isUser = false,
                                                    isError = true
                                                )
                                                isLoading = false
                                            }
                                            is GeminiResponse.Loading -> {
                                                // Already showing loading indicator
                                            }
                                        }
                                    }
                                    
                                    // Clear prompt after sending
                                    prompt = ""
                                }
                            }
                        },
                        enabled = prompt.isNotEmpty() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeMessage() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Welcome to AI Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "I can help you with:",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BulletPoint("Generating code snippets")
                BulletPoint("Explaining code concepts")
                BulletPoint("Fixing errors and bugs")
                BulletPoint("Suggesting optimizations")
                BulletPoint("Answering programming questions")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Try asking me to generate some code or explain a concept!",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onInsertCode: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.isError -> MaterialTheme.colorScheme.errorContainer
                    message.isUser -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Message content
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // If message contains code blocks, add insert button
                if (!message.isUser && extractCodeBlocks(message.content).isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val code = extractCodeBlocks(message.content).joinToString("\n\n")
                                onInsertCode(code)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Insert Code")
                        }
                    }
                }
            }
        }
    }
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// Function to extract code blocks from markdown text
private fun extractCodeBlocks(text: String): List<String> {
    val codeBlocks = mutableListOf<String>()
    val regex = "```(?:\\w+)?\\s*([\\s\\S]*?)```".toRegex()
    
    regex.findAll(text).forEach { matchResult ->
        codeBlocks.add(matchResult.groupValues[1].trim())
    }
    
    return codeBlocks
}