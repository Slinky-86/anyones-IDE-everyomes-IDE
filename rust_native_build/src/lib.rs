use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use std::process::{Command, Stdio};
use std::io::{BufRead, BufReader};
use std::path::{Path, PathBuf};
use std::time::{Duration, Instant};
use std::thread;
use serde::{Serialize, Deserialize};
use serde_json;
use std::env;
use std::fs;
use anyhow::{Result, anyhow};
use lazy_static::lazy_static;

mod buildsystem;
mod compiler;
mod terminal;
mod editor;
mod extensions;
mod pluginsystem;
mod gradlefilemodifier;
mod sdkmanager;

// Build output message
#[derive(Serialize, Deserialize)]
struct OutputMessage {
    message_type: String,
    content: String,
    timestamp: u64,
}

// Build result
#[derive(Serialize, Deserialize)]
struct BuildResult {
    success: bool,
    output_messages: Vec<OutputMessage>,
    duration_ms: u64,
    artifacts: Vec<String>,
}

// Check if Rust is installed
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeCheckRustInstalled(
    _env: JNIEnv,
    _class: JClass,
) -> jni::sys::jboolean {
    match Command::new("rustc").arg("--version").output() {
        Ok(_) => 1, // true
        Err(_) => 0, // false
    }
}

// Get Rust version
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeGetRustVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = match Command::new("rustc").arg("--version").output() {
        Ok(output) => {
            if output.status.success() {
                String::from_utf8_lossy(&output.stdout).to_string()
            } else {
                "Unknown".to_string()
            }
        }
        Err(_) => "Not installed".to_string(),
    };
    
    let output = env.new_string(version).expect("Failed to create Java string");
    output.into_raw()
}

