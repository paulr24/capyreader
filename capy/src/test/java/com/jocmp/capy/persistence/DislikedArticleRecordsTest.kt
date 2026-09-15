package com.jocmp.capy.persistence

import com.jocmp.capy.InMemoryDatabaseProvider
import com.jocmp.capy.common.TimeHelpers.nowUTC
import com.jocmp.capy.db.Database
import com.jocmp.capy.fixtures.ArticleFixture
import com.jocmp.capy.fixtures.FeedFixture
import com.jocmp.capy.stats.PruneReason
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DislikedArticleRecordsTest {
    private lateinit var database: Database
    private lateinit var records: DislikedArticleRecords
    private lateinit var feedFixture: FeedFixture
    private lateinit var articleFixture: ArticleFixture

    @Before
    fun setup() {
        database = InMemoryDatabaseProvider.build("test_disliked")
        records = DislikedArticleRecords(database)
        feedFixture = FeedFixture(database)
        articleFixture = ArticleFixture(database)
    }

    @Test
    fun add_and_all() {
        records.add(id = "art_1", feedID = "feed_1", articleTitle = "Headline One")
        records.add(id = "art_2", feedID = "feed_1", articleTitle = "Headline Two")

        val all = records.all()
        assertEquals(2, all.size)
        assertEquals("art_2", all[0].id)
        assertEquals("Headline Two", all[0].articleTitle)
        assertEquals("art_1", all[1].id)
    }

    @Test
    fun allActive_filtersByAge() {
        val now = nowUTC()
        records.add(id = "recent", feedID = "feed_1", articleTitle = "Recent Story", createdAt = now.minusDays(2))
        records.add(id = "old", feedID = "feed_1", articleTitle = "Old Story", createdAt = now.minusDays(10))

        val active = records.allActive(since = now.minusDays(7))
        assertEquals(1, active.size)
        assertEquals("recent", active[0].id)
    }

    @Test
    fun delete_removesSpecificArticle() {
        records.add(id = "art_1", feedID = "feed_1", articleTitle = "Story 1")
        records.add(id = "art_2", feedID = "feed_1", articleTitle = "Story 2")

        records.delete("art_1")

        val all = records.all()
        assertEquals(1, all.size)
        assertEquals("art_2", all[0].id)
    }

    @Test
    fun deleteOlderThan_purgesStaleRecords() {
        val now = nowUTC()
        records.add(id = "fresh", feedID = "feed_1", articleTitle = "Fresh Story", createdAt = now.minusDays(5))
        records.add(id = "stale", feedID = "feed_1", articleTitle = "Stale Story", createdAt = now.minusDays(35))

        records.deleteOlderThan(cutoff = now.minusDays(30))

        val all = records.all()
        assertEquals(1, all.size)
        assertEquals("fresh", all[0].id)
    }

    @Test
    fun feedStatistics_includesDislikedCountAndFlagsHighDisinterest() {
        val feed = feedFixture.create()
        val articleRecords = ArticleRecords(database)

        // Create 5 articles for this feed, all read
        val articles = (1..5).map { i ->
            articleFixture.create(feed = feed, title = "Article $i", read = true)
        }

        // Thumb down 2 of them (2 out of 5 = 40% disinterest rate)
        records.add(id = articles[0].id, feedID = feed.id, articleTitle = articles[0].title)
        records.add(id = articles[1].id, feedID = feed.id, articleTitle = articles[1].title)

        val stats = articleRecords.feedStatistics().first { it.feedID == feed.id }
        assertEquals(5L, stats.totalArticles)
        assertEquals(2L, stats.dislikedCount)
        assertEquals(0.40f, stats.disinterestRate)
        assertEquals(PruneReason.HIGH_DISINTEREST, stats.pruneReason())
    }
}
