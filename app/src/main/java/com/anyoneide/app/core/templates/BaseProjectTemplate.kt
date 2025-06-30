package com.anyoneide.app.core.templates

import java.io.File

/**
 * Base class for project templates with common functionality
 */
abstract class BaseProjectTemplate : ProjectTemplate {
    
    /**
     * Create a basic project structure with common files
     */
    protected fun createBasicProjectStructure(projectDir: File, packageName: String) {
        // Create root build.gradle
        val rootBuildGradle = """
            // Top-level build file where you can add configuration options common to all sub-projects/modules.
            plugins {
                id 'com.android.application' version '8.1.4' apply false
                id 'com.android.library' version '8.1.4' apply false
                id 'org.jetbrains.kotlin.android' version '1.9.20' apply false
            }
            
            task clean(type: Delete) {
                delete rootProject.buildDir
            }
        """.trimIndent()
        
        File(projectDir, "build.gradle").writeText(rootBuildGradle)
        
        // Create settings.gradle
        val settingsGradle = """
            rootProject.name = "${projectDir.name}"
            include ':app'
        """.trimIndent()
        
        File(projectDir, "settings.gradle").writeText(settingsGradle)
        
        // Create gradle.properties
        val gradleProperties = """
            # Project-wide Gradle settings.
            org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
            android.useAndroidX=true
            kotlin.code.style=official
            android.nonTransitiveRClass=true
        """.trimIndent()
        
        File(projectDir, "gradle.properties").writeText(gradleProperties)
        
        // Create gradle wrapper directory
        val gradleWrapperDir = File(projectDir, "gradle/wrapper")
        gradleWrapperDir.mkdirs()
        
        // Create .gitignore
        val gitignore = """
            *.iml
            .gradle
            /local.properties
            /.idea
            .DS_Store
            /build
            /captures
            .externalNativeBuild
            .cxx
            local.properties
            
            # Rust
            /target
            Cargo.lock
        """.trimIndent()
        
        File(projectDir, ".gitignore").writeText(gitignore)
    }
}