package com.anyoneide.app.core

import com.anyoneide.app.core.templates.android.*
import com.anyoneide.app.core.templates.kotlin.*
import com.anyoneide.app.core.templates.rust.*
import java.io.File

/**
 * Class for creating project templates
 */
class ProjectTemplates(private val context: android.content.Context) {
    
    /**
     * Create a basic Android app template
     */
    fun createAndroidAppTemplate(projectDir: File, packageName: String) {
        val template = AndroidAppTemplate()
        template.create(projectDir, projectDir.name, packageName)
    }
    
    /**
     * Create a Rust Android library template
     */
    fun createRustAndroidLibraryTemplate(projectDir: File, packageName: String) {
        val template = RustAndroidLibraryTemplate()
        template.create(projectDir, projectDir.name, packageName)
    }
    
    /**
     * Create an Android library template
     */
    fun createAndroidLibraryProject(projectDir: File, projectName: String) {
        val template = AndroidLibraryTemplate()
        template.create(projectDir, projectName, "com.example.${projectName.lowercase()}")
    }
    
    /**
     * Create a Kotlin library template
     */
    fun createKotlinLibraryProject(projectDir: File, projectName: String) {
        val template = KotlinLibraryTemplate()
        template.create(projectDir, projectName, "com.example.${projectName.lowercase()}")
    }
    
    /**
     * Create a Kotlin Multiplatform template
     */
    fun createKotlinMultiplatformProject(projectDir: File, projectName: String) {
        val template = KotlinMultiplatformTemplate()
        template.create(projectDir, projectName, "com.example.${projectName.lowercase()}")
    }
    
    /**
     * Create a Compose app template
     */
    fun createComposeAppTemplate(projectDir: File, packageName: String) {
        val template = ComposeAppTemplate()
        template.create(projectDir, projectDir.name, packageName)
    }
    
    /**
     * Create an MVVM app template
     */
    fun createMvvmAppTemplate(projectDir: File, packageName: String) {
        val template = MvvmAppTemplate()
        template.create(projectDir, projectDir.name, packageName)
    }
    
    /**
     * Create a REST API client template
     */
    fun createRestApiClientTemplate(projectDir: File, packageName: String) {
        val template = RestApiClientTemplate()
        template.create(projectDir, projectDir.name, packageName)
    }
    
    /**
     * Create a 2D game template
     */
    fun create2DGameTemplate(projectDir: File, packageName: String) {
        val template = Game2DTemplate()
        template.create(projectDir, projectDir.name, packageName)
    }
}