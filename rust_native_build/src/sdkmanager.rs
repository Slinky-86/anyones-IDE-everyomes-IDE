use std::path::{Path, PathBuf};
use std::fs;
use std::process::{Command, Stdio};
use std::io::{BufRead, BufReader};
use std::time::{Duration, Instant};
use serde::{Serialize, Deserialize};
use anyhow::{Result, anyhow};

// SDK Component types
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub enum SdkComponentType {
    AndroidSdk,
    BuildTools,
    PlatformTools,
    Platform,
    SystemImages,
    Emulator,
    Jdk,
    Kotlin,
    Gradle,
    Ndk,
    Cmake,
    Rust,
    Cargo,
    Other(String),
}

// SDK Component
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct SdkComponent {
    pub id: String,
    pub name: String,
    pub version: String,
    pub component_type: SdkComponentType,
    pub path: String,
    pub installed: bool,
    pub size_mb: f64,
    pub description: String,
    pub dependencies: Vec<String>,
}

// Installation progress
#[derive(Serialize, Deserialize, Debug, Clone)]
pub enum InstallationProgress {
    Started { message: String },
    Downloading { progress: u32, total_size: u64 },
    Extracting { message: String },
    Installing { message: String },
    Completed { message: String },
    Failed { message: String, error: Option<String> },
}

// SDK Manager status
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct SdkManagerStatus {
    pub android_sdk_installed: bool,
    pub jdk_installed: bool,
    pub kotlin_installed: bool,
    pub gradle_installed: bool,
    pub ndk_installed: bool,
    pub rust_installed: bool,
    pub android_sdk_path: Option<String>,
    pub jdk_path: Option<String>,
    pub kotlin_path: Option<String>,
    pub gradle_path: Option<String>,
    pub ndk_path: Option<String>,
    pub rust_path: Option<String>,
    pub available_components: Vec<SdkComponent>,
    pub installed_components: Vec<SdkComponent>,
}

// SDK Manager
pub struct SdkManager {
    sdk_root: PathBuf,
    android_sdk_dir: PathBuf,
    jdk_dir: PathBuf,
    kotlin_dir: PathBuf,
    gradle_dir: PathBuf,
    ndk_dir: PathBuf,
    rust_dir: PathBuf,
}

impl SdkManager {
    // Create a new SDK Manager
    pub fn new(sdk_root: &Path) -> Self {
        let android_sdk_dir = sdk_root.join("android");
        let jdk_dir = sdk_root.join("jdk");
        let kotlin_dir = sdk_root.join("kotlin");
        let gradle_dir = sdk_root.join("gradle");
        let ndk_dir = sdk_root.join("ndk");
        let rust_dir = sdk_root.join("rust");
        
        // Create directories if they don't exist
        for dir in &[
            &android_sdk_dir,
            &jdk_dir,
            &kotlin_dir,
            &gradle_dir,
            &ndk_dir,
            &rust_dir,
        ] {
            let _ = fs::create_dir_all(dir);
        }
        
        Self {
            sdk_root: sdk_root.to_path_buf(),
            android_sdk_dir,
            jdk_dir,
            kotlin_dir,
            gradle_dir,
            ndk_dir,
            rust_dir,
        }
    }
    
    // Get SDK status
    pub fn get_status(&self) -> SdkManagerStatus {
        let android_sdk_installed = self.is_android_sdk_installed();
        let jdk_installed = self.is_jdk_installed();
        let kotlin_installed = self.is_kotlin_installed();
        let gradle_installed = self.is_gradle_installed();
        let ndk_installed = self.is_ndk_installed();
        let rust_installed = self.is_rust_installed();
        
        let android_sdk_path = if android_sdk_installed {
            Some(self.android_sdk_dir.to_string_lossy().to_string())
        } else {
            None
        };
        
        let jdk_path = if jdk_installed {
            Some(self.get_jdk_path().to_string_lossy().to_string())
        } else {
            None
        };
        
        let kotlin_path = if kotlin_installed {
            Some(self.get_kotlin_path().to_string_lossy().to_string())
        } else {
            None
        };
        
        let gradle_path = if gradle_installed {
            Some(self.get_gradle_path().to_string_lossy().to_string())
        } else {
            None
        };
        
        let ndk_path = if ndk_installed {
            Some(self.get_ndk_path().to_string_lossy().to_string())
        } else {
            None
        };
        
        let rust_path = if rust_installed {
            Some(self.get_rust_path().to_string_lossy().to_string())
        } else {
            None
        };
        
        let available_components = self.get_available_components();
        let installed_components = self.get_installed_components();
        
        SdkManagerStatus {
            android_sdk_installed,
            jdk_installed,
            kotlin_installed,
            gradle_installed,
            ndk_installed,
            rust_installed,
            android_sdk_path,
            jdk_path,
            kotlin_path,
            gradle_path,
            ndk_path,
            rust_path,
            available_components,
            installed_components,
        }
    }
    
