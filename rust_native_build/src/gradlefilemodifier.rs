use std::fs;
use std::path::{Path, PathBuf};
use std::collections::HashMap;
use serde::{Serialize, Deserialize};
use anyhow::{Result, anyhow};
use regex::Regex;

// Gradle build analysis
#[derive(Serialize, Deserialize, Debug)]
pub struct GradleBuildAnalysis {
    pub file_path: String,
    pub dependencies: Vec<GradleDependency>,
    pub plugins: Vec<String>,
    pub issues: Vec<GradleBuildIssue>,
    pub compile_sdk_version: Option<String>,
    pub min_sdk_version: Option<String>,
    pub target_sdk_version: Option<String>,
    pub build_tools_version: Option<String>,
}

// Gradle dependency
#[derive(Serialize, Deserialize, Debug)]
pub struct GradleDependency {
    pub configuration: String,
    pub group: String,
    pub name: String,
    pub version: String,
}

// Gradle build issue
#[derive(Serialize, Deserialize, Debug)]
pub struct GradleBuildIssue {
    pub issue_type: GradleBuildIssueType,
    pub description: String,
    pub severity: IssueSeverity,
    pub suggestion: String,
}

// Gradle build issue type
#[derive(Serialize, Deserialize, Debug)]
pub enum GradleBuildIssueType {
    MissingRepository,
    DeprecatedConfiguration,
    OutdatedDependency,
    MissingJavaCompatibility,
    MissingKotlinJvmTarget,
    MissingProguardConfig,
    MissingBuildFeatures,
}

// Issue severity
#[derive(Serialize, Deserialize, Debug)]
pub enum IssueSeverity {
    Error,
    Warning,
    Info,
}

// Gradle build report
#[derive(Serialize, Deserialize, Debug)]
pub struct GradleBuildReport {
    pub project_path: String,
    pub modules: Vec<GradleModuleReport>,
    pub total_dependencies: usize,
    pub total_issues: usize,
    pub gradle_version: Option<String>,
    pub kotlin_version: Option<String>,
    pub agp_version: Option<String>,
}

// Gradle module report
#[derive(Serialize, Deserialize, Debug)]
pub struct GradleModuleReport {
    pub module_name: String,
    pub build_file_path: String,
    pub dependencies: Vec<GradleDependency>,
    pub issues: Vec<GradleBuildIssue>,
    pub build_tools_version: String,
    pub compile_sdk_version: String,
    pub min_sdk_version: String,
    pub target_sdk_version: String,
}

// Optimization result
#[derive(Serialize, Deserialize, Debug)]
pub struct OptimizationResult {
    pub file_path: String,
    pub issues_fixed: Vec<String>,
    pub dependencies_updated: Vec<DependencyUpdate>,
    pub success: bool,
    pub error: Option<String>,
}

// Dependency update
#[derive(Serialize, Deserialize, Debug)]
pub struct DependencyUpdate {
    pub group: String,
    pub name: String,
    pub old_version: String,
    pub new_version: String,
}

// Analyze Gradle build file
pub fn analyze_gradle_file(file_path: &str) -> Result<GradleBuildAnalysis> {
    // Check if file exists
    let path = Path::new(file_path);
    if !path.exists() {
        return Err(anyhow!("File does not exist: {}", file_path));
    }
    
    // Read file content
    let content = fs::read_to_string(path)?;
    
    // Parse dependencies
    let dependencies = extract_dependencies(&content);
    
    // Parse plugins
    let plugins = extract_plugins(&content);
    
    // Find issues
    let issues = find_issues(&content, &dependencies);
    
    // Extract SDK versions
    let compile_sdk_version = extract_compile_sdk_version(&content);
    let min_sdk_version = extract_min_sdk_version(&content);
    let target_sdk_version = extract_target_sdk_version(&content);
    let build_tools_version = extract_build_tools_version(&content);
    
    Ok(GradleBuildAnalysis {
        file_path: file_path.to_string(),
        dependencies,
        plugins,
        issues,
        compile_sdk_version,
        min_sdk_version,
        target_sdk_version,
        build_tools_version,
    })
}

