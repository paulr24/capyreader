package com.capyreader.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object MarkdownFormatter {
    private val INLINE_REGEX = Regex(
        """(\*\*(.+?)\*\*)|(__([^_]+)__)|(`([^`]+)`)|((?<!\*)\*(?!\*)([^*\n]+)(?<!\*)\*(?!\*))|((?<!_)_(?!_)([^_\n]+)(?<!_)_(?!_))"""
    )
    private val HEADER_REGEX = Regex("""^(#{1,6})\s+(.*)$""")
    private val BULLET_REGEX = Regex("""^(\s*)[*-]\s+(.*)$""")

    fun parseMarkdown(markdown: String): AnnotatedString {
        if (markdown.isBlank()) {
            return AnnotatedString("")
        }

        return buildAnnotatedString {
            val lines = markdown.lines()
            lines.forEachIndexed { index, line ->
                val headerMatch = HEADER_REGEX.find(line)
                if (headerMatch != null) {
                    val headerContent = headerMatch.groupValues[2]
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInline(headerContent)
                    }
                } else {
                    val bulletMatch = BULLET_REGEX.find(line)
                    val processedLine = if (bulletMatch != null) {
                        val indent = bulletMatch.groupValues[1]
                        val content = bulletMatch.groupValues[2]
                        "$indent• $content"
                    } else {
                        line
                    }

                    appendInline(processedLine)
                }

                if (index < lines.lastIndex) {
                    append("\n")
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendInline(text: String) {
        var currentIndex = 0
        val matches = INLINE_REGEX.findAll(text)

        for (match in matches) {
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }

            when {
                // **bold** -> group 2
                match.groups[2] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[2]!!.value)
                    }
                }
                // __bold__ -> group 4
                match.groups[4] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[4]!!.value)
                    }
                }
                // `code` -> group 6
                match.groups[6] != null -> {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(match.groups[6]!!.value)
                    }
                }
                // *italic* -> group 8
                match.groups[8] != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groups[8]!!.value)
                    }
                }
                // _italic_ -> group 10
                match.groups[10] != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groups[10]!!.value)
                    }
                }
            }

            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }

    fun cleanMarkdownForSpeech(markdown: String): String {
        if (markdown.isBlank()) return ""

        return markdown.lines().joinToString("\n") { line ->
            var cleaned = line.replace(HEADER_REGEX, "$2")
            cleaned = cleaned.replace(Regex("""^\s*[*-•]\s+"""), "")
            cleaned = cleaned.replace(Regex("""\*{1,3}(.+?)\*{1,3}"""), "$1")
            cleaned = cleaned.replace(Regex("""_{1,3}(.+?)_{1,3}"""), "$1")
            cleaned = cleaned.replace(Regex("""`([^`]+)`"""), "$1")
            cleaned.trim()
        }.trim()
    }
}
