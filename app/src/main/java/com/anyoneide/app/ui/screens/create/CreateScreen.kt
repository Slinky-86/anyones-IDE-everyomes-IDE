package com.anyoneide.app.ui.screens.create

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anyoneide.app.ui.components.ProjectTemplateData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    onNavigateBack: () -> Unit,
    onCreateProject: (ProjectTemplateData) -> Unit = {}
) {
    var selectedTemplate by remember { mutableStateOf<ProjectTemplateData?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Project") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Create a New Project",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "Choose a project template to get started",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            // Project template selection
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(projectTemplates) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = selectedTemplate?.id == template.id,
                        onClick = { selectedTemplate = template }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Create button
            Button(
                onClick = { 
                    selectedTemplate?.let { onCreateProject(it) }
                },
                enabled = selectedTemplate != null,
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
fun TemplateCard(
    template: ProjectTemplateData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = template.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
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
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Surface(
                    color = when (template.difficulty) {
                        "Beginner" -> MaterialTheme.colorScheme.primary
                        "Intermediate" -> MaterialTheme.colorScheme.tertiary
                        "Advanced" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = template.difficulty,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Sample project templates - these would typically come from a repository or ViewModel
private val projectTemplates = listOf(
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
        icon = Icons.Default.Code,
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
    )
)