// Optimize Gradle build file
pub fn optimize_gradle_file(file_path: &str) -> Result<OptimizationResult> {
    // Analyze the build file
    let analysis = analyze_gradle_file(file_path)?;
    
    // Read file content
    let mut content = fs::read_to_string(file_path)?;
    
    let mut issues_fixed = Vec::new();
    let mut dependencies_updated = Vec::new();
    
    // Fix issues
    for issue in &analysis.issues {
        match issue.issue_type {
            GradleBuildIssueType::MissingRepository => {
                content = ensure_repositories(&content);
                issues_fixed.push(issue.description.clone());
            }
            GradleBuildIssueType::DeprecatedConfiguration => {
                content = fix_deprecated_configurations(&content);
                issues_fixed.push(issue.description.clone());
            }
            GradleBuildIssueType::OutdatedDependency => {
                let (updated, updates) = update_dependency_versions(&content);
                content = updated;
                dependencies_updated.extend(updates);
                issues_fixed.push(issue.description.clone());
            }
            GradleBuildIssueType::MissingJavaCompatibility => {
                content = ensure_java_compatibility(&content);
                issues_fixed.push(issue.description.clone());
            }
            GradleBuildIssueType::MissingKotlinJvmTarget => {
                content = ensure_kotlin_jvm_target(&content);
                issues_fixed.push(issue.description.clone());
            }
            GradleBuildIssueType::MissingProguardConfig => {
                content = ensure_proguard_config(&content);
                issues_fixed.push(issue.description.clone());
            }
            GradleBuildIssueType::MissingBuildFeatures => {
                content = ensure_build_features(&content);
                issues_fixed.push(issue.description.clone());
            }
        }
    }
    
    // Write updated content
    fs::write(file_path, &content)?;
    
    Ok(OptimizationResult {
        file_path: file_path.to_string(),
        issues_fixed,
        dependencies_updated,
        success: true,
        error: None,
    })
}

// Add dependency to Gradle file
pub fn add_dependency(file_path: &str, dependency: &str, configuration: &str) -> Result<()> {
    // Read file content
    let mut content = fs::read_to_string(file_path)?;
    
    // Check if dependency already exists
    let dependency_regex = Regex::new(&format!(r"{}\s+['\"]{}['\"]", configuration, regex::escape(dependency)))?;
    if dependency_regex.is_match(&content) {
        return Err(anyhow!("Dependency already exists: {}", dependency));
    }
    
    // Find dependencies block
    let dependencies_regex = Regex::new(r"dependencies\s*\{")?;
    if let Some(dependencies_match) = dependencies_regex.find(&content) {
        // Insert dependency
        let insert_position = dependencies_match.end();
        let dependency_line = format!("\n    {} '{}'", configuration, dependency);
        
        content.insert_str(insert_position, &dependency_line);
    } else {
        // Add dependencies block
        let dependencies_block = format!("\ndependencies {{\n    {} '{}'\n}}\n", configuration, dependency);
        content.push_str(&dependencies_block);
    }
    
    // Write updated content
    fs::write(file_path, content)?;
    
    Ok(())
}

// Remove dependency from Gradle file
pub fn remove_dependency(file_path: &str, dependency: &str) -> Result<()> {
    // Read file content
    let content = fs::read_to_string(file_path)?;
    
    // Find and remove dependency
    let dependency_regex = Regex::new(&format!(r"\s+\w+\s+['\"]{}['\"]", regex::escape(dependency)))?;
    let updated_content = dependency_regex.replace_all(&content, "").to_string();
    
    // Write updated content
    fs::write(file_path, updated_content)?;
    
    Ok(())
}

