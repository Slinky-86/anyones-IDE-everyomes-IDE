package com.anyoneide.app.core

import com.anyoneide.app.model.SyntaxHighlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class SyntaxHighlighter {
    
    private val javaKeywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while"
    )
    
    private val kotlinKeywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
        "var", "when", "while", "by", "catch", "constructor", "delegate",
        "dynamic", "field", "file", "finally", "get", "import", "init",
        "param", "property", "receiver", "set", "setparam", "where",
        "actual", "abstract", "annotation", "companion", "const", "crossinline",
        "data", "enum", "expect", "external", "final", "infix", "inline",
        "inner", "internal", "lateinit", "noinline", "open", "operator",
        "out", "override", "private", "protected", "public", "reified",
        "sealed", "suspend", "tailrec", "vararg"
    )
    
    private val xmlTags = setOf(
        "LinearLayout", "RelativeLayout", "ConstraintLayout", "FrameLayout",
        "TextView", "EditText", "Button", "ImageView", "RecyclerView",
        "ScrollView", "ViewPager", "TabLayout", "AppBarLayout", "Toolbar",
        "FloatingActionButton", "CardView", "ProgressBar", "SeekBar",
        "CheckBox", "RadioButton", "Switch", "Spinner", "WebView"
    )
    
    suspend fun highlightCode(code: String, language: String): List<SyntaxHighlight> = withContext(Dispatchers.Default) {
        when (language.lowercase()) {
            "java" -> highlightJava(code)
            "kotlin" -> highlightKotlin(code)
            "xml" -> highlightXml(code)
            "json" -> highlightJson(code)
            "gradle" -> highlightGradle(code)
            else -> emptyList()
        }
    }
    
    private fun highlightJava(code: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        // Highlight keywords
        highlightKeywords(code, javaKeywords, "keyword", highlights)
        
        // Highlight strings
        highlightPattern(code, "\"([^\"\\\\]|\\\\.)*\"", "string", highlights)
        highlightPattern(code, "'([^'\\\\]|\\\\.)*'", "string", highlights)
        
        // Highlight comments
        highlightPattern(code, "//.*$", "comment", highlights)
        highlightPattern(code, "/\\*[\\s\\S]*?\\*/", "comment", highlights)
        
        // Highlight numbers
        highlightPattern(code, "\\b\\d+(\\.\\d+)?[fFdDlL]?\\b", "number", highlights)
        
        // Highlight annotations
        highlightPattern(code, "@\\w+", "type", highlights)
        
        // Highlight method calls
        highlightPattern(code, "\\b\\w+(?=\\s*\\()", "function", highlights)
        
        return highlights
    }
    
    private fun highlightKotlin(code: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        // Highlight keywords
        highlightKeywords(code, kotlinKeywords, "keyword", highlights)
        
        // Highlight strings
        highlightPattern(code, "\"([^\"\\\\]|\\\\.)*\"", "string", highlights)
        highlightPattern(code, "'([^'\\\\]|\\\\.)*'", "string", highlights)
        highlightPattern(code, "\"\"\"[\\s\\S]*?\"\"\"", "string", highlights) // Triple quoted strings
        
        // Highlight comments
        highlightPattern(code, "//.*$", "comment", highlights)
        highlightPattern(code, "/\\*[\\s\\S]*?\\*/", "comment", highlights)
        
        // Highlight numbers
        highlightPattern(code, "\\b\\d+(\\.\\d+)?[fFdDlL]?\\b", "number", highlights)
        
        // Highlight function declarations
        highlightPattern(code, "\\bfun\\s+(\\w+)", "function", highlights)
        
        // Highlight class/object declarations
        highlightPattern(code, "\\b(class|object|interface)\\s+(\\w+)", "type", highlights)
        
        // Highlight property declarations
        highlightPattern(code, "\\b(val|var)\\s+(\\w+)", "variable", highlights)
        
        return highlights
    }
    
    private fun highlightXml(code: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        // Highlight XML tags
        highlightPattern(code, "</?\\w+", "keyword", highlights)
        highlightPattern(code, "/>", "keyword", highlights)
        highlightPattern(code, ">", "keyword", highlights)
        
        // Highlight attributes
        highlightPattern(code, "\\w+(?=\\s*=)", "type", highlights)
        
        // Highlight attribute values
        highlightPattern(code, "\"[^\"]*\"", "string", highlights)
        highlightPattern(code, "'[^']*'", "string", highlights)
        
        // Highlight comments
        highlightPattern(code, "<!--[\\s\\S]*?-->", "comment", highlights)
        
        // Highlight Android-specific tags
        highlightKeywords(code, xmlTags, "function", highlights)
        
        return highlights
    }
    
    private fun highlightJson(code: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        // Highlight strings (keys and values)
        highlightPattern(code, "\"[^\"]*\"", "string", highlights)
        
        // Highlight numbers
        highlightPattern(code, "\\b\\d+(\\.\\d+)?\\b", "number", highlights)
        
        // Highlight booleans and null
        highlightPattern(code, "\\b(true|false|null)\\b", "keyword", highlights)
        
        // Highlight brackets and braces
        highlightPattern(code, "[\\[\\]{}]", "operator", highlights)
        
        return highlights
    }
    
    private fun highlightGradle(code: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        // Highlight Gradle keywords
        val gradleKeywords = setOf(
            "plugins", "dependencies", "implementation", "api", "testImplementation",
            "androidTestImplementation", "compileOnly", "runtimeOnly", "android",
            "defaultConfig", "buildTypes", "release", "debug", "sourceSets",
            "repositories", "google", "mavenCentral", "jcenter", "maven"
        )
        
        highlightKeywords(code, gradleKeywords, "keyword", highlights)
        
        // Highlight strings
        highlightPattern(code, "\"[^\"]*\"", "string", highlights)
        highlightPattern(code, "'[^']*'", "string", highlights)
        
        // Highlight comments
        highlightPattern(code, "//.*$", "comment", highlights)
        highlightPattern(code, "/\\*[\\s\\S]*?\\*/", "comment", highlights)
        
        // Highlight numbers
        highlightPattern(code, "\\b\\d+(\\.\\d+)?\\b", "number", highlights)
        
        return highlights
    }
    
    private fun highlightKeywords(code: String, keywords: Set<String>, type: String, highlights: MutableList<SyntaxHighlight>) {
        keywords.forEach { keyword ->
            val pattern = Pattern.compile("\\b$keyword\\b")
            val matcher = pattern.matcher(code)
            
            while (matcher.find()) {
                highlights.add(SyntaxHighlight(
                    start = matcher.start(),
                    end = matcher.end(),
                    type = type
                ))
            }
        }
    }
    
    private fun highlightPattern(code: String, regex: String, type: String, highlights: MutableList<SyntaxHighlight>) {
        val pattern = Pattern.compile(regex, Pattern.MULTILINE)
        val matcher = pattern.matcher(code)
        
        while (matcher.find()) {
            highlights.add(SyntaxHighlight(
                start = matcher.start(),
                end = matcher.end(),
                type = type
            ))
        }
    }
}