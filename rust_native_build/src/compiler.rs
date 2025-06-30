use std::process::{Command, Stdio};
use std::io::{BufRead, BufReader};
use std::path::{Path, PathBuf};
use std::time::{Duration, Instant};
use std::fs;
use std::collections::HashMap;
use serde::{Serialize, Deserialize};
use anyhow::{Result, anyhow};

// Compilation result
#[derive(Serialize, Deserialize)]
pub struct CompilationResult {
    pub success: bool,
    pub output: Vec<String>,
    pub errors: Vec<CompilationError>,
    pub warnings: Vec<CompilationWarning>,
    pub duration_ms: u64,
    pub artifacts: Vec<String>,
}

// Compilation error
#[derive(Serialize, Deserialize)]
pub struct CompilationError {
    pub file: String,
    pub line: u32,
    pub column: u32,
    pub message: String,
    pub code: Option<String>,
}

// Compilation warning
#[derive(Serialize, Deserialize)]
pub struct CompilationWarning {
    pub file: String,
    pub line: u32,
    pub column: u32,
    pub message: String,
    pub code: Option<String>,
}

// Compiler options
#[derive(Serialize, Deserialize)]
pub struct CompilerOptions {
    pub optimization_level: u8,
    pub debug_info: bool,
    pub warnings_as_errors: bool,
    pub extra_flags: Vec<String>,
    pub target: Option<String>,
    pub features: Vec<String>,
    pub no_default_features: bool,
    pub all_features: bool,
}

impl Default for CompilerOptions {
    fn default() -> Self {
        CompilerOptions {
            optimization_level: 0,
            debug_info: true,
            warnings_as_errors: false,
            extra_flags: Vec::new(),
            target: None,
            features: Vec::new(),
            no_default_features: false,
            all_features: false,
        }
    }
}

