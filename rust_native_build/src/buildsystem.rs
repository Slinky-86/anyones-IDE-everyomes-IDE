use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::io::{BufRead, BufReader};
use std::time::{Duration, Instant};
use serde::{Serialize, Deserialize};
use anyhow::{Result, anyhow};

// Build system status
#[derive(Serialize, Deserialize)]
pub struct BuildSystemStatus {
    pub available: bool,
    pub version: String,
    pub description: String,
    pub features: Vec<String>,
    pub os_info: String,
    pub ndk_installed: bool,
    pub build_config: BuildConfig,
}

// Build configuration
#[derive(Serialize, Deserialize)]
pub struct BuildConfig {
    pub timeout_seconds: u32,
    pub max_output_lines: u32,
    pub enable_verbose_output: bool,
    pub enable_error_recovery: bool,
}

// Project information
#[derive(Serialize, Deserialize)]
pub struct ProjectInfo {
    pub name: String,
    pub path: String,
    pub project_type: String,
    pub cargo_toml_exists: bool,
    pub build_gradle_exists: bool,
    pub src_dir_exists: bool,
    pub target_dir_exists: bool,
    pub dependencies: Vec<String>,
    pub features: Vec<String>,
}

// Health check
#[derive(Serialize, Deserialize)]
pub struct HealthCheck {
    pub status: String,
    pub message: String,
    pub checks: Vec<Check>,
}

// Check
#[derive(Serialize, Deserialize)]
pub struct Check {
    pub name: String,
    pub status: String,
    pub message: String,
}

// Build result
#[derive(Serialize, Deserialize)]
pub struct BuildResult {
    pub success: bool,
    pub output_messages: Vec<OutputMessage>,
    pub duration_ms: u64,
    pub artifacts: Vec<String>,
}

// Output message
#[derive(Serialize, Deserialize)]
pub struct OutputMessage {
    pub message_type: String,
    pub content: String,
    pub timestamp: u64,
}

// Get build system status
pub fn get_build_system_status() -> BuildSystemStatus {
    let rust_version = match Command::new("rustc").arg("--version").output() {
        Ok(output) => {
            if output.status.success() {
                String::from_utf8_lossy(&output.stdout).trim().to_string()
            } else {
                "Unknown".to_string()
            }
        }
        Err(_) => "Not installed".to_string(),
    };
    
    let os_info = get_os_info();
    
    BuildSystemStatus {
        available: rust_version != "Not installed",
        version: rust_version,
        description: "Native Rust build system for Android".to_string(),
        features: vec![
            "Build".to_string(),
            "Clean".to_string(),
            "Test".to_string(),
            "Android targets".to_string(),
            "JNI bindings".to_string(),
        ],
        os_info,
        ndk_installed: is_ndk_installed(),
        build_config: BuildConfig {
            timeout_seconds: 300,
            max_output_lines: 10000,
            enable_verbose_output: true,
            enable_error_recovery: true,
        },
    }
}

// Get project information
pub fn get_project_info(project_path: &str) -> ProjectInfo {
    let path = Path::new(project_path);
    let cargo_toml_path = path.join("Cargo.toml");
    let build_gradle_path = path.join("build.gradle");
    let build_gradle_kts_path = path.join("build.gradle.kts");
    let src_dir = path.join("src");
    let target_dir = path.join("target");
    
    let cargo_toml_exists = cargo_toml_path.exists();
    let build_gradle_exists = build_gradle_path.exists() || build_gradle_kts_path.exists();
    
    let project_type = if cargo_toml_exists && build_gradle_exists {
        "hybrid"
    } else if cargo_toml_exists {
        "rust"
    } else if build_gradle_exists {
        "gradle"
    } else {
        "unknown"
    };
    
    let name = path.file_name()
        .map(|name| name.to_string_lossy().to_string())
        .unwrap_or_else(|| "unknown".to_string());
    
    ProjectInfo {
        name,
        path: project_path.to_string(),
        project_type: project_type.to_string(),
        cargo_toml_exists,
        build_gradle_exists,
        src_dir_exists: src_dir.exists(),
        target_dir_exists: target_dir.exists(),
        dependencies: Vec::new(),
        features: Vec::new(),
    }
}

