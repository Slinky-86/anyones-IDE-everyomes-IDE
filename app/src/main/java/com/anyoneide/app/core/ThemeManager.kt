package com.anyoneide.app.core

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import androidx.core.content.edit
import androidx.core.graphics.toColorInt

class ThemeManager(private val context: Context) {
    
    private val _currentTheme = MutableStateFlow(getDefaultDarkTheme())
    val currentTheme: StateFlow<EditorTheme> = _currentTheme.asStateFlow()
    
    private val _availableThemes = MutableStateFlow(getBuiltInThemes())
    val availableThemes: StateFlow<List<EditorTheme>> = _availableThemes.asStateFlow()
    
    private val json = Json { ignoreUnknownKeys = true }
    private val themesDir = File(context.filesDir, "themes")
    
    init {
        themesDir.mkdirs()
        loadCustomThemes()
    }
    
    fun setTheme(themeId: String) {
        val theme = _availableThemes.value.find { it.id == themeId }
        if (theme != null) {
            _currentTheme.value = theme
            saveCurrentTheme(themeId)
        }
    }
    
    fun addCustomTheme(theme: EditorTheme) {
        val currentThemes = _availableThemes.value.toMutableList()
        currentThemes.removeAll { it.id == theme.id }
        currentThemes.add(theme)
        _availableThemes.value = currentThemes
        
        saveCustomTheme(theme)
    }
    
    fun removeCustomTheme(themeId: String) {
        val currentThemes = _availableThemes.value.toMutableList()
        currentThemes.removeAll { it.id == themeId && it.isCustom }
        _availableThemes.value = currentThemes
        
        val themeFile = File(themesDir, "$themeId.json")
        if (themeFile.exists()) {
            themeFile.delete()
        }
    }
    
    private fun loadCustomThemes() {
        themesDir.listFiles { file -> file.extension == "json" }?.forEach { file ->
            try {
                val content = file.readText()
                val theme = json.decodeFromString<EditorTheme>(content)
                addCustomTheme(theme.copy(isCustom = true))
            } catch (_: Exception) {
                // Log error and continue
            }
        }
    }
    
    private fun saveCustomTheme(theme: EditorTheme) {
        try {
            val themeFile = File(themesDir, "${theme.id}.json")
            val content = json.encodeToString(EditorTheme.serializer(), theme)
            themeFile.writeText(content)
        } catch (_: Exception) {
            // Log error
        }
    }
    
