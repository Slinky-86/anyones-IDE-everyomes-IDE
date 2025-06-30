use std::collections::HashMap;
use std::sync::{Arc, Mutex, Once};
use serde::{Serialize, Deserialize};
use anyhow::{Result, anyhow};
use lazy_static::lazy_static;
use regex::Regex;

// Optional tree-sitter support
#[cfg(feature = "tree-sitter-support")]
use tree_sitter::{Parser, Language, Tree, Node, TreeCursor, Query, QueryCursor};

// Syntax highlight
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct SyntaxHighlight {
    pub start: usize,
    pub end: usize,
    pub type_: String,
}

// Code completion
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct CompletionItem {
    pub label: String,
    pub kind: String,
    pub detail: Option<String>,
    pub documentation: Option<String>,
    pub insert_text: String,
}

// Code structure
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct CodeStructure {
    pub classes: Vec<ClassInfo>,
    pub functions: Vec<FunctionInfo>,
    pub variables: Vec<VariableInfo>,
    pub imports: Vec<ImportInfo>,
}

// Class information
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ClassInfo {
    pub name: String,
    pub kind: String, // class, interface, enum, etc.
    pub start_line: usize,
    pub end_line: usize,
    pub modifiers: Vec<String>, // public, private, abstract, etc.
}

// Function information
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct FunctionInfo {
    pub name: String,
    pub start_line: usize,
    pub end_line: usize,
    pub parameters: Vec<ParameterInfo>,
    pub return_type: Option<String>,
    pub modifiers: Vec<String>, // public, private, static, etc.
}

// Parameter information
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ParameterInfo {
    pub name: String,
    pub type_: Option<String>,
}

// Variable information
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct VariableInfo {
    pub name: String,
    pub type_: Option<String>,
    pub line: usize,
    pub modifiers: Vec<String>, // const, let, var, etc.
}

// Import information
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ImportInfo {
    pub path: String,
    pub line: usize,
}

// Reference
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct Reference {
    pub start: usize,
    pub end: usize,
    pub line: usize,
    pub column: usize,
}

// Language configuration
#[derive(Debug, Clone)]
struct LanguageConfig {
    id: String,
    name: String,
    extensions: Vec<String>,
    keywords: Vec<String>,
    operators: Vec<String>,
    comment_line: Option<String>,
    comment_block_start: Option<String>,
    comment_block_end: Option<String>,
    string_delimiters: Vec<String>,
    #[cfg(feature = "tree-sitter-support")]
    tree_sitter_language: Option<fn() -> Language>,
}

// Editor state
struct EditorState {
    initialized: bool,
    languages: HashMap<String, LanguageConfig>,
    #[cfg(feature = "tree-sitter-support")]
    parsers: HashMap<String, Parser>,
}

// Global editor state
lazy_static! {
    static ref EDITOR_STATE: Mutex<EditorState> = Mutex::new(EditorState {
        initialized: false,
        languages: HashMap::new(),
        #[cfg(feature = "tree-sitter-support")]
        parsers: HashMap::new(),
    });
    
    static ref INIT_ONCE: Once = Once::new();
}

// Initialize the editor
pub fn initialize_editor() -> Result<bool> {
    let mut state = EDITOR_STATE.lock().unwrap();
    
    if state.initialized {
        return Ok(true);
    }
    
    // Register languages
    register_languages(&mut state);
    
    // Initialize tree-sitter parsers
    #[cfg(feature = "tree-sitter-support")]
    initialize_parsers(&mut state);
    
    state.initialized = true;
    
    Ok(true)
}