// Check build system health
pub fn check_build_system_health() -> HealthCheck {
    let rust_installed = Command::new("rustc").arg("--version").output().is_ok();
    let cargo_installed = Command::new("cargo").arg("--version").output().is_ok();
    
    let checks = vec![
        Check {
            name: "Rust Installation".to_string(),
            status: if rust_installed { "passed" } else { "failed" },
            message: if rust_installed {
                "Rust is installed".to_string()
            } else {
                "Rust is not installed".to_string()
            },
        },
        Check {
            name: "Cargo Installation".to_string(),
            status: if cargo_installed { "passed" } else { "failed" },
            message: if cargo_installed {
                "Cargo is installed".to_string()
            } else {
                "Cargo is not installed".to_string()
            },
        },
        Check {
            name: "NDK Installation".to_string(),
            status: if is_ndk_installed() { "passed" } else { "warning" },
            message: if is_ndk_installed() {
                "Android NDK is installed".to_string()
            } else {
                "Android NDK is not installed".to_string()
            },
        },
    ];
    
    let status = if rust_installed && cargo_installed {
        "healthy"
    } else {
        "unhealthy"
    };
    
    HealthCheck {
        status: status.to_string(),
        message: if status == "healthy" {
            "Build system is healthy".to_string()
        } else {
            "Build system is unhealthy".to_string()
        },
        checks,
    }
}

// Build project
pub fn build_project(project_path: &str, build_type: &str) -> BuildResult {
    let start_time = Instant::now();
    let mut output_messages = Vec::new();
    let mut artifacts = Vec::new();
    
    // Add initial message
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Building project with type: {}", build_type),
        timestamp: current_time_millis(),
    });
    
    // Check if project exists
    let project_dir = Path::new(project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Project directory does not exist: {}", project_path),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Check if Cargo.toml exists
    let cargo_toml_path = project_dir.join("Cargo.toml");
    if !cargo_toml_path.exists() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: "Cargo.toml not found. Not a valid Rust project.".to_string(),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Build command
    let mut cmd = Command::new("cargo");
    cmd.current_dir(project_dir);
    
    match build_type {
        "release" => {
            cmd.arg("build");
            cmd.arg("--release");
        }
        "debug" => {
            cmd.arg("build");
        }
        _ => {
            cmd.arg("build");
            cmd.arg(build_type);
        }
    }
    
    // Execute command
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Running: {:?}", cmd),
        timestamp: current_time_millis(),
    });
    
    match cmd.stdout(Stdio::piped()).stderr(Stdio::piped()).spawn() {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        let message_type = if line.contains("error") {
                            "ERROR"
                        } else if line.contains("warning") {
                            "WARNING"
                        } else {
                            "INFO"
                        };
                        
                        output_messages.push(OutputMessage {
                            message_type: message_type.to_string(),
                            content: line,
                            timestamp: current_time_millis(),
                        });
                    }
                }
            }
            
            // Read stderr
            if let Some(stderr) = child.stderr.take() {
                let reader = BufReader::new(stderr);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: line,
                            timestamp: current_time_millis(),
                        });
                    }
                }
            }
            
            // Wait for the process to complete
            match child.wait() {
                Ok(status) => {
                    let success = status.success();
                    
                    if success {
                        output_messages.push(OutputMessage {
                            message_type: "SUCCESS".to_string(),
                            content: "Build completed successfully".to_string(),
                            timestamp: current_time_millis(),
                        });
                        
                        // Find artifacts
                        let target_dir = project_dir.join("target");
                        let profile_dir = if build_type == "release" {
                            target_dir.join("release")
                        } else {
                            target_dir.join("debug")
                        };
                        
                        if profile_dir.exists() {
                            if let Ok(entries) = std::fs::read_dir(&profile_dir) {
                                for entry in entries.filter_map(Result::ok) {
                                    let path = entry.path();
                                    if path.is_file() {
                                        let file_name = path.file_name().unwrap().to_string_lossy();
                                        
                                        // Skip common non-executable files
                                        if file_name.ends_with(".d") || file_name.ends_with(".rlib") || 
                                           file_name.ends_with(".rmeta") || file_name.ends_with(".pdb") {
                                            continue;
                                        }
                                        
                                        artifacts.push(path.to_string_lossy().to_string());
                                        
                                        output_messages.push(OutputMessage {
                                            message_type: "ARTIFACT".to_string(),
                                            content: format!("Generated: {}", path.to_string_lossy()),
                                            timestamp: current_time_millis(),
                                        });
                                    }
                                }
                            }
                        }
                    } else {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: format!("Build failed with exit code: {}", status.code().unwrap_or(-1)),
                            timestamp: current_time_millis(),
                        });
                    }
                    
                    BuildResult {
                        success,
                        output_messages,
                        duration_ms: start_time.elapsed().as_millis() as u64,
                        artifacts,
                    }
                }
                Err(e) => {
                    output_messages.push(OutputMessage {
                        message_type: "ERROR".to_string(),
                        content: format!("Failed to wait for process: {}", e),
                        timestamp: current_time_millis(),
                    });
                    
                    BuildResult {
                        success: false,
                        output_messages,
                        duration_ms: start_time.elapsed().as_millis() as u64,
                        artifacts,
                    }
                }
            }
        }
        Err(e) => {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to start process: {}", e),
                timestamp: current_time_millis(),
            });
            
            BuildResult {
                success: false,
                output_messages,
                duration_ms: start_time.elapsed().as_millis() as u64,
                artifacts,
            }
        }
    }
}