    private fun saveCurrentTheme(themeId: String) {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString("current_theme", themeId) }
    }
    
    private fun loadCurrentTheme(): String {
        val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        return prefs.getString("current_theme", "dark_default") ?: "dark_default"
    }
    
    private fun getBuiltInThemes(): List<EditorTheme> {
        return listOf(
            getDefaultDarkTheme(),
            getDefaultLightTheme(),
            getVSCodeDarkTheme(),
            getMonokaiTheme(),
            getSolarizedDarkTheme(),
            getSolarizedLightTheme(),
            getDraculaTheme(),
            getGitHubTheme(),
            getAtomOneDarkTheme(),
            getMaterialTheme()
        )
    }
    
    private fun getDefaultDarkTheme(): EditorTheme {
        return EditorTheme(
            id = "dark_default",
            name = "Dark Default",
            description = "Default dark theme for Anyone IDE",
            isCustom = false,
            colors = EditorColors(
                background = "#1E1E1E",
                foreground = "#D4D4D4",
                selection = "#264F78",
                lineNumber = "#858585",
                currentLine = "#2A2D2E",
                cursor = "#FFFFFF",
                keyword = "#569CD6",
                string = "#CE9178",
                comment = "#6A9955",
                number = "#B5CEA8",
                function = "#DCDCAA",
                type = "#4EC9B0",
                variable = "#9CDCFE",
                operator = "#D4D4D4",
                bracket = "#FFD700",
                error = "#F44747",
                warning = "#FF8C00",
                info = "#3794FF"
            )
        )
    }
    
    private fun getDefaultLightTheme(): EditorTheme {
        return EditorTheme(
            id = "light_default",
            name = "Light Default",
            description = "Default light theme for Anyone IDE",
            isCustom = false,
            colors = EditorColors(
                background = "#FFFFFF",
                foreground = "#000000",
                selection = "#ADD6FF",
                lineNumber = "#237893",
                currentLine = "#F5F5F5",
                cursor = "#000000",
                keyword = "#0000FF",
                string = "#A31515",
                comment = "#008000",
                number = "#098658",
                function = "#795E26",
                type = "#267F99",
                variable = "#001080",
                operator = "#000000",
                bracket = "#0431FA",
                error = "#E51400",
                warning = "#BF8803",
                info = "#1a85ff"
            )
        )
    }
    
    private fun getVSCodeDarkTheme(): EditorTheme {
        return EditorTheme(
            id = "vscode_dark",
            name = "VS Code Dark",
            description = "Visual Studio Code dark theme",
            isCustom = false,
            colors = EditorColors(
                background = "#1E1E1E",
                foreground = "#D4D4D4",
                selection = "#264F78",
                lineNumber = "#858585",
                currentLine = "#2A2D2E",
                cursor = "#AEAFAD",
                keyword = "#569CD6",
                string = "#CE9178",
                comment = "#6A9955",
                number = "#B5CEA8",
                function = "#DCDCAA",
                type = "#4EC9B0",
                variable = "#9CDCFE",
                operator = "#D4D4D4",
                bracket = "#DA70D6",
                error = "#F44747",
                warning = "#FF8C00",
                info = "#3794FF"
            )
        )
    }
    
    private fun getMonokaiTheme(): EditorTheme {
        return EditorTheme(
            id = "monokai",
            name = "Monokai",
            description = "Popular Monokai color scheme",
            isCustom = false,
            colors = EditorColors(
                background = "#272822",
                foreground = "#F8F8F2",
                selection = "#49483E",
                lineNumber = "#90908A",
                currentLine = "#3E3D32",
                cursor = "#F8F8F0",
                keyword = "#F92672",
                string = "#E6DB74",
                comment = "#75715E",
                number = "#AE81FF",
                function = "#A6E22E",
                type = "#66D9EF",
                variable = "#F8F8F2",
                operator = "#F92672",
                bracket = "#F8F8F2",
                error = "#F92672",
                warning = "#E6DB74",
                info = "#66D9EF"
            )
        )
    }
    
    private fun getSolarizedDarkTheme(): EditorTheme {
        return EditorTheme(
            id = "solarized_dark",
            name = "Solarized Dark",
            description = "Solarized dark color scheme",
            isCustom = false,
            colors = EditorColors(
                background = "#002B36",
                foreground = "#839496",
                selection = "#073642",
                lineNumber = "#586E75",
                currentLine = "#073642",
                cursor = "#93A1A1",
                keyword = "#859900",
                string = "#2AA198",
                comment = "#586E75",
                number = "#D33682",
                function = "#268BD2",
                type = "#B58900",
                variable = "#839496",
                operator = "#859900",
                bracket = "#93A1A1",
                error = "#DC322F",
                warning = "#B58900",
                info = "#268BD2"
            )
        )
    }
    
    private fun getSolarizedLightTheme(): EditorTheme {
        return EditorTheme(
            id = "solarized_light",
            name = "Solarized Light",
            description = "Solarized light color scheme",
            isCustom = false,
            colors = EditorColors(
                background = "#FDF6E3",
                foreground = "#657B83",
                selection = "#EEE8D5",
                lineNumber = "#93A1A1",
                currentLine = "#EEE8D5",
                cursor = "#586E75",
                keyword = "#859900",
                string = "#2AA198",
                comment = "#93A1A1",
                number = "#D33682",
                function = "#268BD2",
                type = "#B58900",
                variable = "#657B83",
                operator = "#859900",
                bracket = "#586E75",
                error = "#DC322F",
                warning = "#B58900",
                info = "#268BD2"
            )
        )
    }
    
    private fun getDraculaTheme(): EditorTheme {
        return EditorTheme(
            id = "dracula",
            name = "Dracula",
            description = "Dracula dark theme",
            isCustom = false,
            colors = EditorColors(
                background = "#282A36",
                foreground = "#F8F8F2",
                selection = "#44475A",
                lineNumber = "#6272A4",
                currentLine = "#44475A",
                cursor = "#F8F8F0",
                keyword = "#FF79C6",
                string = "#F1FA8C",
                comment = "#6272A4",
                number = "#BD93F9",
                function = "#50FA7B",
                type = "#8BE9FD",
                variable = "#F8F8F2",
                operator = "#FF79C6",
                bracket = "#F8F8F2",
                error = "#FF5555",
                warning = "#FFB86C",
                info = "#8BE9FD"
            )
        )
    }
    
    private fun getGitHubTheme(): EditorTheme {
        return EditorTheme(
            id = "github",
            name = "GitHub",
            description = "GitHub light theme",
            isCustom = false,
            colors = EditorColors(
                background = "#FFFFFF",
                foreground = "#24292E",
                selection = "#C8E1FF",
                lineNumber = "#1B1F23",
                currentLine = "#F6F8FA",
                cursor = "#24292E",
                keyword = "#D73A49",
                string = "#032F62",
                comment = "#6A737D",
                number = "#005CC5",
                function = "#6F42C1",
                type = "#005CC5",
                variable = "#24292E",
                operator = "#D73A49",
                bracket = "#24292E",
                error = "#D73A49",
                warning = "#E36209",
                info = "#005CC5"
            )
        )
    }
    
    private fun getAtomOneDarkTheme(): EditorTheme {
        return EditorTheme(
            id = "atom_one_dark",
            name = "Atom One Dark",
            description = "Atom One Dark theme",
            isCustom = false,
            colors = EditorColors(
                background = "#282C34",
                foreground = "#ABB2BF",
                selection = "#3E4451",
                lineNumber = "#636D83",
                currentLine = "#2C313C",
                cursor = "#528BFF",
                keyword = "#C678DD",
                string = "#98C379",
                comment = "#5C6370",
                number = "#D19A66",
                function = "#61AFEF",
                type = "#E06C75",
                variable = "#ABB2BF",
                operator = "#56B6C2",
                bracket = "#ABB2BF",
                error = "#E06C75",
                warning = "#E5C07B",
                info = "#61AFEF"
            )
        )
    }
    
    private fun getMaterialTheme(): EditorTheme {
        return EditorTheme(
            id = "material",
            name = "Material",
            description = "Material Design theme",
            isCustom = false,
            colors = EditorColors(
                background = "#263238",
                foreground = "#EEFFFF",
                selection = "#314549",
                lineNumber = "#546E7A",
                currentLine = "#314549",
                cursor = "#FFCC02",
                keyword = "#C792EA",
                string = "#C3E88D",
                comment = "#546E7A",
                number = "#F78C6C",
                function = "#82AAFF",
                type = "#FFCB6B",
                variable = "#EEFFFF",
                operator = "#89DDFF",
                bracket = "#EEFFFF",
                error = "#F07178",
                warning = "#FFCB6B",
                info = "#82AAFF"
            )
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EditorTheme(
    val id: String,
    val name: String,
    val description: String,
    val isCustom: Boolean = false,
    val colors: EditorColors
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EditorColors(
    val background: String,
    val foreground: String,
    val selection: String,
    val lineNumber: String,
    val currentLine: String,
    val cursor: String,
    val keyword: String,
    val string: String,
    val comment: String,
    val number: String,
    val function: String,
    val type: String,
    val variable: String,
    val operator: String,
    val bracket: String,
    val error: String,
    val warning: String,
    val info: String
)

// Extension functions to convert hex strings to Compose Colors
fun String.toComposeColor(): Color {
    return Color(this.toColorInt())
}