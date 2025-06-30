use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use std::process::{Command, Stdio, Child, ChildStdin, ChildStdout, ChildStderr};
use std::io::{BufRead, BufReader, Write, Read};
use std::path::{Path, PathBuf};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use std::thread;
use serde::{Serialize, Deserialize};
use serde_json;
use std::env;
use std::fs;
use anyhow::{Result, anyhow};
use lazy_static::lazy_static;
use uuid::Uuid;

// Terminal session data
struct TerminalSession {
    id: String,
    working_directory: PathBuf,
    environment: HashMap<String, String>,
    current_process: Option<TerminalProcess>,
    history: Vec<String>,
    created_at: u64,
    last_activity: u64,
}

// Terminal process
struct TerminalProcess {
    process: Child,
    stdin: Option<ChildStdin>,
    stdout_reader: Option<BufReader<ChildStdout>>,
    stderr_reader: Option<BufReader<ChildStderr>>,
    command: String,
    start_time: u64,
}

// Terminal information
#[derive(Serialize, Deserialize)]
struct TerminalInfo {
    version: String,
    available: bool,
    features: Vec<String>,
    os_info: String,
    root_available: bool,
    shell_path: String,
    environment: HashMap<String, String>,
    capabilities: TerminalCapabilities,
}

// Terminal capabilities
#[derive(Serialize, Deserialize)]
struct TerminalCapabilities {
    supports_color: bool,
    supports_unicode: bool,
    supports_pipe: bool,
    supports_background_processes: bool,
    supports_job_control: bool,
    supports_signals: bool,
}

// Command output
#[derive(Serialize, Deserialize)]
struct CommandOutput {
    success: bool,
    output: Vec<String>,
    error_output: Vec<String>,
    exit_code: i32,
    execution_time_ms: u64,
    command: String,
    working_directory: String,
    timestamp: u64,
}

// Global sessions storage
lazy_static! {
    static ref SESSIONS: Arc<Mutex<HashMap<String, TerminalSession>>> = Arc::new(Mutex::new(HashMap::new()));
}

