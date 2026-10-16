@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.securemart.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Colors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEDE8),
    onPrimaryContainer = Color(0xFF0B3B37),
    secondary = Color(0xFFF97316),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEDE8),
    onSecondaryContainer = Color(0xFF0B3B37),
    background = Color(0xFFF4F7F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3ECEB),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978)
)

@Composable
fun SecureMartTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