// Update dependencies in Gradle file
pub fn update_dependencies(file_path: &str) -> Result<OptimizationResult> {
    // Read file content
    let content = fs::read_to_string(file_path)?;
    
    // Update dependencies
    let (updated_content, updates) = update_dependency_versions(&content);
    
    // Write updated content
    fs::write(file_path, &updated_content)?;
    
    Ok(OptimizationResult {
        file_path: file_path.to_string(),
        issues_fixed: Vec::new(),
        dependencies_updated: updates,
        success: true,
        error: None,
    })
}

// Fix common issues in Gradle file
pub fn fix_common_issues(file_path: &str) -> Result<OptimizationResult> {
    // Read file content
    let mut content = fs::read_to_string(file_path)?;
    
    let mut issues_fixed = Vec::new();
    
    // Fix missing repositories
    if !content.contains("google()") || !content.contains("mavenCentral()") {
        content = ensure_repositories(&content);
        issues_fixed.push("Added missing repositories".to_string());
    }
    
    // Fix deprecated configurations
    if content.contains("compile ") || content.contains("testCompile ") {
        content = fix_deprecated_configurations(&content);
        issues_fixed.push("Updated deprecated dependency configurations".to_string());
    }
    
    // Fix missing Java compatibility
    if !content.contains("sourceCompatibility") || !content.contains("targetCompatibility") {
        content = ensure_java_compatibility(&content);
        issues_fixed.push("Added Java compatibility settings".to_string());
    }
    
    // Fix missing Kotlin JVM target
    if content.contains("kotlin") && !content.contains("jvmTarget") {
        content = ensure_kotlin_jvm_target(&content);
        issues_fixed.push("Added Kotlin JVM target".to_string());
    }
    
    // Fix missing ProGuard configuration
    if content.contains("minifyEnabled true") && !content.contains("proguardFiles") {
        content = ensure_proguard_config(&content);
        issues_fixed.push("Added ProGuard configuration".to_string());
    }
    
    // Fix missing build features
    if content.contains("compose") && !content.contains("buildFeatures") {
        content = ensure_build_features(&content);
        issues_fixed.push("Added missing build features".to_string());
    }
    
    // Write updated content
    fs::write(file_path, &content)?;
    
    Ok(OptimizationResult {
        file_path: file_path.to_string(),
        issues_fixed,
        dependencies_updated: Vec::new(),
        success: true,
        error: None,
    })
}

// Generate build report
pub fn generate_build_report(project_path: &str) -> Result<GradleBuildReport> {
    let project_dir = Path::new(project_path);
    
    // Find build files
    let build_files = find_build_files(project_dir);
    
    let mut modules = Vec::new();
    let mut total_dependencies = 0;
    let mut total_issues = 0;
    
    // Analyze each build file
    for build_file in build_files {
        let analysis = analyze_gradle_file(&build_file.to_string_lossy())?;
        
        total_dependencies += analysis.dependencies.len();
        total_issues += analysis.issues.len();
        
        let module_name = get_module_name(&build_file, project_dir);
        
        modules.push(GradleModuleReport {
            module_name,
            build_file_path: build_file.to_string_lossy().to_string(),
            dependencies: analysis.dependencies,
            issues: analysis.issues,
            build_tools_version: analysis.build_tools_version.unwrap_or_else(|| "Unknown".to_string()),
            compile_sdk_version: analysis.compile_sdk_version.unwrap_or_else(|| "Unknown".to_string()),
            min_sdk_version: analysis.min_sdk_version.unwrap_or_else(|| "Unknown".to_string()),
            target_sdk_version: analysis.target_sdk_version.unwrap_or_else(|| "Unknown".to_string()),
        });
    }
    
    // Detect Gradle version
    let gradle_version = detect_gradle_version(project_dir);
    
    // Detect Kotlin version
    let kotlin_version = detect_kotlin_version(&modules);
    
    // Detect AGP version
    let agp_version = detect_agp_version(&modules);
    
    Ok(GradleBuildReport {
        project_path: project_path.to_string(),
        modules,
        total_dependencies,
        total_issues,
        gradle_version,
        kotlin_version,
        agp_version,
    })
}