// Clean project
pub fn clean_project(project_path: &str) -> BuildResult {
    let start_time = Instant::now();
    let mut output_messages = Vec::new();
    let artifacts = Vec::new();
    
    // Add initial message
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: "Cleaning project".to_string(),
        timestamp: current_time_millis(),
    });
    
    // Check if project exists
    let project_dir = Path::new(project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Project directory does not exist: {}", project_path),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Check if Cargo.toml exists
    let cargo_toml_path = project_dir.join("Cargo.toml");
    if !cargo_toml_path.exists() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: "Cargo.toml not found. Not a valid Rust project.".to_string(),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Clean command
    let mut cmd = Command::new("cargo");
    cmd.current_dir(project_dir);
    cmd.arg("clean");
    
    // Execute command
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Running: {:?}", cmd),
        timestamp: current_time_millis(),
    });
    
    match cmd.stdout(Stdio::piped()).stderr(Stdio::piped()).spawn() {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        output_messages.push(OutputMessage {
                            message_type: "INFO".to_string(),
                            content: line,
                            timestamp: current_time_millis(),
                        });
                    }
                }
            }
            
            // Read stderr
            if let Some(stderr) = child.stderr.take() {
                let reader = BufReader::new(stderr);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: line,
                            timestamp: current_time_millis(),
                        });
                    }
                }
            }
            
            // Wait for the process to complete
            match child.wait() {
                Ok(status) => {
                    let success = status.success();
                    
                    if success {
                        output_messages.push(OutputMessage {
                            message_type: "SUCCESS".to_string(),
                            content: "Clean completed successfully".to_string(),
                            timestamp: current_time_millis(),
                        });
                    } else {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: format!("Clean failed with exit code: {}", status.code().unwrap_or(-1)),
                            timestamp: current_time_millis(),
                        });
                    }
                    
                    BuildResult {
                        success,
                        output_messages,
                        duration_ms: start_time.elapsed().as_millis() as u64,
                        artifacts,
                    }
                }
                Err(e) => {
                    output_messages.push(OutputMessage {
                        message_type: "ERROR".to_string(),
                        content: format!("Failed to wait for process: {}", e),
                        timestamp: current_time_millis(),
                    });
                    
                    BuildResult {
                        success: false,
                        output_messages,
                        duration_ms: start_time.elapsed().as_millis() as u64,
                        artifacts,
                    }
                }
            }
        }
        Err(e) => {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to start process: {}", e),
                timestamp: current_time_millis(),
            });
            
            BuildResult {
                success: false,
                output_messages,
                duration_ms: start_time.elapsed().as_millis() as u64,
                artifacts,
            }
        }
    }
}

