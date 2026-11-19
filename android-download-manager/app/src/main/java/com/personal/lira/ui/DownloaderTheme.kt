package com.personal.lira.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LiraColors = darkColorScheme(
    primary = Color(0xFF65D9C7),
    onPrimary = Color(0xFF05201C),
    secondary = Color(0xFFFFB77B),
    background = Color(0xFF0D1520),
    onBackground = Color(0xFFF1F5F8),
    surface = Color(0xFF172331),
    onSurface = Color(0xFFF1F5F8),
    surfaceVariant = Color(0xFF223141),
    onSurfaceVariant = Color(0xFFB6C5D1),
    error = Color(0xFFFF8B85),
)

@Composable
fun LiraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiraColors,
        content = content,
    )
}
