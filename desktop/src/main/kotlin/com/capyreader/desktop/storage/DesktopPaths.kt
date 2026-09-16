package com.capyreader.desktop.storage

import java.io.File

object DesktopPaths {
    val dataDir: File by lazy {
        val appData = System.getenv("APPDATA")
        val base = if (!appData.isNullOrBlank()) {
            File(appData, "CapyReader")
        } else {
            File(System.getProperty("user.home", "."), ".capyreader")
        }
        base.apply { mkdirs() }
    }

    val accountsDir: File get() = File(dataDir, "accounts").apply { mkdirs() }
    val cacheDir: File get() = File(dataDir, "cache").apply { mkdirs() }
    val dbDir: File get() = File(dataDir, "db").apply { mkdirs() }
    val prefsDir: File get() = File(dataDir, "prefs").apply { mkdirs() }
    val appPrefsFile: File get() = File(prefsDir, "app.properties")
}
