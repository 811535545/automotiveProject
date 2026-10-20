package com.oem.car.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkCarColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),        // Cyan Highlight
    secondary = Color(0xFF7C4DFF),      // Deep Purple Accent
    background = Color(0xFF0A0C10),     // Night Cockpit Black
    surface = Color(0xFF141820),        // Card Surface Dark
    surfaceVariant = Color(0xFF1F2633), // Higher Elevation Surface
    onPrimary = Color.Black,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFCFD8DC),
    error = Color(0xFFFF5252)
)

private val LightCarColorScheme = lightColorScheme(
    primary = Color(0xFF00B0FF),
    secondary = Color(0xFF651FFF),
    background = Color(0xFFF0F4F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE1E8ED),
    onPrimary = Color.White,
    onBackground = Color(0xFF102A43),
    onSurface = Color(0xFF243B53),
    error = Color(0xFFD32F2F)
)

@Composable
fun CarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkCarColorScheme else DarkCarColorScheme // Default to dark for automotive dashboard

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
