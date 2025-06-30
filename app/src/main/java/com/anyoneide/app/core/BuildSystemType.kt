package com.anyoneide.app.core

/**
 * Enum representing the available build systems in the IDE
 */
enum class BuildSystemType {
    GRADLE,
    RUST,
    HYBRID, // For projects that use both Gradle and Rust
    RUST_NATIVE_TEST // Experimental native Rust build system
}