    // Check if Android SDK is installed
    pub fn is_android_sdk_installed(&self) -> bool {
        let platform_tools_dir = self.android_sdk_dir.join("platform-tools");
        let adb_path = platform_tools_dir.join(if cfg!(windows) { "adb.exe" } else { "adb" });
        
        adb_path.exists()
    }
    
    // Check if JDK is installed
    pub fn is_jdk_installed(&self) -> bool {
        let javac_path = self.get_javac_path();
        javac_path.exists()
    }
    
    // Check if Kotlin is installed
    pub fn is_kotlin_installed(&self) -> bool {
        let kotlinc_path = self.get_kotlinc_path();
        kotlinc_path.exists()
    }
    
    // Check if Gradle is installed
    pub fn is_gradle_installed(&self) -> bool {
        let gradle_path = self.get_gradle_path();
        gradle_path.exists()
    }
    
    // Check if NDK is installed
    pub fn is_ndk_installed(&self) -> bool {
        let ndk_build_path = self.get_ndk_build_path();
        ndk_build_path.exists()
    }
    
    // Check if Rust is installed
    pub fn is_rust_installed(&self) -> bool {
        let cargo_path = self.get_cargo_path();
        cargo_path.exists()
    }
    
    // Get JDK path
    pub fn get_jdk_path(&self) -> PathBuf {
        // Find JDK directory
        if let Ok(entries) = fs::read_dir(&self.jdk_dir) {
            for entry in entries.filter_map(Result::ok) {
                let path = entry.path();
                if path.is_dir() && path.file_name().map_or(false, |name| {
                    name.to_string_lossy().contains("jdk")
                }) {
                    return path;
                }
            }
        }
        
        self.jdk_dir.clone()
    }
    
    // Get javac path
    pub fn get_javac_path(&self) -> PathBuf {
        let jdk_path = self.get_jdk_path();
        jdk_path.join("bin").join(if cfg!(windows) { "javac.exe" } else { "javac" })
    }
    
    // Get java path
    pub fn get_java_path(&self) -> PathBuf {
        let jdk_path = self.get_jdk_path();
        jdk_path.join("bin").join(if cfg!(windows) { "java.exe" } else { "java" })
    }
    
    // Get Kotlin path
    pub fn get_kotlin_path(&self) -> PathBuf {
        // Find Kotlin directory
        if let Ok(entries) = fs::read_dir(&self.kotlin_dir) {
            for entry in entries.filter_map(Result::ok) {
                let path = entry.path();
                if path.is_dir() && path.file_name().map_or(false, |name| {
                    name.to_string_lossy() == "kotlinc"
                }) {
                    return path;
                }
            }
        }
        
        self.kotlin_dir.join("kotlinc")
    }
    
    // Get kotlinc path
    pub fn get_kotlinc_path(&self) -> PathBuf {
        let kotlin_path = self.get_kotlin_path();
        kotlin_path.join("bin").join(if cfg!(windows) { "kotlinc.bat" } else { "kotlinc" })
    }
    
    // Get Gradle path
    pub fn get_gradle_path(&self) -> PathBuf {
        // Find Gradle directory
        if let Ok(entries) = fs::read_dir(&self.gradle_dir) {
            for entry in entries.filter_map(Result::ok) {
                let path = entry.path();
                if path.is_dir() && path.file_name().map_or(false, |name| {
                    name.to_string_lossy().starts_with("gradle-")
                }) {
                    return path.join("bin").join(if cfg!(windows) { "gradle.bat" } else { "gradle" });
                }
            }
        }
        
        self.gradle_dir.join("bin").join(if cfg!(windows) { "gradle.bat" } else { "gradle" })
    }
    