// Register supported languages
fn register_languages(state: &mut EditorState) {
    // Rust
    state.languages.insert("rust".to_string(), LanguageConfig {
        id: "rust".to_string(),
        name: "Rust".to_string(),
        extensions: vec!["rs".to_string()],
        keywords: vec![
            "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", 
            "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static", "struct", "super", 
            "trait", "true", "type", "unsafe", "use", "where", "while", "async", "await", "dyn", "abstract", "become", "box", 
            "do", "final", "macro", "override", "priv", "typeof", "unsized", "virtual", "yield"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![
            "+", "-", "*", "/", "%", "=", "==", "!=", ">", "<", ">=", "<=", "&", "|", "^", "!", "~", "&&", "||", "<<", ">>", 
            "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", "=>", "->", "@", "..."
        ].iter().map(|s| s.to_string()).collect(),
        comment_line: Some("//".to_string()),
        comment_block_start: Some("/*".to_string()),
        comment_block_end: Some("*/".to_string()),
        string_delimiters: vec!["\"".to_string(), "r#\"".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_rust::language()),
    });
    
    // Kotlin
    state.languages.insert("kotlin".to_string(), LanguageConfig {
        id: "kotlin".to_string(),
        name: "Kotlin".to_string(),
        extensions: vec!["kt".to_string(), "kts".to_string()],
        keywords: vec![
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null", 
            "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", 
            "while", "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get", "import", "init", 
            "param", "property", "receiver", "set", "setparam", "where", "actual", "abstract", "annotation", "companion", 
            "const", "crossinline", "data", "enum", "expect", "external", "final", "infix", "inline", "inner", "internal", 
            "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected", "public", "reified", 
            "sealed", "suspend", "tailrec", "vararg"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![
            "+", "-", "*", "/", "%", "=", "==", "===", "!=", "!==", ">", "<", ">=", "<=", "&&", "||", "!", "&", "|", "^", "~", 
            "<<", ">>", ">>>", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=", "?:", "!!", "?.", "::"
        ].iter().map(|s| s.to_string()).collect(),
        comment_line: Some("//".to_string()),
        comment_block_start: Some("/*".to_string()),
        comment_block_end: Some("*/".to_string()),
        string_delimiters: vec!["\"".to_string(), "\"\"\"".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_kotlin::language()),
    });
    
    // Java
    state.languages.insert("java".to_string(), LanguageConfig {
        id: "java".to_string(),
        name: "Java".to_string(),
        extensions: vec!["java".to_string()],
        keywords: vec![
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", 
            "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", 
            "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", 
            "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", 
            "void", "volatile", "while", "true", "false", "null"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![
            "+", "-", "*", "/", "%", "=", "==", "!=", ">", "<", ">=", "<=", "&&", "||", "!", "&", "|", "^", "~", "<<", ">>", 
            ">>>", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>="
        ].iter().map(|s| s.to_string()).collect(),
        comment_line: Some("//".to_string()),
        comment_block_start: Some("/*".to_string()),
        comment_block_end: Some("*/".to_string()),
        string_delimiters: vec!["\"".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_java::language()),
    });
    
    // C++
    state.languages.insert("cpp".to_string(), LanguageConfig {
        id: "cpp".to_string(),
        name: "C++".to_string(),
        extensions: vec!["cpp".to_string(), "cc".to_string(), "cxx".to_string(), "h".to_string(), "hpp".to_string()],
        keywords: vec![
            "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor", "bool", "break", "case", "catch", "char", 
            "char16_t", "char32_t", "class", "compl", "const", "constexpr", "const_cast", "continue", "decltype", "default", 
            "delete", "do", "double", "dynamic_cast", "else", "enum", "explicit", "export", "extern", "false", "float", "for", 
            "friend", "goto", "if", "inline", "int", "long", "mutable", "namespace", "new", "noexcept", "not", "not_eq", "nullptr", 
            "operator", "or", "or_eq", "private", "protected", "public", "register", "reinterpret_cast", "return", "short", 
            "signed", "sizeof", "static", "static_assert", "static_cast", "struct", "switch", "template", "this", "thread_local", 
            "throw", "true", "try", "typedef", "typeid", "typename", "union", "unsigned", "using", "virtual", "void", "volatile", 
            "wchar_t", "while", "xor", "xor_eq"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![
            "+", "-", "*", "/", "%", "=", "==", "!=", ">", "<", ">=", "<=", "&&", "||", "!", "&", "|", "^", "~", "<<", ">>", 
            "++", "--", "->", ".", "::", "?", ":", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>="
        ].iter().map(|s| s.to_string()).collect(),
        comment_line: Some("//".to_string()),
        comment_block_start: Some("/*".to_string()),
        comment_block_end: Some("*/".to_string()),
        string_delimiters: vec!["\"".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_cpp::language()),
    });
    
    // Python
    state.languages.insert("python".to_string(), LanguageConfig {
        id: "python".to_string(),
        name: "Python".to_string(),
        extensions: vec!["py".to_string()],
        keywords: vec![
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", 
            "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", 
            "pass", "raise", "return", "try", "while", "with", "yield"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![
            "+", "-", "*", "/", "//", "%", "**", "=", "==", "!=", "<", ">", "<=", ">=", "and", "or", "not", "is", "is not", 
            "in", "not in", "&", "|", "^", "~", "<<", ">>"
        ].iter().map(|s| s.to_string()).collect(),
        comment_line: Some("#".to_string()),
        comment_block_start: Some("\"\"\"".to_string()),
        comment_block_end: Some("\"\"\"".to_string()),
        string_delimiters: vec!["\"".to_string(), "'".to_string(), "\"\"\"".to_string(), "'''".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_python::language()),
    });
    
    // JavaScript
    state.languages.insert("javascript".to_string(), LanguageConfig {
        id: "javascript".to_string(),
        name: "JavaScript".to_string(),
        extensions: vec!["js".to_string(), "jsx".to_string()],
        keywords: vec![
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export", 
            "extends", "finally", "for", "function", "if", "import", "in", "instanceof", "new", "return", "super", "switch", 
            "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield", "enum", "implements", "interface", 
            "let", "package", "private", "protected", "public", "static", "await", "async", "null", "true", "false"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![
            "+", "-", "*", "/", "%", "=", "==", "===", "!=", "!==", ">", "<", ">=", "<=", "&&", "||", "!", "&", "|", "^", "~", 
            "<<", ">>", ">>>", "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>="
        ].iter().map(|s| s.to_string()).collect(),
        comment_line: Some("//".to_string()),
        comment_block_start: Some("/*".to_string()),
        comment_block_end: Some("*/".to_string()),
        string_delimiters: vec!["\"".to_string(), "'".to_string(), "`".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_javascript::language()),
    });
    
    // TypeScript
    state.languages.insert("typescript".to_string(), LanguageConfig {
        id: "typescript".to_string(),
        name: "TypeScript".to_string(),
        extensions: vec!["ts".to_string(), "tsx".to_string()],
        keywords: vec![
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export", 
            "extends", "finally", "for", "function", "if", "import", "in", "instanceof", "new", "return", "super", "switch", 
            "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield", "enum", "implements", "interface", 
            "let", "package", "private", "protected", "public", "static", "await", "async", "null", "true", "false", 
            "abstract", "as", "any", "boolean", "constructor", "declare", "get", "is", "keyof", "module", "namespace", 
            "never", "readonly", "require", "number", "object", "set", "string", "symbol", "type", "undefined", "unique", "unknown", "from"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![
            "+", "-", "*", "/", "%", "=", "==", "===", "!=", "!==", ">", "<", ">=", "<=", "&&", "||", "!", "&", "|", "^", "~", 
            "<<", ">>", ">>>", "++", "--", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>="
        ].iter().map(|s| s.to_string()).collect(),
        comment_line: Some("//".to_string()),
        comment_block_start: Some("/*".to_string()),
        comment_block_end: Some("*/".to_string()),
        string_delimiters: vec!["\"".to_string(), "'".to_string(), "`".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_typescript::language_typescript()),
    });
    
    // HTML
    state.languages.insert("html".to_string(), LanguageConfig {
        id: "html".to_string(),
        name: "HTML".to_string(),
        extensions: vec!["html".to_string(), "htm".to_string()],
        keywords: vec![
            "html", "head", "body", "div", "span", "a", "img", "p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", 
            "table", "tr", "td", "th", "form", "input", "button", "select", "option", "textarea", "script", "style", "link", 
            "meta", "title", "br", "hr", "header", "footer", "main", "section", "article", "aside", "nav", "figure", "figcaption"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![],
        comment_line: None,
        comment_block_start: Some("<!--".to_string()),
        comment_block_end: Some("-->".to_string()),
        string_delimiters: vec!["\"".to_string(), "'".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_html::language()),
    });
    
    // CSS
    state.languages.insert("css".to_string(), LanguageConfig {
        id: "css".to_string(),
        name: "CSS".to_string(),
        extensions: vec!["css".to_string()],
        keywords: vec![
            "align-content", "align-items", "align-self", "animation", "background", "background-color", "border", "border-radius", 
            "box-shadow", "color", "display", "flex", "flex-direction", "font-family", "font-size", "font-weight", "grid", 
            "height", "justify-content", "margin", "padding", "position", "text-align", "width", "z-index", "@media", "@keyframes", 
            "@import", "@font-face", "!important"
        ].iter().map(|s| s.to_string()).collect(),
        operators: vec![],
        comment_line: None,
        comment_block_start: Some("/*".to_string()),
        comment_block_end: Some("*/".to_string()),
        string_delimiters: vec!["\"".to_string(), "'".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_css::language()),
    });
    
    // JSON
    state.languages.insert("json".to_string(), LanguageConfig {
        id: "json".to_string(),
        name: "JSON".to_string(),
        extensions: vec!["json".to_string()],
        keywords: vec!["true", "false", "null"].iter().map(|s| s.to_string()).collect(),
        operators: vec![],
        comment_line: None,
        comment_block_start: None,
        comment_block_end: None,
        string_delimiters: vec!["\"".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_json::language()),
    });
    
    // XML
    state.languages.insert("xml".to_string(), LanguageConfig {
        id: "xml".to_string(),
        name: "XML".to_string(),
        extensions: vec!["xml".to_string()],
        keywords: vec![],
        operators: vec![],
        comment_line: None,
        comment_block_start: Some("<!--".to_string()),
        comment_block_end: Some("-->".to_string()),
        string_delimiters: vec!["\"".to_string(), "'".to_string()],
        #[cfg(feature = "tree-sitter-support")]
        tree_sitter_language: Some(|| tree_sitter_xml::language()),
    });
}

// Initialize tree-sitter parsers
#[cfg(feature = "tree-sitter-support")]
fn initialize_parsers(state: &mut EditorState) {
    for (language_id, language_config) in &state.languages {
        if let Some(get_language) = language_config.tree_sitter_language {
            let mut parser = Parser::new();
            parser.set_language(get_language()).unwrap_or_else(|_| {
                panic!("Failed to load tree-sitter language for {}", language_id);
            });
            state.parsers.insert(language_id.clone(), parser);
        }
    }
}

// Highlight syntax
pub fn highlight_syntax(content: &str, language_id: &str) -> Vec<SyntaxHighlight> {
    let state = EDITOR_STATE.lock().unwrap();
    
    if !state.initialized {
        return Vec::new();
    }
    
    let language_config = match state.languages.get(language_id) {
        Some(config) => config,
        None => return Vec::new(),
    };
    
    #[cfg(feature = "tree-sitter-support")]
    {
        if let Some(parser) = state.parsers.get(language_id) {
            return highlight_with_tree_sitter(content, parser, language_config);
        }
    }
    
    // Fallback to regex-based highlighting
    highlight_with_regex(content, language_config)
}

// Highlight with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn highlight_with_tree_sitter(content: &str, parser: &Parser, language_config: &LanguageConfig) -> Vec<SyntaxHighlight> {
    let mut highlights = Vec::new();
    
    // Parse the code
    let tree = match parser.parse(content, None) {
        Some(tree) => tree,
        None => return Vec::new(),
    };
    
    // Create a cursor for traversing the tree
    let mut cursor = tree.walk();
    
    // Traverse the tree
    traverse_tree(&mut cursor, content, &mut highlights);
    
    highlights
}

// Traverse tree-sitter tree
#[cfg(feature = "tree-sitter-support")]
fn traverse_tree(cursor: &mut TreeCursor, content: &str, highlights: &mut Vec<SyntaxHighlight>) {
    loop {
        let node = cursor.node();
        
        // Get node type
        let node_type = node.kind();
        
        // Determine highlight type based on node type
        let highlight_type = match node_type {
            "identifier" => {
                let text = node.utf8_text(content.as_bytes()).unwrap_or("");
                if is_keyword(text) {
                    "keyword"
                } else {
                    "variable"
                }
            },
            "string" | "string_literal" | "raw_string_literal" => "string",
            "comment" | "line_comment" | "block_comment" => "comment",
            "number" | "integer_literal" | "float_literal" => "number",
            "function" | "function_definition" | "method_definition" => "function",
            "class" | "class_definition" | "struct_definition" | "enum_definition" => "type",
            "operator" | "binary_operator" | "unary_operator" => "operator",
            _ => "",
        };
        
        // Add highlight if type is determined
        if !highlight_type.is_empty() {
            highlights.push(SyntaxHighlight {
                start: node.start_byte(),
                end: node.end_byte(),
                type_: highlight_type.to_string(),
            });
        }
        
        // Go to first child
        if cursor.goto_first_child() {
            continue;
        }
        
        // No children, try to go to next sibling
        if cursor.goto_next_sibling() {
            continue;
        }
        
        // No siblings, go up and try to find a sibling
        loop {
            if !cursor.goto_parent() {
                return;
            }
            
            if cursor.goto_next_sibling() {
                break;
            }
        }
    }
}

// Check if a string is a keyword
#[cfg(feature = "tree-sitter-support")]
fn is_keyword(text: &str) -> bool {
    // Common keywords across languages
    let keywords = [
        "if", "else", "for", "while", "return", "break", "continue", "class", "struct", "enum", "interface", "function", "fn",
        "def", "var", "let", "const", "public", "private", "protected", "static", "final", "abstract", "async", "await",
        "import", "export", "from", "package", "namespace", "using", "true", "false", "null", "undefined", "this", "super",
        "new", "try", "catch", "finally", "throw", "throws", "extends", "implements", "override", "virtual", "void", "int",
        "float", "double", "boolean", "string", "char", "byte", "short", "long"
    ];
    
    keywords.contains(&text)
}

// Highlight with regex
fn highlight_with_regex(content: &str, language_config: &LanguageConfig) -> Vec<SyntaxHighlight> {
    let mut highlights = Vec::new();
    
    // Highlight keywords
    for keyword in &language_config.keywords {
        let pattern = format!(r"\b{}\b", regex::escape(keyword));
        let regex = Regex::new(&pattern).unwrap();
        
        for mat in regex.find_iter(content) {
            highlights.push(SyntaxHighlight {
                start: mat.start(),
                end: mat.end(),
                type_: "keyword".to_string(),
            });
        }
    }
    
    // Highlight operators
    for operator in &language_config.operators {
        let pattern = regex::escape(operator);
        let regex = Regex::new(&pattern).unwrap();
        
        for mat in regex.find_iter(content) {
            highlights.push(SyntaxHighlight {
                start: mat.start(),
                end: mat.end(),
                type_: "operator".to_string(),
            });
        }
    }
    
    // Highlight strings
    for delimiter in &language_config.string_delimiters {
        let escaped_delimiter = regex::escape(delimiter);
        let pattern = format!(r"{0}(?:[^\\{0}]|\\.)*?{0}", escaped_delimiter);
        let regex = Regex::new(&pattern).unwrap();
        
        for mat in regex.find_iter(content) {
            highlights.push(SyntaxHighlight {
                start: mat.start(),
                end: mat.end(),
                type_: "string".to_string(),
            });
        }
    }
    
    // Highlight comments
    if let Some(line_comment) = &language_config.comment_line {
        let pattern = format!(r"{0}.*$", regex::escape(line_comment));
        let regex = Regex::new(&pattern).unwrap();
        
        for mat in regex.find_iter(content) {
            highlights.push(SyntaxHighlight {
                start: mat.start(),
                end: mat.end(),
                type_: "comment".to_string(),
            });
        }
    }
    
    if let (Some(block_start), Some(block_end)) = (&language_config.comment_block_start, &language_config.comment_block_end) {
        let pattern = format!(r"{0}[\s\S]*?{1}", regex::escape(block_start), regex::escape(block_end));
        let regex = Regex::new(&pattern).unwrap();
        
        for mat in regex.find_iter(content) {
            highlights.push(SyntaxHighlight {
                start: mat.start(),
                end: mat.end(),
                type_: "comment".to_string(),
            });
        }
    }
    
    // Highlight numbers
    let number_regex = Regex::new(r"\b\d+(\.\d+)?([eE][+-]?\d+)?\b").unwrap();
    for mat in number_regex.find_iter(content) {
        highlights.push(SyntaxHighlight {
            start: mat.start(),
            end: mat.end(),
            type_: "number".to_string(),
        });
    }
    
    // Highlight function calls
    let function_regex = Regex::new(r"\b(\w+)\s*\(").unwrap();
    for captures in function_regex.captures_iter(content) {
        if let Some(function_match) = captures.get(1) {
            let function_name = function_match.as_str();
            
            // Skip if the function name is a keyword
            if !language_config.keywords.contains(&function_name.to_string()) {
                highlights.push(SyntaxHighlight {
                    start: function_match.start(),
                    end: function_match.end(),
                    type_: "function".to_string(),
                });
            }
        }
    }
    
    // Highlight types (capitalized identifiers)
    let type_regex = Regex::new(r"\b[A-Z][a-zA-Z0-9_]*\b").unwrap();
    for mat in type_regex.find_iter(content) {
        highlights.push(SyntaxHighlight {
            start: mat.start(),
            end: mat.end(),
            type_: "type".to_string(),
        });
    }
    
    highlights
}

// Get code completions
pub fn get_completions(content: &str, position: usize, language_id: &str) -> Vec<CompletionItem> {
    let state = EDITOR_STATE.lock().unwrap();
    
    if !state.initialized {
        return Vec::new();
    }
    
    let language_config = match state.languages.get(language_id) {
        Some(config) => config,
        None => return Vec::new(),
    };
    
    #[cfg(feature = "tree-sitter-support")]
    {
        if let Some(parser) = state.parsers.get(language_id) {
            return get_completions_with_tree_sitter(content, position, parser, language_config);
        }
    }
    
    // Fallback to simple completions
    get_simple_completions(content, position, language_config)
}

// Get completions with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn get_completions_with_tree_sitter(content: &str, position: usize, parser: &Parser, language_config: &LanguageConfig) -> Vec<CompletionItem> {
    let mut completions = Vec::new();
    
    // Parse the code
    let tree = match parser.parse(content, None) {
        Some(tree) => tree,
        None => return Vec::new(),
    };
    
    // Find the node at the cursor position
    let node = find_node_at_position(&tree.root_node(), position);
    
    // Get the node type
    let node_type = node.kind();
    
    // Get the text before the cursor
    let text_before_cursor = &content[..position];
    let current_line = get_current_line(text_before_cursor);
    let current_word = get_current_word(current_line);
    
    // Add completions based on node type and context
    match node_type {
        "identifier" => {
            // Add keyword completions
            for keyword in &language_config.keywords {
                if keyword.starts_with(&current_word) {
                    completions.push(CompletionItem {
                        label: keyword.clone(),
                        kind: "keyword".to_string(),
                        detail: None,
                        documentation: None,
                        insert_text: keyword.clone(),
                    });
                }
            }
            
            // Add language-specific completions
            match language_config.id.as_str() {
                "rust" => add_rust_completions(&mut completions, &current_word),
                "kotlin" => add_kotlin_completions(&mut completions, &current_word),
                "java" => add_java_completions(&mut completions, &current_word),
                "python" => add_python_completions(&mut completions, &current_word),
                "javascript" | "typescript" => add_js_ts_completions(&mut completions, &current_word),
                _ => {}
            }
        },
        "string" | "string_literal" => {
            // Add string-specific completions
            match language_config.id.as_str() {
                "rust" => add_rust_string_completions(&mut completions, &current_word),
                "kotlin" => add_kotlin_string_completions(&mut completions, &current_word),
                "java" => add_java_string_completions(&mut completions, &current_word),
                "python" => add_python_string_completions(&mut completions, &current_word),
                "javascript" | "typescript" => add_js_ts_string_completions(&mut completions, &current_word),
                _ => {}
            }
        },
        _ => {
            // Add general completions
            add_general_completions(&mut completions, &current_word, language_config);
        }
    }
    
    completions
}

// Find node at position
#[cfg(feature = "tree-sitter-support")]
fn find_node_at_position(node: &Node, position: usize) -> Node {
    if position < node.start_byte() || position > node.end_byte() {
        return *node;
    }
    
    for i in 0..node.child_count() {
        if let Some(child) = node.child(i) {
            if position >= child.start_byte() && position <= child.end_byte() {
                return find_node_at_position(&child, position);
            }
        }
    }
    
    *node
}

// Get simple completions
fn get_simple_completions(content: &str, position: usize, language_config: &LanguageConfig) -> Vec<CompletionItem> {
    let mut completions = Vec::new();
    
    // Get the text before the cursor
    let text_before_cursor = &content[..position];
    let current_line = get_current_line(text_before_cursor);
    let current_word = get_current_word(current_line);
    
    // Add keyword completions
    for keyword in &language_config.keywords {
        if keyword.starts_with(&current_word) {
            completions.push(CompletionItem {
                label: keyword.clone(),
                kind: "keyword".to_string(),
                detail: None,
                documentation: None,
                insert_text: keyword.clone(),
            });
        }
    }
    
    // Add language-specific completions
    match language_config.id.as_str() {
        "rust" => add_rust_completions(&mut completions, &current_word),
        "kotlin" => add_kotlin_completions(&mut completions, &current_word),
        "java" => add_java_completions(&mut completions, &current_word),
        "python" => add_python_completions(&mut completions, &current_word),
        "javascript" | "typescript" => add_js_ts_completions(&mut completions, &current_word),
        _ => {}
    }
    
    completions
}

// Get current line
fn get_current_line(text: &str) -> String {
    let lines: Vec<&str> = text.split('\n').collect();
    lines.last().unwrap_or(&"").to_string()
}

// Get current word
fn get_current_word(line: String) -> String {
    let words: Vec<&str> = line.split(|c: char| !c.is_alphanumeric() && c != '_').collect();
    words.last().unwrap_or(&"").to_string()
}

// Add Rust completions
fn add_rust_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    let rust_snippets = [
        CompletionItem {
            label: "fn".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Function declaration".to_string()),
            documentation: Some("Create a new function".to_string()),
            insert_text: "fn ${1:name}(${2:params}) -> ${3:return_type} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "struct".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Struct declaration".to_string()),
            documentation: Some("Create a new struct".to_string()),
            insert_text: "struct ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "impl".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Implementation block".to_string()),
            documentation: Some("Create an implementation block".to_string()),
            insert_text: "impl ${1:Type} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "match".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Match expression".to_string()),
            documentation: Some("Create a match expression".to_string()),
            insert_text: "match ${1:expression} {\n\t${2:pattern} => ${3:expression},\n\t_ => ${0},\n}".to_string(),
        },
        CompletionItem {
            label: "if".to_string(),
            kind: "snippet".to_string(),
            detail: Some("If statement".to_string()),
            documentation: Some("Create an if statement".to_string()),
            insert_text: "if ${1:condition} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "for".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For loop".to_string()),
            documentation: Some("Create a for loop".to_string()),
            insert_text: "for ${1:item} in ${2:collection} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "while".to_string(),
            kind: "snippet".to_string(),
            detail: Some("While loop".to_string()),
            documentation: Some("Create a while loop".to_string()),
            insert_text: "while ${1:condition} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "let".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Variable declaration".to_string()),
            documentation: Some("Create a variable declaration".to_string()),
            insert_text: "let ${1:name}: ${2:type} = ${0:value};".to_string(),
        },
        CompletionItem {
            label: "enum".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Enum declaration".to_string()),
            documentation: Some("Create an enum declaration".to_string()),
            insert_text: "enum ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "trait".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Trait declaration".to_string()),
            documentation: Some("Create a trait declaration".to_string()),
            insert_text: "trait ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "mod".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Module declaration".to_string()),
            documentation: Some("Create a module declaration".to_string()),
            insert_text: "mod ${1:name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "use".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Use declaration".to_string()),
            documentation: Some("Create a use declaration".to_string()),
            insert_text: "use ${1:path};".to_string(),
        },
        CompletionItem {
            label: "println".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Print to console".to_string()),
            documentation: Some("Print to standard output".to_string()),
            insert_text: "println!(\"${1:{}}\", ${0});".to_string(),
        },
        CompletionItem {
            label: "derive".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Derive attribute".to_string()),
            documentation: Some("Add derive attribute".to_string()),
            insert_text: "#[derive(${1:Debug})]".to_string(),
        },
    ];
    
    for snippet in &rust_snippets {
        if snippet.label.starts_with(current_word) {
            completions.push(snippet.clone());
        }
    }
}

