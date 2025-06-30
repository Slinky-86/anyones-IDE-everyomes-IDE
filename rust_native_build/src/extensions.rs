use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use serde::{Serialize, Deserialize};
use anyhow::{Result, anyhow};
use lazy_static::lazy_static;

// Extension types
#[derive(Serialize, Deserialize, Debug, Clone)]
pub enum ExtensionType {
    LanguageSupport,
    Theme,
    Formatter,
    Linter,
    Debugger,
    BuildSystem,
    VcsProvider,
    TerminalEnhancement,
    EditorEnhancement,
    Snippet,
    Custom(String),
}

// Extension metadata
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ExtensionMetadata {
    pub id: String,
    pub name: String,
    pub description: String,
    pub version: String,
    pub author: String,
    pub extension_type: ExtensionType,
    pub supported_languages: Vec<String>,
    pub dependencies: Vec<String>,
    pub configuration: HashMap<String, String>,
}

// Extension instance
#[derive(Debug)]
struct Extension {
    pub metadata: ExtensionMetadata,
    pub enabled: bool,
    pub handler: Arc<dyn ExtensionHandler + Send + Sync>,
}

// Extension handler trait
trait ExtensionHandler {
    fn execute(&self, data: &str) -> Result<String>;
    fn get_capabilities(&self) -> Vec<String>;
    fn shutdown(&self) -> Result<()>;
}

// Extension result
#[derive(Serialize, Deserialize, Debug)]
pub struct ExtensionResult {
    pub success: bool,
    pub data: String,
    pub error: Option<String>,
}

// Global extension registry
lazy_static! {
    static ref EXTENSIONS: Mutex<HashMap<String, Extension>> = Mutex::new(HashMap::new());
}

// Register an extension
pub fn register_extension(extension_id: &str, extension_type: &str, extension_data: &str) -> Result<()> {
    let mut extensions = EXTENSIONS.lock().unwrap();
    
    // Parse extension data
    let metadata: ExtensionMetadata = serde_json::from_str(extension_data)
        .map_err(|e| anyhow!("Failed to parse extension data: {}", e))?;
    
    // Create extension handler based on type
    let handler: Arc<dyn ExtensionHandler + Send + Sync> = match extension_type {
        "language_support" => Arc::new(LanguageSupportHandler::new(metadata.clone())),
        "theme" => Arc::new(ThemeHandler::new(metadata.clone())),
        "formatter" => Arc::new(FormatterHandler::new(metadata.clone())),
        "linter" => Arc::new(LinterHandler::new(metadata.clone())),
        "debugger" => Arc::new(DebuggerHandler::new(metadata.clone())),
        "build_system" => Arc::new(BuildSystemHandler::new(metadata.clone())),
        "vcs_provider" => Arc::new(VcsProviderHandler::new(metadata.clone())),
        "terminal_enhancement" => Arc::new(TerminalEnhancementHandler::new(metadata.clone())),
        "editor_enhancement" => Arc::new(EditorEnhancementHandler::new(metadata.clone())),
        "snippet" => Arc::new(SnippetHandler::new(metadata.clone())),
        _ => Arc::new(CustomHandler::new(metadata.clone())),
    };
    
    // Register extension
    extensions.insert(extension_id.to_string(), Extension {
        metadata,
        enabled: true,
        handler,
    });
    
    Ok(())
}

// Unregister an extension
pub fn unregister_extension(extension_id: &str) -> Result<()> {
    let mut extensions = EXTENSIONS.lock().unwrap();
    
    if let Some(extension) = extensions.remove(extension_id) {
        // Shutdown extension
        extension.handler.shutdown()?;
        Ok(())
    } else {
        Err(anyhow!("Extension not found: {}", extension_id))
    }
}

// Get registered extensions
pub fn get_registered_extensions() -> Vec<ExtensionMetadata> {
    let extensions = EXTENSIONS.lock().unwrap();
    
    extensions.values()
        .map(|extension| extension.metadata.clone())
        .collect()
}

