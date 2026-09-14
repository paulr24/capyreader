package com.jocmp.capy.articles.similarity

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleDeduplicatorTest {

    @Test
    fun `deduplicates similar articles keeping the preferred source`() {
        val now = 100_000L
        val candidates = listOf(
            ArticleCandidate(
                id = "art-ign",
                feedID = "feed-ign",
                title = "Sony announces PS5 Pro for $700",
                publishedAt = now,
                feedTitle = "IGN"
            ),
            ArticleCandidate(
                id = "art-verge",
                feedID = "feed-verge",
                title = "Sony announces PS5 Pro priced at $700",
                publishedAt = now - 3600, // 1 hour earlier
                feedTitle = "The Verge"
            )
        )

        // With Verge as preferred feed, Verge is kept, IGN is marked as read
        val result = ArticleDeduplicator.findDuplicates(
            candidates = candidates,
            preferredFeedIDs = listOf("feed-verge", "feed-ign"),
            similarityThreshold = 0.80f,
        )

        assertEquals(listOf("art-ign"), result.duplicateIDs)
        assertEquals(1, result.matches.size)
        assertEquals("art-verge", result.matches[0].keptArticleId)
        assertEquals("art-ign", result.matches[0].duplicateArticleId)
        assertEquals("The Verge", result.matches[0].keptFeedTitle)
        assertEquals("IGN", result.matches[0].duplicateFeedTitle)
    }

    @Test
    fun `deduplicates similar articles keeping the newest when no preferred feed`() {
        val now = 100_000L
        val candidates = listOf(
            ArticleCandidate(
                id = "art-newer",
                feedID = "feed-a",
                title = "Nintendo announces Switch 2 with backwards compatibility",
                publishedAt = now,
                feedTitle = "Feed A"
            ),
            ArticleCandidate(
                id = "art-older",
                feedID = "feed-b",
                title = "Nintendo announces Switch 2 with backward compatibility",
                publishedAt = now - 1800,
                feedTitle = "Feed B"
            )
        )

        val result = ArticleDeduplicator.findDuplicates(
            candidates = candidates,
            preferredFeedIDs = emptyList(),
            similarityThreshold = 0.80f,
        )

        assertEquals(listOf("art-older"), result.duplicateIDs)
        assertEquals(1, result.matches.size)
        assertEquals("art-newer", result.matches[0].keptArticleId)
    }

    @Test
    fun `bypass keywords prevent deduplication`() {
        val now = 100_000L
        val candidates = listOf(
            ArticleCandidate(
                id = "art-review-1",
                feedID = "feed-ign",
                title = "Death Stranding 2 Review: Kojima's Wildest Vision",
                publishedAt = now
            ),
            ArticleCandidate(
                id = "art-review-2",
                feedID = "feed-gamespot",
                title = "Death Stranding 2 Review: A Bizarre Masterpiece",
                publishedAt = now - 1800
            )
        )

        val result = ArticleDeduplicator.findDuplicates(
            candidates = candidates,
            bypassKeywords = setOf("Review", "Reviews"),
            similarityThreshold = 0.50f,
        )

        assertTrue(result.duplicateIDs.isEmpty())
        assertTrue(result.matches.isEmpty())
    }

    @Test
    fun `articles outside time window are not deduplicated`() {
        val now = 100_000L
        val candidates = listOf(
            ArticleCandidate(
                id = "art-today",
                feedID = "feed-a",
                title = "Monthly Community Update",
                publishedAt = now
            ),
            ArticleCandidate(
                id = "art-last-month",
                feedID = "feed-b",
                title = "Monthly Community Update",
                publishedAt = now - (50 * 3600L) // 50 hours ago
            )
        )

        val result = ArticleDeduplicator.findDuplicates(
            candidates = candidates,
            timeWindowSeconds = 48 * 3600L,
            similarityThreshold = 0.90f,
        )

        assertTrue(result.duplicateIDs.isEmpty())
    }

    @Test
    fun `cluster of three keeps one winner and marks two duplicates read`() {
        val now = 100_000L
        val candidates = listOf(
            ArticleCandidate(
                id = "art-1",
                feedID = "feed-1",
                title = "Federal Reserve cuts interest rates by 50 basis points",
                publishedAt = now,
                feedTitle = "Source 1"
            ),
            ArticleCandidate(
                id = "art-2",
                feedID = "feed-fav",
                title = "Fed cuts interest rates by 50 basis points",
                publishedAt = now - 600,
                feedTitle = "Favorite Source"
            ),
            ArticleCandidate(
                id = "art-3",
                feedID = "feed-3",
                title = "Federal Reserve cuts rates by 50 basis points",
                publishedAt = now - 1200,
                feedTitle = "Source 3"
            )
        )

        val result = ArticleDeduplicator.findDuplicates(
            candidates = candidates,
            preferredFeedIDs = listOf("feed-fav"),
            similarityThreshold = 0.75f,
        )

        assertEquals(setOf("art-1", "art-3"), result.duplicateIDs.toSet())
        assertEquals(2, result.matches.size)
        assertTrue(result.matches.all { it.keptArticleId == "art-2" })
    }
}
