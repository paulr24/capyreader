package com.capyreader.app.ai

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class ArticleSummaryRepositoryTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private lateinit var repository: ArticleSummaryRepository

    @Before
    fun setUp() {
        repository = ArticleSummaryRepository(context)
    }

    @Test
    fun get_returnsNullWhenEmpty() = runTest {
        assertNull(repository.get("non-existent-article"))
    }

    @Test
    fun putAndGet_storesAndRetrievesSummary() = runTest {
        val articleId = "article-123"
        val summary = "This is an AI generated summary."

        repository.put(articleId, summary)

        assertEquals(summary, repository.get(articleId))
    }

    @Test
    fun remove_deletesSummary() = runTest {
        val articleId = "article-456"
        val summary = "Summary to be deleted"

        repository.put(articleId, summary)
        assertEquals(summary, repository.get(articleId))

        repository.remove(articleId)
        assertNull(repository.get(articleId))
    }

    @Test
    fun clearAll_purgesAllSummaries() = runTest {
        repository.put("a1", "Summary 1")
        repository.put("a2", "Summary 2")

        assertEquals("Summary 1", repository.get("a1"))
        assertEquals("Summary 2", repository.get("a2"))

        repository.clearAll()

        assertNull(repository.get("a1"))
        assertNull(repository.get("a2"))
    }
}
