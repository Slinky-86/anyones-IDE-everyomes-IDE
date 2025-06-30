package com.anyoneide.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anyoneide.app.core.RustEditorManager
import com.anyoneide.app.model.EditorFile
import com.anyoneide.app.model.SyntaxHighlight
import com.anyoneide.app.ui.theme.customColors
import kotlinx.coroutines.launch
import android.graphics.Paint
import android.graphics.Typeface

/**
 * A Rust-powered code editor component that uses the native Rust implementation
 * for syntax highlighting, code completion, and other editor features.
 */
@Composable
fun RustEditorPanel(
    modifier: Modifier = Modifier,
    file: EditorFile?,
    onContentChanged: (String, String) -> Unit,
    syntaxHighlighting: List<SyntaxHighlight> = emptyList(),
    availableLanguages: List<com.anyoneide.app.core.LanguageConfig> = emptyList(),
    onLanguageChanged: (String, String) -> Unit = { _, _ -> },
    fontSize: Int = 14,
    editorScaleFactor: Float = 1.0f,
    lineNumbersScaleFactor: Float = 0.9f,
    onEditorStateChanged: (String, Int, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    savedScrollPosition: Pair<Int, Int>? = null,
    savedSelectionPosition: Pair<Int, Int>? = null,
    onAiExplainCode: ((String) -> Unit)? = null,
    onAiFixCode: ((String, String) -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // Initialize Rust editor manager
    val rustEditorManager = remember { RustEditorManager(context) }
    var isNativeEditorAvailable by remember { mutableStateOf(false) }
    
    // Check if native editor is available
    LaunchedEffect(Unit) {
        isNativeEditorAvailable = rustEditorManager.isNativeEditorAvailable()
        if (isNativeEditorAvailable) {
            rustEditorManager.initializeEditor()
        }
    }
    
    var textFieldValue by remember(file?.path) { 
        mutableStateOf(TextFieldValue(file?.content ?: ""))
    }
    
    // For AI features
    var showAiDialog by remember { mutableStateOf(false) }
    var aiDialogContent by remember { mutableStateOf("") }
    var aiDialogTitle by remember { mutableStateOf("") }
    var aiDialogLoading by remember { mutableStateOf(false) }
    
    // Scroll states that can be controlled and synchronized
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    // Restore saved scroll position if available
    LaunchedEffect(savedScrollPosition) {
        savedScrollPosition?.let { (vertical, horizontal) ->
            verticalScrollState.scrollTo(vertical)
            horizontalScrollState.scrollTo(horizontal)
        }
    }
    
    // Restore saved selection position if available
    LaunchedEffect(savedSelectionPosition, file?.content) {
        if (savedSelectionPosition != null && file?.content != null) {
            val (start, end) = savedSelectionPosition
            if (start <= file.content.length && end <= file.content.length) {
                textFieldValue = TextFieldValue(
                    text = file.content,
                    selection = androidx.compose.ui.text.TextRange(start, end)
                )
            }
        }
    }
    
    // Update text field value when file content changes
    LaunchedEffect(file?.content) {
        if (textFieldValue.text != (file?.content ?: "")) {
            textFieldValue = TextFieldValue(
                text = file?.content ?: "",
                selection = textFieldValue.selection
            )
        }
    }
    
    // Save editor state when content or selection changes
    LaunchedEffect(textFieldValue) {
        file?.path?.let { path ->
            onEditorStateChanged(
                path,
                verticalScrollState.value,
                horizontalScrollState.value,
                textFieldValue.selection.start,
                textFieldValue.selection.end
            )
        }
    }
    
    // Get syntax highlighting from Rust implementation
    var rustHighlighting by remember { mutableStateOf<List<SyntaxHighlight>>(emptyList()) }
    
    LaunchedEffect(file?.content, file?.language) {
        if (isNativeEditorAvailable && file != null) {
            rustEditorManager.highlightSyntax(file.content, file.language)
                .onSuccess { highlights ->
                    rustHighlighting = highlights
                }
        }
    }
    
    // Use Rust highlighting if available, otherwise use the provided highlighting
    val effectiveHighlighting = if (isNativeEditorAvailable && rustHighlighting.isNotEmpty()) {
        rustHighlighting
    } else {
        syntaxHighlighting
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
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = "Rust-Powered Editor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            
                            Text(
                                text = "Experimental native implementation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Row {
                        // Status badge
                        Surface(
                            color = if (isNativeEditorAvailable) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = if (isNativeEditorAvailable) "Native" else "Fallback",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
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
            
            // File info bar with language selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language selector
                    if (availableLanguages.isNotEmpty() && file != null) {
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = file.language.uppercase(),
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier
                                    .width(120.dp)
                                    .menuAnchor(),
                                textStyle = MaterialTheme.typography.labelSmall,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                availableLanguages.forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(language.name) },
                                        onClick = { 
                                            onLanguageChanged(file.path, language.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = file?.language?.uppercase() ?: "KOTLIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Syntax highlighting indicator
                    if (effectiveHighlighting.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Syntax Highlighting Active",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Line count
                    Text(
                        text = "${textFieldValue.text.lines().size} lines",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Cursor position
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ln ${getCursorLine(textFieldValue)}, Col ${getCursorColumn(textFieldValue)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Native editor badge
                    if (isNativeEditorAvailable) {
                        Text(
                            text = "Using Rust engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Integrated editor with line numbers
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Integrated editor with line numbers drawn directly in the editor
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue -> 
                        textFieldValue = newValue
                        file?.let { currentFile ->
                            onContentChanged(currentFile.path, newValue.text)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                        .drawBehind { 
                            // Draw line numbers and background
                            drawLineNumbers(
                                text = textFieldValue.text,
                                fontSize = (fontSize * lineNumbersScaleFactor).toInt(),
                                lineHeight = (fontSize * editorScaleFactor * 1.4).toInt()
                            )
                        }
                        .clickable(enabled = false) { /* Prevent click from propagating */ },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = (fontSize * editorScaleFactor).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = (fontSize * editorScaleFactor * 1.4).sp
                    ),
                    decorationBox = { innerTextField ->
                        Row {
                            // Add padding for line numbers
                            Spacer(modifier = Modifier.width(40.dp))
                            
                            // Actual text content with syntax highlighting
                            Box {
                                if (effectiveHighlighting.isNotEmpty()) {
                                    // Render syntax highlighted text
                                    Text(
                                        text = buildSyntaxHighlightedText(textFieldValue.text, effectiveHighlighting),
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = (fontSize * editorScaleFactor).sp,
                                            lineHeight = (fontSize * editorScaleFactor * 1.4).sp
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                // Transparent text field for input
                                SelectionContainer {
                                    Box {
                                        innerTextField()
                                    }
                                }
                            }
                        }
                    }
                )
                
                // Experimental feature badge
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Experimental",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            // Bottom toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Editor features
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Format code button
                        if (isNativeEditorAvailable) {
                            TextButton(
                                onClick = {
                                    file?.let { currentFile ->
                                        coroutineScope.launch {
                                            rustEditorManager.formatCode(currentFile.content, currentFile.language)
                                                .onSuccess { formattedCode ->
                                                    onContentChanged(currentFile.path, formattedCode)
                                                }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatAlignLeft,
                                    contentDescription = "Format Code",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Format")
                            }
                        }
                        
                        // Code structure button
                        if (isNativeEditorAvailable) {
                            TextButton(
                                onClick = {
                                    // Show code structure
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountTree,
                                    contentDescription = "Code Structure",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Structure")
                            }
                        }
                        
                        // Find references button
                        if (isNativeEditorAvailable) {
                            TextButton(
                                onClick = {
                                    // Find references
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FindReplace,
                                    contentDescription = "Find References",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("References")
                            }
                        }
                    }
                    
                    // Status
                    Text(
                        text = if (isNativeEditorAvailable) 
                            "Rust Editor: Active" 
                        else 
                            "Rust Editor: Fallback Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isNativeEditorAvailable) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    // AI Dialog
    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text(aiDialogTitle) },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (aiDialogLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Text(aiDialogContent)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAiDialog = false }
                ) {
                    Text("Close")
                }
            }
        )
    }
}

// Draw line numbers directly in the editor
private fun DrawScope.drawLineNumbers(
    text: String,
    fontSize: Int,
    lineHeight: Int
) {
    val lines = text.lines()
    val lineNumberWidth = 40.dp.toPx()
    val lineNumberColor = Color(0xFF858585) // Line number color
    val lineNumberBackground = Color(0xFF1E1E1E).copy(alpha = 0.1f) // Subtle background for line numbers
    
    // Draw background for line numbers area
    drawRect(
        color = lineNumberBackground,
        topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(lineNumberWidth, size.height)
    )
    
    // Draw vertical separator line
    drawLine(
        color = lineNumberColor.copy(alpha = 0.3f),
        start = Offset(lineNumberWidth, 0f),
        end = Offset(lineNumberWidth, size.height),
        strokeWidth = 1f
    )
    
    // Draw line numbers
    val fontSizePx = fontSize.sp.toPx()
    val lineHeightPx = lineHeight.sp.toPx()
    
    lines.forEachIndexed { index, _ ->
        val y = (index * lineHeightPx) + fontSizePx + 4.dp.toPx() // Adjust for baseline
        
        // Draw line number using Paint
        val paint = Paint().apply {
            color = lineNumberColor.toArgb()
            textSize = fontSizePx
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
        }
        
        drawContext.canvas.nativeCanvas.drawText(
            "${index + 1}",
            lineNumberWidth - 8.dp.toPx(), // Right-align with padding
            y,
            paint
        )
    }
}

// Build annotated string with syntax highlighting
@Composable
private fun buildSyntaxHighlightedText(text: String, highlights: List<SyntaxHighlight>): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        
        highlights.forEach { highlight ->
            if (highlight.start < text.length && highlight.end <= text.length && highlight.start < highlight.end) {
                addStyle(
                    style = SpanStyle(color = getSyntaxHighlightColor(highlight.type)),
                    start = highlight.start,
                    end = highlight.end
                )
            }
        }
    }
}

@Composable
private fun getSyntaxHighlightColor(type: String): Color {
    return when (type) {
        "keyword" -> MaterialTheme.customColors.syntaxKeyword
        "string" -> MaterialTheme.customColors.syntaxString
        "comment" -> MaterialTheme.customColors.syntaxComment
        "number" -> MaterialTheme.customColors.syntaxNumber
        "function" -> MaterialTheme.customColors.syntaxFunction
        "type" -> MaterialTheme.customColors.syntaxType
        "variable" -> MaterialTheme.customColors.syntaxVariable
        "operator" -> MaterialTheme.customColors.syntaxOperator
        "bracket" -> MaterialTheme.customColors.syntaxOperator
        else -> MaterialTheme.colorScheme.onSurface
    }
}

// Helper function to get cursor line number
private fun getCursorLine(textFieldValue: TextFieldValue): Int {
    val text = textFieldValue.text
    val cursorPosition = textFieldValue.selection.start
    if (cursorPosition > text.length) return 1
    
    return text.substring(0, cursorPosition).count { it == '\n' } + 1
}

// Helper function to get cursor column
private fun getCursorColumn(textFieldValue: TextFieldValue): Int {
    val text = textFieldValue.text
    val cursorPosition = textFieldValue.selection.start
    if (cursorPosition > text.length) return 1
    
    val lastNewlineIndex = text.substring(0, cursorPosition).lastIndexOf('\n')
    return if (lastNewlineIndex == -1) {
        cursorPosition + 1
    } else {
        cursorPosition - lastNewlineIndex
    }
}