// Execute an extension
pub fn execute_extension(extension_id: &str, data: &str) -> Result<ExtensionResult> {
    let extensions = EXTENSIONS.lock().unwrap();
    
    if let Some(extension) = extensions.get(extension_id) {
        if !extension.enabled {
            return Ok(ExtensionResult {
                success: false,
                data: String::new(),
                error: Some("Extension is disabled".to_string()),
            });
        }
        
        match extension.handler.execute(data) {
            Ok(result) => Ok(ExtensionResult {
                success: true,
                data: result,
                error: None,
            }),
            Err(e) => Ok(ExtensionResult {
                success: false,
                data: String::new(),
                error: Some(e.to_string()),
            }),
        }
    } else {
        Ok(ExtensionResult {
            success: false,
            data: String::new(),
            error: Some(format!("Extension not found: {}", extension_id)),
        })
    }
}

// Language support handler
struct LanguageSupportHandler {
    metadata: ExtensionMetadata,
}

impl LanguageSupportHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for LanguageSupportHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Check if the language is supported
        let language = input["language"].as_str().ok_or_else(|| anyhow!("Language not specified"))?;
        
        if !self.metadata.supported_languages.contains(&language.to_string()) {
            return Err(anyhow!("Language not supported: {}", language));
        }
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "highlight" => {
                // Syntax highlighting
                let code = input["code"].as_str().ok_or_else(|| anyhow!("Code not specified"))?;
                
                // In a real implementation, this would perform actual syntax highlighting
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "highlights": [
                        { "start": 0, "end": 5, "type": "keyword" }
                    ]
                });
                
                Ok(result.to_string())
            }
            "complete" => {
                // Code completion
                let code = input["code"].as_str().ok_or_else(|| anyhow!("Code not specified"))?;
                let position = input["position"].as_u64().ok_or_else(|| anyhow!("Position not specified"))?;
                
                // In a real implementation, this would provide actual completions
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "completions": [
                        {
                            "label": "example",
                            "kind": "keyword",
                            "detail": "Example completion",
                            "documentation": "This is an example completion",
                            "insert_text": "example"
                        }
                    ]
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["highlight".to_string(), "complete".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Theme handler
struct ThemeHandler {
    metadata: ExtensionMetadata,
}

impl ThemeHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for ThemeHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "get_theme" => {
                // Return theme definition
                let result = serde_json::json!({
                    "name": self.metadata.name,
                    "colors": {
                        "background": "#1E1E1E",
                        "foreground": "#D4D4D4",
                        "selection": "#264F78",
                        "cursor": "#FFFFFF",
                        "lineNumber": "#858585",
                        "keyword": "#569CD6",
                        "string": "#CE9178",
                        "comment": "#6A9955",
                        "number": "#B5CEA8",
                        "function": "#DCDCAA",
                        "type": "#4EC9B0",
                        "variable": "#9CDCFE",
                        "operator": "#D4D4D4",
                        "error": "#F44747",
                        "warning": "#FF8C00",
                        "info": "#3794FF"
                    }
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["get_theme".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Formatter handler
struct FormatterHandler {
    metadata: ExtensionMetadata,
}

impl FormatterHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for FormatterHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Check if the language is supported
        let language = input["language"].as_str().ok_or_else(|| anyhow!("Language not specified"))?;
        
        if !self.metadata.supported_languages.contains(&language.to_string()) {
            return Err(anyhow!("Language not supported: {}", language));
        }
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "format" => {
                // Format code
                let code = input["code"].as_str().ok_or_else(|| anyhow!("Code not specified"))?;
                
                // In a real implementation, this would perform actual formatting
                // For now, just return the input code
                let result = serde_json::json!({
                    "formatted_code": code
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["format".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Linter handler
struct LinterHandler {
    metadata: ExtensionMetadata,
}

impl LinterHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for LinterHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Check if the language is supported
        let language = input["language"].as_str().ok_or_else(|| anyhow!("Language not specified"))?;
        
        if !self.metadata.supported_languages.contains(&language.to_string()) {
            return Err(anyhow!("Language not supported: {}", language));
        }
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "lint" => {
                // Lint code
                let code = input["code"].as_str().ok_or_else(|| anyhow!("Code not specified"))?;
                
                // In a real implementation, this would perform actual linting
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "diagnostics": [
                        {
                            "message": "Example diagnostic",
                            "severity": "warning",
                            "range": {
                                "start": { "line": 0, "character": 0 },
                                "end": { "line": 0, "character": 5 }
                            }
                        }
                    ]
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["lint".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Debugger handler
struct DebuggerHandler {
    metadata: ExtensionMetadata,
}

impl DebuggerHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for DebuggerHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Check if the language is supported
        let language = input["language"].as_str().ok_or_else(|| anyhow!("Language not specified"))?;
        
        if !self.metadata.supported_languages.contains(&language.to_string()) {
            return Err(anyhow!("Language not supported: {}", language));
        }
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "start" => {
                // Start debugging
                let program = input["program"].as_str().ok_or_else(|| anyhow!("Program not specified"))?;
                
                // In a real implementation, this would start a debugging session
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "session_id": "debug_session_1",
                    "status": "started"
                });
                
                Ok(result.to_string())
            }
            "stop" => {
                // Stop debugging
                let session_id = input["session_id"].as_str().ok_or_else(|| anyhow!("Session ID not specified"))?;
                
                // In a real implementation, this would stop a debugging session
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "session_id": session_id,
                    "status": "stopped"
                });
                
                Ok(result.to_string())
            }
            "set_breakpoint" => {
                // Set breakpoint
                let session_id = input["session_id"].as_str().ok_or_else(|| anyhow!("Session ID not specified"))?;
                let file = input["file"].as_str().ok_or_else(|| anyhow!("File not specified"))?;
                let line = input["line"].as_u64().ok_or_else(|| anyhow!("Line not specified"))?;
                
                // In a real implementation, this would set a breakpoint
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "session_id": session_id,
                    "breakpoint_id": "bp_1",
                    "file": file,
                    "line": line,
                    "verified": true
                });
                
                Ok(result.to_string())
            }
            "continue" => {
                // Continue execution
                let session_id = input["session_id"].as_str().ok_or_else(|| anyhow!("Session ID not specified"))?;
                
                // In a real implementation, this would continue execution
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "session_id": session_id,
                    "status": "running"
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["start".to_string(), "stop".to_string(), "set_breakpoint".to_string(), "continue".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Build system handler
struct BuildSystemHandler {
    metadata: ExtensionMetadata,
}

impl BuildSystemHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for BuildSystemHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "build" => {
                // Build project
                let project_path = input["project_path"].as_str().ok_or_else(|| anyhow!("Project path not specified"))?;
                
                // In a real implementation, this would build the project
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "success": true,
                    "output": "Build completed successfully",
                    "artifacts": [
                        {
                            "name": "example.jar",
                            "path": "/path/to/example.jar",
                            "size": 1024
                        }
                    ]
                });
                
                Ok(result.to_string())
            }
            "clean" => {
                // Clean project
                let project_path = input["project_path"].as_str().ok_or_else(|| anyhow!("Project path not specified"))?;
                
                // In a real implementation, this would clean the project
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "success": true,
                    "output": "Clean completed successfully"
                });
                
                Ok(result.to_string())
            }
            "run" => {
                // Run project
                let project_path = input["project_path"].as_str().ok_or_else(|| anyhow!("Project path not specified"))?;
                
                // In a real implementation, this would run the project
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "success": true,
                    "output": "Run completed successfully"
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["build".to_string(), "clean".to_string(), "run".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// VCS provider handler
struct VcsProviderHandler {
    metadata: ExtensionMetadata,
}

impl VcsProviderHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for VcsProviderHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "status" => {
                // Get status
                let repository_path = input["repository_path"].as_str().ok_or_else(|| anyhow!("Repository path not specified"))?;
                
                // In a real implementation, this would get the status of the repository
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "status": [
                        {
                            "path": "file1.txt",
                            "status": "modified"
                        },
                        {
                            "path": "file2.txt",
                            "status": "added"
                        }
                    ]
                });
                
                Ok(result.to_string())
            }
            "commit" => {
                // Commit changes
                let repository_path = input["repository_path"].as_str().ok_or_else(|| anyhow!("Repository path not specified"))?;
                let message = input["message"].as_str().ok_or_else(|| anyhow!("Commit message not specified"))?;
                
                // In a real implementation, this would commit changes
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "success": true,
                    "commit_id": "abcdef123456",
                    "message": message
                });
                
                Ok(result.to_string())
            }
            "push" => {
                // Push changes
                let repository_path = input["repository_path"].as_str().ok_or_else(|| anyhow!("Repository path not specified"))?;
                
                // In a real implementation, this would push changes
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "success": true,
                    "output": "Push completed successfully"
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["status".to_string(), "commit".to_string(), "push".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Terminal enhancement handler
struct TerminalEnhancementHandler {
    metadata: ExtensionMetadata,
}

impl TerminalEnhancementHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for TerminalEnhancementHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "syntax_highlight" => {
                // Syntax highlight terminal output
                let output = input["output"].as_str().ok_or_else(|| anyhow!("Output not specified"))?;
                
                // In a real implementation, this would highlight the output
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "highlighted_output": output
                });
                
                Ok(result.to_string())
            }
            "suggest_command" => {
                // Suggest command
                let partial_command = input["partial_command"].as_str().ok_or_else(|| anyhow!("Partial command not specified"))?;
                
                // In a real implementation, this would suggest commands
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "suggestions": [
                        {
                            "command": format!("{} --help", partial_command),
                            "description": "Show help"
                        },
                        {
                            "command": format!("{} --version", partial_command),
                            "description": "Show version"
                        }
                    ]
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["syntax_highlight".to_string(), "suggest_command".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Editor enhancement handler
struct EditorEnhancementHandler {
    metadata: ExtensionMetadata,
}

impl EditorEnhancementHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for EditorEnhancementHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "fold_code" => {
                // Fold code
                let code = input["code"].as_str().ok_or_else(|| anyhow!("Code not specified"))?;
                let language = input["language"].as_str().ok_or_else(|| anyhow!("Language not specified"))?;
                
                // In a real implementation, this would fold the code
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "folding_ranges": [
                        {
                            "start_line": 0,
                            "end_line": 5
                        },
                        {
                            "start_line": 10,
                            "end_line": 15
                        }
                    ]
                });
                
                Ok(result.to_string())
            }
            "outline" => {
                // Generate code outline
                let code = input["code"].as_str().ok_or_else(|| anyhow!("Code not specified"))?;
                let language = input["language"].as_str().ok_or_else(|| anyhow!("Language not specified"))?;
                
                // In a real implementation, this would generate an outline
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "outline": [
                        {
                            "name": "Class1",
                            "kind": "class",
                            "range": {
                                "start_line": 0,
                                "end_line": 10
                            },
                            "children": [
                                {
                                    "name": "method1",
                                    "kind": "method",
                                    "range": {
                                        "start_line": 2,
                                        "end_line": 5
                                    }
                                }
                            ]
                        }
                    ]
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["fold_code".to_string(), "outline".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Snippet handler
struct SnippetHandler {
    metadata: ExtensionMetadata,
}

impl SnippetHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for SnippetHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        match action {
            "get_snippets" => {
                // Get snippets
                let language = input["language"].as_str().ok_or_else(|| anyhow!("Language not specified"))?;
                
                // In a real implementation, this would return snippets for the language
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "snippets": [
                        {
                            "name": "for",
                            "description": "For loop",
                            "body": "for ($1; $2; $3) {\n\t$0\n}",
                            "language": language
                        },
                        {
                            "name": "if",
                            "description": "If statement",
                            "body": "if ($1) {\n\t$0\n}",
                            "language": language
                        }
                    ]
                });
                
                Ok(result.to_string())
            }
            "insert_snippet" => {
                // Insert snippet
                let snippet_name = input["snippet_name"].as_str().ok_or_else(|| anyhow!("Snippet name not specified"))?;
                let language = input["language"].as_str().ok_or_else(|| anyhow!("Language not specified"))?;
                
                // In a real implementation, this would return the snippet body
                // For now, just return a placeholder result
                let result = serde_json::json!({
                    "body": "// This is a placeholder for the snippet body"
                });
                
                Ok(result.to_string())
            }
            _ => Err(anyhow!("Unsupported action: {}", action)),
        }
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["get_snippets".to_string(), "insert_snippet".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}

// Custom handler
struct CustomHandler {
    metadata: ExtensionMetadata,
}

impl CustomHandler {
    fn new(metadata: ExtensionMetadata) -> Self {
        Self { metadata }
    }
}

impl ExtensionHandler for CustomHandler {
    fn execute(&self, data: &str) -> Result<String> {
        // Parse input data
        let input: serde_json::Value = serde_json::from_str(data)?;
        
        // Process based on action
        let action = input["action"].as_str().ok_or_else(|| anyhow!("Action not specified"))?;
        
        // In a real implementation, this would handle custom actions
        // For now, just return a placeholder result
        let result = serde_json::json!({
            "action": action,
            "result": "Custom action executed"
        });
        
        Ok(result.to_string())
    }
    
    fn get_capabilities(&self) -> Vec<String> {
        vec!["custom".to_string()]
    }
    
    fn shutdown(&self) -> Result<()> {
        Ok(())
    }
}