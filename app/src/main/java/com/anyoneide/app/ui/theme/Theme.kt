package com.anyoneide.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    secondary = SecondaryOrange,
    onSecondary = Black,
    tertiary = InfoBlue,
    onTertiary = White,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    error = ErrorRed,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    secondary = SecondaryOrange,
    onSecondary = White,
    tertiary = InfoBlue,
    onTertiary = White,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    error = ErrorRed,
    onError = White
)

@Composable
fun AnyoneIDETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent IDE theming
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Extension properties for easy access to custom colors
val MaterialTheme.customColors: CustomColors
    @Composable
    get() = if (isSystemInDarkTheme()) {
        CustomColors(
            codeBackground = CodeBackground,
            codeForeground = CodeForeground,
            codeSelection = CodeSelection,
            codeLineNumber = CodeLineNumber,
            codeCurrentLine = CodeCurrentLine,
            syntaxKeyword = SyntaxKeyword,
            syntaxString = SyntaxString,
            syntaxComment = SyntaxComment,
            syntaxNumber = SyntaxNumber,
            syntaxFunction = SyntaxFunction,
            syntaxType = SyntaxType,
            syntaxVariable = SyntaxVariable,
            syntaxOperator = SyntaxOperator,
            successGreen = SuccessGreen,
            warningYellow = WarningYellow,
            errorRed = ErrorRed,
            infoBlue = InfoBlue,
            terminalBackground = TerminalBackground,
            terminalForeground = TerminalForeground,
            terminalCursor = TerminalCursor,
            terminalSelection = TerminalSelection,
            directoryIcon = DirectoryIcon,
            fileIcon = FileIcon,
            modifiedFile = ModifiedFile,
            newFile = NewFile
        )
    } else {
        CustomColors(
            codeBackground = Color.White,
            codeForeground = Color.Black,
            codeSelection = Color(0xFFE3F2FD),
            codeLineNumber = Color(0xFF757575),
            codeCurrentLine = Color(0xFFF5F5F5),
            syntaxKeyword = Color(0xFF0000FF),
            syntaxString = Color(0xFF008000),
            syntaxComment = Color(0xFF808080),
            syntaxNumber = Color(0xFF800080),
            syntaxFunction = Color(0xFF795E26),
            syntaxType = Color(0xFF267F99),
            syntaxVariable = Color(0xFF001080),
            syntaxOperator = Color(0xFF000000),
            successGreen = SuccessGreen,
            warningYellow = WarningYellow,
            errorRed = ErrorRed,
            infoBlue = InfoBlue,
            terminalBackground = Color.White,
            terminalForeground = Color.Black,
            terminalCursor = Color.Black,
            terminalSelection = Color(0xFFE3F2FD),
            directoryIcon = Color(0xFFFF8F00),
            fileIcon = Color(0xFF616161),
            modifiedFile = WarningYellow,
            newFile = SuccessGreen
        )
    }

data class CustomColors(
    val codeBackground: Color,
    val codeForeground: Color,
    val codeSelection: Color,
    val codeLineNumber: Color,
    val codeCurrentLine: Color,
    val syntaxKeyword: Color,
    val syntaxString: Color,
    val syntaxComment: Color,
    val syntaxNumber: Color,
    val syntaxFunction: Color,
    val syntaxType: Color,
    val syntaxVariable: Color,
    val syntaxOperator: Color,
    val successGreen: Color,
    val warningYellow: Color,
    val errorRed: Color,
    val infoBlue: Color,
    val terminalBackground: Color,
    val terminalForeground: Color,
    val terminalCursor: Color,
    val terminalSelection: Color,
    val directoryIcon: Color,
    val fileIcon: Color,
    val modifiedFile: Color,
    val newFile: Color
)