// Extract dependencies from Gradle file
fn extract_dependencies(content: &str) -> Vec<GradleDependency> {
    let mut dependencies = Vec::new();
    
    // Match dependencies
    let dependency_regex = Regex::new(r"(\w+)\s+['\"]([^:]+):([^:]+):([^'\"]+)['\"]").unwrap();
    
    for capture in dependency_regex.captures_iter(content) {
        let configuration = capture[1].to_string();
        let group = capture[2].to_string();
        let name = capture[3].to_string();
        let version = capture[4].to_string();
        
        dependencies.push(GradleDependency {
            configuration,
            group,
            name,
            version,
        });
    }
    
    dependencies
}

// Extract plugins from Gradle file
fn extract_plugins(content: &str) -> Vec<String> {
    let mut plugins = Vec::new();
    
    // Match plugins
    let plugin_regex = Regex::new(r"id\s+['\"]([^'\"]+)['\"]").unwrap();
    
    for capture in plugin_regex.captures_iter(content) {
        plugins.push(capture[1].to_string());
    }
    
    plugins
}

// Find issues in Gradle file
fn find_issues(content: &str, dependencies: &[GradleDependency]) -> Vec<GradleBuildIssue> {
    let mut issues = Vec::new();
    
    // Check for missing repositories
    if !content.contains("google()") {
        issues.push(GradleBuildIssue {
            issue_type: GradleBuildIssueType::MissingRepository,
            description: "Missing Google repository".to_string(),
            severity: IssueSeverity::Warning,
            suggestion: "Add google() to repositories block".to_string(),
        });
    }
    
    if !content.contains("mavenCentral()") {
        issues.push(GradleBuildIssue {
            issue_type: GradleBuildIssueType::MissingRepository,
            description: "Missing Maven Central repository".to_string(),
            severity: IssueSeverity::Warning,
            suggestion: "Add mavenCentral() to repositories block".to_string(),
        });
    }
    
    // Check for deprecated configurations
    if content.contains("compile ") {
        issues.push(GradleBuildIssue {
            issue_type: GradleBuildIssueType::DeprecatedConfiguration,
            description: "Using deprecated 'compile' configuration".to_string(),
            severity: IssueSeverity::Error,
            suggestion: "Replace 'compile' with 'implementation'".to_string(),
        });
    }
    
    if content.contains("testCompile ") {
        issues.push(GradleBuildIssue {
            issue_type: GradleBuildIssueType::DeprecatedConfiguration,
            description: "Using deprecated 'testCompile' configuration".to_string(),
            severity: IssueSeverity::Error,
            suggestion: "Replace 'testCompile' with 'testImplementation'".to_string(),
        });
    }
    
    // Check for outdated dependencies
    for dependency in dependencies {
        if is_outdated_dependency(dependency) {
            issues.push(GradleBuildIssue {
                issue_type: GradleBuildIssueType::OutdatedDependency,
                description: format!("Outdated dependency: {}:{}:{}", dependency.group, dependency.name, dependency.version),
                severity: IssueSeverity::Info,
                suggestion: format!("Update to latest version: {}", get_latest_version(dependency)),
            });
        }
    }
    
    // Check for missing Java compatibility
    if !content.contains("sourceCompatibility") || !content.contains("targetCompatibility") {
        issues.push(GradleBuildIssue {
            issue_type: GradleBuildIssueType::MissingJavaCompatibility,
            description: "Missing Java compatibility settings".to_string(),
            severity: IssueSeverity::Warning,
            suggestion: "Add sourceCompatibility and targetCompatibility settings".to_string(),
        });
    }
    
    // Check for missing Kotlin JVM target
    if content.contains("kotlin") && !content.contains("jvmTarget") {
        issues.push(GradleBuildIssue {
            issue_type: GradleBuildIssueType::MissingKotlinJvmTarget,
            description: "Missing Kotlin JVM target".to_string(),
            severity: IssueSeverity::Warning,
            suggestion: "Add jvmTarget setting to kotlinOptions block".to_string(),
        });
    }
    
    // Check for missing ProGuard configuration
    if content.contains("minifyEnabled true") && !content.contains("proguardFiles") {
        issues.push(GradleBuildIssue {
            issue_type: GradleBuildIssueType::MissingProguardConfig,
            description: "Missing ProGuard configuration".to_string(),
            severity: IssueSeverity::Warning,
            suggestion: "Add proguardFiles setting".to_string(),
        });
    }
    
    // Check for missing build features
    if content.contains("compose") && !content.contains("buildFeatures") {
        issues.push(GradleBuildIssue {
            issue_type: GradleBuildIssueType::MissingBuildFeatures,
            description: "Missing build features configuration".to_string(),
            severity: IssueSeverity::Warning,
            suggestion: "Add buildFeatures block with compose = true".to_string(),
        });
    }
    
    issues
}