// Add Kotlin completions
fn add_kotlin_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    let kotlin_snippets = [
        CompletionItem {
            label: "fun".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Function declaration".to_string()),
            documentation: Some("Create a new function".to_string()),
            insert_text: "fun ${1:name}(${2:params}): ${3:Unit} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "class".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Class declaration".to_string()),
            documentation: Some("Create a new class".to_string()),
            insert_text: "class ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "data class".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Data class declaration".to_string()),
            documentation: Some("Create a data class".to_string()),
            insert_text: "data class ${1:Name}(${0})".to_string(),
        },
        CompletionItem {
            label: "interface".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Interface declaration".to_string()),
            documentation: Some("Create an interface".to_string()),
            insert_text: "interface ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "when".to_string(),
            kind: "snippet".to_string(),
            detail: Some("When expression".to_string()),
            documentation: Some("Create a when expression".to_string()),
            insert_text: "when (${1:expression}) {\n\t${2:value} -> ${3:result}\n\telse -> ${0}\n}".to_string(),
        },
        CompletionItem {
            label: "if".to_string(),
            kind: "snippet".to_string(),
            detail: Some("If statement".to_string()),
            documentation: Some("Create an if statement".to_string()),
            insert_text: "if (${1:condition}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "for".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For loop".to_string()),
            documentation: Some("Create a for loop".to_string()),
            insert_text: "for (${1:item} in ${2:collection}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "while".to_string(),
            kind: "snippet".to_string(),
            detail: Some("While loop".to_string()),
            documentation: Some("Create a while loop".to_string()),
            insert_text: "while (${1:condition}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "val".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Immutable variable declaration".to_string()),
            documentation: Some("Create an immutable variable".to_string()),
            insert_text: "val ${1:name}: ${2:Type} = ${0}".to_string(),
        },
        CompletionItem {
            label: "var".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Mutable variable declaration".to_string()),
            documentation: Some("Create a mutable variable".to_string()),
            insert_text: "var ${1:name}: ${2:Type} = ${0}".to_string(),
        },
        CompletionItem {
            label: "companion object".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Companion object declaration".to_string()),
            documentation: Some("Create a companion object".to_string()),
            insert_text: "companion object {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "object".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Object declaration".to_string()),
            documentation: Some("Create an object".to_string()),
            insert_text: "object ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "enum class".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Enum class declaration".to_string()),
            documentation: Some("Create an enum class".to_string()),
            insert_text: "enum class ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "println".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Print to console".to_string()),
            documentation: Some("Print to standard output".to_string()),
            insert_text: "println(\"${0}\")".to_string(),
        },
    ];
    
    for snippet in &kotlin_snippets {
        if snippet.label.starts_with(current_word) {
            completions.push(snippet.clone());
        }
    }
}

