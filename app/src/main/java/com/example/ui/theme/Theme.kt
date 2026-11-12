package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SmartTapColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF1E1700),
    primaryContainer = Color(0xFF3B2D00),
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = GoldSecondary,
    onSecondary = Color(0xFF261900),
    secondaryContainer = Color(0xFF422E00),
    onSecondaryContainer = Color(0xFFFFD54F),
    tertiary = OrangeAccent,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

@Composable
fun SmartTapTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SmartTapColorScheme,
        typography = Typography,
        content = content
    )
}
