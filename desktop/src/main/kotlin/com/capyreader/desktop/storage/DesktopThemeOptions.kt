package com.capyreader.desktop.storage

enum class DesktopThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    SEPIA,
    BLACK;

    companion object {
        val default = SYSTEM

        val options = listOf(
            SYSTEM to "System Default",
            LIGHT to "Light (Crisp Paper)",
            DARK to "Dark (Warm Charcoal)",
            SEPIA to "Sepia (Warm Reading)",
            BLACK to "Pure Black (OLED)",
        )
    }
}

enum class DesktopFontFamily {
    SYSTEM_DEFAULT,
    LITERATA,
    BOOKERLY,
    GARAMOND,
    INTER,
    ATKINSON_HYPERLEGIBLE,
    JOST,
    POPPINS,
    VOLLKORN,
    SERIF,
    SANS_SERIF,
    MONOSPACE;

    companion object {
        val default = SYSTEM_DEFAULT

        val options = listOf(
            SYSTEM_DEFAULT to "System Default",
            LITERATA to "Literata (Google Books / Editorial)",
            BOOKERLY to "Bookerly (Kindle / Reading)",
            GARAMOND to "Garamond (Classic Serif)",
            INTER to "Inter (Clean & Modern Sans)",
            ATKINSON_HYPERLEGIBLE to "Atkinson Hyperlegible (Accessibility)",
            JOST to "Jost (Futura-inspired Sans)",
            POPPINS to "Poppins (Geometric Sans)",
            VOLLKORN to "Vollkorn (Quiet Serif)",
            SERIF to "System Serif",
            SANS_SERIF to "System Sans-Serif",
            MONOSPACE to "Monospace (Technical)",
        )
    }
}