// Add Java completions
fn add_java_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    let java_snippets = [
        CompletionItem {
            label: "class".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Class declaration".to_string()),
            documentation: Some("Create a new class".to_string()),
            insert_text: "class ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "interface".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Interface declaration".to_string()),
            documentation: Some("Create an interface".to_string()),
            insert_text: "interface ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "enum".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Enum declaration".to_string()),
            documentation: Some("Create an enum".to_string()),
            insert_text: "enum ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "public class".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Public class declaration".to_string()),
            documentation: Some("Create a public class".to_string()),
            insert_text: "public class ${1:Name} {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "public static void main".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Main method".to_string()),
            documentation: Some("Create a main method".to_string()),
            insert_text: "public static void main(String[] args) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "if".to_string(),
            kind: "snippet".to_string(),
            detail: Some("If statement".to_string()),
            documentation: Some("Create an if statement".to_string()),
            insert_text: "if (${1:condition}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "for".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For loop".to_string()),
            documentation: Some("Create a for loop".to_string()),
            insert_text: "for (int ${1:i} = 0; ${1:i} < ${2:size}; ${1:i}++) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "foreach".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For-each loop".to_string()),
            documentation: Some("Create a for-each loop".to_string()),
            insert_text: "for (${1:Type} ${2:item} : ${3:collection}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "while".to_string(),
            kind: "snippet".to_string(),
            detail: Some("While loop".to_string()),
            documentation: Some("Create a while loop".to_string()),
            insert_text: "while (${1:condition}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "switch".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Switch statement".to_string()),
            documentation: Some("Create a switch statement".to_string()),
            insert_text: "switch (${1:expression}) {\n\tcase ${2:value}:\n\t\t${0}\n\t\tbreak;\n\tdefault:\n\t\tbreak;\n}".to_string(),
        },
        CompletionItem {
            label: "try".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Try-catch block".to_string()),
            documentation: Some("Create a try-catch block".to_string()),
            insert_text: "try {\n\t${0}\n} catch (${1:Exception} e) {\n\te.printStackTrace();\n}".to_string(),
        },
        CompletionItem {
            label: "sout".to_string(),
            kind: "snippet".to_string(),
            detail: Some("System.out.println".to_string()),
            documentation: Some("Print to standard output".to_string()),
            insert_text: "System.out.println(${0});".to_string(),
        },
    ];
    
    for snippet in &java_snippets {
        if snippet.label.starts_with(current_word) {
            completions.push(snippet.clone());
        }
    }
}

// Add Python completions
fn add_python_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    let python_snippets = [
        CompletionItem {
            label: "def".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Function definition".to_string()),
            documentation: Some("Define a new function".to_string()),
            insert_text: "def ${1:name}(${2:params}):\n\t${0}".to_string(),
        },
        CompletionItem {
            label: "class".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Class definition".to_string()),
            documentation: Some("Define a new class".to_string()),
            insert_text: "class ${1:Name}:\n\tdef __init__(self, ${2:params}):\n\t\t${0}".to_string(),
        },
        CompletionItem {
            label: "if".to_string(),
            kind: "snippet".to_string(),
            detail: Some("If statement".to_string()),
            documentation: Some("Create an if statement".to_string()),
            insert_text: "if ${1:condition}:\n\t${0}".to_string(),
        },
        CompletionItem {
            label: "for".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For loop".to_string()),
            documentation: Some("Create a for loop".to_string()),
            insert_text: "for ${1:item} in ${2:collection}:\n\t${0}".to_string(),
        },
        CompletionItem {
            label: "while".to_string(),
            kind: "snippet".to_string(),
            detail: Some("While loop".to_string()),
            documentation: Some("Create a while loop".to_string()),
            insert_text: "while ${1:condition}:\n\t${0}".to_string(),
        },
        CompletionItem {
            label: "try".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Try-except block".to_string()),
            documentation: Some("Create a try-except block".to_string()),
            insert_text: "try:\n\t${1}\nexcept ${2:Exception} as e:\n\t${0}".to_string(),
        },
        CompletionItem {
            label: "import".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Import statement".to_string()),
            documentation: Some("Import a module".to_string()),
            insert_text: "import ${0}".to_string(),
        },
        CompletionItem {
            label: "from".to_string(),
            kind: "snippet".to_string(),
            detail: Some("From import statement".to_string()),
            documentation: Some("Import from a module".to_string()),
            insert_text: "from ${1:module} import ${0}".to_string(),
        },
        CompletionItem {
            label: "print".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Print statement".to_string()),
            documentation: Some("Print to standard output".to_string()),
            insert_text: "print(${0})".to_string(),
        },
        CompletionItem {
            label: "lambda".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Lambda expression".to_string()),
            documentation: Some("Create a lambda expression".to_string()),
            insert_text: "lambda ${1:params}: ${0}".to_string(),
        },
    ];
    
    for snippet in &python_snippets {
        if snippet.label.starts_with(current_word) {
            completions.push(snippet.clone());
        }
    }
}