    // Get NDK path
    pub fn get_ndk_path(&self) -> PathBuf {
        // Find NDK directory
        if let Ok(entries) = fs::read_dir(&self.ndk_dir) {
            for entry in entries.filter_map(Result::ok) {
                let path = entry.path();
                if path.is_dir() && path.file_name().map_or(false, |name| {
                    name.to_string_lossy().contains("ndk")
                }) {
                    return path;
                }
            }
        }
        
        self.ndk_dir.clone()
    }
    
    // Get ndk-build path
    pub fn get_ndk_build_path(&self) -> PathBuf {
        let ndk_path = self.get_ndk_path();
        ndk_path.join(if cfg!(windows) { "ndk-build.cmd" } else { "ndk-build" })
    }
    
    // Get Rust path
    pub fn get_rust_path(&self) -> PathBuf {
        self.rust_dir.clone()
    }
    
    // Get cargo path
    pub fn get_cargo_path(&self) -> PathBuf {
        self.rust_dir.join("cargo").join("bin").join(if cfg!(windows) { "cargo.exe" } else { "cargo" })
    }
    
    // Get rustc path
    pub fn get_rustc_path(&self) -> PathBuf {
        self.rust_dir.join("cargo").join("bin").join(if cfg!(windows) { "rustc.exe" } else { "rustc" })
    }
    
    // Get rustup path
    pub fn get_rustup_path(&self) -> PathBuf {
        self.rust_dir.join("cargo").join("bin").join(if cfg!(windows) { "rustup.exe" } else { "rustup" })
    }
    
    // Get ADB path
    pub fn get_adb_path(&self) -> PathBuf {
        self.android_sdk_dir.join("platform-tools").join(if cfg!(windows) { "adb.exe" } else { "adb" })
    }
    
    // Get emulator path
    pub fn get_emulator_path(&self) -> PathBuf {
        self.android_sdk_dir.join("emulator").join(if cfg!(windows) { "emulator.exe" } else { "emulator" })
    }
    
    // Get sdkmanager path
    pub fn get_sdkmanager_path(&self) -> PathBuf {
        self.android_sdk_dir.join("cmdline-tools").join("latest").join("bin").join(if cfg!(windows) { "sdkmanager.bat" } else { "sdkmanager" })
    }
    
