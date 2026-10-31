package com.stonic.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StonicNavy = Color(0xFF0B0F24)
val StonicSurface = Color(0xFF141A3A)
val StonicPurple = Color(0xFF8B5CF6)
val StonicPink = Color(0xFFEC4899)
val StonicText = Color(0xFFE8EAF6)

private val StonicDarkColors = darkColorScheme(
    primary = StonicPurple,
    onPrimary = Color.White,
    secondary = StonicPink,
    background = StonicNavy,
    onBackground = StonicText,
    surface = StonicSurface,
    onSurface = StonicText,
    surfaceVariant = Color(0xFF1C2350),
    onSurfaceVariant = Color(0xFFB8BEE0)
)

@Composable
fun StonicTheme(content: @Composable () -> Unit) {
    // Dark theme only for now (dark is the default in the spec).
    MaterialTheme(colorScheme = StonicDarkColors, content = content)
}
