package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service for interacting with the Google Gemini API
 */
class GeminiApiService(context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val settingsManager = SettingsManager(context)
    
    /**
     * Generate code based on a prompt
     */
    suspend fun generateCode(prompt: String, language: String): Flow<GeminiResponse> = flow {
        emit(GeminiResponse.Loading("Generating code..."))
        
        val apiKey = settingsManager.getGeminiApiKey()
        if (apiKey.isNullOrEmpty()) {
            emit(GeminiResponse.Error("Gemini API key not set. Please add your API key in Settings."))
            return@flow
        }
        
        try {
            val enhancedPrompt = buildCodeGenerationPrompt(prompt, language)
            val response = sendRequest(enhancedPrompt, apiKey)
            
            if (response.isSuccessful) {
                val generatedCode = parseCodeFromResponse(response.content)
                emit(GeminiResponse.Success(generatedCode))
            } else {
                emit(GeminiResponse.Error(response.errorMessage ?: "Failed to generate code"))
            }
        } catch (e: Exception) {
            emit(GeminiResponse.Error("Error generating code: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get code completions based on current context
     */
    suspend fun getCodeCompletions(
        codeContext: String,
        cursorPosition: Int,
        language: String
    ): Flow<GeminiResponse> = flow {
        emit(GeminiResponse.Loading("Getting completions..."))
        
        val apiKey = settingsManager.getGeminiApiKey()
        if (apiKey.isNullOrEmpty()) {
            emit(GeminiResponse.Error("Gemini API key not set. Please add your API key in Settings."))
            return@flow
        }
        
        try {
            val prompt = buildCompletionPrompt(codeContext, cursorPosition, language)
            val response = sendRequest(prompt, apiKey)
            
            if (response.isSuccessful) {
                val completions = parseCompletionsFromResponse(response.content)
                emit(GeminiResponse.Success(completions))
            } else {
                emit(GeminiResponse.Error(response.errorMessage ?: "Failed to get completions"))
            }
        } catch (e: Exception) {
            emit(GeminiResponse.Error("Error getting completions: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Explain code
     */
    suspend fun explainCode(code: String, language: String): Flow<GeminiResponse> = flow {
        emit(GeminiResponse.Loading("Analyzing code..."))
        
        val apiKey = settingsManager.getGeminiApiKey()
        if (apiKey.isNullOrEmpty()) {
            emit(GeminiResponse.Error("Gemini API key not set. Please add your API key in Settings."))
            return@flow
        }
        
        try {
            val prompt = buildExplanationPrompt(code, language)
            val response = sendRequest(prompt, apiKey)
            
            if (response.isSuccessful) {
                emit(GeminiResponse.Success(response.content))
            } else {
                emit(GeminiResponse.Error(response.errorMessage ?: "Failed to explain code"))
            }
        } catch (e: Exception) {
            emit(GeminiResponse.Error("Error explaining code: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Fix code issues
     */
    suspend fun fixCodeIssues(code: String, error: String, language: String): Flow<GeminiResponse> = flow {
        emit(GeminiResponse.Loading("Fixing code issues..."))
        
        val apiKey = settingsManager.getGeminiApiKey()
        if (apiKey.isNullOrEmpty()) {
            emit(GeminiResponse.Error("Gemini API key not set. Please add your API key in Settings."))
            return@flow
        }
        
        try {
            val prompt = buildFixIssuesPrompt(code, error, language)
            val response = sendRequest(prompt, apiKey)
            
            if (response.isSuccessful) {
                val fixedCode = parseCodeFromResponse(response.content)
                emit(GeminiResponse.Success(fixedCode))
            } else {
                emit(GeminiResponse.Error(response.errorMessage ?: "Failed to fix code issues"))
            }
        } catch (e: Exception) {
            emit(GeminiResponse.Error("Error fixing code: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Send a request to the Gemini API
     */
    private suspend fun sendRequest(prompt: String, apiKey: String): ApiResponse = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey"
            
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("topK", 40)
                    put("topP", 0.95)
                    put("maxOutputTokens", 8192)
                })
            }
            
            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    
                    return@withContext ApiResponse(true, content)
                } else {
                    val errorMessage = if (jsonResponse.has("error")) {
                        jsonResponse.getJSONObject("error").getString("message")
                    } else {
                        "No content generated"
                    }
                    return@withContext ApiResponse(false, errorMessage = errorMessage)
                }
            } else {
                val errorBody = response.body?.string() ?: ""
                val errorMessage = try {
                    JSONObject(errorBody).getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP Error: ${response.code}"
                }
                return@withContext ApiResponse(false, errorMessage = errorMessage)
            }
        } catch (e: Exception) {
            return@withContext ApiResponse(false, errorMessage = e.message ?: "Unknown error")
        }
    }
    
    /**
     * Build a prompt for code generation
     */
    private fun buildCodeGenerationPrompt(prompt: String, language: String): String {
        return """
            You are an expert programmer specializing in $language. 
            Generate high-quality, clean, and efficient code based on the following request.
            
            Request: $prompt
            
            Please provide only the code without explanations or comments unless specifically requested.
            Use best practices and modern coding standards for $language.
        """.trimIndent()
    }
    
    /**
     * Build a prompt for code completion
     */
    private fun buildCompletionPrompt(codeContext: String, cursorPosition: Int, language: String): String {
        val beforeCursor = codeContext.substring(0, cursorPosition)
        val afterCursor = codeContext.substring(cursorPosition)
        
        return """
            You are an expert programmer specializing in $language.
            Complete the code at the cursor position marked by [CURSOR].
            Provide only the completion text, not the entire file.
            
            Code:
            $beforeCursor[CURSOR]$afterCursor
            
            Language: $language
            
            Provide only the completion text without any explanations or formatting.
        """.trimIndent()
    }
    
    /**
     * Build a prompt for code explanation
     */
    private fun buildExplanationPrompt(code: String, language: String): String {
        return """
            You are an expert programmer specializing in $language.
            Explain the following code in a clear and concise manner.
            Focus on the purpose, logic, and any important patterns or techniques used.
            
            ```$language
            $code
            ```
            
            Provide a detailed explanation that would help a junior developer understand this code.
        """.trimIndent()
    }
    
    /**
     * Build a prompt for fixing code issues
     */
    private fun buildFixIssuesPrompt(code: String, error: String, language: String): String {
        return """
            You are an expert programmer specializing in $language.
            Fix the following code that has errors or issues.
            
            Code with issues:
            ```$language
            $code
            ```
            
            Error message or issue description:
            $error
            
            Please provide the corrected code and a brief explanation of what was wrong and how you fixed it.
        """.trimIndent()
    }
    
    /**
     * Parse code from the API response
     */
    private fun parseCodeFromResponse(response: String): String {
        // Extract code blocks if present
        val codeBlockRegex = "```(?:\\w+)?\\s*([\\s\\S]*?)```".toRegex()
        val codeBlocks = codeBlockRegex.findAll(response)
        
        return if (codeBlocks.any()) {
            codeBlocks.map { it.groupValues[1].trim() }.joinToString("\n\n")
        } else {
            // If no code blocks, return the whole response
            response.trim()
        }
    }
    
    /**
     * Parse completions from the API response
     */
    private fun parseCompletionsFromResponse(response: String): String {
        // For completions, we want just the raw text without any markdown formatting
        return response.replace("```", "").trim()
    }
    
    /**
     * Data class for API responses
     */
    data class ApiResponse(
        val isSuccessful: Boolean,
        val content: String = "",
        val errorMessage: String? = null
    )
}

/**
 * Sealed class for Gemini API responses
 */
sealed class GeminiResponse {
    data class Success(val content: String) : GeminiResponse()
    data class Error(val message: String) : GeminiResponse()
    data class Loading(val message: String) : GeminiResponse()
}