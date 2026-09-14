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
}
