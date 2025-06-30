package com.anyoneide.app.core

import android.content.Context
import java.util.regex.Pattern

class AutoIndentationEngine(
    private val context: Context,
    private val languageSupport: LanguageSupport
) {
    
    fun calculateIndentation(
        text: String,
        cursorPosition: Int,
        languageId: String,
        tabSize: Int = 4,
        useSpaces: Boolean = true
    ): IndentationResult {
        val languageConfig = languageSupport.getLanguageConfigById(languageId)
            ?: return IndentationResult(0, "")
        
        val lines = text.substring(0, cursorPosition).split('\n')
        val currentLineIndex = lines.size - 1
        val currentLine = lines.lastOrNull() ?: ""
        val previousLine = if (currentLineIndex > 0) lines[currentLineIndex - 1] else ""
        
        val baseIndent = calculateBaseIndentation(previousLine, tabSize)
        val additionalIndent = calculateAdditionalIndentation(
            previousLine, 
            currentLine, 
            languageConfig, 
            tabSize
        )
        
        val totalIndent = baseIndent + additionalIndent
        val indentString = if (useSpaces) {
            " ".repeat(totalIndent)
        } else {
            "\t".repeat(totalIndent / tabSize) + " ".repeat(totalIndent % tabSize)
        }
        
        return IndentationResult(totalIndent, indentString)
    }
    
    fun autoIndentOnNewLine(
        text: String,
        cursorPosition: Int,
        languageId: String,
        tabSize: Int = 4,
        useSpaces: Boolean = true
    ): String {
        val indentResult = calculateIndentation(text, cursorPosition, languageId, tabSize, useSpaces)
        return "\n${indentResult.indentString}"
    }
    
    fun autoIndentOnClosingBracket(
        text: String,
        cursorPosition: Int,
        closingBracket: String,
        languageId: String,
        tabSize: Int = 4,
        useSpaces: Boolean = true
    ): String {
        val languageConfig = languageSupport.getLanguageConfigById(languageId)
            ?: return closingBracket
        
        val bracketPair = languageConfig.brackets.find { bracket -> bracket.close == closingBracket }
            ?: return closingBracket
        
        val lines = text.substring(0, cursorPosition).split('\n')
        val currentLine = lines.lastOrNull() ?: ""
        
        // Find matching opening bracket
        val openingBracketIndent = findMatchingBracketIndentation(
            text, 
            cursorPosition, 
            bracketPair.open, 
            bracketPair.close,
            tabSize
        )
        
        val currentIndent = getLineIndentation(currentLine, tabSize)
        
        return if (openingBracketIndent >= 0 && currentIndent > openingBracketIndent) {
            val targetIndent = openingBracketIndent
            val indentString = if (useSpaces) {
                " ".repeat(targetIndent)
            } else {
                "\t".repeat(targetIndent / tabSize) + " ".repeat(targetIndent % tabSize)
            }
            
            // Apply proper indentation and add closing bracket
            "\n$indentString$closingBracket"
        } else {
            closingBracket
        }
    }
    
    fun indentSelection(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        tabSize: Int = 4,
        useSpaces: Boolean = true,
        indent: Boolean = true
    ): String {
        val lines = text.split('\n')
        val startLineIndex = text.substring(0, selectionStart).count { it == '\n' }
        val endLineIndex = text.substring(0, selectionEnd).count { it == '\n' }
        
        val indentString = if (useSpaces) {
            " ".repeat(tabSize)
        } else {
            "\t"
        }
        
        val modifiedLines = lines.mapIndexed { index, line ->
            when {
                index < startLineIndex || index > endLineIndex -> line
                indent -> indentString + line
                else -> {
                    // Unindent
                    when {
                        line.startsWith(indentString) -> line.substring(indentString.length)
                        line.startsWith("\t") -> line.substring(1)
                        line.startsWith(" ") -> {
                            val spacesToRemove = minOf(tabSize, line.takeWhile { it == ' ' }.length)
                            line.substring(spacesToRemove)
                        }
                        else -> line
                    }
                }
            }
        }
        
        return modifiedLines.joinToString("\n")
    }
    
    private fun calculateBaseIndentation(line: String, tabSize: Int): Int {
        var indent = 0
        for (char in line) {
            when (char) {
                ' ' -> indent++
                '\t' -> indent += tabSize
                else -> break
            }
        }
        return indent
    }
    
    private fun calculateAdditionalIndentation(
        previousLine: String,
        currentLine: String,
        languageConfig: LanguageConfig,
        tabSize: Int
    ): Int {
        val trimmedPrevious = previousLine.trim()
        
        // Check for increase indent patterns
        val increasePattern = Pattern.compile(languageConfig.indentationRules.increaseIndentPattern)
        if (increasePattern.matcher(trimmedPrevious).find()) {
            return tabSize
        }
        
        // Check for decrease indent patterns
        val decreasePattern = Pattern.compile(languageConfig.indentationRules.decreaseIndentPattern)
        if (decreasePattern.matcher(currentLine).find()) {
            return -tabSize
        }
        
        // Language-specific rules
        when (languageConfig.id) {
            "python" -> {
                if (trimmedPrevious.endsWith(":")) {
                    return tabSize
                }
            }
            "kotlin", "java" -> {
                if (trimmedPrevious.endsWith("{") || trimmedPrevious.endsWith("(") || trimmedPrevious.endsWith("[")) {
                    return tabSize
                }
                if (currentLine.startsWith("}") || currentLine.startsWith(")") || currentLine.startsWith("]")) {
                    return -tabSize
                }
            }
            "xml" -> {
                if (trimmedPrevious.matches(Regex("<[^/>]*>\\s*"))) {
                    return tabSize
                }
                if (currentLine.matches(Regex("</.*>\\s*"))) {
                    return -tabSize
                }
            }
        }
        
        return 0
    }
    
    private fun findMatchingBracketIndentation(
        text: String,
        position: Int,
        openBracket: String,
        closeBracket: String,
        tabSize: Int
    ): Int {
        var depth = 0
        var i = position - 1
        
        while (i >= 0) {
            val char = text[i].toString()
            when (char) {
                closeBracket -> depth++
                openBracket -> {
                    if (depth == 0) {
                        // Found matching opening bracket
                        val lineStart = text.lastIndexOf('\n', i) + 1
                        val line = text.substring(lineStart, text.indexOf('\n', i).takeIf { it != -1 } ?: text.length)
                        return getLineIndentation(line, tabSize)
                    }
                    depth--
                }
            }
            i--
        }
        
        return -1 // No matching bracket found
    }
    
    private fun getLineIndentation(line: String, tabSize: Int): Int {
        var indent = 0
        for (char in line) {
            when (char) {
                ' ' -> indent++
                '\t' -> indent += tabSize
                else -> break
            }
        }
        return indent
    }
}

data class IndentationResult(
    val indentLevel: Int,
    val indentString: String
)