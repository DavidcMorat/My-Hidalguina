package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    primary = CyanPrimaryDark,
    secondary = YellowSecondary,
    tertiary = CyanAccentDark,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black, // Black text on Cyan primary elements for high contrast
    onSecondary = Color.Black,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider
)

val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    secondary = YellowSecondary,
    tertiary = BlackTertiary,
    background = BackgroundGray,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = BlackTertiary,
    onSurface = BlackTertiary,
    surfaceVariant = Color.White,
    onSurfaceVariant = TextGray,
    outline = DividerGray
)

// Dynamic helper properties for clean light/dark adaptation
object ThemeColors {
    val primary: Color
        @Composable get() = if (ThemeState.isDarkTheme) CyanPrimaryDark else RedPrimary

    val onPrimary: Color
        @Composable get() = if (ThemeState.isDarkTheme) Color.Black else Color.White

    val primaryAccent: Color
        @Composable get() = if (ThemeState.isDarkTheme) CyanAccentDark else Color(0xFFFF5252)

    val secondary: Color
        @Composable get() = YellowSecondary

    val onSecondary: Color
        @Composable get() = Color.Black

    val background: Color
        @Composable get() = if (ThemeState.isDarkTheme) DarkBackground else BackgroundGray

    val surface: Color
        @Composable get() = if (ThemeState.isDarkTheme) DarkSurface else Color.White

    val cardSurface: Color
        @Composable get() = if (ThemeState.isDarkTheme) DarkCardSurface else Color.White

    val textPrimary: Color
        @Composable get() = if (ThemeState.isDarkTheme) DarkTextPrimary else BlackTertiary

    val textSecondary: Color
        @Composable get() = if (ThemeState.isDarkTheme) DarkTextSecondary else TextGray

    val divider: Color
        @Composable get() = if (ThemeState.isDarkTheme) DarkDivider else DividerGray

    val inputBackground: Color
        @Composable get() = if (ThemeState.isDarkTheme) DarkSurface else Color.White

    val inputTextColor: Color
        @Composable get() = if (ThemeState.isDarkTheme) DarkTextPrimary else BlackTertiary

    val topDecorationColor: Color
        @Composable get() = if (ThemeState.isDarkTheme) CyanPrimaryDark else RedPrimary
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = ThemeState.isDarkTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
