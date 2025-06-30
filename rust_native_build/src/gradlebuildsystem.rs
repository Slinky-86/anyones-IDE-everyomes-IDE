use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use std::process::{Command, Stdio};
use std::io::{BufRead, BufReader};
use std::path::{Path, PathBuf};
use std::time::{Duration, Instant};
use serde::{Serialize, Deserialize};
use serde_json;
use anyhow::{Result, anyhow};

// Gradle task
#[derive(Serialize, Deserialize, Clone, Debug)]
struct GradleTask {
    name: String,
    description: String,
    group: String,
}

// Gradle build result
#[derive(Serialize, Deserialize)]
struct GradleBuildResult {
    success: bool,
    duration_ms: u64,
    output_messages: Vec<OutputMessage>,
    artifacts: Vec<String>,
}

// Output message
#[derive(Serialize, Deserialize)]
struct OutputMessage {
    message_type: String, // "INFO", "WARNING", "ERROR", "SUCCESS", "TASK", "ARTIFACT"
    content: String,
    timestamp: u64,
}

// Execute a Gradle task
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeExecuteGradleTask(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
    task_name: JString,
    args_json: JString,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let task_name: String = env
        .get_string(task_name)
        .expect("Failed to get task name string")
        .into();
    
    let args_json: String = env
        .get_string(args_json)
        .expect("Failed to get args JSON string")
        .into();
    
    let start_time = Instant::now();
    let mut output_messages = Vec::new();
    let mut artifacts = Vec::new();
    
    // Parse additional arguments
    let args: Vec<String> = match serde_json::from_str(&args_json) {
        Ok(args) => args,
        Err(_) => Vec::new(),
    };
    
    // Add initial message
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Executing Gradle task: {}", task_name),
        timestamp: current_time_millis(),
    });
    
    // Check if project directory exists
    let project_dir = Path::new(&project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Project directory does not exist: {}", project_path),
            timestamp: current_time_millis(),
        });
        
        return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
    }
    
    // Check for Gradle wrapper
    let gradlew_file = project_dir.join(if cfg!(windows) { "gradlew.bat" } else { "gradlew" });
    let use_wrapper = gradlew_file.exists();
    
    // Build command
    let gradle_command = if use_wrapper {
        if cfg!(windows) {
            "gradlew.bat"
        } else {
            "./gradlew"
        }
    } else {
        "gradle"
    };
    
    // Make gradlew executable if needed
    if use_wrapper && !cfg!(windows) {
        let _ = Command::new("chmod")
            .arg("+x")
            .arg(&gradlew_file)
            .output();
    }
    
    // Build full command with arguments
    let mut cmd_args = vec![task_name];
    cmd_args.extend(args);
    
    // Add console plain for better output parsing
    cmd_args.push("--console=plain".to_string());
    
    // Execute Gradle task
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Executing: {} {}", gradle_command, cmd_args.join(" ")),
        timestamp: current_time_millis(),
    });
    
    let mut cmd = Command::new(gradle_command);
    cmd.current_dir(project_dir);
    cmd.args(&cmd_args);
    
    match cmd.stdout(Stdio::piped()).stderr(Stdio::piped()).spawn() {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        // Parse output line
                        let (message_type, content) = parse_gradle_output_line(&line);
                        
                        output_messages.push(OutputMessage {
                            message_type: message_type.to_string(),
                            content,
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
                            content: format!("Gradle task '{}' completed successfully", task_name),
                            timestamp: current_time_millis(),
                        });
                        
                        // Find build artifacts
                        if task_name.contains("assemble") || task_name.contains("build") {
                            let artifacts_found = find_build_artifacts(project_dir, &task_name);
                            artifacts.extend(artifacts_found);
                            
                            for artifact in &artifacts {
                                output_messages.push(OutputMessage {
                                    message_type: "ARTIFACT".to_string(),
                                    content: format!("Generated: {}", artifact),
                                    timestamp: current_time_millis(),
                                });
                            }
                        }
                    } else {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: format!("Gradle task '{}' failed with exit code: {}", task_name, status.code().unwrap_or(-1)),
                            timestamp: current_time_millis(),
                        });
                    }
                    
                    return create_gradle_result_json(env, success, start_time, output_messages, artifacts);
                }
                Err(e) => {
                    output_messages.push(OutputMessage {
                        message_type: "ERROR".to_string(),
                        content: format!("Failed to wait for Gradle process: {}", e),
                        timestamp: current_time_millis(),
                    });
                    
                    return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
                }
            }
        }
        Err(e) => {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to start Gradle process: {}", e),
                timestamp: current_time_millis(),
            });
            
            return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
        }
    }
}

