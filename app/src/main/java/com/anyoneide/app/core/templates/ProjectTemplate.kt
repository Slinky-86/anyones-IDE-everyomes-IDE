package com.anyoneide.app.core.templates

import java.io.File

/**
 * Interface for project templates
 * Each template implementation will provide its own logic for creating a project
 */
interface ProjectTemplate {
    /**
     * Create a project from this template
     * 
     * @param projectDir The directory where the project will be created
     * @param projectName The name of the project
     * @param packageName The package name for the project
     * @return Boolean indicating success or failure
     */
    fun create(projectDir: File, projectName: String, packageName: String): Boolean
    
    /**
     * Get the template ID
     */
    fun getId(): String
    
    /**
     * Get the template name
     */
    fun getName(): String
    
    /**
     * Get the template description
     */
    fun getDescription(): String
    
    /**
     * Get the template category
     */
    fun getCategory(): String
}