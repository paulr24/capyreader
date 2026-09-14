package com.capyreader.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AIChatMessageTest {
    @Test
    fun messageCreation_createsValidUserMessage() {
        val message = AIChatMessage(
            isUser = true,
            text = "What is this article about?",
        )

        assertNotNull(message.id)
        assertTrue(message.isUser)
        assertEquals("What is this article about?", message.text)
        assertTrue(message.timestamp > 0)
    }

    @Test
    fun messageCreation_createsValidAiMessage() {
        val message = AIChatMessage(
            isUser = false,
            text = "This article covers the latest tech announcements.",
        )

        assertNotNull(message.id)
        assertEquals(false, message.isUser)
        assertEquals("This article covers the latest tech announcements.", message.text)
    }
}