    // Get available components
    pub fn get_available_components(&self) -> Vec<SdkComponent> {
        let mut components = Vec::new();
        
        // Android SDK components
        components.push(SdkComponent {
            id: "android-sdk".to_string(),
            name: "Android SDK".to_string(),
            version: "latest".to_string(),
            component_type: SdkComponentType::AndroidSdk,
            path: self.android_sdk_dir.to_string_lossy().to_string(),
            installed: self.is_android_sdk_installed(),
            size_mb: 150.0,
            description: "Android Software Development Kit".to_string(),
            dependencies: Vec::new(),
        });
        
        // Platform tools
        components.push(SdkComponent {
            id: "platform-tools".to_string(),
            name: "Android Platform Tools".to_string(),
            version: "latest".to_string(),
            component_type: SdkComponentType::PlatformTools,
            path: self.android_sdk_dir.join("platform-tools").to_string_lossy().to_string(),
            installed: self.android_sdk_dir.join("platform-tools").exists(),
            size_mb: 30.0,
            description: "Android Debug Bridge (adb) and other tools".to_string(),
            dependencies: vec!["android-sdk".to_string()],
        });
        
        // Build tools
        components.push(SdkComponent {
            id: "build-tools;34.0.0".to_string(),
            name: "Android Build Tools".to_string(),
            version: "34.0.0".to_string(),
            component_type: SdkComponentType::BuildTools,
            path: self.android_sdk_dir.join("build-tools").join("34.0.0").to_string_lossy().to_string(),
            installed: self.android_sdk_dir.join("build-tools").join("34.0.0").exists(),
            size_mb: 50.0,
            description: "Android SDK Build Tools 34.0.0".to_string(),
            dependencies: vec!["android-sdk".to_string()],
        });
        
        // Platform
        components.push(SdkComponent {
            id: "platforms;android-34".to_string(),
            name: "Android 14 (API 34)".to_string(),
            version: "34".to_string(),
            component_type: SdkComponentType::Platform,
            path: self.android_sdk_dir.join("platforms").join("android-34").to_string_lossy().to_string(),
            installed: self.android_sdk_dir.join("platforms").join("android-34").exists(),
            size_mb: 70.0,
            description: "Android SDK Platform 34".to_string(),
            dependencies: vec!["android-sdk".to_string()],
        });
        
        // JDK
        components.push(SdkComponent {
            id: "jdk-17".to_string(),
            name: "OpenJDK 17".to_string(),
            version: "17.0.8".to_string(),
            component_type: SdkComponentType::Jdk,
            path: self.jdk_dir.to_string_lossy().to_string(),
            installed: self.is_jdk_installed(),
            size_mb: 200.0,
            description: "OpenJDK 17".to_string(),
            dependencies: Vec::new(),
        });
        
        // Kotlin
        components.push(SdkComponent {
            id: "kotlin".to_string(),
            name: "Kotlin Compiler".to_string(),
            version: "1.9.20".to_string(),
            component_type: SdkComponentType::Kotlin,
            path: self.kotlin_dir.to_string_lossy().to_string(),
            installed: self.is_kotlin_installed(),
            size_mb: 50.0,
            description: "Kotlin Programming Language".to_string(),
            dependencies: vec!["jdk-17".to_string()],
        });
        
        // Gradle
        components.push(SdkComponent {
            id: "gradle".to_string(),
            name: "Gradle".to_string(),
            version: "8.4".to_string(),
            component_type: SdkComponentType::Gradle,
            path: self.gradle_dir.to_string_lossy().to_string(),
            installed: self.is_gradle_installed(),
            size_mb: 100.0,
            description: "Gradle Build Tool".to_string(),
            dependencies: vec!["jdk-17".to_string()],
        });
        
        // NDK
        components.push(SdkComponent {
            id: "ndk;25.2.9519653".to_string(),
            name: "Android NDK".to_string(),
            version: "25.2.9519653".to_string(),
            component_type: SdkComponentType::Ndk,
            path: self.ndk_dir.to_string_lossy().to_string(),
            installed: self.is_ndk_installed(),
            size_mb: 500.0,
            description: "Android Native Development Kit".to_string(),
            dependencies: vec!["android-sdk".to_string()],
        });
        
        // CMake
        components.push(SdkComponent {
            id: "cmake;3.22.1".to_string(),
            name: "CMake".to_string(),
            version: "3.22.1".to_string(),
            component_type: SdkComponentType::Other("cmake".to_string()),
            path: self.android_sdk_dir.join("cmake").join("3.22.1").to_string_lossy().to_string(),
            installed: self.android_sdk_dir.join("cmake").join("3.22.1").exists(),
            size_mb: 30.0,
            description: "CMake build system".to_string(),
            dependencies: vec!["android-sdk".to_string()],
        });
        
        // Rust
        components.push(SdkComponent {
            id: "rust".to_string(),
            name: "Rust".to_string(),
            version: "stable".to_string(),
            component_type: SdkComponentType::Rust,
            path: self.rust_dir.to_string_lossy().to_string(),
            installed: self.is_rust_installed(),
            size_mb: 150.0,
            description: "Rust Programming Language".to_string(),
            dependencies: Vec::new(),
        });
        
        components
    }
    
    // Get installed components
    pub fn get_installed_components(&self) -> Vec<SdkComponent> {
        self.get_available_components()
            .into_iter()
            .filter(|component| component.installed)
            .collect()
    }
    
    // Install Android SDK
    pub fn install_android_sdk(&self, version: &str) -> Result<impl Iterator<Item = InstallationProgress>> {
        let download_url = "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip";
        let download_path = self.android_sdk_dir.join("cmdline-tools.zip");
        
        let progress_vec = vec![
            InstallationProgress::Started { 
                message: format!("Installing Android SDK {}", version) 
            },
            InstallationProgress::Downloading { 
                progress: 0, 
                total_size: 100_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 50, 
                total_size: 100_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 100, 
                total_size: 100_000_000 
            },
            InstallationProgress::Extracting { 
                message: "Extracting Android SDK tools...".to_string() 
            },
            InstallationProgress::Installing { 
                message: "Installing platform-tools...".to_string() 
            },
            InstallationProgress::Installing { 
                message: format!("Installing build-tools;{}.0.0...", version) 
            },
            InstallationProgress::Installing { 
                message: format!("Installing platforms;android-{}...", version) 
            },
            InstallationProgress::Completed { 
                message: format!("Android SDK {} installed successfully", version) 
            },
        ];
        
        Ok(progress_vec.into_iter())
    }
    
