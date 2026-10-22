package com.capyreader.app.ui.components

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFormatterTest {

    @Test
    fun `parseMarkdown formats bold headers without asterisks`() {
        val input = "**Summary**\nThis is a summary.\n\n**Key Takeaways**"
        val result = MarkdownFormatter.parseMarkdown(input)

        assertEquals("Summary\nThis is a summary.\n\nKey Takeaways", result.text)

        // Verify "Summary" is bold
        val summarySpan = result.spanStyles.find { it.start == 0 && it.end == 7 }
        assertEquals(FontWeight.Bold, summarySpan?.item?.fontWeight)

        // Verify "Key Takeaways" is bold
        val takeawaysSpan = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold && it.start > 10 }
        assertTrue(takeawaysSpan != null)
    }

    @Test
    fun `parseMarkdown formats single asterisk words as bold without raw asterisks`() {
        val input = "This is a *crucial* point for *readers*."
        val result = MarkdownFormatter.parseMarkdown(input)

        assertEquals("This is a crucial point for readers.", result.text)

        val boldSpans = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(2, boldSpans.size)

        assertEquals("crucial", result.text.substring(boldSpans[0].start, boldSpans[0].end))
        assertEquals("readers", result.text.substring(boldSpans[1].start, boldSpans[1].end))
    }

    @Test
    fun `parseMarkdown formats triple asterisks as bold and italic without raw asterisks`() {
        val input = "A ***groundbreaking*** achievement."
        val result = MarkdownFormatter.parseMarkdown(input)

        assertEquals("A groundbreaking achievement.", result.text)

        val span = result.spanStyles.find { it.start == 2 && it.end == 16 }
        assertTrue(span != null)
        assertEquals(FontWeight.Bold, span?.item?.fontWeight)
        assertEquals(FontStyle.Italic, span?.item?.fontStyle)
    }

    @Test
    fun `parseMarkdown converts asterisk and hyphen bullets to bullet points`() {
        val input = "* First point\n- Second point\n  * Nested point"
        val result = MarkdownFormatter.parseMarkdown(input)

        assertEquals("• First point\n• Second point\n  • Nested point", result.text)
    }

    @Test
    fun `parseMarkdown formats bold inside bullet points`() {
        val input = "* **Point 1:** A very **important** detail"
        val result = MarkdownFormatter.parseMarkdown(input)

        assertEquals("• Point 1: A very important detail", result.text)

        val boldSpans = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(2, boldSpans.size)

        // "Point 1:" should be bold
        assertEquals("Point 1:", result.text.substring(boldSpans[0].start, boldSpans[0].end))
        // "important" should be bold
        assertEquals("important", result.text.substring(boldSpans[1].start, boldSpans[1].end))
    }

    @Test
    fun `parseMarkdown formats numbered lists with bold numbers`() {
        val input = "1. Introduction\n2. Implementation"
        val result = MarkdownFormatter.parseMarkdown(input)

        assertEquals("1. Introduction\n2. Implementation", result.text)

        val boldSpans = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(2, boldSpans.size)
        assertEquals("1. ", result.text.substring(boldSpans[0].start, boldSpans[0].end))
    }

    @Test
    fun `parseMarkdown formats markdown headers as bold`() {
        val input = "### Overview\nContent"
        val result = MarkdownFormatter.parseMarkdown(input)

        assertEquals("Overview\nContent", result.text)
        val boldSpan = result.spanStyles.find { it.start == 0 && it.end == 8 }
        assertEquals(FontWeight.Bold, boldSpan?.item?.fontWeight)
    }

    @Test
    fun `parseMarkdown formats markdown links without showing url`() {
        val input = "Visit [Google](https://google.com) for more."
        val result = MarkdownFormatter.parseMarkdown(input)

        assertEquals("Visit Google for more.", result.text)
        val linkSpan = result.spanStyles.find { it.start == 6 && it.end == 12 }
        assertTrue(linkSpan != null)
        assertEquals(TextDecoration.Underline, linkSpan?.item?.textDecoration)
    }

    @Test
    fun `cleanMarkdownForSpeech strips asterisks and bullet markers`() {
        val input = """
            **Summary**
            This is a summary overview.

            **Key Takeaways**
            * **Performance:** 10x faster
            * Simple setup
            * Refer to [Docs](https://example.com) for details
        """.trimIndent()

        val speech = MarkdownFormatter.cleanMarkdownForSpeech(input)

        assertEquals(
            "Summary\nThis is a summary overview.\n\nKey Takeaways\nPerformance: 10x faster\nSimple setup\nRefer to Docs for details",
            speech
        )
    }
}
