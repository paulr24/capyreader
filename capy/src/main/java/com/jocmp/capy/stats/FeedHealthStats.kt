package com.jocmp.capy.stats

enum class PruneReason {
    DEAD_FEED,
    DORMANT,
    FIREHOSE_LOW_READ,
    HIGH_DISINTEREST,
}

data class FeedHealthStats(
    val feedID: String,
    val title: String,
    val feedURL: String,
    val siteURL: String?,
    val faviconURL: String?,
    val totalArticles: Long,
    val readArticles: Long,
    val unreadArticles: Long,
    val starredArticles: Long,
    val mostRecentArticleAt: Long?,
    val oldestArticleAt: Long?,
    val lastReadAt: Long?,
    val dislikedCount: Long = 0L,
) {
    val readRate: Float
        get() = if (totalArticles > 0) {
            readArticles.toFloat() / totalArticles.toFloat()
        } else {
            0.0f
        }

    val readPercentage: Int
        get() = (readRate * 100).toInt()

    val weeklyVolume: Float
        get() {
            if (totalArticles <= 1 || mostRecentArticleAt == null || oldestArticleAt == null) {
                return totalArticles.toFloat()
            }
            val timeDiffSeconds = mostRecentArticleAt - oldestArticleAt
            val days = (timeDiffSeconds / 86400L).coerceAtLeast(1L)
            val weeks = days / 7.0f
            return if (weeks > 0.5f) {
                totalArticles / weeks
            } else {
                totalArticles.toFloat()
            }
        }

    val disinterestRate: Float
        get() = if (totalArticles > 0) {
            dislikedCount.toFloat() / totalArticles.toFloat()
        } else {
            0.0f
        }

    fun daysSinceLastPost(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Long? {
        val recent = mostRecentArticleAt ?: return null
        return ((nowEpochSeconds - recent) / 86400L).coerceAtLeast(0L)
    }

    fun daysSinceLastRead(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): Long? {
        if (readArticles == 0L) return null
        val lastRead = lastReadAt ?: return null
        return ((nowEpochSeconds - lastRead) / 86400L).coerceAtLeast(0L)
    }

    fun pruneReason(nowEpochSeconds: Long = System.currentTimeMillis() / 1000L): PruneReason? {
        if (dislikedCount >= 2 && disinterestRate >= 0.20f && starredArticles == 0L) {
            return PruneReason.HIGH_DISINTEREST
        }

        val daysSincePost = daysSinceLastPost(nowEpochSeconds)
        if (daysSincePost != null && daysSincePost >= 180L) {
            return PruneReason.DEAD_FEED
        }

        if (totalArticles >= 10 && weeklyVolume >= 5f && readRate < 0.10f && starredArticles == 0L) {
            return PruneReason.FIREHOSE_LOW_READ
        }

        val daysSinceRead = daysSinceLastRead(nowEpochSeconds)
        if (totalArticles >= 5 && (daysSinceRead == null || daysSinceRead >= 45L) && readRate < 0.15f && starredArticles == 0L) {
            return PruneReason.DORMANT
        }

        return null
    }

    val isRecommendedForPruning: Boolean
        get() = pruneReason() != null
}