// Add JavaScript/TypeScript completions
fn add_js_ts_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    let js_ts_snippets = [
        CompletionItem {
            label: "function".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Function declaration".to_string()),
            documentation: Some("Create a new function".to_string()),
            insert_text: "function ${1:name}(${2:params}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "arrow".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Arrow function".to_string()),
            documentation: Some("Create an arrow function".to_string()),
            insert_text: "(${1:params}) => {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "class".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Class declaration".to_string()),
            documentation: Some("Create a new class".to_string()),
            insert_text: "class ${1:Name} {\n\tconstructor(${2:params}) {\n\t\t${0}\n\t}\n}".to_string(),
        },
        CompletionItem {
            label: "if".to_string(),
            kind: "snippet".to_string(),
            detail: Some("If statement".to_string()),
            documentation: Some("Create an if statement".to_string()),
            insert_text: "if (${1:condition}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "for".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For loop".to_string()),
            documentation: Some("Create a for loop".to_string()),
            insert_text: "for (let ${1:i} = 0; ${1:i} < ${2:array}.length; ${1:i}++) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "forin".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For-in loop".to_string()),
            documentation: Some("Create a for-in loop".to_string()),
            insert_text: "for (const ${1:key} in ${2:object}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "forof".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For-of loop".to_string()),
            documentation: Some("Create a for-of loop".to_string()),
            insert_text: "for (const ${1:item} of ${2:array}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "while".to_string(),
            kind: "snippet".to_string(),
            detail: Some("While loop".to_string()),
            documentation: Some("Create a while loop".to_string()),
            insert_text: "while (${1:condition}) {\n\t${0}\n}".to_string(),
        },
        CompletionItem {
            label: "switch".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Switch statement".to_string()),
            documentation: Some("Create a switch statement".to_string()),
            insert_text: "switch (${1:expression}) {\n\tcase ${2:value}:\n\t\t${0}\n\t\tbreak;\n\tdefault:\n\t\tbreak;\n}".to_string(),
        },
        CompletionItem {
            label: "try".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Try-catch block".to_string()),
            documentation: Some("Create a try-catch block".to_string()),
            insert_text: "try {\n\t${0}\n} catch (${1:error}) {\n\tconsole.error(${1:error});\n}".to_string(),
        },
        CompletionItem {
            label: "import".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Import statement".to_string()),
            documentation: Some("Import a module".to_string()),
            insert_text: "import { ${1} } from '${2:module}';".to_string(),
        },
        CompletionItem {
            label: "console.log".to_string(),
            kind: "snippet".to_string(),
            detail: Some("Console log".to_string()),
            documentation: Some("Log to the console".to_string()),
            insert_text: "console.log(${0});".to_string(),
        },
    ];
    
    for snippet in &js_ts_snippets {
        if snippet.label.starts_with(current_word) {
            completions.push(snippet.clone());
        }
    }
}

// Add Rust string completions
fn add_rust_string_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    // No specific string completions for Rust
}

// Add Kotlin string completions
fn add_kotlin_string_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    // No specific string completions for Kotlin
}

// Add Java string completions
fn add_java_string_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    // No specific string completions for Java
}

// Add Python string completions
fn add_python_string_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    // No specific string completions for Python
}

// Add JavaScript/TypeScript string completions
fn add_js_ts_string_completions(completions: &mut Vec<CompletionItem>, current_word: &str) {
    // No specific string completions for JavaScript/TypeScript
}

// Add general completions
fn add_general_completions(completions: &mut Vec<CompletionItem>, current_word: &str, language_config: &LanguageConfig) {
    // Add common completions for all languages
    let common_snippets = [
        CompletionItem {
            label: "if".to_string(),
            kind: "snippet".to_string(),
            detail: Some("If statement".to_string()),
            documentation: Some("Create an if statement".to_string()),
            insert_text: match language_config.id.as_str() {
                "python" => "if ${1:condition}:\n\t${0}".to_string(),
                "rust" => "if ${1:condition} {\n\t${0}\n}".to_string(),
                _ => "if (${1:condition}) {\n\t${0}\n}".to_string(),
            },
        },
        CompletionItem {
            label: "for".to_string(),
            kind: "snippet".to_string(),
            detail: Some("For loop".to_string()),
            documentation: Some("Create a for loop".to_string()),
            insert_text: match language_config.id.as_str() {
                "python" => "for ${1:item} in ${2:collection}:\n\t${0}".to_string(),
                "rust" => "for ${1:item} in ${2:collection} {\n\t${0}\n}".to_string(),
                "kotlin" => "for (${1:item} in ${2:collection}) {\n\t${0}\n}".to_string(),
                "java" => "for (int ${1:i} = 0; ${1:i} < ${2:size}; ${1:i}++) {\n\t${0}\n}".to_string(),
                "javascript" | "typescript" => "for (let ${1:i} = 0; ${1:i} < ${2:array}.length; ${1:i}++) {\n\t${0}\n}".to_string(),
                _ => "for (${1:init}; ${2:condition}; ${3:update}) {\n\t${0}\n}".to_string(),
            },
        },
    ];
    
    for snippet in &common_snippets {
        if snippet.label.starts_with(current_word) {
            completions.push(snippet.clone());
        }
    }
}

// Format code
pub fn format_code(content: &str, language_id: &str) -> String {
    let state = EDITOR_STATE.lock().unwrap();
    
    if !state.initialized {
        return content.to_string();
    }
    
    let language_config = match state.languages.get(language_id) {
        Some(config) => config,
        None => return content.to_string(),
    };
    
    #[cfg(feature = "tree-sitter-support")]
    {
        if let Some(parser) = state.parsers.get(language_id) {
            return format_with_tree_sitter(content, parser, language_config);
        }
    }
    
    // Fallback to simple formatting
    format_with_regex(content, language_config)
}

// Format with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_with_tree_sitter(content: &str, parser: &Parser, language_config: &LanguageConfig) -> String {
    // Parse the code
    let tree = match parser.parse(content, None) {
        Some(tree) => tree,
        None => return content.to_string(),
    };
    
    // Format based on language
    match language_config.id.as_str() {
        "rust" => format_rust_code(content, &tree),
        "kotlin" => format_kotlin_code(content, &tree),
        "java" => format_java_code(content, &tree),
        "python" => format_python_code(content, &tree),
        "javascript" | "typescript" => format_js_ts_code(content, &tree),
        "html" | "xml" => format_html_xml_code(content, &tree),
        "css" => format_css_code(content, &tree),
        "json" => format_json_code(content, &tree),
        _ => content.to_string(),
    }
}

// Format with regex
fn format_with_regex(content: &str, language_config: &LanguageConfig) -> String {
    // Simple formatting based on language
    match language_config.id.as_str() {
        "rust" => format_rust_code_regex(content),
        "kotlin" => format_kotlin_code_regex(content),
        "java" => format_java_code_regex(content),
        "python" => format_python_code_regex(content),
        "javascript" | "typescript" => format_js_ts_code_regex(content),
        "html" | "xml" => format_html_xml_code_regex(content),
        "css" => format_css_code_regex(content),
        "json" => format_json_code_regex(content),
        _ => content.to_string(),
    }
}

// Format Rust code with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_rust_code(content: &str, tree: &Tree) -> String {
    // In a real implementation, this would use tree-sitter to format Rust code
    // For now, just return the original content
    content.to_string()
}

// Format Kotlin code with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_kotlin_code(content: &str, tree: &Tree) -> String {
    // In a real implementation, this would use tree-sitter to format Kotlin code
    // For now, just return the original content
    content.to_string()
}

// Format Java code with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_java_code(content: &str, tree: &Tree) -> String {
    // In a real implementation, this would use tree-sitter to format Java code
    // For now, just return the original content
    content.to_string()
}

// Format Python code with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_python_code(content: &str, tree: &Tree) -> String {
    // In a real implementation, this would use tree-sitter to format Python code
    // For now, just return the original content
    content.to_string()
}

// Format JavaScript/TypeScript code with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_js_ts_code(content: &str, tree: &Tree) -> String {
    // In a real implementation, this would use tree-sitter to format JavaScript/TypeScript code
    // For now, just return the original content
    content.to_string()
}

// Format HTML/XML code with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_html_xml_code(content: &str, tree: &Tree) -> String {
    // In a real implementation, this would use tree-sitter to format HTML/XML code
    // For now, just return the original content
    content.to_string()
}

// Format CSS code with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_css_code(content: &str, tree: &Tree) -> String {
    // In a real implementation, this would use tree-sitter to format CSS code
    // For now, just return the original content
    content.to_string()
}

// Format JSON code with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn format_json_code(content: &str, tree: &Tree) -> String {
    // In a real implementation, this would use tree-sitter to format JSON code
    // For now, just return the original content
    content.to_string()
}

// Format Rust code with regex
fn format_rust_code_regex(content: &str) -> String {
    // In a real implementation, this would use regex to format Rust code
    // For now, just return the original content
    content.to_string()
}

