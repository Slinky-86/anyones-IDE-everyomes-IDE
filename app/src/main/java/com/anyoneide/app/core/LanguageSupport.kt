package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class LanguageSupport(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val languageConfigs = mutableMapOf<String, LanguageConfig>()
    
    init {
        loadBuiltInLanguages()
    }
    
    private fun loadBuiltInLanguages() {
        // Load comprehensive language configurations
        languageConfigs["kotlin"] = LanguageConfig(
            id = "kotlin",
            name = "Kotlin",
            extensions = listOf("kt", "kts"),
            keywords = kotlinKeywords,
            operators = kotlinOperators,
            brackets = listOf(
                BracketPair("{", "}"),
                BracketPair("(", ")"),
                BracketPair("[", "]")
            ),
            commentRules = CommentRules("//", "/*", "*/"),
            indentationRules = IndentationRules(
                increaseIndentPattern = "\\{\\s*$|\\(\\s*$|\\[\\s*$",
                decreaseIndentPattern = "^\\s*\\}|^\\s*\\)|^\\s*\\]"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("{", "}"),
                AutoClosingPair("(", ")"),
                AutoClosingPair("[", "]"),
                AutoClosingPair("\"", "\""),
                AutoClosingPair("'", "'")
            ),
            surroundingPairs = listOf(
                SurroundingPair("{", "}"),
                SurroundingPair("(", ")"),
                SurroundingPair("[", "]"),
                SurroundingPair("\"", "\""),
                SurroundingPair("'", "'")
            )
        )
        
        languageConfigs["java"] = LanguageConfig(
            id = "java",
            name = "Java",
            extensions = listOf("java"),
            keywords = javaKeywords,
            operators = javaOperators,
            brackets = listOf(
                BracketPair("{", "}"),
                BracketPair("(", ")"),
                BracketPair("[", "]")
            ),
            commentRules = CommentRules("//", "/*", "*/"),
            indentationRules = IndentationRules(
                increaseIndentPattern = "\\{\\s*$|\\(\\s*$|\\[\\s*$",
                decreaseIndentPattern = "^\\s*\\}|^\\s*\\)|^\\s*\\]"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("{", "}"),
                AutoClosingPair("(", ")"),
                AutoClosingPair("[", "]"),
                AutoClosingPair("\"", "\"")
            ),
            surroundingPairs = listOf(
                SurroundingPair("{", "}"),
                SurroundingPair("(", ")"),
                SurroundingPair("[", "]"),
                SurroundingPair("\"", "\"")
            )
        )
        
        languageConfigs["xml"] = LanguageConfig(
            id = "xml",
            name = "XML",
            extensions = listOf("xml"),
            keywords = xmlKeywords,
            operators = emptyList(),
            brackets = listOf(
                BracketPair("<", ">"),
                BracketPair("\"", "\""),
                BracketPair("'", "'")
            ),
            commentRules = CommentRules(null, "<!--", "-->"),
            indentationRules = IndentationRules(
                increaseIndentPattern = "<[^/>]*>\\s*$",
                decreaseIndentPattern = "^\\s*</.*>\\s*$"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("<", ">"),
                AutoClosingPair("\"", "\""),
                AutoClosingPair("'", "'")
            ),
            surroundingPairs = listOf(
                SurroundingPair("\"", "\""),
                SurroundingPair("'", "'")
            )
        )
        
        languageConfigs["json"] = LanguageConfig(
            id = "json",
            name = "JSON",
            extensions = listOf("json"),
            keywords = emptyList(),
            operators = emptyList(),
            brackets = listOf(
                BracketPair("{", "}"),
                BracketPair("[", "]"),
                BracketPair("\"", "\"")
            ),
            commentRules = CommentRules(null, null, null),
            indentationRules = IndentationRules(
                increaseIndentPattern = "\\{\\s*$|\\[\\s*$",
                decreaseIndentPattern = "^\\s*\\}|^\\s*\\]"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("{", "}"),
                AutoClosingPair("[", "]"),
                AutoClosingPair("\"", "\"")
            ),
            surroundingPairs = listOf(
                SurroundingPair("{", "}"),
                SurroundingPair("[", "]"),
                SurroundingPair("\"", "\"")
            )
        )
        
        // Add more languages: JavaScript, TypeScript, Python, C++, etc.
        addJavaScriptSupport()
        addTypeScriptSupport()
        addPythonSupport()
        addCppSupport()
        addGradleSupport()
        addPropertiesSupport()
        addMarkdownSupport()
    }
    
    fun getLanguageConfigByExtension(fileExtension: String): LanguageConfig? {
        return languageConfigs.values.find { config ->
            config.extensions.contains(fileExtension.lowercase())
        }
    }
    
    fun getLanguageConfigById(languageId: String): LanguageConfig? {
        return languageConfigs[languageId]
    }
    
    fun getAllLanguages(): List<LanguageConfig> {
        return languageConfigs.values.toList()
    }
    
    suspend fun loadCustomLanguage(languageFile: File): Result<LanguageConfig> = withContext(Dispatchers.IO) {
        try {
            val content = languageFile.readText()
            val config = json.decodeFromString<LanguageConfig>(content)
            languageConfigs[config.id] = config
            Result.success(config)
        } catch (_: Exception) {
            Result.failure(Exception("Failed to load custom language"))
        }
    }
    
    private fun addJavaScriptSupport() {
        languageConfigs["javascript"] = LanguageConfig(
            id = "javascript",
            name = "JavaScript",
            extensions = listOf("js", "jsx", "mjs"),
            keywords = listOf(
                "break", "case", "catch", "class", "const", "continue", "debugger", "default",
                "delete", "do", "else", "export", "extends", "finally", "for", "function",
                "if", "import", "in", "instanceof", "let", "new", "return", "super", "switch",
                "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
                "async", "await", "of"
            ),
            operators = listOf("+", "-", "*", "/", "%", "=", "==", "===", "!=", "!==", "<", ">", "<=", ">=", "&&", "||", "!", "&", "|", "^", "~", "<<", ">>", ">>>"),
            brackets = listOf(
                BracketPair("{", "}"),
                BracketPair("(", ")"),
                BracketPair("[", "]")
            ),
            commentRules = CommentRules("//", "/*", "*/"),
            indentationRules = IndentationRules(
                increaseIndentPattern = "\\{\\s*$|\\(\\s*$|\\[\\s*$",
                decreaseIndentPattern = "^\\s*\\}|^\\s*\\)|^\\s*\\]"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("{", "}"),
                AutoClosingPair("(", ")"),
                AutoClosingPair("[", "]"),
                AutoClosingPair("\"", "\""),
                AutoClosingPair("'", "'"),
                AutoClosingPair("`", "`")
            ),
            surroundingPairs = listOf(
                SurroundingPair("{", "}"),
                SurroundingPair("(", ")"),
                SurroundingPair("[", "]"),
                SurroundingPair("\"", "\""),
                SurroundingPair("'", "'"),
                SurroundingPair("`", "`")
            )
        )
    }
    
    private fun addTypeScriptSupport() {
        languageConfigs["typescript"] = LanguageConfig(
            id = "typescript",
            name = "TypeScript",
            extensions = listOf("ts", "tsx"),
            keywords = listOf(
                "break", "case", "catch", "class", "const", "continue", "debugger", "default",
                "delete", "do", "else", "export", "extends", "finally", "for", "function",
                "if", "import", "in", "instanceof", "let", "new", "return", "super", "switch",
                "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
                "async", "await", "of", "interface", "type", "namespace", "module", "declare",
                "public", "private", "protected", "readonly", "abstract", "static"
            ),
            operators = listOf("+", "-", "*", "/", "%", "=", "==", "===", "!=", "!==", "<", ">", "<=", ">=", "&&", "||", "!", "&", "|", "^", "~", "<<", ">>", ">>>", ":", "?"),
            brackets = listOf(
                BracketPair("{", "}"),
                BracketPair("(", ")"),
                BracketPair("[", "]"),
                BracketPair("<", ">")
            ),
            commentRules = CommentRules("//", "/*", "*/"),
            indentationRules = IndentationRules(
                increaseIndentPattern = "\\{\\s*$|\\(\\s*$|\\[\\s*$",
                decreaseIndentPattern = "^\\s*\\}|^\\s*\\)|^\\s*\\]"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("{", "}"),
                AutoClosingPair("(", ")"),
                AutoClosingPair("[", "]"),
                AutoClosingPair("<", ">"),
                AutoClosingPair("\"", "\""),
                AutoClosingPair("'", "'"),
                AutoClosingPair("`", "`")
            ),
            surroundingPairs = listOf(
                SurroundingPair("{", "}"),
                SurroundingPair("(", ")"),
                SurroundingPair("[", "]"),
                SurroundingPair("<", ">"),
                SurroundingPair("\"", "\""),
                SurroundingPair("'", "'"),
                SurroundingPair("`", "`")
            )
        )
    }
    
    private fun addPythonSupport() {
        languageConfigs["python"] = LanguageConfig(
            id = "python",
            name = "Python",
            extensions = listOf("py", "pyw", "pyi"),
            keywords = listOf(
                "False", "None", "True", "and", "as", "assert", "break", "class", "continue",
                "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
                "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass",
                "raise", "return", "try", "while", "with", "yield", "async", "await"
            ),
            operators = listOf("+", "-", "*", "/", "//", "%", "**", "=", "==", "!=", "<", ">", "<=", ">=", "and", "or", "not", "&", "|", "^", "~", "<<", ">>"),
            brackets = listOf(
                BracketPair("(", ")"),
                BracketPair("[", "]"),
                BracketPair("{", "}")
            ),
            commentRules = CommentRules("#", "\"\"\"", "\"\"\""),
            indentationRules = IndentationRules(
                increaseIndentPattern = ":\\s*$",
                decreaseIndentPattern = "^\\s*(return|break|continue|pass|raise)\\b"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("(", ")"),
                AutoClosingPair("[", "]"),
                AutoClosingPair("{", "}"),
                AutoClosingPair("\"", "\""),
                AutoClosingPair("'", "'")
            ),
            surroundingPairs = listOf(
                SurroundingPair("(", ")"),
                SurroundingPair("[", "]"),
                SurroundingPair("{", "}"),
                SurroundingPair("\"", "\""),
                SurroundingPair("'", "'")
            )
        )
    }
    
    private fun addCppSupport() {
        languageConfigs["cpp"] = LanguageConfig(
            id = "cpp",
            name = "C++",
            extensions = listOf("cpp", "cc", "cxx", "c++", "c", "h", "hpp", "hxx"),
            keywords = listOf(
                "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor",
                "bool", "break", "case", "catch", "char", "char16_t", "char32_t", "class",
                "compl", "const", "constexpr", "const_cast", "continue", "decltype", "default",
                "delete", "do", "double", "dynamic_cast", "else", "enum", "explicit", "export",
                "extern", "false", "float", "for", "friend", "goto", "if", "inline", "int",
                "long", "mutable", "namespace", "new", "noexcept", "not", "not_eq", "nullptr",
                "operator", "or", "or_eq", "private", "protected", "public", "register",
                "reinterpret_cast", "return", "short", "signed", "sizeof", "static",
                "static_assert", "static_cast", "struct", "switch", "template", "this",
                "thread_local", "throw", "true", "try", "typedef", "typeid", "typename",
                "union", "unsigned", "using", "virtual", "void", "volatile", "wchar_t",
                "while", "xor", "xor_eq"
            ),
            operators = listOf("+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "&", "|", "^", "~", "<<", ">>", "++", "--", "->", ".", "::", "?", ":"),
            brackets = listOf(
                BracketPair("{", "}"),
                BracketPair("(", ")"),
                BracketPair("[", "]"),
                BracketPair("<", ">")
            ),
            commentRules = CommentRules("//", "/*", "*/"),
            indentationRules = IndentationRules(
                increaseIndentPattern = "\\{\\s*$|\\(\\s*$|\\[\\s*$",
                decreaseIndentPattern = "^\\s*\\}|^\\s*\\)|^\\s*\\]"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("{", "}"),
                AutoClosingPair("(", ")"),
                AutoClosingPair("[", "]"),
                AutoClosingPair("<", ">"),
                AutoClosingPair("\"", "\""),
                AutoClosingPair("'", "'")
            ),
            surroundingPairs = listOf(
                SurroundingPair("{", "}"),
                SurroundingPair("(", ")"),
                SurroundingPair("[", "]"),
                SurroundingPair("<", ">"),
                SurroundingPair("\"", "\""),
                SurroundingPair("'", "'")
            )
        )
    }
    
    private fun addGradleSupport() {
        languageConfigs["gradle"] = LanguageConfig(
            id = "gradle",
            name = "Gradle",
            extensions = listOf("gradle", "gradle.kts"),
            keywords = listOf(
                "apply", "plugins", "dependencies", "implementation", "api", "testImplementation",
                "androidTestImplementation", "compileOnly", "runtimeOnly", "android", "defaultConfig",
                "buildTypes", "release", "debug", "sourceSets", "repositories", "google",
                "mavenCentral", "jcenter", "maven", "task", "doLast", "doFirst", "group",
                "description", "version", "ext", "project", "subprojects", "allprojects"
            ),
            operators = listOf("+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "&", "|", "^", "~", "<<", ">>"),
            brackets = listOf(
                BracketPair("{", "}"),
                BracketPair("(", ")"),
                BracketPair("[", "]")
            ),
            commentRules = CommentRules("//", "/*", "*/"),
            indentationRules = IndentationRules(
                increaseIndentPattern = "\\{\\s*$|\\(\\s*$|\\[\\s*$",
                decreaseIndentPattern = "^\\s*\\}|^\\s*\\)|^\\s*\\]"
            ),
            autoClosingPairs = listOf(
                AutoClosingPair("{", "}"),
                AutoClosingPair("(", ")"),
                AutoClosingPair("[", "]"),
                AutoClosingPair("\"", "\""),
                AutoClosingPair("'", "'")
            ),
            surroundingPairs = listOf(
                SurroundingPair("{", "}"),
                SurroundingPair("(", ")"),
                SurroundingPair("[", "]"),
                SurroundingPair("\"", "\""),
                SurroundingPair("'", "'")
            )
        )
    }
    
    private fun addPropertiesSupport() {
        languageConfigs["properties"] = LanguageConfig(
            id = "properties",
            name = "Properties",
            extensions = listOf("properties"),
            keywords = emptyList(),
            operators = listOf("=", ":"),
            brackets = emptyList(),
            commentRules = CommentRules("#", null, null),
            indentationRules = IndentationRules("", ""),
            autoClosingPairs = listOf(
                AutoClosingPair("\"", "\""),
                AutoClosingPair("'", "'")
            ),
            surroundingPairs = listOf(
                SurroundingPair("\"", "\""),
                SurroundingPair("'", "'")
            )
        )
    }
    
    private fun addMarkdownSupport() {
        languageConfigs["markdown"] = LanguageConfig(
            id = "markdown",
            name = "Markdown",
            extensions = listOf("md", "markdown"),
            keywords = emptyList(),
            operators = emptyList(),
            brackets = listOf(
                BracketPair("[", "]"),
                BracketPair("(", ")"),
                BracketPair("`", "`")
            ),
            commentRules = CommentRules(null, "<!--", "-->"),
            indentationRules = IndentationRules("", ""),
            autoClosingPairs = listOf(
                AutoClosingPair("[", "]"),
                AutoClosingPair("(", ")"),
                AutoClosingPair("`", "`"),
                AutoClosingPair("*", "*"),
                AutoClosingPair("_", "_"),
                AutoClosingPair("**", "**"),
                AutoClosingPair("__", "__")
            ),
            surroundingPairs = listOf(
                SurroundingPair("[", "]"),
                SurroundingPair("(", ")"),
                SurroundingPair("`", "`"),
                SurroundingPair("*", "*"),
                SurroundingPair("_", "_"),
                SurroundingPair("**", "**"),
                SurroundingPair("__", "__")
            )
        )
    }
    
    companion object {
        private val kotlinKeywords = listOf(
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
        
        private val javaKeywords = listOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while"
        )
        
        private val xmlKeywords = listOf(
            "LinearLayout", "RelativeLayout", "ConstraintLayout", "FrameLayout",
            "TextView", "EditText", "Button", "ImageView", "RecyclerView",
            "ScrollView", "ViewPager", "TabLayout", "AppBarLayout", "Toolbar",
            "FloatingActionButton", "CardView", "ProgressBar", "SeekBar",
            "CheckBox", "RadioButton", "Switch", "Spinner", "WebView"
        )
        
        private val kotlinOperators = listOf(
            "+", "-", "*", "/", "%", "=", "==", "===", "!=", "!==", "<", ">", "<=", ">=",
            "&&", "||", "!", "&", "|", "^", "~", "<<", ">>", ">>>", "++", "--", "+=", "-=",
            "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=", "?:", "!!", "?.", "::"
        )
        
        private val javaOperators = listOf(
            "+", "-", "*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=",
            "&&", "||", "!", "&", "|", "^", "~", "<<", ">>", ">>>", "++", "--", "+=", "-=",
            "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>="
        )
    }
}

@Serializable
data class LanguageConfig(
    val id: String,
    val name: String,
    val extensions: List<String>,
    val keywords: List<String>,
    val operators: List<String>,
    val brackets: List<BracketPair>,
    val commentRules: CommentRules,
    val indentationRules: IndentationRules,
    val autoClosingPairs: List<AutoClosingPair>,
    val surroundingPairs: List<SurroundingPair>
)

@Serializable
data class BracketPair(
    val open: String,
    val close: String
)

@Serializable
data class CommentRules(
    val lineComment: String?,
    val blockCommentStart: String?,
    val blockCommentEnd: String?
)

@Serializable
data class IndentationRules(
    val increaseIndentPattern: String,
    val decreaseIndentPattern: String
)

@Serializable
data class AutoClosingPair(
    val open: String,
    val close: String
)

@Serializable
data class SurroundingPair(
    val open: String,
    val close: String
)