// Compile Rust code
pub fn compile_rust(project_path: &str, options: &CompilerOptions) -> Result<CompilationResult> {
    let start_time = Instant::now();
    let mut output = Vec::new();
    let mut errors = Vec::new();
    let mut warnings = Vec::new();
    let mut artifacts = Vec::new();
    
    // Check if project exists
    let project_dir = Path::new(project_path);
    if !project_dir.exists() || !project_dir.is_dir() {
        return Err(anyhow!("Project directory does not exist: {}", project_path));
    }
    
    // Check if Cargo.toml exists
    let cargo_toml_path = project_dir.join("Cargo.toml");
    if !cargo_toml_path.exists() {
        return Err(anyhow!("Cargo.toml not found. Not a valid Rust project."));
    }
    
    // Build command
    let mut cmd = Command::new("cargo");
    cmd.arg("build");
    
    // Add optimization level
    match options.optimization_level {
        0 => {} // Default debug build
        1 => cmd.arg("--release"),
        2 => {
            cmd.arg("--release");
            cmd.env("RUSTFLAGS", "-C opt-level=2");
        }
        3 => {
            cmd.arg("--release");
            cmd.env("RUSTFLAGS", "-C opt-level=3");
        }
        _ => {
            cmd.arg("--release");
            cmd.env("RUSTFLAGS", "-C opt-level=s"); // Optimize for size
        }
    }
    
    // Add debug info
    if !options.debug_info {
        let rustflags = cmd.get_envs()
            .find(|(key, _)| key == "RUSTFLAGS")
            .map(|(_, value)| value.unwrap_or_default().to_string() + " -C debuginfo=0")
            .unwrap_or_else(|| "-C debuginfo=0".to_string());
        
        cmd.env("RUSTFLAGS", rustflags);
    }
    
    // Add target
    if let Some(target) = &options.target {
        cmd.args(["--target", target]);
    }
    
    // Add features
    if !options.features.is_empty() {
        cmd.arg("--features");
        cmd.arg(options.features.join(","));
    }
    
    // No default features
    if options.no_default_features {
        cmd.arg("--no-default-features");
    }
    
    // All features
    if options.all_features {
        cmd.arg("--all-features");
    }
    
    // Warnings as errors
    if options.warnings_as_errors {
        cmd.arg("--warnings-as-errors");
    }
    
    // Add extra flags
    for flag in &options.extra_flags {
        cmd.arg(flag);
    }
    
    // Set working directory
    cmd.current_dir(project_dir);
    
    // Capture output
    cmd.stdout(Stdio::piped());
    cmd.stderr(Stdio::piped());
    
    output.push(format!("Running: {:?}", cmd));
    
    // Execute command
    let mut child = cmd.spawn()?;
    
    // Read stdout
    if let Some(stdout) = child.stdout.take() {
        let reader = BufReader::new(stdout);
        for line in reader.lines() {
            if let Ok(line) = line {
                output.push(line.clone());
                
                // Parse warnings
                if line.contains("warning:") {
                    if let Some(warning) = parse_diagnostic(&line, "warning:") {
                        warnings.push(warning);
                    }
                }
            }
        }
    }
    
    // Read stderr
    if let Some(stderr) = child.stderr.take() {
        let reader = BufReader::new(stderr);
        for line in reader.lines() {
            if let Ok(line) = line {
                output.push(line.clone());
                
                // Parse errors
                if line.contains("error:") {
                    if let Some(error) = parse_diagnostic(&line, "error:") {
                        errors.push(error);
                    }
                }
                
                // Parse warnings in stderr
                if line.contains("warning:") {
                    if let Some(warning) = parse_diagnostic(&line, "warning:") {
                        warnings.push(warning);
                    }
                }
            }
        }
    }
    
    // Wait for the process to complete
    let status = child.wait()?;
    let success = status.success();
    
    // Find artifacts if build was successful
    if success {
        let target_dir = project_dir.join("target");
        let profile_dir = if options.optimization_level > 0 {
            if let Some(target) = &options.target {
                target_dir.join(target).join("release")
            } else {
                target_dir.join("release")
            }
        } else {
            if let Some(target) = &options.target {
                target_dir.join(target).join("debug")
            } else {
                target_dir.join("debug")
            }
        };
        
        if profile_dir.exists() {
            if let Ok(entries) = fs::read_dir(&profile_dir) {
                for entry in entries.filter_map(Result::ok) {
                    let path = entry.path();
                    if path.is_file() {
                        let file_name = path.file_name().unwrap().to_string_lossy();
                        
                        // Skip common non-executable files
                        if file_name.ends_with(".d") || file_name.ends_with(".rlib") || 
                           file_name.ends_with(".rmeta") || file_name.ends_with(".pdb") {
                            continue;
                        }
                        
                        // Check if it's an executable or library
                        if is_executable(&path) || file_name.ends_with(".so") || 
                           file_name.ends_with(".dll") || file_name.ends_with(".dylib") {
                            artifacts.push(path.to_string_lossy().to_string());
                        }
                    }
                }
            }
        }
    }
    
    Ok(CompilationResult {
        success,
        output,
        errors,
        warnings,
        duration_ms: start_time.elapsed().as_millis() as u64,
        artifacts,
    })
}