// Get Gradle tasks
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeGetGradleTasks(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    // Check if project directory exists
    let project_dir = Path::new(&project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        let error_json = format!("{{\"success\": false, \"error\": \"Project directory does not exist: {}\"}}", project_path);
        let output = env
            .new_string(error_json)
            .expect("Failed to create Java string");
        return output.into_raw();
    }
    
    // Check for Gradle wrapper
    let gradlew_file = project_dir.join(if cfg!(windows) { "gradlew.bat" } else { "gradlew" });
    let use_wrapper = gradlew_file.exists();
    
    // Build command
    let gradle_command = if use_wrapper {
        if cfg!(windows) {
            "gradlew.bat"
        } else {
            "./gradlew"
        }
    } else {
        "gradle"
    };
    
    // Make gradlew executable if needed
    if use_wrapper && !cfg!(windows) {
        let _ = Command::new("chmod")
            .arg("+x")
            .arg(&gradlew_file)
            .output();
    }
    
    // Execute Gradle tasks command
    let mut cmd = Command::new(gradle_command);
    cmd.current_dir(project_dir);
    cmd.args(&["tasks", "--all", "--console=plain"]);
    
    match cmd.stdout(Stdio::piped()).stderr(Stdio::piped()).spawn() {
        Ok(mut child) => {
            let mut tasks = Vec::new();
            let mut current_group = "Other".to_string();
            let mut in_tasks_section = false;
            
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        // Parse tasks output
                        if line.contains("All tasks runnable from root project") {
                            in_tasks_section = true;
                            continue;
                        }
                        
                        if in_tasks_section {
                            if line.trim().is_empty() {
                                continue;
                            }
                            
                            if line.ends_with("tasks") && line.contains("----") {
                                // This is a group header
                                current_group = line.split_whitespace().next().unwrap_or("Other").to_string();
                                continue;
                            }
                            
                            // Parse task
                            let parts: Vec<&str> = line.splitn(2, " - ").collect();
                            if parts.len() == 2 {
                                let task_name = parts[0].trim().to_string();
                                let description = parts[1].trim().to_string();
                                
                                // Skip help tasks
                                if task_name == "help" || task_name == "tasks" {
                                    continue;
                                }
                                
                                tasks.push(GradleTask {
                                    name: task_name,
                                    description,
                                    group: current_group.clone(),
                                });
                            }
                        }
                    }
                }
            }
            
            // Wait for the process to complete
            let _ = child.wait();
            
            // Convert to JSON
            let json = serde_json::to_string(&tasks).unwrap_or_else(|_| "[]".to_string());
            
            let output = env
                .new_string(json)
                .expect("Failed to create Java string");
            output.into_raw()
        }
        Err(e) => {
            let error_json = format!("{{\"success\": false, \"error\": \"Failed to start Gradle process: {}\"}}", e);
            let output = env
                .new_string(error_json)
                .expect("Failed to create Java string");
            output.into_raw()
        }
    }
}

