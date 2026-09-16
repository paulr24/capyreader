package com.jocmp.capy.stats

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedHealthStatsTest {

    private val now = 1_700_000_000L // arbitrary fixed epoch timestamp

    @Test
    fun `calculates read rate and read percentage accurately`() {
        val stats = FeedHealthStats(
            feedID = "feed-1",
            title = "Tech Crunch",
            feedURL = "https://techcrunch.com/feed",
            siteURL = "https://techcrunch.com",
            faviconURL = null,
            totalArticles = 100,
            readArticles = 25,
            unreadArticles = 75,
            starredArticles = 2,
            mostRecentArticleAt = now - 3600,
            oldestArticleAt = now - (30 * 86400),
            lastReadAt = now - 7200
        )

        assertEquals(0.25f, stats.readRate)
        assertEquals(25, stats.readPercentage)
    }

    @Test
    fun `zero articles yields zero read rate`() {
        val stats = FeedHealthStats(
            feedID = "feed-empty",
            title = "Empty Feed",
            feedURL = "https://empty.org/rss",
            siteURL = null,
            faviconURL = null,
            totalArticles = 0,
            readArticles = 0,
            unreadArticles = 0,
            starredArticles = 0,
            mostRecentArticleAt = null,
            oldestArticleAt = null,
            lastReadAt = null
        )

        assertEquals(0.0f, stats.readRate)
        assertEquals(0, stats.readPercentage)
        assertNull(stats.daysSinceLastPost(now))
        assertNull(stats.daysSinceLastRead(now))
        assertFalse(stats.isRecommendedForPruning)
    }

    @Test
    fun `flags dead feed if no posts in over 180 days`() {
        val stats = FeedHealthStats(
            feedID = "feed-dead",
            title = "Abandoned Blog",
            feedURL = "https://oldblog.com/feed",
            siteURL = null,
            faviconURL = null,
            totalArticles = 20,
            readArticles = 15,
            unreadArticles = 5,
            starredArticles = 1,
            mostRecentArticleAt = now - (200 * 86400L),
            oldestArticleAt = now - (500 * 86400L),
            lastReadAt = now - (210 * 86400L)
        )

        assertEquals(PruneReason.DEAD_FEED, stats.pruneReason(now))
        assertTrue(stats.isRecommendedForPruning)
        assertEquals(200L, stats.daysSinceLastPost(now))
    }

    @Test
    fun `flags high noise firehose feed with very low read rate`() {
        val stats = FeedHealthStats(
            feedID = "feed-firehose",
            title = "Breaking News Firehose",
            feedURL = "https://firehose.com/rss",
            siteURL = null,
            faviconURL = null,
            totalArticles = 100,
            readArticles = 3,
            unreadArticles = 97,
            starredArticles = 0,
            mostRecentArticleAt = now - 3600,
            oldestArticleAt = now - (7 * 86400L), // 100 articles in 1 week
            lastReadAt = now - (2 * 86400L)
        )

        assertEquals(PruneReason.FIREHOSE_LOW_READ, stats.pruneReason(now))
        assertTrue(stats.isRecommendedForPruning)
        assertTrue(stats.weeklyVolume > 50f)
        assertEquals(3, stats.readPercentage)
    }

    @Test
    fun `flags dormant feed if neglected for over 45 days`() {
        val stats = FeedHealthStats(
            feedID = "feed-dormant",
            title = "Unread Newsletter",
            feedURL = "https://newsletter.com/feed",
            siteURL = null,
            faviconURL = null,
            totalArticles = 15,
            readArticles = 1,
            unreadArticles = 14,
            starredArticles = 0,
            mostRecentArticleAt = now - (2 * 86400L),
            oldestArticleAt = now - (60 * 86400L),
            lastReadAt = now - (50 * 86400L) // last read 50 days ago
        )

        assertEquals(PruneReason.DORMANT, stats.pruneReason(now))
        assertTrue(stats.isRecommendedForPruning(now))
    }

    @Test
    fun `does not flag feed as dormant or firehose if it has starred articles`() {
        val stats = FeedHealthStats(
            feedID = "feed-starred",
            title = "Important Feed",
            feedURL = "https://important.org/rss",
            siteURL = null,
            faviconURL = null,
            totalArticles = 20,
            readArticles = 1,
            unreadArticles = 19,
            starredArticles = 2, // User saved articles here!
            mostRecentArticleAt = now - (5 * 86400L),
            oldestArticleAt = now - (60 * 86400L),
            lastReadAt = now - (50 * 86400L)
        )

        assertNull(stats.pruneReason(now))
        assertFalse(stats.isRecommendedForPruning(now))
    }
}