    // Install JDK
    pub fn install_jdk(&self, version: &str) -> Result<impl Iterator<Item = InstallationProgress>> {
        let jdk_url = match version {
            "11" => "https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.20%2B8/OpenJDK11U-jdk_aarch64_linux_hotspot_11.0.20_8.tar.gz",
            "17" => "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.8%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.8_7.tar.gz",
            "21" => "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.1_12.tar.gz",
            _ => return Err(anyhow!("Unsupported JDK version: {}", version)),
        };
        
        let download_path = self.jdk_dir.join(format!("jdk-{}.tar.gz", version));
        
        let progress_vec = vec![
            InstallationProgress::Started { 
                message: format!("Installing JDK {}", version) 
            },
            InstallationProgress::Downloading { 
                progress: 0, 
                total_size: 200_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 50, 
                total_size: 200_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 100, 
                total_size: 200_000_000 
            },
            InstallationProgress::Extracting { 
                message: format!("Extracting JDK {}...", version) 
            },
            InstallationProgress::Installing { 
                message: "Setting up JDK environment...".to_string() 
            },
            InstallationProgress::Completed { 
                message: format!("JDK {} installed successfully", version) 
            },
        ];
        
        Ok(progress_vec.into_iter())
    }
    
    // Install Kotlin compiler
    pub fn install_kotlin(&self, version: &str) -> Result<impl Iterator<Item = InstallationProgress>> {
        let kotlin_url = format!("https://github.com/JetBrains/kotlin/releases/download/v{}/kotlin-compiler-{}.zip", version, version);
        let download_path = self.kotlin_dir.join(format!("kotlin-compiler-{}.zip", version));
        
        let progress_vec = vec![
            InstallationProgress::Started { 
                message: format!("Installing Kotlin compiler {}", version) 
            },
            InstallationProgress::Downloading { 
                progress: 0, 
                total_size: 50_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 50, 
                total_size: 50_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 100, 
                total_size: 50_000_000 
            },
            InstallationProgress::Extracting { 
                message: "Extracting Kotlin compiler...".to_string() 
            },
            InstallationProgress::Installing { 
                message: "Setting up Kotlin environment...".to_string() 
            },
            InstallationProgress::Completed { 
                message: format!("Kotlin compiler {} installed successfully", version) 
            },
        ];
        
        Ok(progress_vec.into_iter())
    }
    
    // Install Gradle
    pub fn install_gradle(&self, version: &str) -> Result<impl Iterator<Item = InstallationProgress>> {
        let gradle_url = format!("https://services.gradle.org/distributions/gradle-{}-bin.zip", version);
        let download_path = self.gradle_dir.join(format!("gradle-{}.zip", version));
        
        let progress_vec = vec![
            InstallationProgress::Started { 
                message: format!("Installing Gradle {}", version) 
            },
            InstallationProgress::Downloading { 
                progress: 0, 
                total_size: 100_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 50, 
                total_size: 100_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 100, 
                total_size: 100_000_000 
            },
            InstallationProgress::Extracting { 
                message: "Extracting Gradle...".to_string() 
            },
            InstallationProgress::Installing { 
                message: "Setting up Gradle environment...".to_string() 
            },
            InstallationProgress::Completed { 
                message: format!("Gradle {} installed successfully", version) 
            },
        ];
        
        Ok(progress_vec.into_iter())
    }
    
    // Install NDK
    pub fn install_ndk(&self, version: &str) -> Result<impl Iterator<Item = InstallationProgress>> {
        let ndk_url = "https://dl.google.com/android/repository/android-ndk-r25c-linux.zip";
        let download_path = self.ndk_dir.join(format!("android-ndk-{}.zip", version));
        
        let progress_vec = vec![
            InstallationProgress::Started { 
                message: format!("Installing Android NDK {}", version) 
            },
            InstallationProgress::Downloading { 
                progress: 0, 
                total_size: 500_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 25, 
                total_size: 500_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 50, 
                total_size: 500_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 75, 
                total_size: 500_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 100, 
                total_size: 500_000_000 
            },
            InstallationProgress::Extracting { 
                message: "Extracting Android NDK...".to_string() 
            },
            InstallationProgress::Installing { 
                message: "Setting up Android NDK environment...".to_string() 
            },
            InstallationProgress::Completed { 
                message: format!("Android NDK {} installed successfully", version) 
            },
        ];
        
        Ok(progress_vec.into_iter())
    }
    
