package com.anyoneide.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProjectTemplatePreview(
    modifier: Modifier = Modifier,
    onTemplateSelected: (ProjectTemplateData) -> Unit = {},
    onCreateProject: (ProjectTemplateData) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTemplate by remember { mutableStateOf<ProjectTemplateData?>(null) }
    
    val categories = listOf("All", "Android", "Kotlin", "Java", "Compose", "Games", "Rust")
    
    val filteredTemplates = if (selectedCategory == "All") {
        projectTemplates
    } else {
        projectTemplates.filter { it.category == selectedCategory }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Template Grid
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            // Category Filter
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        selected = selectedCategory == category
                    )
                }
            }

            // Template Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredTemplates) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = selectedTemplate?.id == template.id,
                        onClick = {
                            selectedTemplate = template
                            onTemplateSelected(template)
                        }
                    )
                }
            }
        }

        // Template Details
        if (selectedTemplate != null) {
            TemplateDetailsPanel(
                modifier = Modifier.width(400.dp).fillMaxHeight(),
                template = selectedTemplate!!,
                onCreateProject = { onCreateProject(selectedTemplate!!) },
                onClose = { selectedTemplate = null }
            )
        }
    }
}

@Composable
fun TemplateCard(
    template: ProjectTemplateData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = template.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = template.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Difficulty Badge
                Surface(
                    color = when (template.difficulty) {
                        "Beginner" -> Color(0xFF4CAF50)
                        "Intermediate" -> Color(0xFFFF9800)
                        "Advanced" -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = template.difficulty,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Features
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(template.features.take(3)) { feature ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = feature,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = template.estimatedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TemplateDetailsPanel(
    modifier: Modifier = Modifier,
    template: ProjectTemplateData,
    onCreateProject: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Template Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Template Info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = template.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = template.category,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Features
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(template.features) { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Project Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Difficulty",
                    value = template.difficulty
                )
                InfoItem(
                    icon = Icons.Default.Schedule,
                    label = "Time",
                    value = template.estimatedTime
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Code Preview
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = template.preview,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Button
            Button(
                onClick = onCreateProject,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Project")
            }
        }
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// Sample project templates - these would typically come from a repository or ViewModel
val projectTemplates = listOf(
    ProjectTemplateData(
        id = "android_basic",
        name = "Basic Android App",
        description = "A simple Android application with basic navigation and Material Design components.",
        icon = Icons.Default.Android,
        category = "Android",
        features = listOf("Material Design 3", "Navigation Component", "ViewBinding", "Kotlin"),
        difficulty = "Beginner",
        estimatedTime = "30 min",
        preview = """
            class MainActivity : AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContentView(R.layout.activity_main)
                }
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "compose_app",
        name = "Jetpack Compose App",
        description = "Modern Android app built with Jetpack Compose for declarative UI development.",
        icon = Icons.Default.Brush,
        category = "Compose",
        features = listOf("Jetpack Compose", "Material Design 3", "State Management", "Navigation"),
        difficulty = "Intermediate",
        estimatedTime = "45 min",
        preview = """
            @Composable
            fun MainScreen() {
                MaterialTheme {
                    Surface {
                        Text("Hello Compose!")
                    }
                }
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "mvvm_app",
        name = "MVVM Architecture",
        description = "Android app following MVVM pattern with Repository, ViewModel, and LiveData.",
        icon = Icons.Default.Architecture,
        category = "Android",
        features = listOf("MVVM Pattern", "Repository Pattern", "LiveData", "Room Database"),
        difficulty = "Advanced",
        estimatedTime = "60 min",
        preview = """
            class MainViewModel : ViewModel() {
                private val repository = UserRepository()
                val users = repository.getAllUsers()
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "game_2d",
        name = "2D Game",
        description = "Simple 2D game using Android Canvas and custom views for game development.",
        icon = Icons.Default.SportsEsports,
        category = "Games",
        features = listOf("Custom Views", "Canvas Drawing", "Touch Input", "Game Loop"),
        difficulty = "Intermediate",
        estimatedTime = "90 min",
        preview = """
            class GameView : View {
                override fun onDraw(canvas: Canvas?) {
                    super.onDraw(canvas)
                    // Game rendering logic
                }
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "rest_api",
        name = "REST API Client",
        description = "Android app that consumes REST APIs with Retrofit and displays data in RecyclerView.",
        icon = Icons.Default.Api,
        category = "Android",
        features = listOf("Retrofit", "RecyclerView", "JSON Parsing", "Network Handling"),
        difficulty = "Intermediate",
        estimatedTime = "50 min",
        preview = """
            interface ApiService {
                @GET("users")
                suspend fun getUsers(): List<User>
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "kotlin_library",
        name = "Kotlin Library",
        description = "Pure Kotlin library project with unit tests and documentation.",
        icon = Icons.AutoMirrored.Filled.LibraryBooks,
        category = "Kotlin",
        features = listOf("Pure Kotlin", "Unit Tests", "Documentation", "Gradle Publishing"),
        difficulty = "Beginner",
        estimatedTime = "25 min",
        preview = """
            class StringUtils {
                fun capitalize(input: String): String {
                    return input.replaceFirstChar { it.uppercase() }
                }
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "kotlin_multiplatform",
        name = "Kotlin Multiplatform",
        description = "Cross-platform Kotlin project targeting Android, iOS, and JVM.",
        icon = Icons.Default.Devices,
        category = "Kotlin",
        features = listOf("Kotlin Multiplatform", "Shared Code", "Platform-Specific Code", "Coroutines"),
        difficulty = "Advanced",
        estimatedTime = "75 min",
        preview = """
            expect class Platform() {
                val platform: String
            }

            actual class Platform actual constructor() {
                actual val platform: String = "Android"
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "android_library",
        name = "Android Library",
        description = "Reusable Android library module with documentation and sample app.",
        icon = Icons.Default.Extension,
        category = "Android",
        features = listOf("Library Module", "AAR Publishing", "Sample App", "Documentation"),
        difficulty = "Intermediate",
        estimatedTime = "40 min",
        preview = """
            class MyLibrary {
                fun doSomething(context: Context): String {
                    return "Library function called"
                }
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "java_app",
        name = "Java Android App",
        description = "Traditional Android application using Java instead of Kotlin.",
        icon = Icons.Default.Coffee,
        category = "Java",
        features = listOf("Java", "Material Design", "ViewBinding", "Fragments"),
        difficulty = "Beginner",
        estimatedTime = "35 min",
        preview = """
            public class MainActivity extends AppCompatActivity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(R.layout.activity_main);
                }
            }
        """.trimIndent()
    ),
    ProjectTemplateData(
        id = "rust_android_lib",
        name = "Rust Android Library",
        description = "Android application with Rust native library using JNI bindings.",
        icon = Icons.Default.Code,
        category = "Rust",
        features = listOf("Rust Native Code", "JNI Bindings", "Cross-platform", "High Performance"),
        difficulty = "Advanced",
        estimatedTime = "60 min",
        preview = """
            // Rust code (lib.rs)
            #[no_mangle]
            pub extern "C" fn Java_com_example_RustLib_getGreeting(
                env: JNIEnv, _: JClass
            ) -> jstring {
                let output = env.new_string("Hello from Rust!")
                    .expect("Couldn't create Java string!");
                output.into_raw()
            }
        """.trimIndent()
    )
)