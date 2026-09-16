package com.capyreader.desktop.storage

import com.jocmp.capy.ArticleFilter
import com.jocmp.capy.ArticleStatus
import com.jocmp.capy.articles.SortOrder
import com.jocmp.capy.preferences.Preference
import com.jocmp.capy.preferences.PreferenceStore
import com.jocmp.capy.preferences.getEnum
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DesktopPreferences(
    val preferenceStore: PreferenceStore = DesktopPreferenceStore(DesktopPaths.appPrefsFile)
) {
    val accountID: Preference<String>
        get() = preferenceStore.getString("account_id", "")

    val isLoggedIn: Boolean
        get() = accountID.get().isNotBlank()

    val filter: Preference<ArticleFilter>
        get() = preferenceStore.getObject(
            key = "article_filter",
            defaultValue = ArticleFilter.default(),
            serializer = { Json.encodeToString(it) },
            deserializer = {
                try {
                    Json.decodeFromString(it)
                } catch (_: Throwable) {
                    ArticleFilter.default()
                }
            }
        )

    val sortOrder: Preference<SortOrder>
        get() = preferenceStore.getEnum("sort_order", SortOrder.NEWEST_FIRST)

    val articleStatus: Preference<ArticleStatus>
        get() = preferenceStore.getEnum("article_status", ArticleStatus.UNREAD)
}