// Check if a dependency is outdated
fn is_outdated_dependency(dependency: &GradleDependency) -> bool {
    let latest_version = get_latest_version(dependency);
    dependency.version != latest_version
}

// Get latest version for a dependency
fn get_latest_version(dependency: &GradleDependency) -> String {
    // Known latest versions for common dependencies
    let latest_versions = HashMap::from([
        ("androidx.core:core-ktx".to_string(), "1.12.0".to_string()),
        ("androidx.appcompat:appcompat".to_string(), "1.6.1".to_string()),
        ("androidx.activity:activity-compose".to_string(), "1.8.2".to_string()),
        ("androidx.compose:compose-bom".to_string(), "2024.02.00".to_string()),
        ("androidx.compose.material3:material3".to_string(), "1.2.0".to_string()),
        ("androidx.lifecycle:lifecycle-runtime-ktx".to_string(), "2.7.0".to_string()),
        ("androidx.lifecycle:lifecycle-viewmodel-compose".to_string(), "2.7.0".to_string()),
        ("androidx.navigation:navigation-compose".to_string(), "2.7.5".to_string()),
        ("androidx.room:room-runtime".to_string(), "2.6.1".to_string()),
        ("androidx.room:room-ktx".to_string(), "2.6.1".to_string()),
        ("com.google.android.material:material".to_string(), "1.11.0".to_string()),
        ("org.jetbrains.kotlinx:kotlinx-coroutines-android".to_string(), "1.7.3".to_string()),
        ("com.squareup.retrofit2:retrofit".to_string(), "2.9.0".to_string()),
        ("com.squareup.okhttp3:okhttp".to_string(), "4.12.0".to_string()),
        ("io.coil-kt:coil-compose".to_string(), "2.5.0".to_string()),
        ("junit:junit".to_string(), "4.13.2".to_string()),
        ("androidx.test.ext:junit".to_string(), "1.1.5".to_string()),
        ("androidx.test.espresso:espresso-core".to_string(), "3.5.1".to_string()),
    ]);
    
    let key = format!("{}:{}", dependency.group, dependency.name);
    
    latest_versions.get(&key).cloned().unwrap_or_else(|| dependency.version.clone())
}

// Ensure repositories are present
fn ensure_repositories(content: &str) -> String {
    if content.contains("repositories {") {
        // Add missing repositories to existing block
        let mut updated_content = content.to_string();
        
        if !content.contains("google()") {
            updated_content = updated_content.replace(
                "repositories {",
                "repositories {\n        google()"
            );
        }
        
        if !content.contains("mavenCentral()") {
            updated_content = updated_content.replace(
                "repositories {",
                "repositories {\n        mavenCentral()"
            );
        }
        
        updated_content
    } else {
        // Add repositories block
        let repositories_block = "repositories {\n    google()\n    mavenCentral()\n}\n\n";
        
        if content.contains("android {") {
            content.replace("android {", &format!("{}\nandroid {{", repositories_block))
        } else {
            format!("{}\n{}", repositories_block, content)
        }
    }
}