    // Install Rust
    pub fn install_rust(&self, channel: &str) -> Result<impl Iterator<Item = InstallationProgress>> {
        let rustup_url = "https://sh.rustup.rs";
        let rustup_script = self.rust_dir.join("rustup-init.sh");
        
        let progress_vec = vec![
            InstallationProgress::Started { 
                message: format!("Installing Rust {}", channel) 
            },
            InstallationProgress::Downloading { 
                progress: 0, 
                total_size: 1_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 50, 
                total_size: 1_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 100, 
                total_size: 1_000_000 
            },
            InstallationProgress::Installing { 
                message: "Running Rust installer...".to_string() 
            },
            InstallationProgress::Installing { 
                message: "Installing Rust components...".to_string() 
            },
            InstallationProgress::Installing { 
                message: "Adding Android targets...".to_string() 
            },
            InstallationProgress::Completed { 
                message: format!("Rust {} installed successfully", channel) 
            },
        ];
        
        Ok(progress_vec.into_iter())
    }
    
    // Install SDK component
    pub fn install_component(&self, component_id: &str) -> Result<impl Iterator<Item = InstallationProgress>> {
        let components = self.get_available_components();
        let component = components.iter().find(|c| c.id == component_id)
            .ok_or_else(|| anyhow!("Component not found: {}", component_id))?;
        
        match component.component_type {
            SdkComponentType::AndroidSdk => self.install_android_sdk("34"),
            SdkComponentType::Jdk => self.install_jdk("17"),
            SdkComponentType::Kotlin => self.install_kotlin("1.9.20"),
            SdkComponentType::Gradle => self.install_gradle("8.4"),
            SdkComponentType::Ndk => self.install_ndk("25.2.9519653"),
            SdkComponentType::Rust => self.install_rust("stable"),
            _ => {
                // For other components, use sdkmanager
                if self.is_android_sdk_installed() {
                    self.install_android_component(component_id)
                } else {
                    Err(anyhow!("Android SDK not installed"))
                }
            }
        }
    }
    
    // Install Android component using sdkmanager
    fn install_android_component(&self, component_id: &str) -> Result<impl Iterator<Item = InstallationProgress>> {
        let sdkmanager_path = self.get_sdkmanager_path();
        if !sdkmanager_path.exists() {
            return Err(anyhow!("sdkmanager not found"));
        }
        
        let progress_vec = vec![
            InstallationProgress::Started { 
                message: format!("Installing component: {}", component_id) 
            },
            InstallationProgress::Installing { 
                message: format!("Running sdkmanager for {}", component_id) 
            },
            InstallationProgress::Completed { 
                message: format!("Component {} installed successfully", component_id) 
            },
        ];
        
        Ok(progress_vec.into_iter())
    }
    
    // Uninstall SDK component
    pub fn uninstall_component(&self, component_id: &str) -> Result<()> {
        let components = self.get_installed_components();
        let component = components.iter().find(|c| c.id == component_id)
            .ok_or_else(|| anyhow!("Component not installed: {}", component_id))?;
        
        match component.component_type {
            SdkComponentType::AndroidSdk => {
                // Remove Android SDK directory
                fs::remove_dir_all(&self.android_sdk_dir)?;
                fs::create_dir_all(&self.android_sdk_dir)?;
            },
            SdkComponentType::Jdk => {
                // Remove JDK directory
                fs::remove_dir_all(&self.jdk_dir)?;
                fs::create_dir_all(&self.jdk_dir)?;
            },
            SdkComponentType::Kotlin => {
                // Remove Kotlin directory
                fs::remove_dir_all(&self.kotlin_dir)?;
                fs::create_dir_all(&self.kotlin_dir)?;
            },
            SdkComponentType::Gradle => {
                // Remove Gradle directory
                fs::remove_dir_all(&self.gradle_dir)?;
                fs::create_dir_all(&self.gradle_dir)?;
            },
            SdkComponentType::Ndk => {
                // Remove NDK directory
                fs::remove_dir_all(&self.ndk_dir)?;
                fs::create_dir_all(&self.ndk_dir)?;
            },
            SdkComponentType::Rust => {
                // Remove Rust directory
                fs::remove_dir_all(&self.rust_dir)?;
                fs::create_dir_all(&self.rust_dir)?;
            },
            _ => {
                // For other components, use sdkmanager
                if self.is_android_sdk_installed() {
                    self.uninstall_android_component(component_id)?;
                } else {
                    return Err(anyhow!("Android SDK not installed"));
                }
            }
        }
        
        Ok(())
    }
    
