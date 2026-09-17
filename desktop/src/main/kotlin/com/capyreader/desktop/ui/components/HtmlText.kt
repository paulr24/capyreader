package com.capyreader.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capyreader.desktop.articles.DesktopArticleExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skia.Image as SkiaImage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import kotlin.math.roundToInt

sealed interface ArticleBlock {
    data class Heading(val text: AnnotatedString, val level: Int) : ArticleBlock
    data class Paragraph(val text: AnnotatedString) : ArticleBlock
    data class Blockquote(val text: AnnotatedString) : ArticleBlock
    data class ListBlock(val items: List<AnnotatedString>, val ordered: Boolean) : ArticleBlock
    data class CodeBlock(val code: String) : ArticleBlock
    data class ImageBlock(val src: String, val alt: String? = null, val caption: String? = null) : ArticleBlock
    data object Divider : ArticleBlock
}

@Composable
fun HtmlArticleView(
    html: String,
    fontSize: Int = 16,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val blocks = remember(html, primaryColor, onSurfaceColor) {
        parseHtmlBlocks(html, primaryColor, onSurfaceColor)
    }

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            if (blocks.isEmpty()) {
                val plain = remember(html) { Jsoup.parse(html).text() }
                if (plain.isNotBlank()) {
                    val bodyLineHeight = (fontSize * 1.625f).roundToInt().sp
                    Text(
                        text = plain,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = fontSize.sp,
                            lineHeight = bodyLineHeight,
                            color = onSurfaceColor
                        )
                    )
                }
            } else {
                blocks.forEach { block ->
                    RenderBlock(
                        block = block,
                        fontSize = fontSize,
                        onOpenUrl = { url ->
                            try {
                                uriHandler.openUri(url)
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderBlock(
    block: ArticleBlock,
    fontSize: Int,
    onOpenUrl: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    when (block) {
        is ArticleBlock.Heading -> {
            val (hSize, hLineHeight, topPadding, bottomPadding) = when (block.level) {
                1 -> Quadruple((fontSize * 1.5f).roundToInt().sp, (fontSize * 2.0f).roundToInt().sp, 20.dp, 8.dp)
                2 -> Quadruple((fontSize * 1.3f).roundToInt().sp, (fontSize * 1.75f).roundToInt().sp, 18.dp, 8.dp)
                3 -> Quadruple((fontSize * 1.15f).roundToInt().sp, (fontSize * 1.55f).roundToInt().sp, 16.dp, 6.dp)
                else -> Quadruple(fontSize.sp, (fontSize * 1.45f).roundToInt().sp, 14.dp, 4.dp)
            }
            ClickableText(
                text = block.text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = hSize,
                    lineHeight = hLineHeight,
                    color = onSurfaceColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPadding, bottom = bottomPadding),
                onClick = { offset ->
                    block.text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            onOpenUrl(annotation.item)
                        }
                }
            )
        }
        is ArticleBlock.Paragraph -> {
            val pLineHeight = (fontSize * 1.625f).roundToInt().sp
            ClickableText(
                text = block.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp,
                    lineHeight = pLineHeight,
                    color = onSurfaceColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                onClick = { offset ->
                    block.text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            onOpenUrl(annotation.item)
                        }
                }
            )
        }
        is ArticleBlock.Blockquote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(IntrinsicSize.Min)
                        .background(primaryColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    val qSize = (fontSize * 0.95f).roundToInt().sp
                    val qLineHeight = (fontSize * 1.55f).roundToInt().sp
                    ClickableText(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = qSize,
                            lineHeight = qLineHeight,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(12.dp),
                        onClick = { offset ->
                            block.text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    onOpenUrl(annotation.item)
                                }
                        }
                    )
                }
            }
        }
        is ArticleBlock.ListBlock -> {
            val itemSize = (fontSize * 0.95f).roundToInt().sp
            val itemLineHeight = (fontSize * 1.55f).roundToInt().sp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                block.items.forEachIndexed { index, itemText ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val prefix = if (block.ordered) "${index + 1}." else "•"
                        Text(
                            text = prefix,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                fontSize = itemSize
                            ),
                            lineHeight = itemLineHeight
                        )
                        ClickableText(
                            text = itemText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = itemSize,
                                lineHeight = itemLineHeight,
                                color = onSurfaceColor
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = { offset ->
                                itemText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        onOpenUrl(annotation.item)
                                    }
                            }
                        )
                    }
                }
            }
        }
        is ArticleBlock.CodeBlock -> {
            val codeSize = (fontSize * 0.85f).roundToInt().sp
            val codeLineHeight = (fontSize * 1.4f).roundToInt().sp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(14.dp)
            ) {
                Text(
                    text = block.code,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = codeSize,
                        color = Color(0xFFD4D4D4),
                        lineHeight = codeLineHeight
                    )
                )
            }
        }
        is ArticleBlock.ImageBlock -> {
            AsyncArticleImage(
                src = block.src,
                alt = block.alt,
                caption = block.caption,
                onOpenUrl = onOpenUrl
            )
        }
        ArticleBlock.Divider -> {
            Divider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun AsyncArticleImage(
    src: String,
    alt: String? = null,
    caption: String? = null,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageBitmap by remember(src) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(src) {
        if (src.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(src).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            val skiaImage = SkiaImage.makeFromEncoded(bytes)
                            imageBitmap = skiaImage.toComposeImageBitmap()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    if (imageBitmap != null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = alt ?: caption,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenUrl(src) },
                contentScale = ContentScale.FillWidth
            )
            if (!caption.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun parseHtmlBlocks(
    html: String,
    linkColor: Color,
    textColor: Color
): List<ArticleBlock> {
    if (html.isBlank()) return emptyList()

    val cleanHtml = if (html.contains("<html", ignoreCase = true) || html.contains("<!DOCTYPE", ignoreCase = true)) {
        DesktopArticleExtractor.extract(html)
    } else {
        html
    }

    val doc = Jsoup.parse(cleanHtml)
    val body = doc.body() ?: return emptyList()

    val blocks = mutableListOf<ArticleBlock>()
    val pendingInlineNodes = mutableListOf<Node>()

    fun flushInline() {
        if (pendingInlineNodes.isNotEmpty()) {
            val text = buildAnnotatedStringFromNodes(pendingInlineNodes, linkColor, textColor)
            if (text.isNotBlank()) {
                blocks.add(ArticleBlock.Paragraph(text))
            }
            pendingInlineNodes.clear()
        }
    }

    fun isBlockElement(el: Element): Boolean {
        val tag = el.tagName().lowercase()
        if (tag in listOf("p", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre", "ul", "ol", "hr", "figure", "table", "img")) {
            return true
        }
        if (tag in listOf("div", "section", "article", "main", "header", "footer", "aside")) {
            return el.children().any { isBlockElement(it) }
        }
        return false
    }

    fun processElement(el: Element) {
        val tag = el.tagName().lowercase()
        when {
            tag in listOf("h1", "h2", "h3", "h4", "h5", "h6") -> {
                flushInline()
                val level = tag.substring(1).toIntOrNull() ?: 1
                val text = buildAnnotatedStringFromNodes(listOf(el), linkColor, textColor)
                if (text.isNotBlank()) {
                    blocks.add(ArticleBlock.Heading(text, level))
                }
            }
            tag == "p" -> {
                flushInline()
                val text = buildAnnotatedStringFromNodes(listOf(el), linkColor, textColor)
                if (text.isNotBlank()) {
                    blocks.add(ArticleBlock.Paragraph(text))
                }
            }
            tag == "blockquote" -> {
                flushInline()
                val text = buildAnnotatedStringFromNodes(listOf(el), linkColor, textColor)
                if (text.isNotBlank()) {
                    blocks.add(ArticleBlock.Blockquote(text))
                }
            }
            tag == "pre" -> {
                flushInline()
                val code = el.wholeText().trimEnd()
                if (code.isNotBlank()) {
                    blocks.add(ArticleBlock.CodeBlock(code))
                }
            }
            tag in listOf("ul", "ol") -> {
                flushInline()
                val isOrdered = tag == "ol"
                val items = el.children()
                    .filter { it.tagName().equals("li", ignoreCase = true) }
                    .map { buildAnnotatedStringFromNodes(listOf(it), linkColor, textColor) }
                    .filter { it.isNotBlank() }
                if (items.isNotEmpty()) {
                    blocks.add(ArticleBlock.ListBlock(items, isOrdered))
                }
            }
            tag == "hr" -> {
                flushInline()
                blocks.add(ArticleBlock.Divider)
            }
            tag == "figure" -> {
                flushInline()
                val img = el.selectFirst("img")
                val caption = el.selectFirst("figcaption")?.text()
                if (img != null) {
                    val src = img.attr("abs:src").ifBlank { img.attr("src") }
                    if (src.isNotBlank()) {
                        blocks.add(ArticleBlock.ImageBlock(src = src, alt = img.attr("alt"), caption = caption))
                    }
                }
            }
            tag == "img" -> {
                flushInline()
                val src = el.attr("abs:src").ifBlank { el.attr("src") }
                if (src.isNotBlank()) {
                    blocks.add(ArticleBlock.ImageBlock(src = src, alt = el.attr("alt")))
                }
            }
            tag in listOf("div", "section", "article", "main", "header", "footer", "aside", "body") -> {
                if (isBlockElement(el)) {
                    for (child in el.childNodes()) {
                        when (child) {
                            is Element -> {
                                if (isBlockElement(child)) {
                                    flushInline()
                                    processElement(child)
                                } else {
                                    pendingInlineNodes.add(child)
                                }
                            }
                            is TextNode -> {
                                pendingInlineNodes.add(child)
                            }
                        }
                    }
                    flushInline()
                } else {
                    pendingInlineNodes.add(el)
                }
            }
            else -> {
                pendingInlineNodes.add(el)
            }
        }
    }

    for (child in body.childNodes()) {
        when (child) {
            is Element -> {
                if (isBlockElement(child)) {
                    flushInline()
                    processElement(child)
                } else {
                    pendingInlineNodes.add(child)
                }
            }
            is TextNode -> {
                pendingInlineNodes.add(child)
            }
        }
    }
    flushInline()

    return blocks
}

private fun buildAnnotatedStringFromNodes(
    nodes: List<Node>,
    linkColor: Color,
    textColor: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var lastChar: Char? = null

    fun appendText(str: String) {
        if (str.isEmpty()) return
        builder.append(str)
        lastChar = str.last()
    }

    fun appendNode(curr: Node) {
        when (curr) {
            is TextNode -> {
                val whole = curr.wholeText
                if (whole.isNotEmpty()) {
                    val normalized = whole.replace(Regex("\\s+"), " ")
                    val hasLeadingSpace = whole.first().isWhitespace()
                    val hasTrailingSpace = whole.last().isWhitespace()
                    val trimmed = normalized.trim()

                    if (trimmed.isEmpty()) {
                        if (lastChar != null && !lastChar!!.isWhitespace()) {
                            appendText(" ")
                        }
                    } else {
                        if (hasLeadingSpace && lastChar != null && !lastChar!!.isWhitespace()) {
                            appendText(" ")
                        }
                        appendText(trimmed)
                        if (hasTrailingSpace && lastChar != null && !lastChar!!.isWhitespace()) {
                            appendText(" ")
                        }
                    }
                }
            }
            is Element -> {
                val tag = curr.tagName().lowercase()
                if (tag == "br") {
                    appendText("\n")
                    return
                }

                val isLink = tag == "a" && curr.hasAttr("href")
                val href = curr.attr("href")

                if (isLink) {
                    builder.pushStringAnnotation(tag = "URL", annotation = href)
                    builder.pushStyle(
                        SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                } else if (tag == "b" || tag == "strong") {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                } else if (tag == "i" || tag == "em") {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                } else if (tag == "u") {
                    builder.pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                } else if (tag == "s" || tag == "del" || tag == "strike") {
                    builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                } else if (tag == "code") {
                    builder.pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = textColor.copy(alpha = 0.12f)
                        )
                    )
                }

                curr.childNodes().forEach { appendNode(it) }

                if (isLink) {
                    builder.pop()
                    builder.pop()
                } else if (tag == "b" || tag == "strong" || tag == "i" || tag == "em" || tag == "u" || tag == "s" || tag == "del" || tag == "strike" || tag == "code") {
                    builder.pop()
                }
            }
        }
    }

    nodes.forEach { appendNode(it) }
    return builder.toAnnotatedString()
}
