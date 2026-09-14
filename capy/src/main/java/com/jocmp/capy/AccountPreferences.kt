package com.jocmp.capy

import com.jocmp.capy.accounts.AutoDelete
import com.jocmp.capy.accounts.MaxArticles
import com.jocmp.capy.accounts.Source
import com.jocmp.capy.articles.similarity.DeduplicationMatch
import com.jocmp.capy.common.TimeHelpers
import com.jocmp.capy.preferences.Preference
import com.jocmp.capy.preferences.PreferenceStore
import com.jocmp.capy.preferences.getEnum
import kotlinx.serialization.json.Json

class AccountPreferences(
    private val store: PreferenceStore,
) {
    val source: Preference<Source>
        get() = store.getEnum("source", Source.LOCAL)

    val username: Preference<String>
        get() = store.getString("username", "")

    val url: Preference<String>
        get() = store.getString("api_url", "")

    val clientCertAlias: Preference<String>
        get() = store.getString("client_cert_alias", "")

    val password: Preference<String>
        get() = store.getString("password", "")

    val autoDelete: Preference<AutoDelete>
        get() = store.getEnum("auto_delete_articles", AutoDelete.default)

    val maxArticles: Preference<MaxArticles>
        get() = store.getEnum("max_unread_articles", MaxArticles.default)

    val filterKeywords: Preference<Set<String>>
        get() = store.getStringSet("keyword_blocklist")

    val canSaveArticleExternally: Preference<Boolean>
        get() = store.getBoolean("can_save_article_externally", false)

    val lastRefreshedAt: Preference<Long>
        get() = store.getLong("last_refreshed_at", 0L)

    val deduplicationEnabled: Preference<Boolean>
        get() = store.getBoolean("deduplication_enabled", true)

    val deduplicationThreshold: Preference<Int>
        get() = store.getInt("deduplication_threshold", 90)

    val deduplicationTimeWindowHours: Preference<Int>
        get() = store.getInt("deduplication_time_window_hours", 48)

    val deduplicationBypassWords: Preference<Set<String>>
        get() = store.getStringSet(
            "deduplication_bypass_words",
            setOf("Review", "Reviews", "Preview", "Impressions")
        )

    val preferredFeedIDs: Preference<List<String>>
        get() = store.getObject(
            key = "preferred_feed_ids",
            defaultValue = emptyList(),
            serializer = { Json.encodeToString(it) },
            deserializer = {
                try {
                    Json.decodeFromString<List<String>>(it)
                } catch (e: Throwable) {
                    emptyList()
                }
            }
        )

    val recentDeduplicationMatches: Preference<List<DeduplicationMatch>>
        get() = store.getObject(
            key = "recent_deduplication_matches",
            defaultValue = emptyList(),
            serializer = { Json.encodeToString(it) },
            deserializer = {
                try {
                    Json.decodeFromString<List<DeduplicationMatch>>(it)
                } catch (e: Throwable) {
                    emptyList()
                }
            }
        )

    suspend fun touchLastRefreshedAt() {
        lastRefreshedAt.set(TimeHelpers.nowUTC().toEpochSecond())
    }
}