// Test project
pub fn test_project(project_path: &str, release: bool) -> BuildResult {
    let start_time = Instant::now();
    let mut output_messages = Vec::new();
    let artifacts = Vec::new();
    
    // Add initial message
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: "Running tests".to_string(),
        timestamp: current_time_millis(),
    });
    
    // Check if project exists
    let project_dir = Path::new(project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Project directory does not exist: {}", project_path),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Check if Cargo.toml exists
    let cargo_toml_path = project_dir.join("Cargo.toml");
    if !cargo_toml_path.exists() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: "Cargo.toml not found. Not a valid Rust project.".to_string(),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Test command
    let mut cmd = Command::new("cargo");
    cmd.current_dir(project_dir);
    cmd.arg("test");
    
    if release {
        cmd.arg("--release");
    }
    
    // Execute command
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Running: {:?}", cmd),
        timestamp: current_time_millis(),
    });
    
    match cmd.stdout(Stdio::piped()).stderr(Stdio::piped()).spawn() {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        let message_type = if line.contains("error") {
                            "ERROR"
                        } else if line.contains("warning") {
                            "WARNING"
                        } else if line.contains("test result: ok") {
                            "SUCCESS"
                        } else {
                            "INFO"
                        };
                        
                        output_messages.push(OutputMessage {
                            message_type: message_type.to_string(),
                            content: line,
                            timestamp: current_time_millis(),
                        });
                    }
                }
            }
            
            // Read stderr
            if let Some(stderr) = child.stderr.take() {
                let reader = BufReader::new(stderr);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: line,
                            timestamp: current_time_millis(),
                        });
                    }
                }
            }
            
            // Wait for the process to complete
            match child.wait() {
                Ok(status) => {
                    let success = status.success();
                    
                    if success {
                        output_messages.push(OutputMessage {
                            message_type: "SUCCESS".to_string(),
                            content: "Tests completed successfully".to_string(),
                            timestamp: current_time_millis(),
                        });
                    } else {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: format!("Tests failed with exit code: {}", status.code().unwrap_or(-1)),
                            timestamp: current_time_millis(),
                        });
                    }
                    
                    BuildResult {
                        success,
                        output_messages,
                        duration_ms: start_time.elapsed().as_millis() as u64,
                        artifacts,
                    }
                }
                Err(e) => {
                    output_messages.push(OutputMessage {
                        message_type: "ERROR".to_string(),
                        content: format!("Failed to wait for process: {}", e),
                        timestamp: current_time_millis(),
                    });
                    
                    BuildResult {
                        success: false,
                        output_messages,
                        duration_ms: start_time.elapsed().as_millis() as u64,
                        artifacts,
                    }
                }
            }
        }
        Err(e) => {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to start process: {}", e),
                timestamp: current_time_millis(),
            });
            
            BuildResult {
                success: false,
                output_messages,
                duration_ms: start_time.elapsed().as_millis() as u64,
                artifacts,
            }
        }
    }
}

