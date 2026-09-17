package com.capyreader.desktop.storage

import com.jocmp.capy.ArticleFilter
import com.jocmp.capy.ArticleStatus
import com.jocmp.capy.articles.FontSize
import com.jocmp.capy.articles.SortOrder
import com.jocmp.capy.preferences.Preference
import com.jocmp.capy.preferences.PreferenceStore
import com.jocmp.capy.preferences.getEnum
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DesktopPreferences(
    val preferenceStore: PreferenceStore = DesktopPreferenceStore(DesktopPaths.appPrefsFile)
) {
    val accountID: Preference<String> =
        preferenceStore.getString("account_id", "")

    val isLoggedIn: Boolean
        get() = accountID.get().isNotBlank()

    val filter: Preference<ArticleFilter> =
        preferenceStore.getObject(
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

    val sortOrder: Preference<SortOrder> =
        preferenceStore.getEnum("sort_order", SortOrder.NEWEST_FIRST)

    val articleStatus: Preference<ArticleStatus> =
        preferenceStore.getEnum("article_status", ArticleStatus.UNREAD)

    val aiOptions: DesktopAIOptions = DesktopAIOptions(preferenceStore)

    val themeMode: Preference<DesktopThemeMode> =
        preferenceStore.getEnum("app_theme_mode", DesktopThemeMode.default)

    val fontFamily: Preference<DesktopFontFamily> =
        preferenceStore.getEnum("app_font_family", DesktopFontFamily.default)

    val fontSize: Preference<Int> =
        preferenceStore.getInt("article_font_size", FontSize.DEFAULT)

    val accentColor: Preference<String> =
        preferenceStore.getString("app_accent_color", "")

    val enableStickyFullContent: Preference<Boolean> =
        preferenceStore.getBoolean("enable_sticky_full_content", true)

    val stickyFullContentScope: Preference<DesktopStickyFullContentScope> =
        preferenceStore.getEnum("sticky_full_content_scope", DesktopStickyFullContentScope.default)
}

enum class DesktopStickyFullContentScope {
    PER_FEED,
    ALL_FEEDS;

    companion object {
        val default = PER_FEED
    }
}
