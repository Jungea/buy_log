package com.example.buylog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF9A452A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = Color(0xFF75574D),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59D),
    primaryContainer = Color(0xFF7C2E16),
    secondary = Color(0xFFE5BDB0),
)

@Composable
fun BuyLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