// Build for Android target
pub fn build_for_android_target(project_path: &str, target: &str, release: bool) -> BuildResult {
    let start_time = Instant::now();
    let mut output_messages = Vec::new();
    let mut artifacts = Vec::new();
    
    // Add initial message
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Building for Android target: {}", target),
        timestamp: current_time_millis(),
    });
    
    // Check if project exists
    let project_dir = Path::new(project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Project directory does not exist: {}", project_path),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Check if Cargo.toml exists
    let cargo_toml_path = project_dir.join("Cargo.toml");
    if !cargo_toml_path.exists() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: "Cargo.toml not found. Not a valid Rust project.".to_string(),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Build command
    let mut cmd = Command::new("cargo");
    cmd.current_dir(project_dir);
    cmd.arg("build");
    cmd.arg("--target");
    cmd.arg(target);
    
    if release {
        cmd.arg("--release");
    }
    
    // Execute command
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Running: {:?}", cmd),
        timestamp: current_time_millis(),
    });
    
    match cmd.stdout(Stdio::piped()).stderr(Stdio::piped()).spawn() {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        let message_type = if line.contains("error") {
                            "ERROR"
                        } else if line.contains("warning") {
                            "WARNING"
                        } else {
                            "INFO"
                        };
                        
                        output_messages.push(OutputMessage {
                            message_type: message_type.to_string(),
                            content: line,
                            timestamp: current_time_millis(),
                        });
                    }
                }
            }
            
            // Read stderr
            if let Some(stderr) = child.stderr.take() {
                let reader = BufReader::new(stderr);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: line,
                            timestamp: current_time_millis(),
                        });
                    }
                }
            }
            
            // Wait for the process to complete
            match child.wait() {
                Ok(status) => {
                    let success = status.success();
                    
                    if success {
                        output_messages.push(OutputMessage {
                            message_type: "SUCCESS".to_string(),
                            content: format!("Build for {} completed successfully", target),
                            timestamp: current_time_millis(),
                        });
                        
                        // Find artifacts
                        let target_dir = project_dir.join("target").join(target);
                        let profile_dir = if release {
                            target_dir.join("release")
                        } else {
                            target_dir.join("debug")
                        };
                        
                        if profile_dir.exists() {
                            if let Ok(entries) = std::fs::read_dir(&profile_dir) {
                                for entry in entries.filter_map(Result::ok) {
                                    let path = entry.path();
                                    if path.is_file() {
                                        let file_name = path.file_name().unwrap().to_string_lossy();
                                        
                                        // Skip common non-executable files
                                        if file_name.ends_with(".d") || file_name.ends_with(".rlib") || 
                                           file_name.ends_with(".rmeta") || file_name.ends_with(".pdb") {
                                            continue;
                                        }
                                        
                                        artifacts.push(path.to_string_lossy().to_string());
                                        
                                        output_messages.push(OutputMessage {
                                            message_type: "ARTIFACT".to_string(),
                                            content: format!("Generated: {}", path.to_string_lossy()),
                                            timestamp: current_time_millis(),
                                        });
                                    }
                                }
                            }
                        }
                    } else {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: format!("Build for {} failed with exit code: {}", target, status.code().unwrap_or(-1)),
                            timestamp: current_time_millis(),
                        });
                    }
                    
                    BuildResult {
                        success,
                        output_messages,
                        duration_ms: start_time.elapsed().as_millis() as u64,
                        artifacts,
                    }
                }
                Err(e) => {
                    output_messages.push(OutputMessage {
                        message_type: "ERROR".to_string(),
                        content: format!("Failed to wait for process: {}", e),
                        timestamp: current_time_millis(),
                    });
                    
                    BuildResult {
                        success: false,
                        output_messages,
                        duration_ms: start_time.elapsed().as_millis() as u64,
                        artifacts,
                    }
                }
            }
        }
        Err(e) => {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to start process: {}", e),
                timestamp: current_time_millis(),
            });
            
            BuildResult {
                success: false,
                output_messages,
                duration_ms: start_time.elapsed().as_millis() as u64,
                artifacts,
            }
        }
    }
}

// Generate Android bindings
pub fn generate_android_bindings(project_path: &str, package_name: &str) -> BuildResult {
    let start_time = Instant::now();
    let mut output_messages = Vec::new();
    let mut artifacts = Vec::new();
    
    // Add initial message
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: "Generating Android bindings".to_string(),
        timestamp: current_time_millis(),
    });
    
    // Check if project exists
    let project_dir = Path::new(project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Project directory does not exist: {}", project_path),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Check if Cargo.toml exists
    let cargo_toml_path = project_dir.join("Cargo.toml");
    if !cargo_toml_path.exists() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: "Cargo.toml not found. Not a valid Rust project.".to_string(),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    // Create src directory if it doesn't exist
    let src_dir = project_dir.join("src");
    if !src_dir.exists() {
        if let Err(e) = std::fs::create_dir_all(&src_dir) {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to create src directory: {}", e),
                timestamp: current_time_millis(),
            });
            
            return BuildResult {
                success: false,
                output_messages,
                duration_ms: start_time.elapsed().as_millis() as u64,
                artifacts,
            };
        }
    }
    
    // Create lib.rs with JNI bindings
    let lib_rs_path = src_dir.join("lib.rs");
    let lib_rs_content = format!(
        r#"use jni::JNIEnv;
use jni::objects::{{JClass, JString}};
use jni::sys::jstring;

#[no_mangle]
pub extern "C" fn Java_{0}_RustLib_getGreeting(env: JNIEnv, _class: JClass) -> jstring {{
    let output = env.new_string("Hello from Rust!")
        .expect("Couldn't create Java string!");
    output.into_raw()
}}

#[no_mangle]
pub extern "C" fn Java_{0}_RustLib_processString(env: JNIEnv, _class: JClass, input: JString) -> jstring {{
    let input: String = env.get_string(input)
        .expect("Couldn't get Java string!")
        .into();
    let output = format!("Rust processed: {{}}", input);
    let output = env.new_string(output)
        .expect("Couldn't create Java string!");
    output.into_raw()
}}"#,
        package_name.replace(".", "_")
    );
    
    if let Err(e) = std::fs::write(&lib_rs_path, lib_rs_content) {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Failed to write lib.rs: {}", e),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: "Generated lib.rs with JNI bindings".to_string(),
        timestamp: current_time_millis(),
    });
    
    artifacts.push(lib_rs_path.to_string_lossy().to_string());
    
    // Create .cargo directory and config.toml
    let cargo_config_dir = project_dir.join(".cargo");
    if !cargo_config_dir.exists() {
        if let Err(e) = std::fs::create_dir_all(&cargo_config_dir) {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to create .cargo directory: {}", e),
                timestamp: current_time_millis(),
            });
            
            return BuildResult {
                success: false,
                output_messages,
                duration_ms: start_time.elapsed().as_millis() as u64,
                artifacts,
            };
        }
    }
    
    let cargo_config_path = cargo_config_dir.join("config.toml");
    let cargo_config_content = r#"[target.aarch64-linux-android]
