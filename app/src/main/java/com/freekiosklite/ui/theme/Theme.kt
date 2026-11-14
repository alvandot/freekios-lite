package com.freekiosklite.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KioskColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFF94A3B8),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
)

@Composable
fun FreeKioskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KioskColors,
        content = content,
    )
}
