package com.anyoneide.app.core.templates.kotlin

import com.anyoneide.app.core.templates.BaseProjectTemplate
import java.io.File

/**
 * Template for creating a pure Kotlin library project
 */
class KotlinLibraryTemplate : BaseProjectTemplate() {
    
    override fun getId(): String = "kotlin_library"
    
    override fun getName(): String = "Kotlin Library"
    
    override fun getDescription(): String = "Pure Kotlin library project with unit tests and documentation."
    
    override fun getCategory(): String = "Kotlin"
    
    override fun create(projectDir: File, projectName: String, packageName: String): Boolean {
        try {
            // Create project structure
            val srcDir = File(projectDir, "src/main/kotlin")
            val testDir = File(projectDir, "src/test/kotlin")
            val packageDir = File(srcDir, packageName.replace(".", "/"))
            val testPackageDir = File(testDir, packageName.replace(".", "/"))
            
            packageDir.mkdirs()
            testPackageDir.mkdirs()
            
            // Create build.gradle.kts
            val buildGradle = """
                plugins {
                    kotlin("jvm") version "1.9.20"
                    `java-library`
                    `maven-publish`
                }
                
                group = "$packageName"
                version = "0.1.0"
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation(kotlin("stdlib"))
                    
                    // Testing
                    testImplementation(kotlin("test"))
                    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
                    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
                }
                
                tasks.test {
                    useJUnitPlatform()
                }
                
                java {
                    withSourcesJar()
                    withJavadocJar()
                }
                
                publishing {
                    publications {
                        create<MavenPublication>("maven") {
                            from(components["java"])
                            
                            pom {
                                name.set("${projectName}")
                                description.set("A Kotlin library for ${projectName}")
                                url.set("https://github.com/yourusername/${projectName.lowercase()}")
                                
                                licenses {
                                    license {
                                        name.set("MIT License")
                                        url.set("https://opensource.org/licenses/MIT")
                                    }
                                }
                                
                                developers {
                                    developer {
                                        id.set("yourusername")
                                        name.set("Your Name")
                                        email.set("your.email@example.com")
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent()
            
            File(projectDir, "build.gradle.kts").writeText(buildGradle)
            
            // Create settings.gradle.kts
            val settingsGradle = """
                rootProject.name = "$projectName"
            """.trimIndent()
            
            File(projectDir, "settings.gradle.kts").writeText(settingsGradle)
            
            // Create gradle.properties
            val gradleProperties = """
                kotlin.code.style=official
                org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
            """.trimIndent()
            
            File(projectDir, "gradle.properties").writeText(gradleProperties)
            
            // Create .gitignore
            val gitignore = """
                .gradle
                build/
                !gradle/wrapper/gradle-wrapper.jar
                !**/src/main/**/build/
                !**/src/test/**/build/
                
                ### IntelliJ IDEA ###
                .idea/
                *.iws
                *.iml
                *.ipr
                out/
                !**/src/main/**/out/
                !**/src/test/**/out/
                
                ### VS Code ###
                .vscode/
                
                ### Mac OS ###
                .DS_Store
            """.trimIndent()
            
            File(projectDir, ".gitignore").writeText(gitignore)
            
            // Create README.md
            val readme = """
                # $projectName
                
                A Kotlin library for [describe your library purpose here].
                
                ## Features
                
                * Feature 1
                * Feature 2
                * Feature 3
                
                ## Installation
                
                ### Gradle
                
                ```kotlin
                dependencies {
                    implementation("$packageName:$projectName:0.1.0")
                }
                ```
                
                ### Maven
                
                ```xml
                <dependency>
                    <groupId>$packageName</groupId>
                    <artifactId>$projectName</artifactId>
                    <version>0.1.0</version>
                </dependency>
                ```
                
                ## Usage
                
                ```kotlin
                import $packageName.StringUtils
                
                val result = StringUtils.capitalize("hello world")
                println(result) // "Hello world"
                ```
                
                ## License
                
                This project is licensed under the MIT License - see the LICENSE file for details.
            """.trimIndent()
            
            File(projectDir, "README.md").writeText(readme)
            
            // Create LICENSE
            val license = """
                MIT License
                
                Copyright (c) ${java.time.Year.now().value} Your Name
                
                Permission is hereby granted, free of charge, to any person obtaining a copy
                of this software and associated documentation files (the "Software"), to deal
                in the Software without restriction, including without limitation the rights
                to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                copies of the Software, and to permit persons to whom the Software is
                furnished to do so, subject to the following conditions:
                
                The above copyright notice and this permission notice shall be included in all
                copies or substantial portions of the Software.
                
                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
                IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
                FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
                AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
                LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
                OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
                SOFTWARE.
            """.trimIndent()
            
            File(projectDir, "LICENSE").writeText(license)
            
            // Create StringUtils.kt
            val stringUtils = """
                package $packageName
                
                /**
                 * Utility functions for string manipulation.
                 */
                object StringUtils {
                    
                    /**
                     * Capitalizes the first character of the given string.
                     *
                     * @param input The string to capitalize
                     * @return The capitalized string
                     */
                    fun capitalize(input: String): String {
                        if (input.isEmpty()) return input
                        return input.replaceFirstChar { it.uppercase() }
                    }
                    
                    /**
                     * Reverses the given string.
                     *
                     * @param input The string to reverse
                     * @return The reversed string
                     */
                    fun reverse(input: String): String {
                        return input.reversed()
                    }
                    
                    /**
                     * Checks if the string is a palindrome (reads the same backward as forward).
                     *
                     * @param input The string to check
                     * @return True if the string is a palindrome, false otherwise
                     */
                    fun isPalindrome(input: String): Boolean {
                        val cleaned = input.lowercase().replace(Regex("[^a-z0-9]"), "")
                        return cleaned == cleaned.reversed()
                    }
                    
                    /**
                     * Counts the occurrences of a substring within a string.
                     *
                     * @param input The string to search in
                     * @param substring The substring to search for
                     * @return The number of occurrences
                     */
                    fun countOccurrences(input: String, substring: String): Int {
                        if (substring.isEmpty()) return 0
                        
                        var count = 0
                        var index = input.indexOf(substring)
                        
                        while (index != -1) {
                            count++
                            index = input.indexOf(substring, index + 1)
                        }
                        
                        return count
                    }
                }
            """.trimIndent()
            
            File(packageDir, "StringUtils.kt").writeText(stringUtils)
            
            // Create StringUtilsTest.kt
            val stringUtilsTest = """
                package $packageName
                
                import kotlin.test.Test
                import kotlin.test.assertEquals
                import kotlin.test.assertFalse
                import kotlin.test.assertTrue
                
                class StringUtilsTest {
                    
                    @Test
                    fun `capitalize should uppercase first letter`() {
                        assertEquals("Hello", StringUtils.capitalize("hello"))
                        assertEquals("Hello", StringUtils.capitalize("Hello"))
                        assertEquals("", StringUtils.capitalize(""))
                        assertEquals("1abc", StringUtils.capitalize("1abc"))
                    }
                    
                    @Test
                    fun `reverse should reverse the string`() {
                        assertEquals("olleh", StringUtils.reverse("hello"))
                        assertEquals("", StringUtils.reverse(""))
                        assertEquals("a", StringUtils.reverse("a"))
                        assertEquals("321", StringUtils.reverse("123"))
                    }
                    
                    @Test
                    fun `isPalindrome should correctly identify palindromes`() {
                        assertTrue(StringUtils.isPalindrome("racecar"))
                        assertTrue(StringUtils.isPalindrome("A man, a plan, a canal: Panama"))
                        assertTrue(StringUtils.isPalindrome(""))
                        assertTrue(StringUtils.isPalindrome("a"))
                        
                        assertFalse(StringUtils.isPalindrome("hello"))
                        assertFalse(StringUtils.isPalindrome("kotlin"))
                    }
                    
                    @Test
                    fun `countOccurrences should count substring occurrences`() {
                        assertEquals(2, StringUtils.countOccurrences("hello hello", "hello"))
                        assertEquals(3, StringUtils.countOccurrences("abababa", "aba"))
                        assertEquals(0, StringUtils.countOccurrences("hello", "world"))
                        assertEquals(0, StringUtils.countOccurrences("hello", ""))
                        assertEquals(0, StringUtils.countOccurrences("", "hello"))
                    }
                }
            """.trimIndent()
            
            File(testPackageDir, "StringUtilsTest.kt").writeText(stringUtilsTest)
            
            // Create NumberUtils.kt
            val numberUtils = """
                package $packageName
                
                /**
                 * Utility functions for number manipulation.
                 */
                object NumberUtils {
                    
                    /**
                     * Checks if a number is prime.
                     *
                     * @param n The number to check
                     * @return True if the number is prime, false otherwise
                     */
                    fun isPrime(n: Int): Boolean {
                        if (n <= 1) return false
                        if (n <= 3) return true
                        
                        if (n % 2 == 0 || n % 3 == 0) return false
                        
                        var i = 5
                        while (i * i <= n) {
                            if (n % i == 0 || n % (i + 2) == 0) return false
                            i += 6
                        }
                        
                        return true
                    }
                    
                    /**
                     * Calculates the factorial of a number.
                     *
                     * @param n The number to calculate factorial for
                     * @return The factorial of n
                     * @throws IllegalArgumentException if n is negative
                     */
                    fun factorial(n: Int): Long {
                        if (n < 0) throw IllegalArgumentException("Factorial not defined for negative numbers")
                        if (n == 0 || n == 1) return 1
                        
                        var result = 1L
                        for (i in 2..n) {
                            result *= i
                        }
                        
                        return result
                    }
                    
                    /**
                     * Calculates the greatest common divisor (GCD) of two numbers.
                     *
                     * @param a First number
                     * @param b Second number
                     * @return The GCD of a and b
                     */
                    fun gcd(a: Int, b: Int): Int {
                        var x = a
                        var y = b
                        
                        while (y != 0) {
                            val temp = y
                            y = x % y
                            x = temp
                        }
                        
                        return x
                    }
                    
                    /**
                     * Calculates the least common multiple (LCM) of two numbers.
                     *
                     * @param a First number
                     * @param b Second number
                     * @return The LCM of a and b
                     */
                    fun lcm(a: Int, b: Int): Int {
                        return if (a == 0 || b == 0) 0 else Math.abs(a * b) / gcd(a, b)
                    }
                }
            """.trimIndent()
            
            File(packageDir, "NumberUtils.kt").writeText(numberUtils)
            
            // Create NumberUtilsTest.kt
            val numberUtilsTest = """
                package $packageName
                
                import kotlin.test.Test
                import kotlin.test.assertEquals
                import kotlin.test.assertFailsWith
                import kotlin.test.assertFalse
                import kotlin.test.assertTrue
                
                class NumberUtilsTest {
                    
                    @Test
                    fun `isPrime should correctly identify prime numbers`() {
                        assertFalse(NumberUtils.isPrime(1))
                        assertTrue(NumberUtils.isPrime(2))
                        assertTrue(NumberUtils.isPrime(3))
                        assertFalse(NumberUtils.isPrime(4))
                        assertTrue(NumberUtils.isPrime(5))
                        assertFalse(NumberUtils.isPrime(6))
                        assertTrue(NumberUtils.isPrime(7))
                        assertFalse(NumberUtils.isPrime(8))
                        assertFalse(NumberUtils.isPrime(9))
                        assertFalse(NumberUtils.isPrime(10))
                        assertTrue(NumberUtils.isPrime(11))
                        assertTrue(NumberUtils.isPrime(17))
                        assertTrue(NumberUtils.isPrime(19))
                        assertTrue(NumberUtils.isPrime(23))
                        assertFalse(NumberUtils.isPrime(25))
                        assertTrue(NumberUtils.isPrime(29))
                        assertTrue(NumberUtils.isPrime(97))
                    }
                    
                    @Test
                    fun `factorial should calculate correct values`() {
                        assertEquals(1, NumberUtils.factorial(0))
                        assertEquals(1, NumberUtils.factorial(1))
                        assertEquals(2, NumberUtils.factorial(2))
                        assertEquals(6, NumberUtils.factorial(3))
                        assertEquals(24, NumberUtils.factorial(4))
                        assertEquals(120, NumberUtils.factorial(5))
                        assertEquals(3628800, NumberUtils.factorial(10))
                    }
                    
                    @Test
                    fun `factorial should throw exception for negative numbers`() {
                        assertFailsWith<IllegalArgumentException> {
                            NumberUtils.factorial(-1)
                        }
                    }
                    
                    @Test
                    fun `gcd should calculate correct values`() {
                        assertEquals(1, NumberUtils.gcd(1, 1))
                        assertEquals(2, NumberUtils.gcd(2, 4))
                        assertEquals(1, NumberUtils.gcd(3, 5))
                        assertEquals(6, NumberUtils.gcd(12, 18))
                        assertEquals(12, NumberUtils.gcd(12, 60))
                        assertEquals(1, NumberUtils.gcd(17, 13))
                    }
                    
                    @Test
                    fun `lcm should calculate correct values`() {
                        assertEquals(0, NumberUtils.lcm(0, 5))
                        assertEquals(0, NumberUtils.lcm(5, 0))
                        assertEquals(1, NumberUtils.lcm(1, 1))
                        assertEquals(6, NumberUtils.lcm(2, 3))
                        assertEquals(12, NumberUtils.lcm(4, 6))
                        assertEquals(60, NumberUtils.lcm(12, 15))
                        assertEquals(221, NumberUtils.lcm(13, 17))
                    }
                }
            """.trimIndent()
            
            File(testPackageDir, "NumberUtilsTest.kt").writeText(numberUtilsTest)
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}