ar = "aarch64-linux-android-ar"
linker = "aarch64-linux-android-clang"

[target.armv7-linux-androideabi]
ar = "arm-linux-androideabi-ar"
linker = "arm-linux-androideabi-clang"

[target.i686-linux-android]
ar = "i686-linux-android-ar"
linker = "i686-linux-android-clang"

[target.x86_64-linux-android]
ar = "x86_64-linux-android-ar"
linker = "x86_64-linux-android-clang"
"#;
    
    if let Err(e) = std::fs::write(&cargo_config_path, cargo_config_content) {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Failed to write .cargo/config.toml: {}", e),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: "Generated .cargo/config.toml for Android targets".to_string(),
        timestamp: current_time_millis(),
    });
    
    artifacts.push(cargo_config_path.to_string_lossy().to_string());
    
    // Update Cargo.toml to ensure it's a library with cdylib type
    if let Ok(cargo_toml_content) = std::fs::read_to_string(&cargo_toml_path) {
        let mut updated_content = cargo_toml_content.clone();
        
        if !cargo_toml_content.contains("[lib]") {
            updated_content.push_str("\n[lib]\ncrate-type = [\"cdylib\", \"staticlib\", \"rlib\"]\n");
        } else if !cargo_toml_content.contains("crate-type") {
            updated_content = updated_content.replace(
                "[lib]",
                "[lib]\ncrate-type = [\"cdylib\", \"staticlib\", \"rlib\"]"
            );
        }
        
        if !cargo_toml_content.contains("jni =") {
            if cargo_toml_content.contains("[dependencies]") {
                updated_content = updated_content.replace(
                    "[dependencies]",
                    "[dependencies]\njni = { version = \"0.21.1\", features = [\"invocation\"] }"
                );
            } else {
                updated_content.push_str("\n[dependencies]\njni = { version = \"0.21.1\", features = [\"invocation\"] }\n");
            }
        }
        
        if updated_content != cargo_toml_content {
            if let Err(e) = std::fs::write(&cargo_toml_path, updated_content) {
                output_messages.push(OutputMessage {
                    message_type: "ERROR".to_string(),
                    content: format!("Failed to update Cargo.toml: {}", e),
                    timestamp: current_time_millis(),
                });
                
                return BuildResult {
                    success: false,
                    output_messages,
                    duration_ms: start_time.elapsed().as_millis() as u64,
                    artifacts,
                };
            }
            
            output_messages.push(OutputMessage {
                message_type: "INFO".to_string(),
                content: "Updated Cargo.toml with library configuration".to_string(),
                timestamp: current_time_millis(),
            });
            
            artifacts.push(cargo_toml_path.to_string_lossy().to_string());
        }
    }
    
    // Create Java wrapper class
    let java_dir = project_dir.join("android/src/main/java");
    let package_path = package_name.replace(".", "/");
    let java_package_dir = java_dir.join(&package_path);
    
    if let Err(e) = std::fs::create_dir_all(&java_package_dir) {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Failed to create Java package directory: {}", e),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    let java_wrapper_path = java_package_dir.join("RustLib.java");
    let java_wrapper_content = format!(
        r#"package {0};

import androidx.annotation.NonNull;

/**
 * Java wrapper for Rust library
 */
public class RustLib {{
    
    static {{
        System.loadLibrary("rust_lib");
    }}
    
    /**
     * Example method that calls into Rust code
     * @return String returned from Rust
     */
    @NonNull
    public static native String getGreeting();
    
    /**
     * Example method that passes data to Rust
     * @param input String to process
     * @return Processed string from Rust
     */
    @NonNull
    public static native String processString(@NonNull String input);
}}"#,
        package_name
    );
    
    if let Err(e) = std::fs::write(&java_wrapper_path, java_wrapper_content) {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Failed to write Java wrapper class: {}", e),
            timestamp: current_time_millis(),
        });
        
        return BuildResult {
            success: false,
            output_messages,
            duration_ms: start_time.elapsed().as_millis() as u64,
            artifacts,
        };
    }
    
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: "Generated Java wrapper class: RustLib.java".to_string(),
        timestamp: current_time_millis(),
    });
    
    artifacts.push(java_wrapper_path.to_string_lossy().to_string());
    
    output_messages.push(OutputMessage {
        message_type: "SUCCESS".to_string(),
        content: "Android bindings generated successfully".to_string(),
        timestamp: current_time_millis(),
    });
    
    BuildResult {
        success: true,
        output_messages,
        duration_ms: start_time.elapsed().as_millis() as u64,
        artifacts,
    }
}

