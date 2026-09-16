package com.capyreader.desktop.storage

import com.jocmp.capy.AccountPreferences
import com.jocmp.capy.PreferenceStoreProvider
import java.io.File

class DesktopPreferenceStoreProvider(private val prefsDir: File = DesktopPaths.prefsDir) : PreferenceStoreProvider {
    init {
        prefsDir.mkdirs()
    }

    override fun build(accountID: String): AccountPreferences {
        val file = File(prefsDir, "${accountID}.properties")
        return AccountPreferences(DesktopPreferenceStore(file))
    }

    override fun delete(accountID: String) {
        File(prefsDir, "${accountID}.properties").delete()
    }
}