// Format Kotlin code with regex
fn format_kotlin_code_regex(content: &str) -> String {
    // In a real implementation, this would use regex to format Kotlin code
    // For now, just return the original content
    content.to_string()
}

// Format Java code with regex
fn format_java_code_regex(content: &str) -> String {
    // In a real implementation, this would use regex to format Java code
    // For now, just return the original content
    content.to_string()
}

// Format Python code with regex
fn format_python_code_regex(content: &str) -> String {
    // In a real implementation, this would use regex to format Python code
    // For now, just return the original content
    content.to_string()
}

// Format JavaScript/TypeScript code with regex
fn format_js_ts_code_regex(content: &str) -> String {
    // In a real implementation, this would use regex to format JavaScript/TypeScript code
    // For now, just return the original content
    content.to_string()
}

// Format HTML/XML code with regex
fn format_html_xml_code_regex(content: &str) -> String {
    // In a real implementation, this would use regex to format HTML/XML code
    // For now, just return the original content
    content.to_string()
}

// Format CSS code with regex
fn format_css_code_regex(content: &str) -> String {
    // In a real implementation, this would use regex to format CSS code
    // For now, just return the original content
    content.to_string()
}

// Format JSON code with regex
fn format_json_code_regex(content: &str) -> String {
    // In a real implementation, this would use regex to format JSON code
    // For now, just return the original content
    content.to_string()
}

// Parse code structure
pub fn parse_code_structure(content: &str, language_id: &str) -> CodeStructure {
    let state = EDITOR_STATE.lock().unwrap();
    
    if !state.initialized {
        return CodeStructure {
            classes: Vec::new(),
            functions: Vec::new(),
            variables: Vec::new(),
            imports: Vec::new(),
        };
    }
    
    let language_config = match state.languages.get(language_id) {
        Some(config) => config,
        None => return CodeStructure {
            classes: Vec::new(),
            functions: Vec::new(),
            variables: Vec::new(),
            imports: Vec::new(),
        },
    };
    
    #[cfg(feature = "tree-sitter-support")]
    {
        if let Some(parser) = state.parsers.get(language_id) {
            return parse_structure_with_tree_sitter(content, parser, language_config);
        }
    }
    
    // Fallback to regex-based parsing
    parse_structure_with_regex(content, language_config)
}

// Parse code structure with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn parse_structure_with_tree_sitter(content: &str, parser: &Parser, language_config: &LanguageConfig) -> CodeStructure {
    let mut classes = Vec::new();
    let mut functions = Vec::new();
    let mut variables = Vec::new();
    let mut imports = Vec::new();
    
    // Parse the code
    let tree = match parser.parse(content, None) {
        Some(tree) => tree,
        None => return CodeStructure {
            classes: Vec::new(),
            functions: Vec::new(),
            variables: Vec::new(),
            imports: Vec::new(),
        },
    };
    
    // Create a cursor for traversing the tree
    let mut cursor = tree.walk();
    
    // Traverse the tree
    traverse_structure_tree(&mut cursor, content, &mut classes, &mut functions, &mut variables, &mut imports, language_config);
    
    CodeStructure {
        classes,
        functions,
        variables,
        imports,
    }
}

// Traverse tree-sitter tree for structure
#[cfg(feature = "tree-sitter-support")]
fn traverse_structure_tree(
    cursor: &mut TreeCursor,
    content: &str,
    classes: &mut Vec<ClassInfo>,
    functions: &mut Vec<FunctionInfo>,
    variables: &mut Vec<VariableInfo>,
    imports: &mut Vec<ImportInfo>,
    language_config: &LanguageConfig
) {
    loop {
        let node = cursor.node();
        
        // Get node type
        let node_type = node.kind();
        
        // Process node based on type and language
        match language_config.id.as_str() {
            "rust" => {
                match node_type {
                    "struct_definition" | "enum_definition" | "trait_definition" => {
                        if let Some(name_node) = find_child_by_type(&node, "type_identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            let kind = match node_type {
                                "struct_definition" => "struct",
                                "enum_definition" => "enum",
                                "trait_definition" => "trait",
                                _ => "class",
                            };
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            classes.push(ClassInfo {
                                name,
                                kind: kind.to_string(),
                                start_line,
                                end_line,
                                modifiers,
                            });
                        }
                    },
                    "function_definition" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let parameters = extract_parameters(&node, content);
                            let return_type = extract_return_type(&node, content);
                            let modifiers = extract_modifiers(&node, content);
                            
                            functions.push(FunctionInfo {
                                name,
                                start_line,
                                end_line,
                                parameters,
                                return_type,
                                modifiers,
                            });
                        }
                    },
                    "let_declaration" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let line = node.start_position().row;
                            
                            let type_node = find_child_by_type(&node, "type_annotation");
                            let type_ = type_node.map(|n| n.utf8_text(content.as_bytes()).unwrap_or("").to_string());
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            variables.push(VariableInfo {
                                name,
                                type_,
                                line,
                                modifiers,
                            });
                        }
                    },
                    "use_declaration" => {
                        let path = node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                        let line = node.start_position().row;
                        
                        imports.push(ImportInfo {
                            path,
                            line,
                        });
                    },
                    _ => {}
                }
            },
            "kotlin" => {
                match node_type {
                    "class_declaration" | "interface_declaration" | "enum_class" => {
                        if let Some(name_node) = find_child_by_type(&node, "simple_identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            let kind = match node_type {
                                "class_declaration" => "class",
                                "interface_declaration" => "interface",
                                "enum_class" => "enum",
                                _ => "class",
                            };
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            classes.push(ClassInfo {
                                name,
                                kind: kind.to_string(),
                                start_line,
                                end_line,
                                modifiers,
                            });
                        }
                    },
                    "function_declaration" => {
                        if let Some(name_node) = find_child_by_type(&node, "simple_identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let parameters = extract_parameters(&node, content);
                            let return_type = extract_return_type(&node, content);
                            let modifiers = extract_modifiers(&node, content);
                            
                            functions.push(FunctionInfo {
                                name,
                                start_line,
                                end_line,
                                parameters,
                                return_type,
                                modifiers,
                            });
                        }
                    },
                    "property_declaration" => {
                        if let Some(name_node) = find_child_by_type(&node, "simple_identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let line = node.start_position().row;
                            
                            let type_node = find_child_by_type(&node, "type_reference");
                            let type_ = type_node.map(|n| n.utf8_text(content.as_bytes()).unwrap_or("").to_string());
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            variables.push(VariableInfo {
                                name,
                                type_,
                                line,
                                modifiers,
                            });
                        }
                    },
                    "import_header" => {
                        let path = node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                        let line = node.start_position().row;
                        
                        imports.push(ImportInfo {
                            path,
                            line,
                        });
                    },
                    _ => {}
                }
            },
            "java" => {
                match node_type {
                    "class_declaration" | "interface_declaration" | "enum_declaration" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            let kind = match node_type {
                                "class_declaration" => "class",
                                "interface_declaration" => "interface",
                                "enum_declaration" => "enum",
                                _ => "class",
                            };
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            classes.push(ClassInfo {
                                name,
                                kind: kind.to_string(),
                                start_line,
                                end_line,
                                modifiers,
                            });
                        }
                    },
                    "method_declaration" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let parameters = extract_parameters(&node, content);
                            let return_type = extract_return_type(&node, content);
                            let modifiers = extract_modifiers(&node, content);
                            
                            functions.push(FunctionInfo {
                                name,
                                start_line,
                                end_line,
                                parameters,
                                return_type,
                                modifiers,
                            });
                        }
                    },
                    "field_declaration" => {
                        if let Some(name_node) = find_child_by_type(&node, "variable_declarator") {
                            if let Some(identifier_node) = find_child_by_type(&name_node, "identifier") {
                                let name = identifier_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                                
                                let line = node.start_position().row;
                                
                                let type_node = find_child_by_type(&node, "type_identifier");
                                let type_ = type_node.map(|n| n.utf8_text(content.as_bytes()).unwrap_or("").to_string());
                                
                                let modifiers = extract_modifiers(&node, content);
                                
                                variables.push(VariableInfo {
                                    name,
                                    type_,
                                    line,
                                    modifiers,
                                });
                            }
                        }
                    },
                    "import_declaration" => {
                        let path = node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                        let line = node.start_position().row;
                        
                        imports.push(ImportInfo {
                            path,
                            line,
                        });
                    },
                    _ => {}
                }
            },
            "python" => {
                match node_type {
                    "class_definition" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            classes.push(ClassInfo {
                                name,
                                kind: "class".to_string(),
                                start_line,
                                end_line,
                                modifiers,
                            });
                        }
                    },
                    "function_definition" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let parameters = extract_parameters(&node, content);
                            let return_type = extract_return_type(&node, content);
                            let modifiers = extract_modifiers(&node, content);
                            
                            functions.push(FunctionInfo {
                                name,
                                start_line,
                                end_line,
                                parameters,
                                return_type,
                                modifiers,
                            });
                        }
                    },
                    "assignment" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let line = node.start_position().row;
                            
                            let type_node = find_child_by_type(&node, "type_annotation");
                            let type_ = type_node.map(|n| n.utf8_text(content.as_bytes()).unwrap_or("").to_string());
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            variables.push(VariableInfo {
                                name,
                                type_,
                                line,
                                modifiers,
                            });
                        }
                    },
                    "import_statement" | "import_from_statement" => {
                        let path = node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                        let line = node.start_position().row;
                        
                        imports.push(ImportInfo {
                            path,
                            line,
                        });
                    },
                    _ => {}
                }
            },
            "javascript" | "typescript" => {
                match node_type {
                    "class_declaration" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            classes.push(ClassInfo {
                                name,
                                kind: "class".to_string(),
                                start_line,
                                end_line,
                                modifiers,
                            });
                        }
                    },
                    "function_declaration" | "method_definition" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let start_line = node.start_position().row;
                            let end_line = node.end_position().row;
                            
                            let parameters = extract_parameters(&node, content);
                            let return_type = extract_return_type(&node, content);
                            let modifiers = extract_modifiers(&node, content);
                            
                            functions.push(FunctionInfo {
                                name,
                                start_line,
                                end_line,
                                parameters,
                                return_type,
                                modifiers,
                            });
                        }
                    },
                    "variable_declaration" => {
                        if let Some(name_node) = find_child_by_type(&node, "identifier") {
                            let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                            
                            let line = node.start_position().row;
                            
                            let type_node = find_child_by_type(&node, "type_annotation");
                            let type_ = type_node.map(|n| n.utf8_text(content.as_bytes()).unwrap_or("").to_string());
                            
                            let modifiers = extract_modifiers(&node, content);
                            
                            variables.push(VariableInfo {
                                name,
                                type_,
                                line,
                                modifiers,
                            });
                        }
                    },
                    "import_statement" | "import_declaration" => {
                        let path = node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                        let line = node.start_position().row;
                        
                        imports.push(ImportInfo {
                            path,
                            line,
                        });
                    },
                    _ => {}
                }
            },
            _ => {}
        }
        
        // Go to first child
        if cursor.goto_first_child() {
            continue;
        }
        
        // No children, try to go to next sibling
        if cursor.goto_next_sibling() {
            continue;
        }
        
        // No siblings, go up and try to find a sibling
        loop {
            if !cursor.goto_parent() {
                return;
            }
            
            if cursor.goto_next_sibling() {
                break;
            }
        }
    }
}

