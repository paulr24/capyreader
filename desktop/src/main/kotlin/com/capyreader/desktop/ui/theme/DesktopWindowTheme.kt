package com.capyreader.desktop.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Window

/**
 * Windows DWM (Desktop Window Manager) integration to theme the native
 * top title bar (caption, minimize, maximize, and close buttons) on Windows 10/11.
 */
object DesktopWindowTheme {
    private interface Dwmapi : Library {
        fun DwmSetWindowAttribute(
            hwnd: Pointer,
            dwAttribute: Int,
            pvAttribute: Pointer,
            cbAttribute: Int
        ): Int

        companion object {
            val INSTANCE: Dwmapi? = try {
                Native.load("dwmapi", Dwmapi::class.java)
            } catch (_: Throwable) {
                null
            }
        }
    }

    // Windows DWM attribute constants
    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_BORDER_COLOR = 34
    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_TEXT_COLOR = 36

    fun applyTheme(window: Window, colorScheme: ColorScheme, isDark: Boolean) {
        val osName = System.getProperty("os.name", "").lowercase()
        if (!osName.contains("win")) return

        val dwmapi = Dwmapi.INSTANCE ?: return

        try {
            val hwnd = Native.getWindowPointer(window) ?: return

            // 1. Dark Mode for caption buttons (minimize, maximize, close)
            val darkModeMem = Memory(4).apply {
                setInt(0, if (isDark) 1 else 0)
            }
            dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeMem, 4)

            // 2. Caption Background Color (DWMWA_CAPTION_COLOR = 35, Win 11 Build 22000+)
            // COLORREF format is 0x00BBGGRR
            val captionColorRef = toColorRef(colorScheme.surface)
            val captionMem = Memory(4).apply {
                setInt(0, captionColorRef)
            }
            dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, captionMem, 4)

            // 3. Caption Text Color (DWMWA_TEXT_COLOR = 36, Win 11 Build 22000+)
            val textColorRef = toColorRef(colorScheme.onSurface)
            val textMem = Memory(4).apply {
                setInt(0, textColorRef)
            }
            dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, textMem, 4)

            // 4. Subtle Window Border Color (DWMWA_BORDER_COLOR = 34, Win 11 Build 22000+)
            val borderColorRef = toColorRef(colorScheme.outline.copy(alpha = 0.25f))
            val borderMem = Memory(4).apply {
                setInt(0, borderColorRef)
            }
            dwmapi.DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, borderMem, 4)
        } catch (_: Throwable) {
            // Gracefully ignore on unsupported Windows builds or when native calls fail
        }
    }

    private fun toColorRef(color: Color): Int {
        val argb = color.toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        // Win32 COLORREF format: 0x00BBGGRR
        return (b shl 16) or (g shl 8) or r
    }
}
