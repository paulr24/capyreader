package com.capyreader.app.ui.settings.panels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jocmp.capy.Account
import com.jocmp.capy.stats.FeedHealthStats
import com.jocmp.capy.stats.PruneReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FeedSortOrder {
    LOWEST_READ_PERCENTAGE,
    HIGHEST_VOLUME,
    MOST_DORMANT,
    HIGHEST_READ_PERCENTAGE,
    ALPHABETICAL
}

data class FeedHealthOverview(
    val totalFeeds: Int = 0,
    val pruneCandidatesCount: Int = 0,
    val overallReadRate: Float = 0.0f,
    val totalWeeklyVolume: Float = 0.0f,
)

class FeedHealthSettingsViewModel(
    private val account: Account,
) : ViewModel() {
    private val _rawStats = MutableStateFlow<List<FeedHealthStats>>(emptyList())
    private val _sortOrder = MutableStateFlow(FeedSortOrder.LOWEST_READ_PERCENTAGE)
    private val _isLoading = MutableStateFlow(true)
    private val _feedPendingUnsubscribe = MutableStateFlow<FeedHealthStats?>(null)

    val sortOrder: StateFlow<FeedSortOrder> = _sortOrder
    val isLoading: StateFlow<Boolean> = _isLoading
    val feedPendingUnsubscribe: StateFlow<FeedHealthStats?> = _feedPendingUnsubscribe

    init {
        loadStats()
    }

    fun loadStats() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val stats = account.feedStatistics()
                _rawStats.value = stats
            } finally {
                _isLoading.value = false
            }
        }
    }

    val overview: StateFlow<FeedHealthOverview> = _rawStats
        .combine(_sortOrder) { stats, _ ->
            if (stats.isEmpty()) {
                FeedHealthOverview()
            } else {
                val totalArticles = stats.sumOf { it.totalArticles }
                val readArticles = stats.sumOf { it.readArticles }
                val overallRate = if (totalArticles > 0) readArticles.toFloat() / totalArticles.toFloat() else 0f
                val pruneCount = stats.count { it.isRecommendedForPruning }
                val totalVolume = stats.sumOf { it.weeklyVolume.toDouble() }.toFloat()

                FeedHealthOverview(
                    totalFeeds = stats.size,
                    pruneCandidatesCount = pruneCount,
                    overallReadRate = overallRate,
                    totalWeeklyVolume = totalVolume,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedHealthOverview())

    val recommendations: StateFlow<List<FeedHealthStats>> = _rawStats
        .combine(_sortOrder) { stats, _ ->
            stats
                .filter { it.isRecommendedForPruning }
                .sortedWith(
                    compareBy<FeedHealthStats> {
                        when (it.pruneReason()) {
                            PruneReason.DEAD_FEED -> 0
                            PruneReason.HIGH_DISINTEREST -> 1
                            PruneReason.FIREHOSE_LOW_READ -> 2
                            PruneReason.DORMANT -> 3
                            null -> 4
                        }
                    }.thenBy { it.readRate }
                )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortedFeeds: StateFlow<List<FeedHealthStats>> = _rawStats
        .combine(_sortOrder) { stats, sort ->
            when (sort) {
                FeedSortOrder.LOWEST_READ_PERCENTAGE -> stats.sortedWith(
                    compareBy<FeedHealthStats> { it.readRate }
                        .thenByDescending { it.weeklyVolume }
                )
                FeedSortOrder.HIGHEST_VOLUME -> stats.sortedByDescending { it.weeklyVolume }
                FeedSortOrder.MOST_DORMANT -> stats.sortedWith(
                    compareByDescending<FeedHealthStats> {
                        if (it.readArticles == 0L) Long.MAX_VALUE else (it.daysSinceLastRead() ?: 0L)
                    }.thenBy { it.readRate }
                )
                FeedSortOrder.HIGHEST_READ_PERCENTAGE -> stats.sortedByDescending { it.readRate }
                FeedSortOrder.ALPHABETICAL -> stats.sortedBy { it.title.lowercase() }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSortOrder(order: FeedSortOrder) {
        _sortOrder.value = order
    }

    fun requestUnsubscribe(feed: FeedHealthStats) {
        _feedPendingUnsubscribe.value = feed
    }

    fun cancelUnsubscribe() {
        _feedPendingUnsubscribe.value = null
    }

    fun confirmUnsubscribe(onSuccess: (String) -> Unit = {}) {
        val feed = _feedPendingUnsubscribe.value ?: return
        _feedPendingUnsubscribe.value = null

        viewModelScope.launch {
            val feedTitle = feed.title
            val result = account.removeFeed(feed.feedID)
            result.onSuccess {
                _rawStats.value = _rawStats.value.filter { it.feedID != feed.feedID }
                onSuccess(feedTitle)
            }
        }
    }
}
