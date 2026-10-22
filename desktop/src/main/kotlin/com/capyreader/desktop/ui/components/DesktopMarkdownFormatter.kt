package com.capyreader.desktop.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

object DesktopMarkdownFormatter {
    private val INLINE_REGEX = Regex(
        """(\*\*\*(.+?)\*\*\*)|(___([^_]+)___)|(\*\*(.+?)\*\*)|(__([^_]+)__)|(`([^`]+)`)|(\[([^\]]+)\]\(([^)]+)\))|((?<!\*)\*(?!\s)([^*\n]+?)(?<!\s)\*(?!\*))|((?<!_)_(?!\s)([^_\n]+?)(?<!\s)_(?!_))"""
    )
    private val HEADER_REGEX = Regex("""^(#{1,6})\s+(.*)$""")
    private val BULLET_REGEX = Regex("""^(\s*)[*-•]\s+(.*)$""")
    private val NUMBERED_LIST_REGEX = Regex("""^(\s*)(\d+)\.\s+(.*)$""")
    private val BLOCKQUOTE_REGEX = Regex("""^(\s*)>\s*(.*)$""")

    fun parseMarkdown(
        markdown: String,
        linkColor: Color = Color.Unspecified,
        codeBackground: Color = Color.Unspecified,
    ): AnnotatedString {
        if (markdown.isBlank()) {
            return AnnotatedString("")
        }

        return buildAnnotatedString {
            val lines = markdown.lines()
            var inCodeBlock = false

            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("```")) {
                    inCodeBlock = !inCodeBlock
                    if (index < lines.lastIndex) {
                        append("\n")
                    }
                    return@forEachIndexed
                }

                if (inCodeBlock) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                        )
                    ) {
                        append(line)
                    }
                } else {
                    val headerMatch = HEADER_REGEX.find(line)
                    val quoteMatch = BLOCKQUOTE_REGEX.find(line)
                    val bulletMatch = BULLET_REGEX.find(line)
                    val numberedMatch = NUMBERED_LIST_REGEX.find(line)

                    when {
                        headerMatch != null -> {
                            val level = headerMatch.groupValues[1].length
                            val headerContent = headerMatch.groupValues[2]
                            val headerSize = when (level) {
                                1 -> 1.25.em
                                2 -> 1.15.em
                                3 -> 1.08.em
                                else -> 1.0.em
                            }
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = headerSize,
                                )
                            ) {
                                appendInline(headerContent, linkColor, codeBackground)
                            }
                        }

                        quoteMatch != null -> {
                            val indent = quoteMatch.groupValues[1]
                            val quoteContent = quoteMatch.groupValues[2]
                            append("$indent“ ")
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                appendInline(quoteContent, linkColor, codeBackground)
                            }
                            append(" ”")
                        }

                        bulletMatch != null -> {
                            val indent = bulletMatch.groupValues[1]
                            val content = bulletMatch.groupValues[2]
                            append("$indent• ")
                            appendInline(content, linkColor, codeBackground)
                        }

                        numberedMatch != null -> {
                            val indent = numberedMatch.groupValues[1]
                            val number = numberedMatch.groupValues[2]
                            val content = numberedMatch.groupValues[3]
                            append(indent)
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("$number. ")
                            }
                            appendInline(content, linkColor, codeBackground)
                        }

                        else -> {
                            appendInline(line, linkColor, codeBackground)
                        }
                    }
                }

                if (index < lines.lastIndex) {
                    append("\n")
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendInline(
        text: String,
        linkColor: Color = Color.Unspecified,
        codeBackground: Color = Color.Unspecified,
    ) {
        var currentIndex = 0
        val matches = INLINE_REGEX.findAll(text)

        for (match in matches) {
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }

            when {
                // ***bold italic*** -> group 2
                match.groups[2] != null -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append(match.groups[2]!!.value)
                    }
                }
                // ___bold italic___ -> group 4
                match.groups[4] != null -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        )
                    ) {
                        append(match.groups[4]!!.value)
                    }
                }
                // **bold** -> group 6
                match.groups[6] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[6]!!.value)
                    }
                }
                // __bold__ -> group 8
                match.groups[8] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[8]!!.value)
                    }
                }
                // `code` -> group 10
                match.groups[10] != null -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground,
                        )
                    ) {
                        append(match.groups[10]!!.value)
                    }
                }
                // [label](url) -> group 12 (label) & group 13 (url)
                match.groups[12] != null -> {
                    withStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        )
                    ) {
                        append(match.groups[12]!!.value)
                    }
                }
                // *word* -> group 15: Emphasize with bold
                match.groups[15] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[15]!!.value)
                    }
                }
                // _word_ -> group 17: Italic
                match.groups[17] != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groups[17]!!.value)
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
            cleaned = cleaned.replace(BLOCKQUOTE_REGEX, "$2")
            cleaned = cleaned.replace(Regex("""^\s*[*-•]\s+"""), "")
            cleaned = cleaned.replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "$1")
            cleaned = cleaned.replace(Regex("""\*{1,3}(.+?)\*{1,3}"""), "$1")
            cleaned = cleaned.replace(Regex("""_{1,3}(.+?)_{1,3}"""), "$1")
            cleaned = cleaned.replace(Regex("""`([^`]+)`"""), "$1")
            cleaned = cleaned.replace(Regex("""^```.*$"""), "")
            cleaned.trim()
        }.trim()
    }
}