// Compile Kotlin code
pub fn compile_kotlin(source_files: &[&str], output_dir: &str, classpath: &[&str], options: &HashMap<String, String>) -> Result<CompilationResult> {
    let start_time = Instant::now();
    let mut output = Vec::new();
    let mut errors = Vec::new();
    let mut warnings = Vec::new();
    let mut artifacts = Vec::new();
    
    // Check if Kotlin compiler is available
    let kotlinc = match find_kotlinc() {
        Some(path) => path,
        None => return Err(anyhow!("Kotlin compiler not found")),
    };
    
    // Create output directory if it doesn't exist
    let output_path = Path::new(output_dir);
    if !output_path.exists() {
        fs::create_dir_all(output_path)?;
    }
    
    // Build command
    let mut cmd = Command::new(kotlinc);
    
    // Add source files
    for source_file in source_files {
        cmd.arg(source_file);
    }
    
    // Add output directory
    cmd.args(["-d", output_dir]);
    
    // Add classpath
    if !classpath.is_empty() {
        cmd.arg("-classpath");
        cmd.arg(classpath.join(":"));
    }
    
    // Add options
    for (key, value) in options {
        if key == "jvmTarget" {
            cmd.args(["-jvm-target", value]);
        } else if key == "languageVersion" {
            cmd.args(["-language-version", value]);
        } else if key == "apiVersion" {
            cmd.args(["-api-version", value]);
        } else if key == "noStdlib" && value == "true" {
            cmd.arg("-no-stdlib");
        } else if key == "noReflect" && value == "true" {
            cmd.arg("-no-reflect");
        } else if key == "includeRuntime" && value == "true" {
            cmd.arg("-include-runtime");
        } else if key == "progressive" && value == "true" {
            cmd.arg("-progressive");
        } else if key == "verbose" && value == "true" {
            cmd.arg("-verbose");
        }
    }
    
    // Capture output
    cmd.stdout(Stdio::piped());
    cmd.stderr(Stdio::piped());
    
    output.push(format!("Running: {:?}", cmd));
    
    // Execute command
    let mut child = cmd.spawn()?;
    
    // Read stdout
    if let Some(stdout) = child.stdout.take() {
        let reader = BufReader::new(stdout);
        for line in reader.lines() {
            if let Ok(line) = line {
                output.push(line.clone());
            }
        }
    }
    
    // Read stderr
    if let Some(stderr) = child.stderr.take() {
        let reader = BufReader::new(stderr);
        for line in reader.lines() {
            if let Ok(line) = line {
                output.push(line.clone());
                
                // Parse errors and warnings
                if line.contains("error:") {
                    if let Some(error) = parse_kotlin_diagnostic(&line, "error:") {
                        errors.push(error);
                    }
                } else if line.contains("warning:") {
                    if let Some(warning) = parse_kotlin_diagnostic(&line, "warning:") {
                        warnings.push(warning);
                    }
                }
            }
        }
    }
    
    // Wait for the process to complete
    let status = child.wait()?;
    let success = status.success();
    
    // Find artifacts if build was successful
    if success {
        if let Ok(entries) = fs::read_dir(output_path) {
            for entry in entries.filter_map(Result::ok) {
                let path = entry.path();
                if path.is_file() && path.extension().map_or(false, |ext| ext == "class" || ext == "jar") {
                    artifacts.push(path.to_string_lossy().to_string());
                }
            }
        }
    }
    
    Ok(CompilationResult {
        success,
        output,
        errors,
        warnings,
        duration_ms: start_time.elapsed().as_millis() as u64,
        artifacts,
    })
}

