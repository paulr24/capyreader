package com.capyreader.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE28434),
    onPrimary = Color(0xFF381E00),
    primaryContainer = Color(0xFF552F00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFE0C1A3),
    onSecondary = Color(0xFF3E2C17),
    secondaryContainer = Color(0xFF56422C),
    onSecondaryContainer = Color(0xFFFDDEBF),
    background = Color(0xFF1B1B1C),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF242426),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF303033),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF944A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF2F1500),
    secondary = Color(0xFF725A42),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEDEBE),
    onSecondaryContainer = Color(0xFF291806),
    background = Color(0xFFFCF9F7),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFF3EFEA),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFEBE3DC),
    onSurfaceVariant = Color(0xFF4F453E),
    outline = Color(0xFF81756D),
)

@Composable
fun DesktopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
