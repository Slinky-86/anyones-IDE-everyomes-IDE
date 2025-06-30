package com.anyoneide.app.core

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import com.anyoneide.app.model.CompletionItem
import com.anyoneide.app.model.SyntaxHighlight

class EditorManager(private val context: Context) {
    
    private val syntaxHighlighter = SyntaxHighlighter()
    private val openEditors = ConcurrentHashMap<String, EditorInstance>()
    private val highlightJobs = ConcurrentHashMap<String, Job>()
    
    fun createEditor(filePath: String, content: String, language: String): EditorInstance {
        val editor = EditorInstance(
            filePath = filePath,
            content = content,
            language = language,
            isModified = false,
            cursorPosition = 0,
            selectionStart = 0,
            selectionEnd = 0
        )
        
        openEditors[filePath] = editor
        return editor
    }
    
    fun getEditor(filePath: String): EditorInstance? {
        return openEditors[filePath]
    }
    
    fun closeEditor(filePath: String) {
        highlightJobs[filePath]?.cancel()
        highlightJobs.remove(filePath)
        openEditors.remove(filePath)
    }
    
    fun updateContent(filePath: String, content: String) {
        val editor = openEditors[filePath] ?: return
        editor.content = content
        editor.isModified = true
        
        // Cancel previous highlighting job
        highlightJobs[filePath]?.cancel()
        
        // Start new highlighting job with debounce
        highlightJobs[filePath] = CoroutineScope(Dispatchers.Main).launch {
            delay(300) // Debounce for 300ms
            
            val highlighted = syntaxHighlighter.highlightCode(content, editor.language)
            editor.highlightedContent = highlighted
            editor.onContentHighlighted?.invoke(highlighted)
        }
    }
    