// Compile Java code
pub fn compile_java(source_files: &[&str], output_dir: &str, classpath: &[&str], options: &HashMap<String, String>) -> Result<CompilationResult> {
    let start_time = Instant::now();
    let mut output = Vec::new();
    let mut errors = Vec::new();
    let mut warnings = Vec::new();
    let mut artifacts = Vec::new();
    
    // Check if Java compiler is available
    let javac = match find_javac() {
        Some(path) => path,
        None => return Err(anyhow!("Java compiler not found")),
    };
    
    // Create output directory if it doesn't exist
    let output_path = Path::new(output_dir);
    if !output_path.exists() {
        fs::create_dir_all(output_path)?;
    }
    
    // Build command
    let mut cmd = Command::new(javac);
    
    // Add source files
    for source_file in source_files {
        cmd.arg(source_file);
    }
    
    // Add output directory
    cmd.args(["-d", output_dir]);
    
    // Add classpath
    if !classpath.is_empty() {
        cmd.arg("-classpath");
        cmd.arg(classpath.join(":"));
    }
    
    // Add options
    for (key, value) in options {
        if key == "source" {
            cmd.args(["-source", value]);
        } else if key == "target" {
            cmd.args(["-target", value]);
        } else if key == "encoding" {
            cmd.args(["-encoding", value]);
        } else if key == "verbose" && value == "true" {
            cmd.arg("-verbose");
        } else if key == "deprecation" && value == "true" {
            cmd.arg("-deprecation");
        } else if key == "nowarn" && value == "true" {
            cmd.arg("-nowarn");
        } else if key == "parameters" && value == "true" {
            cmd.arg("-parameters");
        }
    }
    
    // Capture output
    cmd.stdout(Stdio::piped());
    cmd.stderr(Stdio::piped());
    
    output.push(format!("Running: {:?}", cmd));
    
    // Execute command
    let mut child = cmd.spawn()?;
    
    // Read stdout
    if let Some(stdout) = child.stdout.take() {
        let reader = BufReader::new(stdout);
        for line in reader.lines() {
            if let Ok(line) = line {
                output.push(line.clone());
            }
        }
    }
    
    // Read stderr
    if let Some(stderr) = child.stderr.take() {
        let reader = BufReader::new(stderr);
        for line in reader.lines() {
            if let Ok(line) = line {
                output.push(line.clone());
                
                // Parse errors and warnings
                if line.contains("error:") {
                    if let Some(error) = parse_java_diagnostic(&line, "error:") {
                        errors.push(error);
                    }
                } else if line.contains("warning:") {
                    if let Some(warning) = parse_java_diagnostic(&line, "warning:") {
                        warnings.push(warning);
                    }
                }
            }
        }
    }
    
    // Wait for the process to complete
    let status = child.wait()?;
    let success = status.success();
    
    // Find artifacts if build was successful
    if success {
        if let Ok(entries) = fs::read_dir(output_path) {
            for entry in entries.filter_map(Result::ok) {
                let path = entry.path();
                if path.is_file() && path.extension().map_or(false, |ext| ext == "class") {
                    artifacts.push(path.to_string_lossy().to_string());
                }
            }
        }
    }
    
    Ok(CompilationResult {
        success,
        output,
        errors,
        warnings,
        duration_ms: start_time.elapsed().as_millis() as u64,
        artifacts,
    })
}

// Find Kotlin compiler
fn find_kotlinc() -> Option<String> {
    // Try to find kotlinc in PATH
    if let Ok(output) = Command::new("which").arg("kotlinc").output() {
        if output.status.success() {
            let path = String::from_utf8_lossy(&output.stdout).trim().to_string();
            if !path.is_empty() {
                return Some(path);
            }
        }
    }
    
    // Try common locations
    let common_locations = [
        "/usr/bin/kotlinc",
        "/usr/local/bin/kotlinc",
        "/opt/kotlinc/bin/kotlinc",
    ];
    
    for location in &common_locations {
        if Path::new(location).exists() {
            return Some(location.to_string());
        }
    }
    
    // Try KOTLIN_HOME environment variable
    if let Ok(kotlin_home) = std::env::var("KOTLIN_HOME") {
        let path = Path::new(&kotlin_home).join("bin/kotlinc");
        if path.exists() {
            return Some(path.to_string_lossy().to_string());
        }
    }
    
    None
}

// Find Java compiler
fn find_javac() -> Option<String> {
    // Try to find javac in PATH
    if let Ok(output) = Command::new("which").arg("javac").output() {
        if output.status.success() {
            let path = String::from_utf8_lossy(&output.stdout).trim().to_string();
            if !path.is_empty() {
                return Some(path);
            }
        }
    }
    
    // Try common locations
    let common_locations = [
        "/usr/bin/javac",
        "/usr/local/bin/javac",
        "/opt/jdk/bin/javac",
    ];
    
    for location in &common_locations {
        if Path::new(location).exists() {
            return Some(location.to_string());
        }
    }
    
    // Try JAVA_HOME environment variable
    if let Ok(java_home) = std::env::var("JAVA_HOME") {
        let path = Path::new(&java_home).join("bin/javac");
        if path.exists() {
            return Some(path.to_string_lossy().to_string());
        }
    }
    
    None
}

