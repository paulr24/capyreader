package com.capyreader.app.ui.settings.panels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jocmp.capy.Account
import com.jocmp.capy.DislikedArticle
import com.jocmp.capy.Feed
import com.jocmp.capy.articles.similarity.DeduplicationMatch
import com.jocmp.capy.articles.similarity.DeduplicationResult
import com.jocmp.capy.preferences.getAndSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SimilarArticlesSettingsViewModel(
    val account: Account,
) : ViewModel() {

    private val _mutedArticles = MutableStateFlow<List<DislikedArticle>>(emptyList())
    val mutedArticles: StateFlow<List<DislikedArticle>> = _mutedArticles.asStateFlow()

    init {
        loadMutedArticles()
    }

    var enabled by mutableStateOf(account.preferences.deduplicationEnabled.get())
        private set

    var threshold by mutableStateOf(account.preferences.deduplicationThreshold.get())
        private set

    var timeWindowHours by mutableStateOf(account.preferences.deduplicationTimeWindowHours.get())
        private set

    val bypassWords = account.preferences.deduplicationBypassWords.stateIn(viewModelScope)

    val preferredFeedIDs = account.preferences.preferredFeedIDs.stateIn(viewModelScope)

    val recentMatches = account.preferences.recentDeduplicationMatches.stateIn(viewModelScope)

    val allFeeds = account.allFeeds

    /**
     * Feeds in user's priority order.
     */
    val preferredFeeds = combine(allFeeds, preferredFeedIDs) { feeds, ids ->
        val feedMap = feeds.associateBy { it.id }
        ids.mapNotNull { feedMap[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Subscribed feeds that are not yet in the preferred list.
     */
    val availableFeeds = combine(allFeeds, preferredFeedIDs) { feeds, ids ->
        val idSet = ids.toSet()
        feeds.filter { !idSet.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var isCleaningUp by mutableStateOf(false)
        private set

    fun updateEnabled(value: Boolean) {
        account.preferences.deduplicationEnabled.set(value)
        enabled = value
    }

    fun updateThreshold(value: Int) {
        account.preferences.deduplicationThreshold.set(value)
        threshold = value
    }

    fun updateTimeWindowHours(value: Int) {
        account.preferences.deduplicationTimeWindowHours.set(value)
        timeWindowHours = value
    }

    fun addBypassKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) return
        account.preferences.deduplicationBypassWords.getAndSet { words ->
            words.toMutableSet().apply { add(trimmed) }
        }
    }

    fun removeBypassKeyword(keyword: String) {
        account.preferences.deduplicationBypassWords.getAndSet { words ->
            words.toMutableSet().apply { remove(keyword) }
        }
    }

    fun addPreferredFeed(feed: Feed) {
        account.preferences.preferredFeedIDs.getAndSet { list ->
            if (!list.contains(feed.id)) {
                list + feed.id
            } else {
                list
            }
        }
    }

    fun removePreferredFeed(feedID: String) {
        account.preferences.preferredFeedIDs.getAndSet { list ->
            list.filter { it != feedID }
        }
    }

    fun movePreferredFeedUp(feedID: String) {
        account.preferences.preferredFeedIDs.getAndSet { list ->
            val index = list.indexOf(feedID)
            if (index > 0) {
                val mutable = list.toMutableList()
                val item = mutable.removeAt(index)
                mutable.add(index - 1, item)
                mutable
            } else {
                list
            }
        }
    }

    fun movePreferredFeedDown(feedID: String) {
        account.preferences.preferredFeedIDs.getAndSet { list ->
            val index = list.indexOf(feedID)
            if (index >= 0 && index < list.size - 1) {
                val mutable = list.toMutableList()
                val item = mutable.removeAt(index)
                mutable.add(index + 1, item)
                mutable
            } else {
                list
            }
        }
    }

    fun clearRecentMatches() {
        account.preferences.recentDeduplicationMatches.set(emptyList())
    }

    fun cleanUpNow(onResult: (DeduplicationResult) -> Unit) {
        if (isCleaningUp) return
        isCleaningUp = true
        viewModelScope.launch {
            try {
                val result = account.deduplicateArticles()
                onResult(result)
            } finally {
                isCleaningUp = false
            }
        }
    }

    fun loadMutedArticles() {
        viewModelScope.launch {
            _mutedArticles.value = account.allDislikedArticles()
        }
    }

    fun unmuteArticle(articleID: String) {
        viewModelScope.launch {
            account.undislikeArticle(articleID)
            loadMutedArticles()
        }
    }
}