// Find child node by type
#[cfg(feature = "tree-sitter-support")]
fn find_child_by_type<'a>(node: &'a Node, node_type: &str) -> Option<Node<'a>> {
    for i in 0..node.child_count() {
        if let Some(child) = node.child(i) {
            if child.kind() == node_type {
                return Some(child);
            }
            
            if let Some(found) = find_child_by_type(&child, node_type) {
                return Some(found);
            }
        }
    }
    
    None
}

// Extract parameters from a function node
#[cfg(feature = "tree-sitter-support")]
fn extract_parameters<'a>(node: &'a Node, content: &str) -> Vec<ParameterInfo> {
    let mut parameters = Vec::new();
    
    if let Some(params_node) = find_child_by_type(node, "parameters") {
        for i in 0..params_node.child_count() {
            if let Some(param_node) = params_node.child(i) {
                if param_node.kind() == "parameter" {
                    if let Some(name_node) = find_child_by_type(&param_node, "identifier") {
                        let name = name_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                        
                        let type_node = find_child_by_type(&param_node, "type_annotation");
                        let type_ = type_node.map(|n| n.utf8_text(content.as_bytes()).unwrap_or("").to_string());
                        
                        parameters.push(ParameterInfo {
                            name,
                            type_,
                        });
                    }
                }
            }
        }
    }
    
    parameters
}

// Extract return type from a function node
#[cfg(feature = "tree-sitter-support")]
fn extract_return_type<'a>(node: &'a Node, content: &str) -> Option<String> {
    if let Some(return_type_node) = find_child_by_type(node, "return_type") {
        return Some(return_type_node.utf8_text(content.as_bytes()).unwrap_or("").to_string());
    }
    
    None
}

// Extract modifiers from a node
#[cfg(feature = "tree-sitter-support")]
fn extract_modifiers<'a>(node: &'a Node, content: &str) -> Vec<String> {
    let mut modifiers = Vec::new();
    
    if let Some(modifiers_node) = find_child_by_type(node, "modifiers") {
        for i in 0..modifiers_node.child_count() {
            if let Some(modifier_node) = modifiers_node.child(i) {
                let modifier = modifier_node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
                modifiers.push(modifier);
            }
        }
    }
    
    modifiers
}

// Parse code structure with regex
fn parse_structure_with_regex(content: &str, language_config: &LanguageConfig) -> CodeStructure {
    let mut classes = Vec::new();
    let mut functions = Vec::new();
    let mut variables = Vec::new();
    let mut imports = Vec::new();
    
    // Parse based on language
    match language_config.id.as_str() {
        "rust" => parse_rust_structure_regex(content, &mut classes, &mut functions, &mut variables, &mut imports),
        "kotlin" => parse_kotlin_structure_regex(content, &mut classes, &mut functions, &mut variables, &mut imports),
        "java" => parse_java_structure_regex(content, &mut classes, &mut functions, &mut variables, &mut imports),
        "python" => parse_python_structure_regex(content, &mut classes, &mut functions, &mut variables, &mut imports),
        "javascript" | "typescript" => parse_js_ts_structure_regex(content, &mut classes, &mut functions, &mut variables, &mut imports),
        _ => {}
    }
    
    CodeStructure {
        classes,
        functions,
        variables,
        imports,
    }
}

// Parse Rust structure with regex
fn parse_rust_structure_regex(
    content: &str,
    classes: &mut Vec<ClassInfo>,
    functions: &mut Vec<FunctionInfo>,
    variables: &mut Vec<VariableInfo>,
    imports: &mut Vec<ImportInfo>
) {
    // Parse structs
    let struct_regex = Regex::new(r"(?m)^(?:pub\s+)?struct\s+(\w+)").unwrap();
    for captures in struct_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "struct".to_string(),
            start_line: line,
            end_line: line + 5, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse enums
    let enum_regex = Regex::new(r"(?m)^(?:pub\s+)?enum\s+(\w+)").unwrap();
    for captures in enum_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "enum".to_string(),
            start_line: line,
            end_line: line + 5, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse traits
    let trait_regex = Regex::new(r"(?m)^(?:pub\s+)?trait\s+(\w+)").unwrap();
    for captures in trait_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "trait".to_string(),
            start_line: line,
            end_line: line + 5, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse functions
    let function_regex = Regex::new(r"(?m)^(?:pub\s+)?fn\s+(\w+)").unwrap();
    for captures in function_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        functions.push(FunctionInfo {
            name,
            start_line: line,
            end_line: line + 5, // Approximate
            parameters: Vec::new(),
            return_type: None,
            modifiers: Vec::new(),
        });
    }
    
    // Parse variables
    let variable_regex = Regex::new(r"(?m)^(?:let|const|static)\s+(?:mut\s+)?(\w+)").unwrap();
    for captures in variable_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        variables.push(VariableInfo {
            name,
            type_: None,
            line,
            modifiers: Vec::new(),
        });
    }
    
    // Parse imports
    let import_regex = Regex::new(r"(?m)^use\s+([^;]+);").unwrap();
    for captures in import_regex.captures_iter(content) {
        let path = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        imports.push(ImportInfo {
            path,
            line,
        });
    }
}

// Parse Kotlin structure with regex
fn parse_kotlin_structure_regex(
    content: &str,
    classes: &mut Vec<ClassInfo>,
    functions: &mut Vec<FunctionInfo>,
    variables: &mut Vec<VariableInfo>,
    imports: &mut Vec<ImportInfo>
) {
    // Parse classes
    let class_regex = Regex::new(r"(?m)^(?:(?:public|private|protected|internal)\s+)?(?:abstract\s+)?class\s+(\w+)").unwrap();
    for captures in class_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "class".to_string(),
            start_line: line,
            end_line: line + 10, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse interfaces
    let interface_regex = Regex::new(r"(?m)^(?:(?:public|private|protected|internal)\s+)?interface\s+(\w+)").unwrap();
    for captures in interface_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "interface".to_string(),
            start_line: line,
            end_line: line + 5, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse functions
    let function_regex = Regex::new(r"(?m)^(?:(?:public|private|protected|internal)\s+)?(?:fun\s+)(\w+)").unwrap();
    for captures in function_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        functions.push(FunctionInfo {
            name,
            start_line: line,
            end_line: line + 5, // Approximate
            parameters: Vec::new(),
            return_type: None,
            modifiers: Vec::new(),
        });
    }
    
    // Parse variables
    let variable_regex = Regex::new(r"(?m)^(?:(?:public|private|protected|internal)\s+)?(?:val|var)\s+(\w+)").unwrap();
    for captures in variable_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        variables.push(VariableInfo {
            name,
            type_: None,
            line,
            modifiers: Vec::new(),
        });
    }
    
    // Parse imports
    let import_regex = Regex::new(r"(?m)^import\s+([^;]+)").unwrap();
    for captures in import_regex.captures_iter(content) {
        let path = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        imports.push(ImportInfo {
            path,
            line,
        });
    }
}

