package com.capyreader.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import java.awt.Window
import com.capyreader.desktop.storage.DesktopFontFamily
import com.capyreader.desktop.storage.DesktopThemeMode

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

private val SepiaColorScheme = lightColorScheme(
    primary = Color(0xFF8D531B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0DEC5),
    onPrimaryContainer = Color(0xFF321A04),
    secondary = Color(0xFF705B44),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E4CE),
    onSecondaryContainer = Color(0xFF26190B),
    background = Color(0xFFF6EFE0),
    onBackground = Color(0xFF33271D),
    surface = Color(0xFFEFE6D5),
    onSurface = Color(0xFF33271D),
    surfaceVariant = Color(0xFFE4D9C6),
    onSurfaceVariant = Color(0xFF534538),
    outline = Color(0xFF9E8E7D),
)

private val BlackColorScheme = darkColorScheme(
    primary = Color(0xFFFF9E45),
    onPrimary = Color(0xFF432100),
    primaryContainer = Color(0xFF623400),
    onPrimaryContainer = Color(0xFFFFDCBF),
    secondary = Color(0xFFD7C2B4),
    onSecondary = Color(0xFF3B2D24),
    secondaryContainer = Color(0xFF524339),
    onSecondaryContainer = Color(0xFFF5DEC8),
    background = Color(0xFF000000),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF101010),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFFC4C4C4),
    outline = Color(0xFF6B6B6B),
)

fun parseHexColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    return try {
        when (clean.length) {
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun getDesktopTypography(fontFamily: FontFamily): Typography {
    val defaultTypography = Typography()
    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun DesktopTheme(
    window: Window? = null,
    themeMode: DesktopThemeMode = DesktopThemeMode.SYSTEM,
    fontFamily: DesktopFontFamily = DesktopFontFamily.SYSTEM_DEFAULT,
    accentColorHex: String = "",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        DesktopThemeMode.SYSTEM -> isSystemDark
        DesktopThemeMode.DARK, DesktopThemeMode.BLACK -> true
        DesktopThemeMode.LIGHT, DesktopThemeMode.SEPIA -> false
    }

    val baseColorScheme = when (themeMode) {
        DesktopThemeMode.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
        DesktopThemeMode.LIGHT -> LightColorScheme
        DesktopThemeMode.DARK -> DarkColorScheme
        DesktopThemeMode.SEPIA -> SepiaColorScheme
        DesktopThemeMode.BLACK -> BlackColorScheme
    }

    val customAccent = remember(accentColorHex) { parseHexColor(accentColorHex) }

    val colorScheme = if (customAccent != null) {
        baseColorScheme.copy(
            primary = customAccent,
            primaryContainer = customAccent.copy(alpha = 0.2f),
            onPrimaryContainer = customAccent
        )
    } else {
        baseColorScheme
    }

    if (window != null) {
        LaunchedEffect(window, colorScheme, isDark) {
            DesktopWindowTheme.applyTheme(window, colorScheme, isDark)
        }
    }

    val composeFontFamily = remember(fontFamily) {
        DesktopFontLoader.load(fontFamily)
    }

    val typography = remember(composeFontFamily) {
        getDesktopTypography(composeFontFamily)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