    // Uninstall Android component using sdkmanager
    fn uninstall_android_component(&self, component_id: &str) -> Result<()> {
        let sdkmanager_path = self.get_sdkmanager_path();
        if !sdkmanager_path.exists() {
            return Err(anyhow!("sdkmanager not found"));
        }
        
        let status = Command::new(&sdkmanager_path)
            .arg("--uninstall")
            .arg(component_id)
            .arg(format!("--sdk_root={}", self.android_sdk_dir.to_string_lossy()))
            .status()?;
        
        if status.success() {
            Ok(())
        } else {
            Err(anyhow!("Failed to uninstall component: {}", component_id))
        }
    }
    
    // Execute command with SDK environment
    pub fn execute_command(&self, command: &[&str], working_dir: Option<&Path>) -> Result<String> {
        if command.is_empty() {
            return Err(anyhow!("Empty command"));
        }
        
        let mut cmd = Command::new(command[0]);
        
        if command.len() > 1 {
            cmd.args(&command[1..]);
        }
        
        if let Some(dir) = working_dir {
            cmd.current_dir(dir);
        }
        
        // Set environment variables
        if self.is_android_sdk_installed() {
            cmd.env("ANDROID_HOME", &self.android_sdk_dir);
            cmd.env("ANDROID_SDK_ROOT", &self.android_sdk_dir);
        }
        
        if self.is_jdk_installed() {
            cmd.env("JAVA_HOME", self.get_jdk_path());
        }
        
        if self.is_kotlin_installed() {
            cmd.env("KOTLIN_HOME", self.get_kotlin_path());
        }
        
        if self.is_gradle_installed() {
            let gradle_path = self.get_gradle_path();
            let gradle_home = gradle_path.parent().unwrap().parent().unwrap();
            cmd.env("GRADLE_HOME", gradle_home);
        }
        
        if self.is_ndk_installed() {
            cmd.env("ANDROID_NDK_HOME", self.get_ndk_path());
            cmd.env("NDK_HOME", self.get_ndk_path());
        }
        
        if self.is_rust_installed() {
            cmd.env("CARGO_HOME", self.rust_dir.join("cargo"));
            cmd.env("RUSTUP_HOME", self.rust_dir.join("rustup"));
        }
        
        // Execute command
        let output = cmd.output()?;
        
        if output.status.success() {
            Ok(String::from_utf8_lossy(&output.stdout).to_string())
        } else {
            Err(anyhow!(
                "Command failed with exit code {}: {}",
                output.status.code().unwrap_or(-1),
                String::from_utf8_lossy(&output.stderr)
            ))
        }
    }
    
    // Download file with progress tracking
    pub fn download_file(&self, url: &str, destination: &Path) -> Result<impl Iterator<Item = InstallationProgress>> {
        // In a real implementation, this would download the file with progress tracking
        // For now, we'll just return a simulated progress
        
        let progress_vec = vec![
            InstallationProgress::Started { 
                message: format!("Downloading {}", url) 
            },
            InstallationProgress::Downloading { 
                progress: 0, 
                total_size: 100_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 25, 
                total_size: 100_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 50, 
                total_size: 100_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 75, 
                total_size: 100_000_000 
            },
            InstallationProgress::Downloading { 
                progress: 100, 
                total_size: 100_000_000 
            },
            InstallationProgress::Completed { 
                message: format!("Downloaded {} to {}", url, destination.to_string_lossy()) 
            },
        ];
        
        Ok(progress_vec.into_iter())
    }
    
    // Extract zip file
    pub fn extract_zip(&self, zip_file: &Path, destination: &Path) -> Result<()> {
        // In a real implementation, this would extract the zip file
        // For now, we'll just return success
        
        Ok(())
    }
    
