package com.anyoneide.app.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.anyoneide.app.model.CompletionItem
import com.anyoneide.app.model.SyntaxHighlight
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager for the native Rust-based editor implementation
 * This is an experimental feature that uses JNI to call into a Rust library
 */
class RustEditorManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RustEditorManager"
        private val isLibraryLoaded = AtomicBoolean(false)
        
        // Load the native library
        init {
            try {
                System.loadLibrary("rust_native_build")
                isLibraryLoaded.set(true)
                Log.d(TAG, "Native editor library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                // Library not loaded - will be handled gracefully
                Log.e(TAG, "Failed to load native editor library", e)
            } catch (e: Exception) {
                // Other exceptions - will be handled gracefully
                Log.e(TAG, "Exception loading native editor library", e)
            }
        }
        
        // Native method declarations
        @JvmStatic external fun nativeInitializeEditor(): Boolean
        @JvmStatic external fun nativeHighlightSyntax(content: String, language: String): String
        @JvmStatic external fun nativeGetCompletions(content: String, position: Int, language: String): String
        @JvmStatic external fun nativeFormatCode(content: String, language: String): String
        @JvmStatic external fun nativeParseCodeStructure(content: String, language: String): String
        @JvmStatic external fun nativeFindReferences(content: String, position: Int, language: String): String
    }
    
    private val fallbackSyntaxHighlighter = SyntaxHighlighter()
    
    /**
     * Check if the native editor is available
     */
    fun isNativeEditorAvailable(): Boolean {
        return isLibraryLoaded.get()
    }
    
    /**
     * Initialize the editor
     */
    suspend fun initializeEditor(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            return@withContext Result.failure(Exception("Native editor library not loaded"))
        }
        
        try {
            val success = nativeInitializeEditor()
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to initialize native editor"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing native editor", e)
            Result.failure(e)
        }
    }
    
    /**
     * Highlight syntax using the native implementation
     */
    suspend fun highlightSyntax(content: String, language: String): Result<List<SyntaxHighlight>> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Fall back to Java implementation
            return@withContext Result.success(fallbackSyntaxHighlighter.highlightCode(content, language))
        }
        
        try {
            val highlightsJson = nativeHighlightSyntax(content, language)
            val highlights = parseHighlights(highlightsJson)
            Result.success(highlights)
        } catch (e: Exception) {
            Log.e(TAG, "Error highlighting syntax with native editor", e)
            // Fall back to Java implementation
            Result.success(fallbackSyntaxHighlighter.highlightCode(content, language))
        }
    }
    
    /**
     * Get code completions using the native implementation
     */
    suspend fun getCompletions(content: String, position: Int, language: String): Result<List<CompletionItem>> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Return empty list if native library is not available
            return@withContext Result.success(emptyList())
        }
        
        try {
            val completionsJson = nativeGetCompletions(content, position, language)
            val completions = parseCompletions(completionsJson)
            Result.success(completions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting completions with native editor", e)
            Result.failure(e)
        }
    }
    
    /**
     * Format code using the native implementation
     */
    suspend fun formatCode(content: String, language: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Return original content if native library is not available
            return@withContext Result.success(content)
        }
        
        try {
            val formattedCode = nativeFormatCode(content, language)
            Result.success(formattedCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting code with native editor", e)
            Result.success(content) // Return original content on error
        }
    }
    
    /**
     * Parse code structure using the native implementation
     */
    suspend fun parseCodeStructure(content: String, language: String): Result<CodeStructure> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Return empty structure if native library is not available
            return@withContext Result.success(CodeStructure(emptyList(), emptyList(), emptyList(), emptyList()))
        }
        
        try {
            val structureJson = nativeParseCodeStructure(content, language)
            val structure = parseCodeStructure(structureJson)
            Result.success(structure)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing code structure with native editor", e)
            Result.failure(e)
        }
    }
    
    /**
     * Find references using the native implementation
     */
    suspend fun findReferences(content: String, position: Int, language: String): Result<List<Reference>> = withContext(Dispatchers.IO) {
        if (!isLibraryLoaded.get()) {
            // Return empty list if native library is not available
            return@withContext Result.success(emptyList())
        }
        
        try {
            val referencesJson = nativeFindReferences(content, position, language)
            val references = parseReferences(referencesJson)
            Result.success(references)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding references with native editor", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse highlights from JSON
     */
    private fun parseHighlights(json: String): List<SyntaxHighlight> {
        val highlights = mutableListOf<SyntaxHighlight>()
        
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val highlight = jsonArray.getJSONObject(i)
                highlights.add(
                    SyntaxHighlight(
                        start = highlight.getInt("start"),
                        end = highlight.getInt("end"),
                        type = highlight.getString("type_")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing highlights JSON", e)
        }
        
        return highlights
    }
    
    /**
     * Parse completions from JSON
     */
    private fun parseCompletions(json: String): List<CompletionItem> {
        val completions = mutableListOf<CompletionItem>()
        
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val completion = jsonArray.getJSONObject(i)
                completions.add(
                    CompletionItem(
                        label = completion.getString("label"),
                        kind = completion.getString("kind"),
                        detail = if (completion.has("detail") && !completion.isNull("detail")) 
                            completion.getString("detail") else null,
                        documentation = if (completion.has("documentation") && !completion.isNull("documentation")) 
                            completion.getString("documentation") else null,
                        insertText = completion.getString("insert_text")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing completions JSON", e)
        }
        
        return completions
    }
    
    /**
     * Parse code structure from JSON
     */
    private fun parseCodeStructure(json: String): CodeStructure {
        try {
            val jsonObject = JSONObject(json)
            
            val classes = mutableListOf<ClassInfo>()
            val functions = mutableListOf<FunctionInfo>()
            val variables = mutableListOf<VariableInfo>()
            val imports = mutableListOf<ImportInfo>()
            
            // Parse classes
            val classesArray = jsonObject.getJSONArray("classes")
            for (i in 0 until classesArray.length()) {
                val classObj = classesArray.getJSONObject(i)
                classes.add(
                    ClassInfo(
                        name = classObj.getString("name"),
                        kind = classObj.getString("kind"),
                        startLine = classObj.getInt("start_line"),
                        endLine = classObj.getInt("end_line"),
                        modifiers = parseStringArray(classObj.getJSONArray("modifiers"))
                    )
                )
            }
            
            // Parse functions
            val functionsArray = jsonObject.getJSONArray("functions")
            for (i in 0 until functionsArray.length()) {
                val functionObj = functionsArray.getJSONObject(i)
                
                val parameters = mutableListOf<ParameterInfo>()
                val parametersArray = functionObj.getJSONArray("parameters")
                for (j in 0 until parametersArray.length()) {
                    val paramObj = parametersArray.getJSONObject(j)
                    parameters.add(
                        ParameterInfo(
                            name = paramObj.getString("name"),
                            type = if (paramObj.has("type_") && !paramObj.isNull("type_")) 
                                paramObj.getString("type_") else null
                        )
                    )
                }
                
                functions.add(
                    FunctionInfo(
                        name = functionObj.getString("name"),
                        startLine = functionObj.getInt("start_line"),
                        endLine = functionObj.getInt("end_line"),
                        parameters = parameters,
                        returnType = if (functionObj.has("return_type") && !functionObj.isNull("return_type")) 
                            functionObj.getString("return_type") else null,
                        modifiers = parseStringArray(functionObj.getJSONArray("modifiers"))
                    )
                )
            }
            
            // Parse variables
            val variablesArray = jsonObject.getJSONArray("variables")
            for (i in 0 until variablesArray.length()) {
                val variableObj = variablesArray.getJSONObject(i)
                variables.add(
                    VariableInfo(
                        name = variableObj.getString("name"),
                        type = if (variableObj.has("type_") && !variableObj.isNull("type_")) 
                            variableObj.getString("type_") else null,
                        line = variableObj.getInt("line"),
                        modifiers = parseStringArray(variableObj.getJSONArray("modifiers"))
                    )
                )
            }
            
            // Parse imports
            val importsArray = jsonObject.getJSONArray("imports")
            for (i in 0 until importsArray.length()) {
                val importObj = importsArray.getJSONObject(i)
                imports.add(
                    ImportInfo(
                        path = importObj.getString("path"),
                        line = importObj.getInt("line")
                    )
                )
            }
            
            return CodeStructure(classes, functions, variables, imports)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing code structure JSON", e)
            return CodeStructure(emptyList(), emptyList(), emptyList(), emptyList())
        }
    }
    
    /**
     * Parse references from JSON
     */
    private fun parseReferences(json: String): List<Reference> {
        val references = mutableListOf<Reference>()
        
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val reference = jsonArray.getJSONObject(i)
                references.add(
                    Reference(
                        start = reference.getInt("start"),
                        end = reference.getInt("end"),
                        line = reference.getInt("line"),
                        column = reference.getInt("column")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing references JSON", e)
        }
        
        return references
    }
    
    /**
     * Parse string array from JSON
     */
    private fun parseStringArray(jsonArray: JSONArray): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getString(i))
        }
        return result
    }
    
    /**
     * Data classes for code structure
     */
    data class CodeStructure(
        val classes: List<ClassInfo>,
        val functions: List<FunctionInfo>,
        val variables: List<VariableInfo>,
        val imports: List<ImportInfo>
    )
    
    data class ClassInfo(
        val name: String,
        val kind: String,
        val startLine: Int,
        val endLine: Int,
        val modifiers: List<String>
    )
    
    data class FunctionInfo(
        val name: String,
        val startLine: Int,
        val endLine: Int,
        val parameters: List<ParameterInfo>,
        val returnType: String?,
        val modifiers: List<String>
    )
    
    data class ParameterInfo(
        val name: String,
        val type: String?
    )
    
    data class VariableInfo(
        val name: String,
        val type: String?,
        val line: Int,
        val modifiers: List<String>
    )
    
    data class ImportInfo(
        val path: String,
        val line: Int
    )
    
    data class Reference(
        val start: Int,
        val end: Int,
        val line: Int,
        val column: Int
    )
}