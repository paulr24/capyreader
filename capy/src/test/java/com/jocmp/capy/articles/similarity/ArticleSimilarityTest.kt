package com.jocmp.capy.articles.similarity

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleSimilarityTest {

    @Test
    fun `exact match returns 1_0`() {
        val title = "Sony announces PlayStation 5 Pro"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(title, title)
        assertEquals(1.0f, similarity)
    }

    @Test
    fun `outlet brand suffix is stripped effectively`() {
        val titleA = "Sony announces PlayStation 5 Pro"
        val titleB = "Sony announces PlayStation 5 Pro - The Verge"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertEquals(1.0f, similarity)
    }

    @Test
    fun `pipe outlet suffix is stripped effectively`() {
        val titleA = "Nintendo Direct announced for June 18"
        val titleB = "Nintendo Direct announced for June 18 | IGN"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertEquals(1.0f, similarity)
    }

    @Test
    fun `similar headlines have high similarity score`() {
        val titleA = "Nintendo officially announces the Switch 2 with backward compatibility"
        val titleB = "Nintendo announces Switch 2 with backwards compatibility"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertTrue(similarity >= 0.85f, "Expected similarity >= 0.85, got $similarity")
    }

    @Test
    fun `dissimilar headlines have low similarity score`() {
        val titleA = "Apple releases iOS 18 with new customization features"
        val titleB = "Sony reveals PlayStation 5 Pro pricing and release date"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertTrue(similarity < 0.30f, "Expected similarity < 0.30, got $similarity")
    }

    @Test
    fun `matchesBypassKeywords detects review and preview keywords`() {
        val bypass = setOf("Review", "Reviews", "Preview", "Impressions")

        assertTrue(ArticleSimilarity.matchesBypassKeywords("Death Stranding 2 Review: Kojima's Wildest Vision", bypass))
        assertTrue(ArticleSimilarity.matchesBypassKeywords("Review: Zelda Echoes of Wisdom", bypass))
        assertTrue(ArticleSimilarity.matchesBypassKeywords("Silent Hill 2 Remake - Hands-on Preview", bypass))
        assertTrue(ArticleSimilarity.matchesBypassKeywords("Early Impressions of the PS5 Pro", bypass))
        assertFalse(ArticleSimilarity.matchesBypassKeywords("Sony announces PS5 Pro console", bypass))
    }

    @Test
    fun `conflicting numbers return 0 similarity even if rest of title matches`() {
        val titleA = "Daily News Stuff 14 September 2026"
        val titleB = "Daily News Stuff 12 September 2026"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertEquals(0.0f, similarity, "Different edition numbers/dates should have 0 similarity")
    }

    @Test
    fun `ordinal dates with conflicting days return 0 similarity`() {
        val titleA = "Daily Roundup September 14th"
        val titleB = "Daily Roundup September 12th"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertEquals(0.0f, similarity)
    }

    @Test
    fun `conflicting episode numbers return 0 similarity`() {
        val titleA = "The Daily Show Episode 104: AI Special"
        val titleB = "The Daily Show Episode 105: AI Special"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertEquals(0.0f, similarity)
    }

    @Test
    fun `non conflicting numbers allow duplicate detection`() {
        val titleA = "Sony announces PS5 Pro for $700"
        val titleB = "Sony announces PS5 Pro"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertTrue(similarity >= 0.70f, "Non-conflicting detail should allow match, got $similarity")
    }

    @Test
    fun `conflicting days of the week return 0 similarity`() {
        val titleA = "Morning Briefing - Monday Edition"
        val titleB = "Morning Briefing - Tuesday Edition"
        val similarity = ArticleSimilarity.calculateTitleSimilarity(titleA, titleB)
        assertEquals(0.0f, similarity)
    }
}
