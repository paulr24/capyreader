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
    SANS_SERIF,
    SERIF,
    MONOSPACE;

    companion object {
        val default = SYSTEM_DEFAULT

        val options = listOf(
            SYSTEM_DEFAULT to "System Default",
            SANS_SERIF to "Sans-Serif (Clean & Modern)",
            SERIF to "Serif (Book & Editorial)",
            MONOSPACE to "Monospace (Technical)",
        )
    }
}