// Create a new terminal session
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeCreateSession(
    env: JNIEnv,
    _class: JClass,
    working_dir: JString,
) -> jstring {
    let working_dir: String = env
        .get_string(working_dir)
        .expect("Failed to get working directory string")
        .into();
    
    let session_id = format!("terminal_{}", Uuid::new_v4().to_string());
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    // Create environment variables
    let mut environment = HashMap::new();
    for (key, value) in env::vars() {
        environment.insert(key, value);
    }
    
    // Add custom environment variables
    environment.insert("TERM".to_string(), "xterm-256color".to_string());
    environment.insert("LANG".to_string(), "en_US.UTF-8".to_string());
    environment.insert("HOME".to_string(), working_dir.clone());
    environment.insert("PS1".to_string(), "\\[\\e[32m\\]\\u@\\h:\\[\\e[34m\\]\\w\\[\\e[0m\\]\\$ ".to_string());
    environment.insert("HISTSIZE".to_string(), "1000".to_string());
    environment.insert("HISTFILESIZE".to_string(), "2000".to_string());
    
    // Create session
    sessions.insert(
        session_id.clone(),
        TerminalSession {
            id: session_id.clone(),
            working_directory: PathBuf::from(working_dir),
            environment,
            current_process: None,
            history: Vec::new(),
            created_at: current_time_millis(),
            last_activity: current_time_millis(),
        },
    );
    
    let output = env
        .new_string(session_id)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Close a terminal session
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeCloseSession(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
) -> jni::sys::jboolean {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    if let Some(session) = sessions.get_mut(&session_id) {
        // Kill any running process
        if let Some(mut terminal_process) = session.current_process.take() {
            let _ = terminal_process.process.kill();
        }
        
        // Remove the session
        sessions.remove(&session_id);
        return 1; // true
    }
    
    0 // false
}

// Execute a command
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeExecuteCommand(
    env: JNIEnv,
    _class: JClass,
    command: JString,
    working_dir: JString,
) -> jstring {
    let command: String = env
        .get_string(command)
        .expect("Failed to get command string")
        .into();
    
    let working_dir: String = env
        .get_string(working_dir)
        .expect("Failed to get working directory string")
        .into();
    
    // Execute the command
    let start_time = Instant::now();
    let output = execute_command(&command, &working_dir);
    
    // Add execution time
    let mut output_with_time = output;
    output_with_time.execution_time_ms = start_time.elapsed().as_millis() as u64;
    output_with_time.command = command;
    output_with_time.working_directory = working_dir;
    output_with_time.timestamp = current_time_millis();
    
    // Convert output to JSON
    let json = serde_json::to_string(&output_with_time).unwrap_or_else(|_| "{}".to_string());
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Execute a root command
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeExecuteRootCommand(
    env: JNIEnv,
    _class: JClass,
    command: JString,
) -> jstring {
    let command: String = env
        .get_string(command)
        .expect("Failed to get command string")
        .into();
    
    // Execute the root command
    let start_time = Instant::now();
    let output = execute_root_command(&command);
    
    // Add execution time
    let mut output_with_time = output;
    output_with_time.execution_time_ms = start_time.elapsed().as_millis() as u64;
    output_with_time.command = format!("su -c '{}'", command);
    output_with_time.working_directory = "/".to_string();
    output_with_time.timestamp = current_time_millis();
    
    // Convert output to JSON
    let json = serde_json::to_string(&output_with_time).unwrap_or_else(|_| "{}".to_string());
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Check if root is available
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeIsRootAvailable(
    _env: JNIEnv,
    _class: JClass,
) -> jni::sys::jboolean {
    if is_root_available() {
        1 // true
    } else {
        0 // false
    }
}

// Get terminal information
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeGetTerminalInfo(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let info = TerminalInfo {
        version: "1.0.0".to_string(),
        available: true,
        features: vec![
            "Command execution".to_string(),
            "Root commands".to_string(),
            "Environment variables".to_string(),
            "Working directory management".to_string(),
            "Command history".to_string(),
            "Multiple sessions".to_string(),
            "Process management".to_string(),
            "File operations".to_string(),
            "Package management simulation".to_string(),
            "Git integration".to_string(),
            "Android tools".to_string(),
        ],
        os_info: get_os_info(),
        root_available: is_root_available(),
        shell_path: get_shell_path(),
        environment: env::vars().collect(),
        capabilities: TerminalCapabilities {
            supports_color: true,
            supports_unicode: true,
            supports_pipe: true,
            supports_background_processes: true,
            supports_job_control: false,
            supports_signals: true,
        },
    };
    
    // Convert to JSON
    let json = serde_json::to_string(&info).unwrap_or_else(|_| "{}".to_string());
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Get environment variables
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeGetEnvironmentVariables(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let environment: HashMap<String, String> = env::vars().collect();
    
    // Convert to JSON
    let json = serde_json::to_string(&environment).unwrap_or_else(|_| "{}".to_string());
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Set an environment variable
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeSetEnvironmentVariable(
    env: JNIEnv,
    _class: JClass,
    name: JString,
    value: JString,
) -> jni::sys::jboolean {
    let name: String = env
        .get_string(name)
        .expect("Failed to get name string")
        .into();
    
    let value: String = env
        .get_string(value)
        .expect("Failed to get value string")
        .into();
    
    // Set the environment variable
    env::set_var(name, value);
    
    // Update environment in all sessions
    let mut sessions = SESSIONS.lock().unwrap();
    for (_, session) in sessions.iter_mut() {
        session.environment.insert(name.clone(), value.clone());
    }
    
    1 // true
}

// Get working directory for a session
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeGetWorkingDirectory(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
) -> jstring {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let sessions = SESSIONS.lock().unwrap();
    
    let working_dir = if let Some(session) = sessions.get(&session_id) {
        session.working_directory.to_string_lossy().to_string()
    } else {
        "/".to_string()
    };
    
    let output = env
        .new_string(working_dir)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Change directory for a session
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeChangeDirectory(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
    directory: JString,
) -> jni::sys::jboolean {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let directory: String = env
        .get_string(directory)
        .expect("Failed to get directory string")
        .into();
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    if let Some(session) = sessions.get_mut(&session_id) {
        let new_dir = if directory.starts_with('/') {
            PathBuf::from(directory)
        } else if directory == "~" || directory == "$HOME" {
            if let Some(home) = session.environment.get("HOME") {
                PathBuf::from(home)
            } else {
                PathBuf::from("/")
            }
        } else if directory.starts_with("~/") || directory.starts_with("$HOME/") {
            if let Some(home) = session.environment.get("HOME") {
                let rel_path = if directory.starts_with("~/") {
                    &directory[2..]
                } else {
                    &directory[6..]
                };
                PathBuf::from(home).join(rel_path)
            } else {
                PathBuf::from("/")
            }
        } else {
            session.working_directory.join(directory)
        };
        
        if new_dir.exists() && new_dir.is_dir() {
            session.working_directory = new_dir;
            session.last_activity = current_time_millis();
            return 1; // true
        }
    }
    
    0 // false
}

// Stop a running command
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeStopCommand(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
) -> jni::sys::jboolean {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    if let Some(session) = sessions.get_mut(&session_id) {
        if let Some(terminal_process) = session.current_process.take() {
            let _ = terminal_process.process.kill();
            session.last_activity = current_time_millis();
            return 1; // true
        }
    }
    
    0 // false
}

// Interactive shell session
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeStartInteractiveShell(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
) -> jstring {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    let result = if let Some(session) = sessions.get_mut(&session_id) {
        // Check if there's already a process running
        if session.current_process.is_some() {
            serde_json::json!({
                "success": false,
                "message": "A process is already running in this session"
            })
        } else {
            // Start a new shell process
            let shell_path = get_shell_path();
            
            let process_result = Command::new(&shell_path)
                .current_dir(&session.working_directory)
                .env_clear()
                .envs(&session.environment)
                .stdin(Stdio::piped())
                .stdout(Stdio::piped())
                .stderr(Stdio::piped())
                .spawn();
            
            match process_result {
                Ok(mut process) => {
                    let stdin = process.stdin.take();
                    let stdout = process.stdout.take();
                    let stderr = process.stderr.take();
                    
                    let stdout_reader = stdout.map(BufReader::new);
                    let stderr_reader = stderr.map(BufReader::new);
                    
                    session.current_process = Some(TerminalProcess {
                        process,
                        stdin,
                        stdout_reader,
                        stderr_reader,
                        command: "interactive shell".to_string(),
                        start_time: current_time_millis(),
                    });
                    
                    session.last_activity = current_time_millis();
                    
                    serde_json::json!({
                        "success": true,
                        "message": "Interactive shell started",
                        "shell_path": shell_path
                    })
                }
                Err(e) => {
                    serde_json::json!({
                        "success": false,
                        "message": format!("Failed to start shell: {}", e)
                    })
                }
            }
        }
    } else {
        serde_json::json!({
            "success": false,
            "message": "Session not found"
        })
    };
    
    let json = result.to_string();
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Send input to interactive shell
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeSendInput(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
    input: JString,
) -> jstring {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let input: String = env
        .get_string(input)
        .expect("Failed to get input string")
        .into();
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    let result = if let Some(session) = sessions.get_mut(&session_id) {
        if let Some(terminal_process) = &mut session.current_process {
            if let Some(stdin) = &mut terminal_process.stdin {
                match writeln!(stdin, "{}", input) {
                    Ok(_) => {
                        session.history.push(input);
                        session.last_activity = current_time_millis();
                        
                        serde_json::json!({
                            "success": true,
                            "message": "Input sent to shell"
                        })
                    }
                    Err(e) => {
                        serde_json::json!({
                            "success": false,
                            "message": format!("Failed to send input to shell: {}", e)
                        })
                    }
                }
            } else {
                serde_json::json!({
                    "success": false,
                    "message": "Shell stdin not available"
                })
            }
        } else {
            serde_json::json!({
                "success": false,
                "message": "No interactive shell running in this session"
            })
        }
    } else {
        serde_json::json!({
            "success": false,
            "message": "Session not found"
        })
    };
    
    let json = result.to_string();
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Read output from interactive shell
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeReadOutput(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
    timeout_ms: jni::sys::jlong,
) -> jstring {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let timeout_ms = timeout_ms as u64;
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    let result = if let Some(session) = sessions.get_mut(&session_id) {
        if let Some(terminal_process) = &mut session.current_process {
            let mut stdout_output = Vec::new();
            let mut stderr_output = Vec::new();
            
            // Read stdout
            if let Some(stdout_reader) = &mut terminal_process.stdout_reader {
                let start_time = Instant::now();
                let mut buffer = String::new();
                
                while start_time.elapsed().as_millis() < timeout_ms as u128 {
                    match stdout_reader.read_line(&mut buffer) {
                        Ok(0) => break, // EOF
                        Ok(_) => {
                            stdout_output.push(buffer.clone());
                            buffer.clear();
                        }
                        Err(_) => break,
                    }
                }
            }
            
            // Read stderr
            if let Some(stderr_reader) = &mut terminal_process.stderr_reader {
                let start_time = Instant::now();
                let mut buffer = String::new();
                
                while start_time.elapsed().as_millis() < timeout_ms as u128 {
                    match stderr_reader.read_line(&mut buffer) {
                        Ok(0) => break, // EOF
                        Ok(_) => {
                            stderr_output.push(buffer.clone());
                            buffer.clear();
                        }
                        Err(_) => break,
                    }
                }
            }
            
            session.last_activity = current_time_millis();
            
            serde_json::json!({
                "success": true,
                "stdout": stdout_output,
                "stderr": stderr_output,
                "timestamp": current_time_millis()
            })
        } else {
            serde_json::json!({
                "success": false,
                "message": "No interactive shell running in this session"
            })
        }
    } else {
        serde_json::json!({
            "success": false,
            "message": "Session not found"
        })
    };
    
    let json = result.to_string();
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Check if interactive shell is running
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeIsShellRunning(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
) -> jni::sys::jboolean {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let sessions = SESSIONS.lock().unwrap();
    
    if let Some(session) = sessions.get(&session_id) {
        if let Some(terminal_process) = &session.current_process {
            match terminal_process.process.try_wait() {
                Ok(None) => return 1, // Process is still running
                _ => return 0,        // Process has exited or error
            }
        }
    }
    
    0 // false
}

// Get command history
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeGetCommandHistory(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
) -> jstring {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let sessions = SESSIONS.lock().unwrap();
    
    let result = if let Some(session) = sessions.get(&session_id) {
        serde_json::json!({
            "success": true,
            "history": session.history
        })
    } else {
        serde_json::json!({
            "success": false,
            "message": "Session not found"
        })
    };
    
    let json = result.to_string();
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Save command history to file
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeSaveCommandHistory(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
    file_path: JString,
) -> jni::sys::jboolean {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let file_path: String = env
        .get_string(file_path)
        .expect("Failed to get file path string")
        .into();
    
    let sessions = SESSIONS.lock().unwrap();
    
    if let Some(session) = sessions.get(&session_id) {
        let history = session.history.join("\n");
        match fs::write(file_path, history) {
            Ok(_) => return 1, // true
            Err(_) => return 0, // false
        }
    }
    
    0 // false
}

// Load command history from file
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeLoadCommandHistory(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
    file_path: JString,
) -> jni::sys::jboolean {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let file_path: String = env
        .get_string(file_path)
        .expect("Failed to get file path string")
        .into();
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    if let Some(session) = sessions.get_mut(&session_id) {
        match fs::read_to_string(file_path) {
            Ok(content) => {
                let history: Vec<String> = content.lines().map(|s| s.to_string()).collect();
                session.history = history;
                return 1; // true
            }
            Err(_) => return 0, // false
        }
    }
    
    0 // false
}

// Get session information
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeGetSessionInfo(
    env: JNIEnv,
    _class: JClass,
    session_id: JString,
) -> jstring {
    let session_id: String = env
        .get_string(session_id)
        .expect("Failed to get session ID string")
        .into();
    
    let sessions = SESSIONS.lock().unwrap();
    
    let result = if let Some(session) = sessions.get(&session_id) {
        let process_running = if let Some(terminal_process) = &session.current_process {
            match terminal_process.process.try_wait() {
                Ok(None) => true, // Process is still running
                _ => false,       // Process has exited or error
            }
        } else {
            false
        };
        
        let current_command = if let Some(terminal_process) = &session.current_process {
            terminal_process.command.clone()
        } else {
            String::new()
        };
        
        let process_start_time = if let Some(terminal_process) = &session.current_process {
            terminal_process.start_time
        } else {
            0
        };
        
        serde_json::json!({
            "success": true,
            "session_id": session.id,
            "working_directory": session.working_directory.to_string_lossy(),
            "process_running": process_running,
            "current_command": current_command,
            "process_start_time": process_start_time,
            "history_size": session.history.len(),
            "created_at": session.created_at,
            "last_activity": session.last_activity,
            "idle_time_seconds": (current_time_millis() - session.last_activity) / 1000
        })
    } else {
        serde_json::json!({
            "success": false,
            "message": "Session not found"
        })
    };
    
    let json = result.to_string();
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// List all sessions
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeListSessions(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let sessions = SESSIONS.lock().unwrap();
    
    let session_list: Vec<_> = sessions.values().map(|session| {
        let process_running = if let Some(terminal_process) = &session.current_process {
            match terminal_process.process.try_wait() {
                Ok(None) => true, // Process is still running
                _ => false,       // Process has exited or error
            }
        } else {
            false
        };
        
        let current_command = if let Some(terminal_process) = &session.current_process {
            terminal_process.command.clone()
        } else {
            String::new()
        };
        
        serde_json::json!({
            "session_id": session.id,
            "working_directory": session.working_directory.to_string_lossy(),
            "process_running": process_running,
            "current_command": current_command,
            "history_size": session.history.len(),
            "created_at": session.created_at,
            "last_activity": session.last_activity,
            "idle_time_seconds": (current_time_millis() - session.last_activity) / 1000
        })
    }).collect();
    
    let result = serde_json::json!({
        "success": true,
        "sessions": session_list
    });
    
    let json = result.to_string();
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Clean up inactive sessions
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustTerminalManager_00024Companion_nativeCleanupInactiveSessions(
    env: JNIEnv,
    _class: JClass,
    max_idle_time_seconds: jni::sys::jlong,
) -> jstring {
    let max_idle_time_seconds = max_idle_time_seconds as u64;
    let max_idle_time_millis = max_idle_time_seconds * 1000;
    let current_time = current_time_millis();
    
    let mut sessions = SESSIONS.lock().unwrap();
    
    let mut sessions_to_remove = Vec::new();
    
    for (id, session) in sessions.iter() {
        let idle_time = current_time - session.last_activity;
        if idle_time > max_idle_time_millis {
            sessions_to_remove.push(id.clone());
        }
    }
    
    for id in &sessions_to_remove {
        if let Some(session) = sessions.get_mut(id) {
            // Kill any running process
            if let Some(mut terminal_process) = session.current_process.take() {
                let _ = terminal_process.process.kill();
            }
        }
        
        sessions.remove(id);
    }
    
    let result = serde_json::json!({
        "success": true,
        "removed_sessions": sessions_to_remove,
        "remaining_sessions": sessions.len()
    });
    
    let json = result.to_string();
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

// Helper function to execute a command
fn execute_command(command: &str, working_dir: &str) -> CommandOutput {
    let mut output_lines = Vec::new();
    let mut error_lines = Vec::new();
    let start_time = Instant::now();
    
    // Handle built-in commands
    if command.trim() == "clear" {
        return CommandOutput {
            success: true,
            output: Vec::new(),
            error_output: Vec::new(),
            exit_code: 0,
            execution_time_ms: 0,
            command: command.to_string(),
            working_directory: working_dir.to_string(),
            timestamp: current_time_millis(),
        };
    }
    
    if command.starts_with("cd ") {
        let dir = command[3..].trim();
        let path = if dir.starts_with('/') {
            PathBuf::from(dir)
        } else {
            PathBuf::from(working_dir).join(dir)
        };
        
        if path.exists() && path.is_dir() {
            return CommandOutput {
                success: true,
                output: Vec::new(),
                error_output: Vec::new(),
                exit_code: 0,
                execution_time_ms: 0,
                command: command.to_string(),
                working_directory: path.to_string_lossy().to_string(),
                timestamp: current_time_millis(),
            };
        } else {
            return CommandOutput {
                success: false,
                output: Vec::new(),
                error_output: vec![format!("cd: {}: No such file or directory", dir)],
                exit_code: 1,
                execution_time_ms: 0,
                command: command.to_string(),
                working_directory: working_dir.to_string(),
                timestamp: current_time_millis(),
            };
        }
    }
    
    // Execute command with shell
    let result = Command::new("sh")
        .arg("-c")
        .arg(command)
        .current_dir(working_dir)
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn();
    
    match result {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        output_lines.push(line);
                    }
                }
            }
            
            // Read stderr
            if let Some(stderr) = child.stderr.take() {
                let reader = BufReader::new(stderr);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        error_lines.push(line);
                    }
                }
            }
            
            // Wait for the process to complete
            match child.wait() {
                Ok(status) => {
                    CommandOutput {
                        success: status.success(),
                        output: output_lines,
                        error_output: error_lines,
                        exit_code: status.code().unwrap_or(-1),
                        execution_time_ms: start_time.elapsed().as_millis() as u64,
                        command: command.to_string(),
                        working_directory: working_dir.to_string(),
                        timestamp: current_time_millis(),
                    }
                }
                Err(e) => {
                    CommandOutput {
                        success: false,
                        output: output_lines,
                        error_output: vec![format!("Failed to wait for process: {}", e)],
                        exit_code: -1,
                        execution_time_ms: start_time.elapsed().as_millis() as u64,
                        command: command.to_string(),
                        working_directory: working_dir.to_string(),
                        timestamp: current_time_millis(),
                    }
                }
            }
        }
        Err(e) => {
            CommandOutput {
                success: false,
                output: Vec::new(),
                error_output: vec![format!("Failed to execute command: {}", e)],
                exit_code: -1,
                execution_time_ms: start_time.elapsed().as_millis() as u64,
                command: command.to_string(),
                working_directory: working_dir.to_string(),
                timestamp: current_time_millis(),
            }
        }
    }
}

// Helper function to execute a root command
fn execute_root_command(command: &str) -> CommandOutput {
    let mut output_lines = Vec::new();
    let mut error_lines = Vec::new();
    let start_time = Instant::now();
    
    let result = Command::new("su")
        .arg("-c")
        .arg(command)
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn();
    
    match result {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let reader = BufReader::new(stdout);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        output_lines.push(line);
                    }
                }
            }
            
            // Read stderr
            if let Some(stderr) = child.stderr.take() {
                let reader = BufReader::new(stderr);
                for line in reader.lines() {
                    if let Ok(line) = line {
                        error_lines.push(line);
                    }
                }
            }
            
            // Wait for the process to complete
            match child.wait() {
                Ok(status) => {
                    CommandOutput {
                        success: status.success(),
                        output: output_lines,
                        error_output: error_lines,
                        exit_code: status.code().unwrap_or(-1),
                        execution_time_ms: start_time.elapsed().as_millis() as u64,
                        command: format!("su -c '{}'", command),
                        working_directory: "/".to_string(),
                        timestamp: current_time_millis(),
                    }
                }
                Err(e) => {
                    CommandOutput {
                        success: false,
                        output: output_lines,
                        error_output: vec![format!("Failed to wait for process: {}", e)],
                        exit_code: -1,
                        execution_time_ms: start_time.elapsed().as_millis() as u64,
                        command: format!("su -c '{}'", command),
                        working_directory: "/".to_string(),
                        timestamp: current_time_millis(),
                    }
                }
            }
        }
        Err(e) => {
            CommandOutput {
                success: false,
                output: Vec::new(),
                error_output: vec![format!("Failed to execute root command: {}", e)],
                exit_code: -1,
                execution_time_ms: start_time.elapsed().as_millis() as u64,
                command: format!("su -c '{}'", command),
                working_directory: "/".to_string(),
                timestamp: current_time_millis(),
            }
        }
    }
}

// Check if root is available
fn is_root_available() -> bool {
    let result = Command::new("su")
        .arg("-c")
        .arg("id -u")
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .spawn();
    
    match result {
        Ok(mut child) => {
            // Read stdout
            if let Some(stdout) = child.stdout.take() {
                let mut reader = BufReader::new(stdout);
                let mut output = String::new();
                if reader.read_line(&mut output).is_ok() {
                    let output = output.trim();
                    if output == "0" {
                        return true;
                    }
                }
            }
            
            match child.wait() {
                Ok(status) => status.success(),
                Err(_) => false,
            }
        }
        Err(_) => false,
    }
}

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

// Get shell path
fn get_shell_path() -> String {
    // Try to get shell from environment
    if let Ok(shell) = env::var("SHELL") {
        if Path::new(&shell).exists() {
            return shell;
        }
    }
    
    // Check common shell locations
    let common_shells = vec![
        "/system/bin/sh",
        "/bin/bash",
        "/bin/sh",
        "/bin/zsh",
        "/system/bin/bash",
    ];
    
    for shell in common_shells {
        if Path::new(shell).exists() {
            return shell.to_string();
        }
    }
    
    // Default to /system/bin/sh for Android
    "/system/bin/sh".to_string()
}

// Get current time in milliseconds
fn current_time_millis() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or(Duration::from_secs(0))
        .as_millis() as u64
}

// Advanced terminal features

// Parse command for auto-completion
pub fn parse_command_for_completion(command: &str) -> Vec<String> {
    let mut completions = Vec::new();
    
    // Get the last word in the command
    let words: Vec<&str> = command.split_whitespace().collect();
    if let Some(last_word) = words.last() {
        // If the last word is a path, complete it
        if last_word.contains('/') {
            let path = Path::new(last_word);
            let parent = path.parent().unwrap_or_else(|| Path::new(""));
            let prefix = path.file_name().map_or("", |name| name.to_str().unwrap_or(""));
            
            if let Ok(entries) = fs::read_dir(parent) {
                for entry in entries.filter_map(Result::ok) {
                    let file_name = entry.file_name();
                    let file_name_str = file_name.to_string_lossy();
                    
                    if file_name_str.starts_with(prefix) {
                        let mut completion = parent.join(&file_name).to_string_lossy().to_string();
                        
                        // Add trailing slash for directories
                        if entry.file_type().map(|ft| ft.is_dir()).unwrap_or(false) {
                            completion.push('/');
                        }
                        
                        completions.push(completion);
                    }
                }
            }
        } else {
            // Complete commands
            let common_commands = vec![
                "ls", "cd", "pwd", "mkdir", "rm", "cp", "mv", "cat", "grep", "find",
                "ps", "kill", "chmod", "which", "uname", "whoami", "date", "clear",
                "echo", "sh", "bash", "touch", "ln", "du", "df", "tar", "gzip", "gunzip",
                "apt", "dpkg", "git", "adb", "gradle", "javac", "kotlinc", "cargo", "rustc",
            ];
            
            for cmd in common_commands {
                if cmd.starts_with(last_word) {
                    completions.push(cmd.to_string());
                }
            }
        }
    }
    
    completions
}

// Syntax highlight terminal output
pub fn syntax_highlight_terminal_output(output: &str) -> String {
    // This is a simplified implementation
    // In a real implementation, this would use a proper syntax highlighter
    
    let mut highlighted = String::new();
    
    for line in output.lines() {
        if line.starts_with("error:") || line.contains(": error:") {
            highlighted.push_str(&format!("\x1b[31m{}\x1b[0m\n", line)); // Red for errors
        } else if line.starts_with("warning:") || line.contains(": warning:") {
            highlighted.push_str(&format!("\x1b[33m{}\x1b[0m\n", line)); // Yellow for warnings
        } else if line.starts_with("#") || line.starts_with("//") {
            highlighted.push_str(&format!("\x1b[32m{}\x1b[0m\n", line)); // Green for comments
        } else if line.contains("success") || line.contains("completed") {
            highlighted.push_str(&format!("\x1b[32m{}\x1b[0m\n", line)); // Green for success
        } else {
            highlighted.push_str(&format!("{}\n", line));
        }
    }
    
    highlighted
}

// Process management functions

// List processes
pub fn list_processes() -> Result<Vec<ProcessInfo>> {
    let mut processes = Vec::new();
    
    let output = Command::new("ps")
        .arg("-ef")
        .output()?;
    
    if output.status.success() {
        let stdout = String::from_utf8_lossy(&output.stdout);
        
        for line in stdout.lines().skip(1) { // Skip header
            let fields: Vec<&str> = line.split_whitespace().collect();
            if fields.len() >= 8 {
                let pid = fields[1].parse::<u32>().unwrap_or(0);
                let ppid = fields[2].parse::<u32>().unwrap_or(0);
                let cpu = fields[3].parse::<f32>().unwrap_or(0.0);
                let mem = fields[4].parse::<f32>().unwrap_or(0.0);
                let vsz = fields[5].parse::<u64>().unwrap_or(0);
                let rss = fields[6].parse::<u64>().unwrap_or(0);
                let command = fields[7..].join(" ");
                
                processes.push(ProcessInfo {
                    pid,
                    ppid,
                    user: fields[0].to_string(),
                    cpu,
                    mem,
                    vsz,
                    rss,
                    command,
                });
            }
        }
    }
    
    Ok(processes)
}

// Kill process
pub fn kill_process(pid: u32) -> Result<()> {
    let status = Command::new("kill")
        .arg(pid.to_string())
        .status()?;
    
    if status.success() {
        Ok(())
    } else {
        Err(anyhow!("Failed to kill process"))
    }
}

// Process information
#[derive(Serialize, Deserialize)]
pub struct ProcessInfo {
    pub pid: u32,
    pub ppid: u32,
    pub user: String,
    pub cpu: f32,
    pub mem: f32,
    pub vsz: u64,
    pub rss: u64,
    pub command: String,
}