    fun setupTextWatcher(editText: EditText, filePath: String): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { updateContent(filePath, it.toString()) }
            }
            
            override fun afterTextChanged(s: Editable?) {
                val editor = openEditors[filePath] ?: return
                editor.cursorPosition = editText.selectionStart
                editor.selectionStart = editText.selectionStart
                editor.selectionEnd = editText.selectionEnd
            }
        }
    }
    
    fun getAutoCompletions(filePath: String, position: Int): List<CompletionItem> {
        val editor = openEditors[filePath] ?: return emptyList()
        
        // Get current line and column
        val lines = editor.content.substring(0, position).split('\n')
        val line = lines.size - 1
        val column = lines.lastOrNull()?.length ?: 0
        
        // Get word at cursor
        val currentLine = editor.content.lines().getOrNull(line) ?: ""
        val wordStart = findWordStart(currentLine, column)
        val wordEnd = findWordEnd(currentLine, column)
        val currentWord = currentLine.substring(wordStart, wordEnd)
        
        return generateCompletions(editor.language, currentWord, currentLine)
    }
    
    private fun findWordStart(line: String, position: Int): Int {
        var start = position
        while (start > 0 && (line[start - 1].isLetterOrDigit() || line[start - 1] == '_')) {
            start--
        }
        return start
    }
    
    private fun findWordEnd(line: String, position: Int): Int {
        var end = position
        while (end < line.length && (line[end].isLetterOrDigit() || line[end] == '_')) {
            end++
        }
        return end
    }
    
    private fun generateCompletions(language: String, currentWord: String, currentLine: String): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()
        
        when (language.lowercase()) {
            "java" -> {
                // Java completions
                if (currentWord.isNotEmpty()) {
                    javaCompletions.filter { it.label.startsWith(currentWord, ignoreCase = true) }
                        .forEach { completions.add(it) }
                }
                
                // Context-specific completions
                if (currentLine.contains("System.")) {
                    completions.addAll(systemCompletions)
                }
            }
            "kotlin" -> {
                // Kotlin completions
                if (currentWord.isNotEmpty()) {
                    kotlinCompletions.filter { it.label.startsWith(currentWord, ignoreCase = true) }
                        .forEach { completions.add(it) }
                }
            }
            "xml" -> {
                // XML completions
                if (currentLine.contains("android:")) {
                    completions.addAll(androidAttributeCompletions)
                }
            }
        }
        
        return completions.take(20) // Limit to 20 suggestions
    }
    
    companion object {
        private val javaCompletions = listOf(
            CompletionItem("System.out.println", "method", "Print to console", null,
                "System.out.println($1);"
            ),
            CompletionItem("String", "class", "String class", null, "String"),
            CompletionItem("ArrayList", "class", "ArrayList class", null, "ArrayList<$1>"),
            CompletionItem("HashMap", "class", "HashMap class", null, "HashMap<$1, $2>"),
            CompletionItem("public", "keyword", "Public modifier", null, "public"),
            CompletionItem("private", "keyword", "Private modifier", null, "private"),
            CompletionItem("protected", "keyword", "Protected modifier", null, "protected"),
            CompletionItem("static", "keyword", "Static modifier", null, "static"),
            CompletionItem("final", "keyword", "Final modifier", null, "final"),
            CompletionItem("class", "keyword", "Class declaration", null, "class $1 {\n\t$2\n}")
        )
        
        private val kotlinCompletions = listOf(
            CompletionItem("println", "function", "Print to console", null, "println($1)"),
            CompletionItem("fun", "keyword", "Function declaration", null,
                "fun $1($2): $3 {\n\t$4\n}"
            ),
            CompletionItem("class", "keyword", "Class declaration", null, "class $1 {\n\t$2\n}"),
            CompletionItem("data class", "snippet", "Data class", null, "data class $1($2)"),
            CompletionItem("val", "keyword", "Immutable property", null, "val $1 = $2"),
            CompletionItem("var", "keyword", "Mutable property", null, "var $1 = $2"),
            CompletionItem("when", "keyword", "When expression", null,
                "when ($1) {\n\t$2 -> $3\n\telse -> $4\n}"
            ),
            CompletionItem("if", "keyword", "If statement", null, "if ($1) {\n\t$2\n}"),
            CompletionItem("for", "keyword", "For loop", null, "for ($1 in $2) {\n\t$3\n}"),
            CompletionItem("while", "keyword", "While loop", null, "while ($1) {\n\t$2\n}")
        )
        
        private val systemCompletions = listOf(
            CompletionItem("out.println", "method", "Print line", null, "out.println($1)"),
            CompletionItem("out.print", "method", "Print", null, "out.print($1)"),
            CompletionItem("err.println", "method", "Print error", null, "err.println($1)"),
            CompletionItem("currentTimeMillis", "method", "Current time", null, "currentTimeMillis()"),
            CompletionItem("exit", "method", "Exit program", null, "exit($1)")
        )
        
        private val androidAttributeCompletions = listOf(
            CompletionItem("layout_width", "property", "Layout width", null,
                "android:layout_width=\"$1\""
            ),
            CompletionItem("layout_height", "property", "Layout height", null,
                "android:layout_height=\"$1\""
            ),
            CompletionItem("text", "property", "Text content", null, "android:text=\"$1\""),
            CompletionItem("id", "property", "View ID", null, "android:id=\"@+id/$1\""),
            CompletionItem("background", "property", "Background", null,
                "android:background=\"$1\""
            ),
            CompletionItem("padding", "property", "Padding", null, "android:padding=\"$1\""),
            CompletionItem("margin", "property", "Margin", null, "android:layout_margin=\"$1\""),
            CompletionItem("gravity", "property", "Gravity", null, "android:gravity=\"$1\""),
            CompletionItem("orientation", "property", "Orientation", null,
                "android:orientation=\"$1\""
            ),
            CompletionItem("visibility", "property", "Visibility", null,
                "android:visibility=\"$1\""
            )
        )
    }
}

data class EditorInstance(
    val filePath: String,
    var content: String,
    val language: String,
    var isModified: Boolean,
    var cursorPosition: Int,
    var selectionStart: Int,
    var selectionEnd: Int,
    var highlightedContent: List<SyntaxHighlight>? = null,
    var onContentHighlighted: ((List<SyntaxHighlight>) -> Unit)? = null
)