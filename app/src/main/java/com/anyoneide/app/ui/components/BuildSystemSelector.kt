package com.anyoneide.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.R
import com.anyoneide.app.core.BuildSystemType
import com.anyoneide.app.core.ProjectType

@Composable
fun BuildSystemSelector(
    modifier: Modifier = Modifier,
    selectedBuildSystem: BuildSystemType,
    projectType: ProjectType,
    onBuildSystemSelected: (BuildSystemType) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.build_configuration),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Only show build system options if the project supports multiple build systems
            if (projectType == ProjectType.RUST_ANDROID_LIB) {
                // Show both Gradle and Rust options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BuildSystemOption(
                        name = "Gradle",
                        icon = Icons.Default.Build,
                        isSelected = selectedBuildSystem == BuildSystemType.GRADLE,
                        onClick = { onBuildSystemSelected(BuildSystemType.GRADLE) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    BuildSystemOption(
                        name = "Rust (Cargo)",
                        icon = Icons.Default.Code,
                        isSelected = selectedBuildSystem == BuildSystemType.RUST,
                        onClick = { onBuildSystemSelected(BuildSystemType.RUST) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    BuildSystemOption(
                        name = "Hybrid",
                        icon = Icons.Default.Sync,
                        isSelected = selectedBuildSystem == BuildSystemType.HYBRID,
                        onClick = { onBuildSystemSelected(BuildSystemType.HYBRID) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    BuildSystemOption(
                        name = "Rust Native (Test)",
                        icon = Icons.Default.Science,
                        isSelected = selectedBuildSystem == BuildSystemType.RUST_NATIVE_TEST,
                        onClick = { onBuildSystemSelected(BuildSystemType.RUST_NATIVE_TEST) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Description of selected build system
                Text(
                    text = when (selectedBuildSystem) {
                        BuildSystemType.GRADLE -> stringResource(R.string.rust_build_description)
                        BuildSystemType.RUST -> stringResource(R.string.rust_build_description)
                        BuildSystemType.HYBRID -> stringResource(R.string.rust_build_description)
                        BuildSystemType.RUST_NATIVE_TEST -> stringResource(R.string.rust_native_build_description)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Warning for experimental build system
                if (selectedBuildSystem == BuildSystemType.RUST_NATIVE_TEST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = stringResource(R.string.rust_native_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                // For regular Android projects, only show Gradle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "Gradle",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = "Standard Android build system",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuildSystemOption(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}