// Helper functions

// Get OS information
fn get_os_info() -> String {
    let mut info = String::new();
    
    // Try to get OS information from uname
    let uname_result = Command::new("uname")
        .arg("-a")
        .output();
    
    if let Ok(output) = uname_result {
        if output.status.success() {
            info = String::from_utf8_lossy(&output.stdout).trim().to_string();
            return info;
        }
    }
    
    // Fallback to basic OS detection
    #[cfg(target_os = "linux")]
    {
        info.push_str("Linux");
    }
    
    #[cfg(target_os = "android")]
    {
        info.push_str("Android");
    }
    
    #[cfg(target_os = "windows")]
    {
        info.push_str("Windows");
    }
    
    #[cfg(target_os = "macos")]
    {
        info.push_str("macOS");
    }
    
    #[cfg(not(any(target_os = "linux", target_os = "android", target_os = "windows", target_os = "macos")))]
    {
        info.push_str("Unknown OS");
    }
    
    info
}

// Check if NDK is installed
fn is_ndk_installed() -> bool {
    let ndk_build_paths = [
        "/opt/android-ndk/ndk-build",
        "/opt/android-sdk/ndk-bundle/ndk-build",
        "/opt/android-sdk/ndk/*/ndk-build",
        "/usr/local/android-ndk/ndk-build",
        "/usr/local/android-sdk/ndk-bundle/ndk-build",
        "/usr/local/android-sdk/ndk/*/ndk-build",
        "/home/*/Android/Sdk/ndk-bundle/ndk-build",
        "/home/*/Android/Sdk/ndk/*/ndk-build",
    ];
    
    for path_pattern in &ndk_build_paths {
        if path_pattern.contains("*") {
            // Handle wildcard paths
            let base_path = path_pattern.split("*").next().unwrap_or("");
            let base_dir = Path::new(base_path);
            
            if base_dir.exists() && base_dir.is_dir() {
                if let Ok(entries) = std::fs::read_dir(base_dir) {
                    for entry in entries.filter_map(Result::ok) {
                        let path = entry.path();
                        if path.is_dir() {
                            let ndk_build = path.join("ndk-build");
                            if ndk_build.exists() {
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            // Handle exact paths
            if Path::new(path_pattern).exists() {
                return true;
            }
        }
    }
    
    false
}

// Get current time in milliseconds
fn current_time_millis() -> u64 {
    use std::time::{SystemTime, UNIX_EPOCH};
    
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or(Duration::from_secs(0))
        .as_millis() as u64
}