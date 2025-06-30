use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::path::{Path, PathBuf};
use std::fs;
use serde::{Serialize, Deserialize};
use anyhow::{Result, anyhow};
use lazy_static::lazy_static;

// Plugin metadata
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct PluginMetadata {
    pub id: String,
    pub name: String,
    pub description: String,
    pub version: String,
    pub author: String,
    pub category: String,
    pub tags: Vec<String>,
    pub extension_points: Vec<String>,
    pub dependencies: Vec<PluginDependency>,
    pub min_ide_version: String,
    pub icon_url: Option<String>,
    pub screenshots: Vec<String>,
    pub rating: f32,
    pub download_count: u64,
}

// Plugin dependency
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct PluginDependency {
    pub id: String,
    pub min_version: String,
    pub optional: bool,
}

// Plugin instance
#[derive(Debug)]
struct Plugin {
    pub metadata: PluginMetadata,
    pub path: PathBuf,
    pub enabled: bool,
    pub hooks: HashMap<String, Arc<dyn PluginHook + Send + Sync>>,
}

// Plugin hook trait
trait PluginHook {
    fn execute(&self, data: &str) -> Result<String>;
}

// Plugin hook result
#[derive(Serialize, Deserialize, Debug)]
pub struct PluginHookResult {
    pub success: bool,
    pub data: String,
    pub error: Option<String>,
}

// Global plugin registry
lazy_static! {
    static ref PLUGINS: Mutex<HashMap<String, Plugin>> = Mutex::new(HashMap::new());
}

// Load a plugin
pub fn load_plugin(plugin_path: &str) -> Result<PluginMetadata> {
    let path = Path::new(plugin_path);
    
    // Check if path exists
    if !path.exists() {
        return Err(anyhow!("Plugin path does not exist: {}", plugin_path));
    }
    
    // Check if it's a directory
    if !path.is_dir() {
        return Err(anyhow!("Plugin path is not a directory: {}", plugin_path));
    }
    
    // Check for plugin.json
    let plugin_json_path = path.join("plugin.json");
    if !plugin_json_path.exists() {
        return Err(anyhow!("Plugin manifest not found: {}", plugin_json_path.to_string_lossy()));
    }
    
    // Read plugin.json
    let plugin_json = fs::read_to_string(plugin_json_path)?;
    
    // Parse plugin metadata
    let metadata: PluginMetadata = serde_json::from_str(&plugin_json)?;
    
    // Check for plugin ID
    if metadata.id.is_empty() {
        return Err(anyhow!("Plugin ID is empty"));
    }
    
    // Check for plugin name
    if metadata.name.is_empty() {
        return Err(anyhow!("Plugin name is empty"));
    }
    
    // Check for plugin version
    if metadata.version.is_empty() {
        return Err(anyhow!("Plugin version is empty"));
    }
    
    // Check for plugin dependencies
    for dependency in &metadata.dependencies {
        if dependency.id.is_empty() {
            return Err(anyhow!("Plugin dependency ID is empty"));
        }
        
        if dependency.min_version.is_empty() {
            return Err(anyhow!("Plugin dependency minimum version is empty"));
        }
    }
    
    // Register plugin
    let mut plugins = PLUGINS.lock().unwrap();
    
    // Create plugin instance
    let plugin = Plugin {
        metadata: metadata.clone(),
        path: path.to_path_buf(),
        enabled: true,
        hooks: HashMap::new(),
    };
    
    // Register plugin
    plugins.insert(metadata.id.clone(), plugin);
    
    Ok(metadata)
}

// Unload a plugin
pub fn unload_plugin(plugin_id: &str) -> Result<()> {
    let mut plugins = PLUGINS.lock().unwrap();
    
    if plugins.remove(plugin_id).is_some() {
        Ok(())
    } else {
        Err(anyhow!("Plugin not found: {}", plugin_id))
    }
}

// Get loaded plugins
pub fn get_loaded_plugins() -> Vec<PluginMetadata> {
    let plugins = PLUGINS.lock().unwrap();
    
    plugins.values()
        .map(|plugin| plugin.metadata.clone())
        .collect()
}

// Execute plugin hook
pub fn execute_plugin_hook(plugin_id: &str, hook_name: &str, data: &str) -> Result<PluginHookResult> {
    let plugins = PLUGINS.lock().unwrap();
    
    if let Some(plugin) = plugins.get(plugin_id) {
        if !plugin.enabled {
            return Ok(PluginHookResult {
                success: false,
                data: String::new(),
                error: Some("Plugin is disabled".to_string()),
            });
        }
        
        if let Some(hook) = plugin.hooks.get(hook_name) {
            match hook.execute(data) {
                Ok(result) => Ok(PluginHookResult {
                    success: true,
                    data: result,
                    error: None,
                }),
                Err(e) => Ok(PluginHookResult {
                    success: false,
                    data: String::new(),
                    error: Some(e.to_string()),
                }),
            }
        } else {
            Ok(PluginHookResult {
                success: false,
                data: String::new(),
                error: Some(format!("Hook not found: {}", hook_name)),
            })
        }
    } else {
        Ok(PluginHookResult {
            success: false,
            data: String::new(),
            error: Some(format!("Plugin not found: {}", plugin_id)),
        })
    }
}

