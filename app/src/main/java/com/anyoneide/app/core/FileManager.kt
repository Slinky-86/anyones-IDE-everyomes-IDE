package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class FileManager(private val context: Context) {
    
    suspend fun readFile(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(IOException("File does not exist: $filePath"))
            }
            
            val content = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
            Result.success(content)
        } catch (_: Exception) {
            Result.failure(IOException("Failed to read file: $filePath"))
        }
    }
    
    suspend fun writeFile(filePath: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8)
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(IOException("Failed to write file: $filePath"))
        }
    }
    
    suspend fun createFile(filePath: String, content: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                return@withContext Result.failure(IOException("File already exists: $filePath"))
            }
            
            file.parentFile?.mkdirs()
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8)
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(IOException("Failed to create file: $filePath"))
        }
    }
    
    suspend fun createDirectory(dirPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dir = File(dirPath)
            if (dir.exists()) {
                return@withContext Result.failure(IOException("Directory already exists: $dirPath"))
            }
            
            val success = dir.mkdirs()
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to create directory: $dirPath"))
            }
        } catch (_: Exception) {
            Result.failure(IOException("Failed to create directory: $dirPath"))
        }
    }
    
    suspend fun deleteFile(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(IOException("File does not exist: $filePath"))
            }
            
            if (file.isDirectory) {
                FileUtils.deleteDirectory(file)
            } else {
                file.delete()
            }
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(IOException("Failed to delete file: $filePath"))
        }
    }
    
    suspend fun renameFile(oldPath: String, newPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            val newFile = File(newPath)
            
            if (!oldFile.exists()) {
                return@withContext Result.failure(IOException("Source file does not exist: $oldPath"))
            }
            
            if (newFile.exists()) {
                return@withContext Result.failure(IOException("Destination file already exists: $newPath"))
            }
            
            val success = oldFile.renameTo(newFile)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to rename file from $oldPath to $newPath"))
            }
        } catch (_: Exception) {
            Result.failure(IOException("Failed to rename file"))
        }
    }
    
    suspend fun copyFile(sourcePath: String, destPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IOException("Source file does not exist: $sourcePath"))
            }
            
            destFile.parentFile?.mkdirs()
            
            if (sourceFile.isDirectory) {
                FileUtils.copyDirectory(sourceFile, destFile)
            } else {
                FileUtils.copyFile(sourceFile, destFile)
            }
            Result.success(Unit)
        } catch (_: Exception) {
            Result.failure(IOException("Failed to copy file"))
        }
    }
    
    suspend fun listFiles(dirPath: String): Result<List<FileInfo>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                return@withContext Result.failure(IOException("Directory does not exist: $dirPath"))
            }
            
            val files = dir.listFiles()?.map { file ->
                FileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0,
                    lastModified = file.lastModified(),
                    canRead = file.canRead(),
                    canWrite = file.canWrite(),
                    isHidden = file.isHidden
                )
            }?.sortedWith(compareBy<FileInfo> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
            
            Result.success(files)
        } catch (_: Exception) {
            Result.failure(IOException("Failed to list files"))
        }
    }
    
    suspend fun searchFiles(dirPath: String, pattern: String, includeContent: Boolean = false): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                return@withContext Result.failure(IOException("Directory does not exist: $dirPath"))
            }
            
            val regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            val results = mutableListOf<SearchResult>()
            
            dir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    // Check filename match
                    if (regex.matcher(file.name).find()) {
                        results.add(SearchResult(
                            filePath = file.absolutePath,
                            fileName = file.name,
                            lineNumber = -1,
                            lineContent = "",
                            matchType = SearchMatchType.FILENAME
                        ))
                    }
                    
                    // Check content match if requested
                    if (includeContent && isTextFile(file)) {
                        try {
                            val content = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
                            content.lines().forEachIndexed { index, line ->
                                if (regex.matcher(line).find()) {
                                    results.add(SearchResult(
                                        filePath = file.absolutePath,
                                        fileName = file.name,
                                        lineNumber = index + 1,
                                        lineContent = line.trim(),
                                        matchType = SearchMatchType.CONTENT
                                    ))
                                }
                            }
                        } catch (_: Exception) {
                            // Skip files that can't be read as text
                        }
                    }
                }
            }
            
            Result.success(results)
        } catch (_: Exception) {
            Result.failure(IOException("Failed to search files"))
        }
    }
    
    private fun isTextFile(file: File): Boolean {
        val textExtensions = setOf(
            "txt", "java", "kt", "xml", "json", "gradle", "properties", 
            "md", "yml", "yaml", "html", "css", "js", "ts", "py", "cpp", 
            "c", "h", "hpp", "cs", "php", "rb", "go", "rs", "swift"
        )
        
        val extension = file.extension.lowercase()
        return textExtensions.contains(extension) || file.length() < 1024 * 1024 // Max 1MB for text files
    }
    
    fun getFileExtension(filePath: String): String {
        return File(filePath).extension.lowercase()
    }
    
    fun getLanguageFromExtension(extension: String): String {
        return when (extension) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "xml" -> "xml"
            "json" -> "json"
            "gradle" -> "gradle"
            "properties" -> "properties"
            "md" -> "markdown"
            "html", "htm" -> "html"
            "css" -> "css"
            "js" -> "javascript"
            "ts" -> "typescript"
            "py" -> "python"
            "cpp", "cc", "cxx" -> "cpp"
            "c" -> "c"
            "h", "hpp" -> "header"
            "cs" -> "csharp"
            "php" -> "php"
            "rb" -> "ruby"
            "go" -> "go"
            "rs" -> "rust"
            "swift" -> "swift"
            "yml", "yaml" -> "yaml"
            else -> "text"
        }
    }
}

data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val canRead: Boolean,
    val canWrite: Boolean,
    val isHidden: Boolean
)

data class SearchResult(
    val filePath: String,
    val fileName: String,
    val lineNumber: Int,
    val lineContent: String,
    val matchType: SearchMatchType
)

enum class SearchMatchType {
    FILENAME,
    CONTENT
}