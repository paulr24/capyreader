package com.capyreader.desktop.ai

data class DesktopChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