// Get build system status
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeGetBuildSystemStatus(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let status = buildsystem::get_build_system_status();
    let json = serde_json::to_string(&status).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Check if a project is a valid Rust project
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeIsValidRustProject(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
) -> jni::sys::jboolean {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let cargo_toml_path = Path::new(&project_path).join("Cargo.toml");
    if cargo_toml_path.exists() {
        1 // true
    } else {
        0 // false
    }
}

// Get project information
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeGetProjectInfo(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let project_info = buildsystem::get_project_info(&project_path);
    let json = serde_json::to_string(&project_info).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Check build system health
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeCheckBuildSystemHealth(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let health_check = buildsystem::check_build_system_health();
    let json = serde_json::to_string(&health_check).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Build a project
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeBuildProject(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
    build_type: JString,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let build_type: String = env
        .get_string(build_type)
        .expect("Failed to get build type string")
        .into();
    
    let result = buildsystem::build_project(&project_path, &build_type);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Clean a project
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeCleanProject(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let result = buildsystem::clean_project(&project_path);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Test a project
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeTestProject(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
    release: jni::sys::jboolean,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let release = release != 0;
    
    let result = buildsystem::test_project(&project_path, release);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Build for Android target
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeBuildForAndroidTarget(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
    target: JString,
    release: jni::sys::jboolean,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let target: String = env
        .get_string(target)
        .expect("Failed to get target string")
        .into();
    
    let release = release != 0;
    
    let result = buildsystem::build_for_android_target(&project_path, &target, release);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Generate Android bindings
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeGenerateAndroidBindings(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
    package_name: JString,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let package_name: String = env
        .get_string(package_name)
        .expect("Failed to get package name string")
        .into();
    
    let result = buildsystem::generate_android_bindings(&project_path, &package_name);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Editor-related functions

// Initialize the editor
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustEditorManager_00024Companion_nativeInitializeEditor(
    env: JNIEnv,
    _class: JClass,
) -> jni::sys::jboolean {
    match editor::initialize_editor() {
        Ok(_) => 1, // true
        Err(_) => 0, // false
    }
}

// Highlight syntax
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustEditorManager_00024Companion_nativeHighlightSyntax(
    env: JNIEnv,
    _class: JClass,
    content: JString,
    language: JString,
) -> jstring {
    let content: String = env
        .get_string(content)
        .expect("Failed to get content string")
        .into();
    
    let language: String = env
        .get_string(language)
        .expect("Failed to get language string")
        .into();
    
    let highlights = editor::highlight_syntax(&content, &language);
    let json = serde_json::to_string(&highlights).unwrap_or_else(|_| "[]".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Get code completions
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustEditorManager_00024Companion_nativeGetCompletions(
    env: JNIEnv,
    _class: JClass,
    content: JString,
    position: jni::sys::jint,
    language: JString,
) -> jstring {
    let content: String = env
        .get_string(content)
        .expect("Failed to get content string")
        .into();
    
    let language: String = env
        .get_string(language)
        .expect("Failed to get language string")
        .into();
    
    let completions = editor::get_completions(&content, position as usize, &language);
    let json = serde_json::to_string(&completions).unwrap_or_else(|_| "[]".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Format code
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustEditorManager_00024Companion_nativeFormatCode(
    env: JNIEnv,
    _class: JClass,
    content: JString,
    language: JString,
) -> jstring {
    let content: String = env
        .get_string(content)
        .expect("Failed to get content string")
        .into();
    
    let language: String = env
        .get_string(language)
        .expect("Failed to get language string")
        .into();
    
    let formatted = editor::format_code(&content, &language);
    
    let output = env.new_string(formatted).expect("Failed to create Java string");
    output.into_raw()
}

// Parse code structure
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustEditorManager_00024Companion_nativeParseCodeStructure(
    env: JNIEnv,
    _class: JClass,
    content: JString,
    language: JString,
) -> jstring {
    let content: String = env
        .get_string(content)
        .expect("Failed to get content string")
        .into();
    
    let language: String = env
        .get_string(language)
        .expect("Failed to get language string")
        .into();
    
    let structure = editor::parse_code_structure(&content, &language);
    let json = serde_json::to_string(&structure).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Find references
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustEditorManager_00024Companion_nativeFindReferences(
    env: JNIEnv,
    _class: JClass,
    content: JString,
    position: jni::sys::jint,
    language: JString,
) -> jstring {
    let content: String = env
        .get_string(content)
        .expect("Failed to get content string")
        .into();
    
    let language: String = env
        .get_string(language)
        .expect("Failed to get language string")
        .into();
    
    let references = editor::find_references(&content, position as usize, &language);
    let json = serde_json::to_string(&references).unwrap_or_else(|_| "[]".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Plugin system functions

// Load plugin
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustPluginManager_00024Companion_nativeLoadPlugin(
    env: JNIEnv,
    _class: JClass,
    plugin_path: JString,
) -> jstring {
    let plugin_path: String = env
        .get_string(plugin_path)
        .expect("Failed to get plugin path string")
        .into();
    
    let result = pluginsystem::load_plugin(&plugin_path);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Unload plugin
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustPluginManager_00024Companion_nativeUnloadPlugin(
    env: JNIEnv,
    _class: JClass,
    plugin_id: JString,
) -> jni::sys::jboolean {
    let plugin_id: String = env
        .get_string(plugin_id)
        .expect("Failed to get plugin ID string")
        .into();
    
    match pluginsystem::unload_plugin(&plugin_id) {
        Ok(_) => 1, // true
        Err(_) => 0, // false
    }
}

// Get loaded plugins
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustPluginManager_00024Companion_nativeGetLoadedPlugins(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let plugins = pluginsystem::get_loaded_plugins();
    let json = serde_json::to_string(&plugins).unwrap_or_else(|_| "[]".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Execute plugin hook
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustPluginManager_00024Companion_nativeExecutePluginHook(
    env: JNIEnv,
    _class: JClass,
    plugin_id: JString,
    hook_name: JString,
    data: JString,
) -> jstring {
    let plugin_id: String = env
        .get_string(plugin_id)
        .expect("Failed to get plugin ID string")
        .into();
    
    let hook_name: String = env
        .get_string(hook_name)
        .expect("Failed to get hook name string")
        .into();
    
    let data: String = env
        .get_string(data)
        .expect("Failed to get data string")
        .into();
    
    let result = pluginsystem::execute_plugin_hook(&plugin_id, &hook_name, &data);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Extensions functions

// Register extension
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustExtensionManager_00024Companion_nativeRegisterExtension(
    env: JNIEnv,
    _class: JClass,
    extension_id: JString,
    extension_type: JString,
    extension_data: JString,
) -> jni::sys::jboolean {
    let extension_id: String = env
        .get_string(extension_id)
        .expect("Failed to get extension ID string")
        .into();
    
    let extension_type: String = env
        .get_string(extension_type)
        .expect("Failed to get extension type string")
        .into();
    
    let extension_data: String = env
        .get_string(extension_data)
        .expect("Failed to get extension data string")
        .into();
    
    match extensions::register_extension(&extension_id, &extension_type, &extension_data) {
        Ok(_) => 1, // true
        Err(_) => 0, // false
    }
}

// Unregister extension
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustExtensionManager_00024Companion_nativeUnregisterExtension(
    env: JNIEnv,
    _class: JClass,
    extension_id: JString,
) -> jni::sys::jboolean {
    let extension_id: String = env
        .get_string(extension_id)
        .expect("Failed to get extension ID string")
        .into();
    
    match extensions::unregister_extension(&extension_id) {
        Ok(_) => 1, // true
        Err(_) => 0, // false
    }
}

// Get registered extensions
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustExtensionManager_00024Companion_nativeGetRegisteredExtensions(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let extensions = extensions::get_registered_extensions();
    let json = serde_json::to_string(&extensions).unwrap_or_else(|_| "[]".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Execute extension
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustExtensionManager_00024Companion_nativeExecuteExtension(
    env: JNIEnv,
    _class: JClass,
    extension_id: JString,
    data: JString,
) -> jstring {
    let extension_id: String = env
        .get_string(extension_id)
        .expect("Failed to get extension ID string")
        .into();
    
    let data: String = env
        .get_string(data)
        .expect("Failed to get data string")
        .into();
    
    let result = extensions::execute_extension(&extension_id, &data);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Gradle file modifier functions

// Analyze Gradle file
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustGradleManager_00024Companion_nativeAnalyzeGradleFile(
    env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jstring {
    let file_path: String = env
        .get_string(file_path)
        .expect("Failed to get file path string")
        .into();
    
    let analysis = gradlefilemodifier::analyze_gradle_file(&file_path);
    let json = serde_json::to_string(&analysis).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Optimize Gradle file
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustGradleManager_00024Companion_nativeOptimizeGradleFile(
    env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jstring {
    let file_path: String = env
        .get_string(file_path)
        .expect("Failed to get file path string")
        .into();
    
    let result = gradlefilemodifier::optimize_gradle_file(&file_path);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Add dependency to Gradle file
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustGradleManager_00024Companion_nativeAddDependency(
    env: JNIEnv,
    _class: JClass,
    file_path: JString,
    dependency: JString,
    configuration: JString,
) -> jni::sys::jboolean {
    let file_path: String = env
        .get_string(file_path)
        .expect("Failed to get file path string")
        .into();
    
    let dependency: String = env
        .get_string(dependency)
        .expect("Failed to get dependency string")
        .into();
    
    let configuration: String = env
        .get_string(configuration)
        .expect("Failed to get configuration string")
        .into();
    
    match gradlefilemodifier::add_dependency(&file_path, &dependency, &configuration) {
        Ok(_) => 1, // true
        Err(_) => 0, // false
    }
}

// Remove dependency from Gradle file
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustGradleManager_00024Companion_nativeRemoveDependency(
    env: JNIEnv,
    _class: JClass,
    file_path: JString,
    dependency: JString,
) -> jni::sys::jboolean {
    let file_path: String = env
        .get_string(file_path)
        .expect("Failed to get file path string")
        .into();
    
    let dependency: String = env
        .get_string(dependency)
        .expect("Failed to get dependency string")
        .into();
    
    match gradlefilemodifier::remove_dependency(&file_path, &dependency) {
        Ok(_) => 1, // true
        Err(_) => 0, // false
    }
}

// Update dependencies in Gradle file
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustGradleManager_00024Companion_nativeUpdateDependencies(
    env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jstring {
    let file_path: String = env
        .get_string(file_path)
        .expect("Failed to get file path string")
        .into();
    
    let result = gradlefilemodifier::update_dependencies(&file_path);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Fix common issues in Gradle file
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustGradleManager_00024Companion_nativeFixCommonIssues(
    env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jstring {
    let file_path: String = env
        .get_string(file_path)
        .expect("Failed to get file path string")
        .into();
    
    let result = gradlefilemodifier::fix_common_issues(&file_path);
    let json = serde_json::to_string(&result).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Generate build report
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustGradleManager_00024Companion_nativeGenerateBuildReport(
    env: JNIEnv,
    _class: JClass,
    project_path: JString,
) -> jstring {
    let project_path: String = env
        .get_string(project_path)
        .expect("Failed to get project path string")
        .into();
    
    let report = gradlefilemodifier::generate_build_report(&project_path);
    let json = serde_json::to_string(&report).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// SDK Manager functions

// Get SDK status
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeGetSdkManagerStatus(
    env: JNIEnv,
    _class: JClass,
    sdk_root: JString,
) -> jstring {
    let sdk_root: String = env
        .get_string(sdk_root)
        .expect("Failed to get SDK root string")
        .into();
    
    let sdk_manager = sdkmanager::SdkManager::new(Path::new(&sdk_root));
    let status = sdk_manager.get_status();
    
    let json = serde_json::to_string(&status).unwrap_or_else(|_| "{}".to_string());
    
    let output = env.new_string(json).expect("Failed to create Java string");
    output.into_raw()
}

// Install SDK component
#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeInstallSdkComponent(
    env: JNIEnv,
    _class: JClass,
    sdk_root: JString,
    component_id: JString,
) -> jstring {
    let sdk_root: String = env
        .get_string(sdk_root)
        .expect("Failed to get SDK root string")
        .into();
    
    let component_id: String = env
        .get_string(component_id)
        .expect("Failed to get component ID string")
        .into();
    
    let sdk_manager = sdkmanager::SdkManager::new(Path::new(&sdk_root));
    
    match sdk_manager.install_component(&component_id) {
        Ok(progress_iter) => {
            // Convert progress iterator to JSON array
            let progress_vec: Vec<sdkmanager::InstallationProgress> = progress_iter.collect();
            let json = serde_json::to_string(&progress_vec).unwrap_or_else(|_| "[]".to_string());
            
            let output = env.new_string(json).expect("Failed to create Java string");
            output.into_raw()
        }
        Err(e) => {
            let error_json = format!("{{\"error\": \"{}\"}}", e);
            let output = env.new_string(error_json).expect("Failed to create Java string");
            output.into_raw()
        }
    }
}