// Build an Android project
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeBuildAndroidProject(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
    build_type: JString,
    args_json: JString,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let build_type: String = env
        .get_string(build_type)
        .expect("Failed to get build type string")
        .into();
    
    let args_json: String = env
        .get_string(args_json)
        .expect("Failed to get args JSON string")
        .into();
    
    let start_time = Instant::now();
    let mut output_messages = Vec::new();
    let mut artifacts = Vec::new();
    
    // Parse additional arguments
    let args: Vec<String> = match serde_json::from_str(&args_json) {
        Ok(args) => args,
        Err(_) => Vec::new(),
    };
    
    // Add initial message
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Building Android project with type: {}", build_type),
        timestamp: current_time_millis(),
    });
    
    // Check if project directory exists
    let project_dir = Path::new(&project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("Project directory does not exist: {}", project_path),
            timestamp: current_time_millis(),
        });
        
        return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
    }
    
    // Determine build task
    let task = match build_type.as_str() {
        "debug" => "assembleDebug",
        "release" => "assembleRelease",
        "clean" => "clean",
        "test" => "test",
        _ => build_type.as_str(),
    };
    
    // Check for Gradle wrapper
    let gradlew_file = project_dir.join(if cfg!(windows) { "gradlew.bat" } else { "gradlew" });
    let use_wrapper = gradlew_file.exists();
    
    // Build command
    let gradle_command = if use_wrapper {
        if cfg!(windows) {
            "gradlew.bat"
        } else {
            "./gradlew"
        }
    } else {
        "gradle"
    };
    
    // Make gradlew executable if needed
    if use_wrapper && !cfg!(windows) {
        let _ = Command::new("chmod")
            .arg("+x")
            .arg(&gradlew_file)
            .output();
    }
    
    // Build full command with arguments
    let mut cmd_args = vec![task.to_string()];
    cmd_args.extend(args);
    
    // Add console plain for better output parsing
    cmd_args.push("--console=plain".to_string());
    
    // Execute Gradle build
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Executing: {} {}", gradle_command, cmd_args.join(" ")),
        timestamp: current_time_millis(),
    });
    
    let mut cmd = Command::new(gradle_command);
    cmd.current_dir(project_dir);
    cmd.args(&cmd_args);
    
    match cmd.stdout(Stdio::piped()).stderr(Stdio::piped()).spawn() {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        // Parse output line
                        let (message_type, content) = parse_gradle_output_line(&line);
                        
                        output_messages.push(OutputMessage {
                            message_type: message_type.to_string(),
                            content,
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
                        
                        // Find build artifacts
                        let artifacts_found = find_build_artifacts(project_dir, task);
                        artifacts.extend(artifacts_found);
                        
                        for artifact in &artifacts {
                            output_messages.push(OutputMessage {
                                message_type: "ARTIFACT".to_string(),
                                content: format!("Generated: {}", artifact),
                                timestamp: current_time_millis(),
                            });
                        }
                    } else {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: format!("Build failed with exit code: {}", status.code().unwrap_or(-1)),
                            timestamp: current_time_millis(),
                        });
                    }
                    
                    return create_gradle_result_json(env, success, start_time, output_messages, artifacts);
                }
                Err(e) => {
                    output_messages.push(OutputMessage {
                        message_type: "ERROR".to_string(),
                        content: format!("Failed to wait for Gradle process: {}", e),
                        timestamp: current_time_millis(),
                    });
                    
                    return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
                }
            }
        }
        Err(e) => {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to start Gradle process: {}", e),
                timestamp: current_time_millis(),
            });
            
            return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
        }
    }
}

