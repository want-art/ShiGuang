package com.shiguang.moments.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB4556B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DC),
    onPrimaryContainer = Color(0xFF3E0023),
    secondary = Color(0xFF76565E),
    secondaryContainer = Color(0xFFFFD9DC),
    onSecondaryContainer = Color(0xFF2C151B),
    tertiary = Color(0xFF8A5700),
    tertiaryContainer = Color(0xFFFFDDB6),
    onTertiaryContainer = Color(0xFF2D1800),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF251A1B),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF251A1B),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524345),
    outline = Color(0xFF857375),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF2F0),
    surfaceContainer = Color(0xFFFDECE9),
    surfaceContainerHigh = Color(0xFFF7E6E4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1BF),
    onPrimary = Color(0xFF5E0F24),
    primaryContainer = Color(0xFF7E2D3D),
    onPrimaryContainer = Color(0xFFFFD9DC),
    secondary = Color(0xFFE5BDC3),
    onSecondary = Color(0xFF442A30),
    secondaryContainer = Color(0xFF5D4046),
    onSecondaryContainer = Color(0xFFFFD9DC),
    tertiary = Color(0xFFF2BC6E),
    onTertiary = Color(0xFF4A2C00),
    tertiaryContainer = Color(0xFF6A4000),
    onTertiaryContainer = Color(0xFFFFDDB6),
    background = Color(0xFF171113),
    onBackground = Color(0xFFF0E3E3),
    surface = Color(0xFF171113),
    onSurface = Color(0xFFF0E3E3),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD7C2C5),
    outline = Color(0xFF9F8B8E),
    surfaceContainerLowest = Color(0xFF120B0D),
    surfaceContainerLow = Color(0xFF201719),
    surfaceContainer = Color(0xFF241B1D),
    surfaceContainerHigh = Color(0xFF2F2528),
)

private val AppTypography = Typography()

@Composable
fun ShiguangTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}