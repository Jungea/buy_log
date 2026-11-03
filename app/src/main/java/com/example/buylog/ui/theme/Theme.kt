package com.example.buylog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF496579),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9F0F4),
    onPrimaryContainer = Color(0xFF1B313E),
    secondary = Color(0xFF5F6F78),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5EBEE),
    onSecondaryContainer = Color(0xFF1E2C33),
    background = Color.White,
    onBackground = Color(0xFF1B1D1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1D1F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFB),
    surfaceContainer = Color(0xFFF0F4F6),
    surfaceContainerHigh = Color(0xFFEAF0F3),
    surfaceContainerHighest = Color(0xFFE3EAED),
    surfaceTint = Color(0xFF496579),
    surfaceVariant = Color(0xFFEEF1F2),
    onSurfaceVariant = Color(0xFF465157),
    outline = Color(0xFF707B80),
)

@Composable
fun BuyLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