// Fix deprecated configurations
fn fix_deprecated_configurations(content: &str) -> String {
    content
        .replace("compile ", "implementation ")
        .replace("testCompile ", "testImplementation ")
        .replace("androidTestCompile ", "androidTestImplementation ")
        .replace("compile(", "implementation(")
        .replace("testCompile(", "testImplementation(")
        .replace("androidTestCompile(", "androidTestImplementation(")
}

// Update dependency versions
fn update_dependency_versions(content: &str) -> (String, Vec<DependencyUpdate>) {
    let mut updated_content = content.to_string();
    let mut updates = Vec::new();
    
    // Extract all dependencies
    let dependencies = extract_dependencies(&content);
    
    // For each dependency, try to find a newer version
    for dependency in dependencies {
        let latest_version = get_latest_version(&dependency);
        
        if dependency.version != latest_version {
            let dependency_key = format!("{}:{}", dependency.group, dependency.name);
            
            // Different formats of dependency declarations
            let patterns = vec![
                format!("{}:\"{}\"", dependency_key, dependency.version),
                format!("{}:'{}'", dependency_key, dependency.version),
                format!("{}:{}", dependency_key, dependency.version),
            ];
            
            for pattern in patterns {
                if updated_content.contains(&pattern) {
                    let replacement = if pattern.contains("\"") {
                        format!("{}:\"{}\"", dependency_key, latest_version)
                    } else if pattern.contains("'") {
                        format!("{}:'{}'", dependency_key, latest_version)
                    } else {
                        format!("{}:{}", dependency_key, latest_version)
                    };
                    
                    updated_content = updated_content.replace(&pattern, &replacement);
                    
                    updates.push(DependencyUpdate {
                        group: dependency.group.clone(),
                        name: dependency.name.clone(),
                        old_version: dependency.version.clone(),
                        new_version: latest_version.clone(),
                    });
                    
                    break;
                }
            }
        }
    }
    
    (updated_content, updates)
}

// Ensure Java compatibility
fn ensure_java_compatibility(content: &str) -> String {
    if content.contains("compileOptions {") {
        content.to_string()
    } else {
        let java_compatibility_block = "compileOptions {\n        sourceCompatibility JavaVersion.VERSION_1_8\n        targetCompatibility JavaVersion.VERSION_1_8\n    }\n    ";
        
        if content.contains("android {") {
            content.replace(
                "android {",
                &format!("android {{\n    {}", java_compatibility_block)
            )
        } else {
            content.to_string()
        }
    }
}

// Ensure Kotlin JVM target
fn ensure_kotlin_jvm_target(content: &str) -> String {
    if content.contains("kotlinOptions {") {
        content.to_string()
    } else {
        let kotlin_options_block = "kotlinOptions {\n        jvmTarget = '1.8'\n    }\n    ";
        
        if content.contains("compileOptions {") {
            content.replace(
                "compileOptions {",
                &format!("{}\n    compileOptions {{", kotlin_options_block)
            )
        } else if content.contains("android {") {
            content.replace(
                "android {",
                &format!("android {{\n    {}", kotlin_options_block)
            )
        } else {
            content.to_string()
        }
    }
}

// Ensure ProGuard configuration
fn ensure_proguard_config(content: &str) -> String {
    if content.contains("proguardFiles") {
        content.to_string()
    } else {
        content.replace(
            "minifyEnabled true",
            "minifyEnabled true\n            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'"
        )
    }
}

// Ensure build features
fn ensure_build_features(content: &str) -> String {
    if content.contains("buildFeatures {") {
        content.to_string()
    } else {
        let build_features_block = if content.contains("compose") {
            "buildFeatures {\n        compose true\n    }\n    "
        } else {
            "buildFeatures {\n        viewBinding true\n    }\n    "
        };
        
        if content.contains("compileOptions {") {
            content.replace(
                "compileOptions {",
                &format!("{}\n    compileOptions {{", build_features_block)
            )
        } else if content.contains("android {") {
            content.replace(
                "android {",
                &format!("android {{\n    {}", build_features_block)
            )
        } else {
            content.to_string()
        }
    }
}