// Parse Rust compiler diagnostic message
fn parse_diagnostic(line: &str, diagnostic_type: &str) -> Option<CompilationError> {
    // Example: src/main.rs:10:5: error: expected `;`, found `}`
    let parts: Vec<&str> = line.split(diagnostic_type).collect();
    if parts.len() < 2 {
        return None;
    }
    
    let location_parts: Vec<&str> = parts[0].split(':').collect();
    if location_parts.len() < 3 {
        return None;
    }
    
    let file = location_parts[0].trim().to_string();
    let line_num = location_parts[1].trim().parse::<u32>().unwrap_or(0);
    let column_num = location_parts[2].trim().parse::<u32>().unwrap_or(0);
    let message = parts[1].trim().to_string();
    
    // Extract error code if present
    let code = if message.contains("[E") {
        let start_idx = message.find("[E").unwrap_or(0);
        let end_idx = message[start_idx..].find(']').map(|i| i + start_idx + 1).unwrap_or(start_idx);
        if start_idx < end_idx {
            Some(message[start_idx..end_idx].to_string())
        } else {
            None
        }
    } else {
        None
    };
    
    if diagnostic_type == "error:" {
        Some(CompilationError {
            file,
            line: line_num,
            column: column_num,
            message,
            code,
        })
    } else {
        Some(CompilationError {
            file,
            line: line_num,
            column: column_num,
            message,
            code,
        })
    }
}

// Parse Kotlin compiler diagnostic message
fn parse_kotlin_diagnostic(line: &str, diagnostic_type: &str) -> Option<CompilationError> {
    // Example: src/main/kotlin/com/example/Main.kt:10:5: error: expected ';', found '}'
    let parts: Vec<&str> = line.split(diagnostic_type).collect();
    if parts.len() < 2 {
        return None;
    }
    
    let location_parts: Vec<&str> = parts[0].split(':').collect();
    if location_parts.len() < 3 {
        return None;
    }
    
    let file = location_parts[0].trim().to_string();
    let line_num = location_parts[1].trim().parse::<u32>().unwrap_or(0);
    let column_num = location_parts[2].trim().parse::<u32>().unwrap_or(0);
    let message = parts[1].trim().to_string();
    
    if diagnostic_type == "error:" {
        Some(CompilationError {
            file,
            line: line_num,
            column: column_num,
            message,
            code: None,
        })
    } else {
        Some(CompilationError {
            file,
            line: line_num,
            column: column_num,
            message,
            code: None,
        })
    }
}

// Parse Java compiler diagnostic message
fn parse_java_diagnostic(line: &str, diagnostic_type: &str) -> Option<CompilationError> {
    // Example: /path/to/Main.java:10: error: ';' expected
    let parts: Vec<&str> = line.split(diagnostic_type).collect();
    if parts.len() < 2 {
        return None;
    }
    
    let location_parts: Vec<&str> = parts[0].split(':').collect();
    if location_parts.len() < 2 {
        return None;
    }
    
    let file = location_parts[0].trim().to_string();
    let line_num = location_parts[1].trim().parse::<u32>().unwrap_or(0);
    let column_num = 0; // Java compiler doesn't always provide column information
    let message = parts[1].trim().to_string();
    
    if diagnostic_type == "error:" {
        Some(CompilationError {
            file,
            line: line_num,
            column: column_num,
            message,
            code: None,
        })
    } else {
        Some(CompilationError {
            file,
            line: line_num,
            column: column_num,
            message,
            code: None,
        })
    }
}

// Helper function to check if a file is executable
fn is_executable(path: &Path) -> bool {
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        if let Ok(metadata) = fs::metadata(path) {
            return metadata.permissions().mode() & 0o111 != 0;
        }
    }
    
    // On non-Unix platforms, check for common executable extensions
    #[cfg(not(unix))]
    {
        if let Some(extension) = path.extension() {
            let ext = extension.to_string_lossy().to_lowercase();
            return ext == "exe" || ext == "bat" || ext == "cmd";
        }
    }
    
    false
}