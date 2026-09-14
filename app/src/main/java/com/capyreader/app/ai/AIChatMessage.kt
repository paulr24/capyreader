package com.capyreader.app.ai

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class AIChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)