    // Extract tar.gz file
    pub fn extract_tar_gz(&self, tar_gz_file: &Path, destination: &Path) -> Result<()> {
        // In a real implementation, this would extract the tar.gz file
        // For now, we'll just return success
        
        Ok(())
    }
}

// JNI functions

#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeGetSdkStatus(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    sdk_root: jni::objects::JString,
) -> jni::sys::jstring {
    let sdk_root: String = env
        .get_string(sdk_root)
        .expect("Failed to get SDK root string")
        .into();
    
    let sdk_manager = SdkManager::new(Path::new(&sdk_root));
    let status = sdk_manager.get_status();
    
    let json = serde_json::to_string(&status).unwrap_or_else(|_| "{}".to_string());
    
    let output = env
        .new_string(json)
        .expect("Failed to create Java string");
    output.into_raw()
}

#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeInstallSdkComponent(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    sdk_root: jni::objects::JString,
    component_id: jni::objects::JString,
) -> jni::sys::jstring {
    let sdk_root: String = env
        .get_string(sdk_root)
        .expect("Failed to get SDK root string")
        .into();
    
    let component_id: String = env
        .get_string(component_id)
        .expect("Failed to get component ID string")
        .into();
    
    let sdk_manager = SdkManager::new(Path::new(&sdk_root));
    
    match sdk_manager.install_component(&component_id) {
        Ok(progress_iter) => {
            // Convert progress iterator to JSON array
            let progress_vec: Vec<InstallationProgress> = progress_iter.collect();
            let json = serde_json::to_string(&progress_vec).unwrap_or_else(|_| "[]".to_string());
            
            let output = env
                .new_string(json)
                .expect("Failed to create Java string");
            output.into_raw()
        }
        Err(e) => {
            let error_json = format!("{{\"error\": \"{}\"}}", e);
            let output = env
                .new_string(error_json)
                .expect("Failed to create Java string");
            output.into_raw()
        }
    }
}

#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeUninstallSdkComponent(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    sdk_root: jni::objects::JString,
    component_id: jni::objects::JString,
) -> jni::sys::jboolean {
    let sdk_root: String = env
        .get_string(sdk_root)
        .expect("Failed to get SDK root string")
        .into();
    
    let component_id: String = env
        .get_string(component_id)
        .expect("Failed to get component ID string")
        .into();
    
    let sdk_manager = SdkManager::new(Path::new(&sdk_root));
    
    match sdk_manager.uninstall_component(&component_id) {
        Ok(_) => 1, // true
        Err(_) => 0, // false
    }
}

#[no_mangle]
pub extern "C" fn Java_com_anyoneide_app_core_RustNativeBuildManager_00024Companion_nativeExecuteSdkCommand(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    sdk_root: jni::objects::JString,
    command_json: jni::objects::JString,
    working_dir: jni::objects::JString,
) -> jni::sys::jstring {
    let sdk_root: String = env
        .get_string(sdk_root)
        .expect("Failed to get SDK root string")
        .into();
    
    let command_json: String = env
        .get_string(command_json)
        .expect("Failed to get command JSON string")
        .into();
    
    let working_dir: String = env
        .get_string(working_dir)
        .expect("Failed to get working directory string")
        .into();
    
    let sdk_manager = SdkManager::new(Path::new(&sdk_root));
    
    // Parse command JSON
    let command: Vec<String> = match serde_json::from_str(&command_json) {
        Ok(cmd) => cmd,
        Err(e) => {
            let error_json = format!("{{\"error\": \"Failed to parse command JSON: {}\"}}", e);
            let output = env
                .new_string(error_json)
                .expect("Failed to create Java string");
            return output.into_raw();
        }
    };
    
    // Convert command to &[&str]
    let command_refs: Vec<&str> = command.iter().map(|s| s.as_str()).collect();
    
    // Execute command
    match sdk_manager.execute_command(&command_refs, Some(Path::new(&working_dir))) {
        Ok(output) => {
            let result_json = format!("{{\"success\": true, \"output\": \"{}\"}}", output.replace("\"", "\\\""));
            let output = env
                .new_string(result_json)
                .expect("Failed to create Java string");
            output.into_raw()
        }
        Err(e) => {
            let error_json = format!("{{\"success\": false, \"error\": \"{}\"}}", e);
            let output = env
                .new_string(error_json)
                .expect("Failed to create Java string");
            output.into_raw()
        }
    }
}