package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.core.GitBranch
import com.anyoneide.app.core.GitCommit
import com.anyoneide.app.core.GitFileStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitPanel(
    modifier: Modifier = Modifier,
    gitStatus: List<GitFileStatus> = emptyList(),
    gitBranches: List<GitBranch> = emptyList(),
    gitCommits: List<GitCommit> = emptyList(),
    currentBranch: String = "main",
    isGitInitialized: Boolean = false,
    onGitInit: () -> Unit,
    onGitStatus: () -> Unit,
    onGitAdd: (String) -> Unit = {},
    onGitCommit: (String) -> Unit = {},
    onGitPush: () -> Unit = {},
    onGitPull: () -> Unit = {},
    onGitCheckout: (String) -> Unit = {},
    onGitCreateBranch: (String) -> Unit = {},
    onGitLog: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Status", "Branches", "History")
    var commitMessage by remember { mutableStateOf("") }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
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
                Text(
                    text = "Git",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (isGitInitialized) {
                    Text(
                        text = "Branch: $currentBranch",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row {
                    if (!isGitInitialized) {
                        Button(
                            onClick = onGitInit
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Initialize Git")
                        }
                    } else {
                        IconButton(
                            onClick = onGitStatus
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Git Status"
                            )
                        }
                    }
                }
            }
        }

        if (!isGitInitialized) {
            // Not initialized state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Source,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Git Not Initialized",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Initialize Git to start version control",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onGitInit
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Initialize Git Repository")
                    }
                }
            }
        } else {
            // Initialized state
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                1 -> Icon(Icons.Default.AccountTree, contentDescription = null)
                                2 -> Icon(Icons.Default.History, contentDescription = null)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Status tab
                    // Call gitStatus when this tab is selected
                    LaunchedEffect(Unit) {
                        onGitStatus()
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Commit section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Commit Changes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedTextField(
                                    value = commitMessage,
                                    onValueChange = { commitMessage = it },
                                    label = { Text("Commit Message") },
                                    placeholder = { Text("Enter commit message") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onGitAdd(".") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stage All")
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            if (commitMessage.isNotEmpty()) {
                                                onGitCommit(commitMessage)
                                                commitMessage = ""
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = commitMessage.isNotEmpty()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Commit")
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Status list
                        Text(
                            text = "Changed Files",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (gitStatus.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No changes detected",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(gitStatus) { fileStatus ->
                                    GitFileStatusItem(
                                        fileStatus = fileStatus,
                                        onStage = { onGitAdd(fileStatus.file) }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Push/Pull buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onGitPull,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pull")
                            }
                            
                            Button(
                                onClick = onGitPush,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Push")
                            }
                        }
                    }
                }
                1 -> {
                    // Branches tab
                    // Call gitBranches when this tab is selected
                    LaunchedEffect(Unit) {
                        // This would call a function to get branches
                        // For now, we'll use the status function
                        onGitStatus()
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Branches",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Button(
                                onClick = { showNewBranchDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Branch")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (gitBranches.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No branches found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(gitBranches) { branch ->
                                    GitBranchItem(
                                        branch = branch,
                                        onCheckout = { onGitCheckout(branch.name) }
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // History tab
                    // Call gitLog when this tab is selected
                    LaunchedEffect(Unit) {
                        onGitLog()
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Commit History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (gitCommits.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No commit history",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(gitCommits) { commit ->
                                    GitCommitItem(commit = commit)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // New branch dialog
    if (showNewBranchDialog) {
        AlertDialog(
            onDismissRequest = { showNewBranchDialog = false },
            title = { Text("Create New Branch") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        label = { Text("Branch Name") },
                        placeholder = { Text("feature/new-feature") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBranchName.isNotEmpty()) {
                            onGitCreateBranch(newBranchName)
                            showNewBranchDialog = false
                            newBranchName = ""
                        }
                    },
                    enabled = newBranchName.isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showNewBranchDialog = false
                        newBranchName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GitFileStatusItem(
    fileStatus: GitFileStatus,
    onStage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            val (icon, color) = when (fileStatus.status) {
                "M " -> Icons.Default.Edit to MaterialTheme.colorScheme.tertiary
                " M" -> Icons.Default.Edit to MaterialTheme.colorScheme.tertiary
                "MM" -> Icons.Default.Edit to MaterialTheme.colorScheme.tertiary
                "A " -> Icons.Default.Add to MaterialTheme.colorScheme.primary
                "??" -> Icons.Default.QuestionMark to MaterialTheme.colorScheme.error
                "D " -> Icons.Default.Delete to MaterialTheme.colorScheme.error
                " D" -> Icons.Default.Delete to MaterialTheme.colorScheme.error
                else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = fileStatus.file,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            if (fileStatus.status == "??") {
                IconButton(
                    onClick = onStage,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Stage File",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GitBranchItem(
    branch: GitBranch,
    onCheckout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = branch.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (branch.isCurrent) FontWeight.Bold else FontWeight.Normal
                )
                
                if (branch.isRemote) {
                    Text(
                        text = "Remote",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!branch.isCurrent) {
                Button(
                    onClick = onCheckout,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Checkout")
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Current",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun GitCommitItem(
    commit: GitCommit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Commit,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Commit: ${commit.hash}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}