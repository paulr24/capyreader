package com.capyreader.desktop.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.capyreader.desktop.storage.DesktopFontFamily
import java.io.File

object DesktopFontLoader {
    private val cache = mutableMapOf<DesktopFontFamily, FontFamily>()

    fun load(family: DesktopFontFamily): FontFamily {
        return cache.getOrPut(family) {
            when (family) {
                DesktopFontFamily.SYSTEM_DEFAULT -> FontFamily.Default
                DesktopFontFamily.SANS_SERIF -> FontFamily.SansSerif
                DesktopFontFamily.SERIF -> FontFamily.Serif
                DesktopFontFamily.MONOSPACE -> FontFamily.Monospace
                DesktopFontFamily.LITERATA -> loadFromResource("font/literata.ttf", "literata") ?: FontFamily.Serif
                DesktopFontFamily.BOOKERLY -> loadFromResource("font/bookerly.ttf", "bookerly") ?: FontFamily.Serif
                DesktopFontFamily.GARAMOND -> loadFromResource("font/garamond.ttf", "garamond")
                    ?: loadFromFile("C:\\Windows\\Fonts\\GARA.TTF", "garamond")
                    ?: FontFamily.Serif
                DesktopFontFamily.INTER -> loadFromResource("font/inter.ttf", "inter") ?: FontFamily.SansSerif
                DesktopFontFamily.ATKINSON_HYPERLEGIBLE -> loadFromResource("font/atkinson_hyperlegible.ttf", "atkinson_hyperlegible") ?: FontFamily.SansSerif
                DesktopFontFamily.JOST -> loadFromResource("font/jost.ttf", "jost") ?: FontFamily.SansSerif
                DesktopFontFamily.POPPINS -> loadFromResource("font/poppins.ttf", "poppins") ?: FontFamily.SansSerif
                DesktopFontFamily.VOLLKORN -> loadFromResource("font/vollkorn.ttf", "vollkorn") ?: FontFamily.Serif
            }
        }
    }

    private fun loadFromResource(resourcePath: String, identity: String): FontFamily? {
        return try {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
                ?: DesktopFontLoader::class.java.classLoader.getResourceAsStream(resourcePath)
                ?: return null
            val bytes = stream.use { it.readBytes() }
            if (bytes.isEmpty()) return null
            val font = androidx.compose.ui.text.platform.Font(
                identity = identity,
                data = bytes,
                weight = FontWeight.Normal,
                style = FontStyle.Normal
            )
            FontFamily(font)
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadFromFile(filePath: String, identity: String): FontFamily? {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return null
            val bytes = file.readBytes()
            if (bytes.isEmpty()) return null
            val font = androidx.compose.ui.text.platform.Font(
                identity = identity,
                data = bytes,
                weight = FontWeight.Normal,
                style = FontStyle.Normal
            )
            FontFamily(font)
        } catch (_: Throwable) {
            null
        }
    }
}