// Extract compile SDK version
fn extract_compile_sdk_version(content: &str) -> Option<String> {
    let regex = Regex::new(r"compileSdk\s*=?\s*(\d+)").unwrap();
    regex.captures(content).map(|cap| cap[1].to_string())
}

// Extract min SDK version
fn extract_min_sdk_version(content: &str) -> Option<String> {
    let regex = Regex::new(r"minSdk\s*=?\s*(\d+)").unwrap();
    regex.captures(content).map(|cap| cap[1].to_string())
}

// Extract target SDK version
fn extract_target_sdk_version(content: &str) -> Option<String> {
    let regex = Regex::new(r"targetSdk\s*=?\s*(\d+)").unwrap();
    regex.captures(content).map(|cap| cap[1].to_string())
}

// Extract build tools version
fn extract_build_tools_version(content: &str) -> Option<String> {
    let regex = Regex::new(r"buildToolsVersion\s*=?\s*['\"]([^'\"]+)['\"]").unwrap();
    regex.captures(content).map(|cap| cap[1].to_string())
}

// Find build files in a project
fn find_build_files(project_dir: &Path) -> Vec<PathBuf> {
    let mut build_files = Vec::new();
    
    if let Ok(entries) = fs::read_dir(project_dir) {
        for entry in entries.filter_map(Result::ok) {
            let path = entry.path();
            
            if path.is_dir() {
                // Skip common directories to ignore
                let dir_name = path.file_name().unwrap_or_default().to_string_lossy();
                if dir_name == "build" || dir_name == ".gradle" || dir_name == ".idea" || dir_name.starts_with(".") {
                    continue;
                }
                
                // Recursively search subdirectories
                let mut subdirectory_build_files = find_build_files(&path);
                build_files.append(&mut subdirectory_build_files);
            } else if path.is_file() {
                let file_name = path.file_name().unwrap_or_default().to_string_lossy();
                if file_name == "build.gradle" || file_name == "build.gradle.kts" {
                    build_files.push(path);
                }
            }
        }
    }
    
    build_files
}

// Get module name from build file path
fn get_module_name(build_file: &Path, project_dir: &Path) -> String {
    if let Ok(relative_path) = build_file.strip_prefix(project_dir) {
        let parent = relative_path.parent().unwrap_or_else(|| Path::new(""));
        let parent_str = parent.to_string_lossy();
        
        if parent_str.is_empty() {
            "root".to_string()
        } else {
            parent_str.to_string()
        }
    } else {
        build_file.parent().unwrap_or_else(|| Path::new(""))
            .file_name().unwrap_or_default()
            .to_string_lossy().to_string()
    }
}

// Detect Gradle version
fn detect_gradle_version(project_dir: &Path) -> Option<String> {
    let gradle_wrapper_props = project_dir.join("gradle/wrapper/gradle-wrapper.properties");
    
    if gradle_wrapper_props.exists() {
        if let Ok(content) = fs::read_to_string(gradle_wrapper_props) {
            let regex = Regex::new(r"gradle-([0-9.]+)-").unwrap();
            if let Some(captures) = regex.captures(&content) {
                return Some(captures[1].to_string());
            }
        }
    }
    
    None
}

// Detect Kotlin version
fn detect_kotlin_version(modules: &[GradleModuleReport]) -> Option<String> {
    for module in modules {
        for dependency in &module.dependencies {
            if dependency.group == "org.jetbrains.kotlin" && dependency.name.starts_with("kotlin-") {
                return Some(dependency.version.clone());
            }
        }
    }
    
    None
}

// Detect Android Gradle Plugin version
fn detect_agp_version(modules: &[GradleModuleReport]) -> Option<String> {
    for module in modules {
        for dependency in &module.dependencies {
            if dependency.group == "com.android.tools.build" && dependency.name == "gradle" {
                return Some(dependency.version.clone());
            }
        }
    }
    
    None
}