// Install an APK
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeInstallApk(
    env: JNIEnv,
    _class: JClass,
    apk_path: JString,
) -> jstring {
    let apk_path: String = env
        .get_string(apk_path)
        .expect("Failed to get APK path string")
        .into();
    
    let start_time = Instant::now();
    let mut output_messages = Vec::new();
    let artifacts = Vec::new();
    
    // Add initial message
    output_messages.push(OutputMessage {
        message_type: "INFO".to_string(),
        content: format!("Installing APK: {}", apk_path),
        timestamp: current_time_millis(),
    });
    
    // Check if APK file exists
    let apk_file = Path::new(&apk_path);
    if !apk_file.exists() || !apk_file.is_file() {
        output_messages.push(OutputMessage {
            message_type: "ERROR".to_string(),
            content: format!("APK file does not exist: {}", apk_path),
            timestamp: current_time_millis(),
        });
        
        return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
    }
    
    // Install APK using adb
    let mut cmd = Command::new("adb");
    cmd.args(&["install", "-r", "-t", &apk_path]);
    
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
                            content: "APK installed successfully".to_string(),
                            timestamp: current_time_millis(),
                        });
                    } else {
                        output_messages.push(OutputMessage {
                            message_type: "ERROR".to_string(),
                            content: format!("APK installation failed with exit code: {}", status.code().unwrap_or(-1)),
                            timestamp: current_time_millis(),
                        });
                    }
                    
                    return create_gradle_result_json(env, success, start_time, output_messages, artifacts);
                }
                Err(e) => {
                    output_messages.push(OutputMessage {
                        message_type: "ERROR".to_string(),
                        content: format!("Failed to wait for adb process: {}", e),
                        timestamp: current_time_millis(),
                    });
                    
                    return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
                }
            }
        }
        Err(e) => {
            output_messages.push(OutputMessage {
                message_type: "ERROR".to_string(),
                content: format!("Failed to start adb process: {}", e),
                timestamp: current_time_millis(),
            });
            
            return create_gradle_result_json(env, false, start_time, output_messages, artifacts);
        }
    }
}

// Helper function to parse Gradle output line
fn parse_gradle_output_line(line: &str) -> (&str, String) {
    if line.contains("BUILD SUCCESSFUL") {
        ("SUCCESS", line.to_string())
    } else if line.contains("BUILD FAILED") {
        ("ERROR", line.to_string())
    } else if line.contains("error") || line.contains("Error") || line.contains("ERROR") {
        ("ERROR", line.to_string())
    } else if line.contains("warning") || line.contains("Warning") || line.contains("WARNING") {
        ("WARNING", line.to_string())
    } else if line.starts_with("> Task :") {
        ("TASK", line.to_string())
    } else {
        ("INFO", line.to_string())
    }
}

// Helper function to find build artifacts
fn find_build_artifacts(project_dir: &Path, task: &str) -> Vec<String> {
    let mut artifacts = Vec::new();
    
    // Determine build type from task
    let build_type = if task.contains("Debug") {
        "debug"
    } else if task.contains("Release") {
        "release"
    } else {
        return artifacts;
    };
    
    // Look for APK files
    let app_dir = project_dir.join("app");
    if app_dir.exists() {
        let apk_dir = app_dir.join("build/outputs/apk").join(build_type);
        if apk_dir.exists() {
            if let Ok(entries) = std::fs::read_dir(apk_dir) {
                for entry in entries.flatten() {
                    let path = entry.path();
                    if path.is_file() && path.extension().map_or(false, |ext| ext == "apk") {
                        artifacts.push(path.to_string_lossy().to_string());
                    }
                }
            }
        }
    }
    
    // Look for AAB files
    if app_dir.exists() {
        let aab_dir = app_dir.join("build/outputs/bundle").join(build_type);
        if aab_dir.exists() {
            if let Ok(entries) = std::fs::read_dir(aab_dir) {
                for entry in entries.flatten() {
                    let path = entry.path();
                    if path.is_file() && path.extension().map_or(false, |ext| ext == "aab") {
                        artifacts.push(path.to_string_lossy().to_string());
                    }
                }
            }
        }
    }
    
    artifacts
}

// Helper function to create Gradle result JSON
fn create_gradle_result_json(
    env: JNIEnv,
    success: bool,
    start_time: Instant,
    output_messages: Vec<OutputMessage>,
    artifacts: Vec<String>,
) -> jstring {
    let duration_ms = start_time.elapsed().as_millis() as u64;
    
    let result = GradleBuildResult {
        success,
        duration_ms,
        output_messages,
        artifacts,
    };
    
    // Convert to JSON
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Helper function to get current time in milliseconds
fn current_time_millis() -> u64 {
    use std::time::{SystemTime, UNIX_EPOCH};
    
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or(Duration::from_secs(0))
        .as_millis() as u64
}