// Parse Java structure with regex
fn parse_java_structure_regex(
    content: &str,
    classes: &mut Vec<ClassInfo>,
    functions: &mut Vec<FunctionInfo>,
    variables: &mut Vec<VariableInfo>,
    imports: &mut Vec<ImportInfo>
) {
    // Parse classes
    let class_regex = Regex::new(r"(?m)^(?:(?:public|private|protected)\s+)?(?:abstract\s+)?class\s+(\w+)").unwrap();
    for captures in class_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "class".to_string(),
            start_line: line,
            end_line: line + 10, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse interfaces
    let interface_regex = Regex::new(r"(?m)^(?:(?:public|private|protected)\s+)?interface\s+(\w+)").unwrap();
    for captures in interface_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "interface".to_string(),
            start_line: line,
            end_line: line + 5, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse methods
    let method_regex = Regex::new(r"(?m)^(?:(?:public|private|protected)\s+)?(?:static\s+)?(?:final\s+)?(?:[\w<>[\],\s]+)\s+(\w+)\s*\(").unwrap();
    for captures in method_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        functions.push(FunctionInfo {
            name,
            start_line: line,
            end_line: line + 5, // Approximate
            parameters: Vec::new(),
            return_type: None,
            modifiers: Vec::new(),
        });
    }
    
    // Parse fields
    let field_regex = Regex::new(r"(?m)^(?:(?:public|private|protected)\s+)?(?:static\s+)?(?:final\s+)?(?:[\w<>[\],\s]+)\s+(\w+)\s*=").unwrap();
    for captures in field_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        variables.push(VariableInfo {
            name,
            type_: None,
            line,
            modifiers: Vec::new(),
        });
    }
    
    // Parse imports
    let import_regex = Regex::new(r"(?m)^import\s+([^;]+);").unwrap();
    for captures in import_regex.captures_iter(content) {
        let path = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        imports.push(ImportInfo {
            path,
            line,
        });
    }
}

// Parse Python structure with regex
fn parse_python_structure_regex(
    content: &str,
    classes: &mut Vec<ClassInfo>,
    functions: &mut Vec<FunctionInfo>,
    variables: &mut Vec<VariableInfo>,
    imports: &mut Vec<ImportInfo>
) {
    // Parse classes
    let class_regex = Regex::new(r"(?m)^class\s+(\w+)").unwrap();
    for captures in class_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "class".to_string(),
            start_line: line,
            end_line: line + 10, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse functions
    let function_regex = Regex::new(r"(?m)^def\s+(\w+)").unwrap();
    for captures in function_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        functions.push(FunctionInfo {
            name,
            start_line: line,
            end_line: line + 5, // Approximate
            parameters: Vec::new(),
            return_type: None,
            modifiers: Vec::new(),
        });
    }
    
    // Parse variables
    let variable_regex = Regex::new(r"(?m)^(\w+)\s*=").unwrap();
    for captures in variable_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        variables.push(VariableInfo {
            name,
            type_: None,
            line,
            modifiers: Vec::new(),
        });
    }
    
    // Parse imports
    let import_regex = Regex::new(r"(?m)^(?:import|from)\s+([^\n]+)").unwrap();
    for captures in import_regex.captures_iter(content) {
        let path = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        imports.push(ImportInfo {
            path,
            line,
        });
    }
}

// Parse JavaScript/TypeScript structure with regex
fn parse_js_ts_structure_regex(
    content: &str,
    classes: &mut Vec<ClassInfo>,
    functions: &mut Vec<FunctionInfo>,
    variables: &mut Vec<VariableInfo>,
    imports: &mut Vec<ImportInfo>
) {
    // Parse classes
    let class_regex = Regex::new(r"(?m)^(?:export\s+)?class\s+(\w+)").unwrap();
    for captures in class_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        classes.push(ClassInfo {
            name,
            kind: "class".to_string(),
            start_line: line,
            end_line: line + 10, // Approximate
            modifiers: Vec::new(),
        });
    }
    
    // Parse functions
    let function_regex = Regex::new(r"(?m)^(?:export\s+)?function\s+(\w+)").unwrap();
    for captures in function_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        functions.push(FunctionInfo {
            name,
            start_line: line,
            end_line: line + 5, // Approximate
            parameters: Vec::new(),
            return_type: None,
            modifiers: Vec::new(),
        });
    }
    
    // Parse variables
    let variable_regex = Regex::new(r"(?m)^(?:export\s+)?(?:const|let|var)\s+(\w+)").unwrap();
    for captures in variable_regex.captures_iter(content) {
        let name = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        variables.push(VariableInfo {
            name,
            type_: None,
            line,
            modifiers: Vec::new(),
        });
    }
    
    // Parse imports
    let import_regex = Regex::new(r"(?m)^import\s+([^\n]+)").unwrap();
    for captures in import_regex.captures_iter(content) {
        let path = captures.get(1).unwrap().as_str().to_string();
        let line = get_line_number(content, captures.get(0).unwrap().start());
        
        imports.push(ImportInfo {
            path,
            line,
        });
    }
}

// Get line number from position
fn get_line_number(content: &str, position: usize) -> usize {
    content[..position].chars().filter(|&c| c == '\n').count()
}

// Find references
pub fn find_references(content: &str, position: usize, language_id: &str) -> Vec<Reference> {
    let state = EDITOR_STATE.lock().unwrap();
    
    if !state.initialized {
        return Vec::new();
    }
    
    let language_config = match state.languages.get(language_id) {
        Some(config) => config,
        None => return Vec::new(),
    };
    
    #[cfg(feature = "tree-sitter-support")]
    {
        if let Some(parser) = state.parsers.get(language_id) {
            return find_references_with_tree_sitter(content, position, parser, language_config);
        }
    }
    
    // Fallback to regex-based reference finding
    find_references_with_regex(content, position, language_config)
}

// Find references with tree-sitter
#[cfg(feature = "tree-sitter-support")]
fn find_references_with_tree_sitter(content: &str, position: usize, parser: &Parser, language_config: &LanguageConfig) -> Vec<Reference> {
    let mut references = Vec::new();
    
    // Parse the code
    let tree = match parser.parse(content, None) {
        Some(tree) => tree,
        None => return Vec::new(),
    };
    
    // Find the node at the cursor position
    let node = find_node_at_position(&tree.root_node(), position);
    
    // Get the node text
    let node_text = node.utf8_text(content.as_bytes()).unwrap_or("").to_string();
    
    // Find all occurrences of the node text
    let mut cursor = tree.walk();
    
    // Traverse the tree to find references
    find_references_in_tree(&mut cursor, content, &node_text, &mut references);
    
    references
}

// Find references in tree-sitter tree
#[cfg(feature = "tree-sitter-support")]
fn find_references_in_tree(cursor: &mut TreeCursor, content: &str, target_text: &str, references: &mut Vec<Reference>) {
    loop {
        let node = cursor.node();
        
        // Check if this node is a reference to the target
        if node.kind() == "identifier" {
            let node_text = node.utf8_text(content.as_bytes()).unwrap_or("");
            
            if node_text == target_text {
                let start = node.start_byte();
                let end = node.end_byte();
                let line = node.start_position().row;
                let column = node.start_position().column;
                
                references.push(Reference {
                    start,
                    end,
                    line,
                    column,
                });
            }
        }
        
        // Go to first child
        if cursor.goto_first_child() {
            continue;
        }
        
        // No children, try to go to next sibling
        if cursor.goto_next_sibling() {
            continue;
        }
        
        // No siblings, go up and try to find a sibling
        loop {
            if !cursor.goto_parent() {
                return;
            }
            
            if cursor.goto_next_sibling() {
                break;
            }
        }
    }
}

// Find references with regex
fn find_references_with_regex(content: &str, position: usize, language_config: &LanguageConfig) -> Vec<Reference> {
    let mut references = Vec::new();
    
    // Get the word at the cursor position
    let word = get_word_at_position(content, position);
    
    if word.is_empty() {
        return references;
    }
    
    // Find all occurrences of the word
    let pattern = format!(r"\b{}\b", regex::escape(&word));
    let regex = Regex::new(&pattern).unwrap();
    
    for mat in regex.find_iter(content) {
        let start = mat.start();
        let end = mat.end();
        let line = get_line_number(content, start);
        let column = get_column_number(content, start);
        
        references.push(Reference {
            start,
            end,
            line,
            column,
        });
    }
    
    references
}

// Get word at position
fn get_word_at_position(content: &str, position: usize) -> String {
    if position >= content.len() {
        return String::new();
    }
    
    let mut start = position;
    let mut end = position;
    
    // Find start of word
    while start > 0 && is_word_char(content.chars().nth(start - 1).unwrap()) {
        start -= 1;
    }
    
    // Find end of word
    while end < content.len() && is_word_char(content.chars().nth(end).unwrap()) {
        end += 1;
    }
    
    content[start..end].to_string()
}

// Check if character is part of a word
fn is_word_char(c: char) -> bool {
    c.is_alphanumeric() || c == '_'
}

// Get column number from position
fn get_column_number(content: &str, position: usize) -> usize {
    let line_start = content[..position].rfind('\n').map_or(0, |i| i + 1);
    position - line_start
}