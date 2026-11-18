package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = ObsidianBg,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = OnCyanContainerDark,
    secondary = EmeraldReady,
    onSecondary = ObsidianBg,
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = OnEmeraldContainerDark,
    tertiary = AmberLan,
    onTertiary = ObsidianBg,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1F2D45),
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralContainerDark,
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = Color(0xFF002028),
    secondary = EmeraldSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = Color(0xFF022C22),
    tertiary = AmberTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF451A03),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NucleoIaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
