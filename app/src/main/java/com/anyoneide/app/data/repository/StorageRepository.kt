package com.anyoneide.app.data.repository

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Repository for file storage operations
 */
class StorageRepository(private val context: Context) {
    
    private val filesDir = context.filesDir
    private val cacheDir = context.cacheDir
    private val externalFilesDir = context.getExternalFilesDir(null)
    
    fun getInternalStorageDir(): File {
        return filesDir
    }
    
    fun getCacheDir(): File {
        return cacheDir
    }
    
    fun getExternalStorageDir(): File? {
        return externalFilesDir
    }
    
    fun getProjectsDir(): File {
        val dir = File(filesDir, "projects")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun getPluginsDir(): File {
        val dir = File(filesDir, "plugins")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun getThemesDir(): File {
        val dir = File(filesDir, "themes")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun getTemplatesDir(): File {
        val dir = File(filesDir, "templates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun getSdkDir(): File {
        val dir = File(filesDir, "sdk")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun getTempDir(): File {
        val dir = File(cacheDir, "temp")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    fun createFile(directory: File, fileName: String): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        val file = File(directory, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }
    
    fun writeToFile(file: File, content: String): Boolean {
        return try {
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun readFromFile(file: File): String? {
        return try {
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun copyFile(source: File, destination: File): Boolean {
        return try {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun deleteFile(file: File): Boolean {
        return if (file.exists()) {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } else {
            true
        }
    }
    
    fun copyStream(input: InputStream, output: OutputStream): Boolean {
        return try {
            input.copyTo(output)
            true
        } catch (e: Exception) {
            false
        }
    }
}