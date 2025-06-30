package com.anyoneide.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class GitIntegration(private val context: Context) {
    
    suspend fun initRepository(projectPath: String): Flow<GitResult> = flow {
        emit(GitResult.Progress("Initializing Git repository..."))
        
        try {
            val result = executeGitCommand(projectPath, listOf("init"))
            if (result.exitCode == 0) {
                emit(GitResult.Success("Git repository initialized successfully", result.output))
            } else {
                emit(GitResult.Error("Failed to initialize Git repository", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git init error"))
        }
    }
    
    suspend fun cloneRepository(url: String, destinationPath: String): Flow<GitResult> = flow {
        emit(GitResult.Progress("Cloning repository from $url..."))
        
        try {
            val result = executeGitCommand(
                File(destinationPath).parent ?: "/",
                listOf("clone", url, destinationPath)
            )
            
            if (result.exitCode == 0) {
                emit(GitResult.Success("Repository cloned successfully", result.output))
            } else {
                emit(GitResult.Error("Failed to clone repository", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git clone error"))
        }
    }
    
    suspend fun addFiles(projectPath: String, files: List<String>): Flow<GitResult> = flow {
        emit(GitResult.Progress("Adding files to Git..."))
        
        try {
            val command = listOf("add") + files
            val result = executeGitCommand(projectPath, command)
            
            if (result.exitCode == 0) {
                emit(GitResult.Success("Files added successfully", result.output))
            } else {
                emit(GitResult.Error("Failed to add files", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git add error"))
        }
    }
    
    suspend fun commit(projectPath: String, message: String, author: String? = null): Flow<GitResult> = flow {
        emit(GitResult.Progress("Committing changes..."))
        
        try {
            val command = mutableListOf("commit", "-m", message)
            if (author != null) {
                command.addAll(listOf("--author", author))
            }
            
            val result = executeGitCommand(projectPath, command)
            
            if (result.exitCode == 0) {
                emit(GitResult.Success("Changes committed successfully", result.output))
            } else {
                emit(GitResult.Error("Failed to commit changes", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git commit error"))
        }
    }
    
    suspend fun push(projectPath: String, remote: String = "origin", branch: String = "main"): Flow<GitResult> = flow {
        emit(GitResult.Progress("Pushing to remote repository..."))
        
        try {
            val result = executeGitCommand(projectPath, listOf("push", remote, branch))
            
            if (result.exitCode == 0) {
                emit(GitResult.Success("Changes pushed successfully", result.output))
            } else {
                emit(GitResult.Error("Failed to push changes", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git push error"))
        }
    }
    
    suspend fun pull(projectPath: String, remote: String = "origin", branch: String = "main"): Flow<GitResult> = flow {
        emit(GitResult.Progress("Pulling from remote repository..."))
        
        try {
            val result = executeGitCommand(projectPath, listOf("pull", remote, branch))
            
            if (result.exitCode == 0) {
                emit(GitResult.Success("Changes pulled successfully", result.output))
            } else {
                emit(GitResult.Error("Failed to pull changes", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git pull error"))
        }
    }
    
    suspend fun getStatus(projectPath: String): Flow<GitResult> = flow {
        try {
            val result = executeGitCommand(projectPath, listOf("status", "--porcelain"))
            
            if (result.exitCode == 0) {
                val status = parseGitStatus(result.output)
                emit(GitResult.StatusResult("Git status retrieved", status))
            } else {
                emit(GitResult.Error("Failed to get Git status", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git status error"))
        }
    }
    
    suspend fun getBranches(projectPath: String): Flow<GitResult> = flow {
        try {
            val result = executeGitCommand(projectPath, listOf("branch", "-a"))
            
            if (result.exitCode == 0) {
                val branches = parseGitBranches(result.output)
                emit(GitResult.BranchResult("Branches retrieved", branches))
            } else {
                emit(GitResult.Error("Failed to get branches", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git branch error"))
        }
    }
    
    suspend fun createBranch(projectPath: String, branchName: String): Flow<GitResult> = flow {
        emit(GitResult.Progress("Creating branch $branchName..."))
        
        try {
            val result = executeGitCommand(projectPath, listOf("checkout", "-b", branchName))
            
            if (result.exitCode == 0) {
                emit(GitResult.Success("Branch $branchName created successfully", result.output))
            } else {
                emit(GitResult.Error("Failed to create branch", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git branch creation error"))
        }
    }
    
    suspend fun switchBranch(projectPath: String, branchName: String): Flow<GitResult> = flow {
        emit(GitResult.Progress("Switching to branch $branchName..."))
        
        try {
            val result = executeGitCommand(projectPath, listOf("checkout", branchName))
            
            if (result.exitCode == 0) {
                emit(GitResult.Success("Switched to branch $branchName", result.output))
            } else {
                emit(GitResult.Error("Failed to switch branch", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git checkout error"))
        }
    }
    
    suspend fun getLog(projectPath: String, limit: Int = 20): Flow<GitResult> = flow {
        try {
            val result = executeGitCommand(
                projectPath, 
                listOf("log", "--oneline", "--graph", "-$limit")
            )
            
            if (result.exitCode == 0) {
                val commits = parseGitLog(result.output)
                emit(GitResult.LogResult("Git log retrieved", commits))
            } else {
                emit(GitResult.Error("Failed to get Git log", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git log error"))
        }
    }
    
    suspend fun getDiff(projectPath: String, file: String? = null): Flow<GitResult> = flow {
        try {
            val command = if (file != null) {
                listOf("diff", file)
            } else {
                listOf("diff")
            }
            
            val result = executeGitCommand(projectPath, command)
            
            if (result.exitCode == 0) {
                emit(GitResult.DiffResult("Git diff retrieved", result.output))
            } else {
                emit(GitResult.Error("Failed to get Git diff", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git diff error"))
        }
    }
    
    suspend fun addRemote(projectPath: String, name: String, url: String): Flow<GitResult> = flow {
        emit(GitResult.Progress("Adding remote $name..."))
        
        try {
            val result = executeGitCommand(projectPath, listOf("remote", "add", name, url))
            
            if (result.exitCode == 0) {
                emit(GitResult.Success("Remote $name added successfully", result.output))
            } else {
                emit(GitResult.Error("Failed to add remote", result.output))
            }
        } catch (_: Exception) {
            emit(GitResult.Error("Git remote add error"))
        }
    }
    
    private suspend fun executeGitCommand(workingDirectory: String, command: List<String>): CommandResult = withContext(Dispatchers.IO) {
        try {
            val fullCommand = listOf("git") + command
            val processBuilder = ProcessBuilder(fullCommand)
            processBuilder.directory(File(workingDirectory))
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            
            val exitCode = process.waitFor()
            
            CommandResult(exitCode, output.toString())
        } catch (_: Exception) {
            CommandResult(-1, "Error executing Git command")
        }
    }
    
    private fun parseGitStatus(output: String): List<GitFileStatus> {
        return output.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                val status = line.substring(0, 2)
                val file = line.substring(3)
                GitFileStatus(file, status)
            }
    }
    
    private fun parseGitBranches(output: String): List<GitBranch> {
        return output.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                val isCurrent = line.startsWith("*")
                val name = line.removePrefix("*").trim()
                val isRemote = name.startsWith("remotes/")
                GitBranch(name, isCurrent, isRemote)
            }
    }
    
    private fun parseGitLog(output: String): List<GitCommit> {
        return output.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split(" ", limit = 2)
                val hash = parts[0].removePrefix("*").removePrefix("|").removePrefix("\\").trim()
                val message = if (parts.size > 1) parts[1] else ""
                GitCommit(hash, message)
            }
    }
}

sealed class GitResult {
    data class Progress(val message: String) : GitResult()
    data class Success(val message: String, val output: String = "") : GitResult()
    data class Error(val message: String, val output: String = "") : GitResult()
    data class StatusResult(val message: String, val status: List<GitFileStatus>) : GitResult()
    data class BranchResult(val message: String, val branches: List<GitBranch>) : GitResult()
    data class LogResult(val message: String, val commits: List<GitCommit>) : GitResult()
    data class DiffResult(val message: String, val diff: String) : GitResult()
}

data class CommandResult(
    val exitCode: Int,
    val output: String
)

data class GitFileStatus(
    val file: String,
    val status: String
)

data class GitBranch(
    val name: String,
    val isCurrent: Boolean,
    val isRemote: Boolean
)

data class GitCommit(
    val hash: String,
    val message: String
)