// Register plugin hook
pub fn register_plugin_hook(plugin_id: &str, hook_name: &str, hook: Arc<dyn PluginHook + Send + Sync>) -> Result<()> {
    let mut plugins = PLUGINS.lock().unwrap();
    
    if let Some(plugin) = plugins.get_mut(plugin_id) {
        plugin.hooks.insert(hook_name.to_string(), hook);
        Ok(())
    } else {
        Err(anyhow!("Plugin not found: {}", plugin_id))
    }
}

// Unregister plugin hook
pub fn unregister_plugin_hook(plugin_id: &str, hook_name: &str) -> Result<()> {
    let mut plugins = PLUGINS.lock().unwrap();
    
    if let Some(plugin) = plugins.get_mut(plugin_id) {
        if plugin.hooks.remove(hook_name).is_some() {
            Ok(())
        } else {
            Err(anyhow!("Hook not found: {}", hook_name))
        }
    } else {
        Err(anyhow!("Plugin not found: {}", plugin_id))
    }
}

// Enable plugin
pub fn enable_plugin(plugin_id: &str) -> Result<()> {
    let mut plugins = PLUGINS.lock().unwrap();
    
    if let Some(plugin) = plugins.get_mut(plugin_id) {
        plugin.enabled = true;
        Ok(())
    } else {
        Err(anyhow!("Plugin not found: {}", plugin_id))
    }
}

// Disable plugin
pub fn disable_plugin(plugin_id: &str) -> Result<()> {
    let mut plugins = PLUGINS.lock().unwrap();
    
    if let Some(plugin) = plugins.get_mut(plugin_id) {
        plugin.enabled = false;
        Ok(())
    } else {
        Err(anyhow!("Plugin not found: {}", plugin_id))
    }
}

// Check if plugin is enabled
pub fn is_plugin_enabled(plugin_id: &str) -> bool {
    let plugins = PLUGINS.lock().unwrap();
    
    if let Some(plugin) = plugins.get(plugin_id) {
        plugin.enabled
    } else {
        false
    }
}

// Get plugin metadata
pub fn get_plugin_metadata(plugin_id: &str) -> Option<PluginMetadata> {
    let plugins = PLUGINS.lock().unwrap();
    
    plugins.get(plugin_id).map(|plugin| plugin.metadata.clone())
}

// Get plugin path
pub fn get_plugin_path(plugin_id: &str) -> Option<PathBuf> {
    let plugins = PLUGINS.lock().unwrap();
    
    plugins.get(plugin_id).map(|plugin| plugin.path.clone())
}

// Get plugin hooks
pub fn get_plugin_hooks(plugin_id: &str) -> Vec<String> {
    let plugins = PLUGINS.lock().unwrap();
    
    if let Some(plugin) = plugins.get(plugin_id) {
        plugin.hooks.keys().cloned().collect()
    } else {
        Vec::new()
    }
}

// Check plugin dependencies
pub fn check_plugin_dependencies(plugin_id: &str) -> Result<Vec<String>> {
    let plugins = PLUGINS.lock().unwrap();
    
    if let Some(plugin) = plugins.get(plugin_id) {
        let mut missing_dependencies = Vec::new();
        
        for dependency in &plugin.metadata.dependencies {
            if !dependency.optional && !plugins.contains_key(&dependency.id) {
                missing_dependencies.push(dependency.id.clone());
            }
        }
        
        Ok(missing_dependencies)
    } else {
        Err(anyhow!("Plugin not found: {}", plugin_id))
    }
}

// Get plugins by category
pub fn get_plugins_by_category(category: &str) -> Vec<PluginMetadata> {
    let plugins = PLUGINS.lock().unwrap();
    
    plugins.values()
        .filter(|plugin| plugin.metadata.category == category)
        .map(|plugin| plugin.metadata.clone())
        .collect()
}

// Get plugins by tag
pub fn get_plugins_by_tag(tag: &str) -> Vec<PluginMetadata> {
    let plugins = PLUGINS.lock().unwrap();
    
    plugins.values()
        .filter(|plugin| plugin.metadata.tags.contains(&tag.to_string()))
        .map(|plugin| plugin.metadata.clone())
        .collect()
}

// Get plugins by extension point
pub fn get_plugins_by_extension_point(extension_point: &str) -> Vec<PluginMetadata> {
    let plugins = PLUGINS.lock().unwrap();
    
    plugins.values()
        .filter(|plugin| plugin.metadata.extension_points.contains(&extension_point.to_string()))
        .map(|plugin| plugin.metadata.clone())
        .collect()
}

// Search plugins
pub fn search_plugins(query: &str) -> Vec<PluginMetadata> {
    let plugins = PLUGINS.lock().unwrap();
    
    plugins.values()
        .filter(|plugin| {
            plugin.metadata.name.to_lowercase().contains(&query.to_lowercase()) ||
            plugin.metadata.description.to_lowercase().contains(&query.to_lowercase()) ||
            plugin.metadata.tags.iter().any(|tag| tag.to_lowercase().contains(&query.to_lowercase()))
        })
        .map(|plugin| plugin.metadata.clone())
        .collect()
}