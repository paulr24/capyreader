package com.capyreader.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

@Composable
fun HtmlArticleView(
    html: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val doc = remember(html) { Jsoup.parse(html) }
    val body = doc.body()

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val children = body.children()
            if (children.isEmpty()) {
                val plainText = body.text()
                if (plainText.isNotBlank()) {
                    Text(
                        text = plainText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                children.forEach { child ->
                    RenderElement(
                        element = child,
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
private fun RenderElement(
    element: Element,
    onOpenUrl: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    when (element.tagName().lowercase()) {
        "h1" -> {
            Text(
                text = element.text(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = onSurfaceColor
            )
        }
        "h2" -> {
            Text(
                text = element.text(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = onSurfaceColor
            )
        }
        "h3", "h4" -> {
            Text(
                text = element.text(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = onSurfaceColor
            )
        }
        "h5", "h6" -> {
            Text(
                text = element.text(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = onSurfaceColor
            )
        }
        "blockquote" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = element.text(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        "pre", "code" -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(12.dp)
            ) {
                Text(
                    text = element.text(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFFD4D4D4),
                        lineHeight = 20.sp
                    )
                )
            }
        }
        "ul" -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                element.children().forEach { li ->
                    Text(
                        text = "•  ${li.text()}",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = onSurfaceColor
                    )
                }
            }
        }
        "ol" -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                element.children().forEachIndexed { index, li ->
                    Text(
                        text = "${index + 1}.  ${li.text()}",
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = onSurfaceColor
                    )
                }
            }
        }
        else -> {
            val annotated = buildAnnotatedStringFromNode(element, primaryColor, onSurfaceColor)
            if (annotated.isNotBlank()) {
                ClickableText(
                    text = annotated,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 26.sp,
                        color = onSurfaceColor
                    ),
                    onClick = { offset ->
                        annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                onOpenUrl(annotation.item)
                            }
                    }
                )
            }
        }
    }
}

private fun buildAnnotatedStringFromNode(
    node: Node,
    linkColor: Color,
    textColor: Color
): AnnotatedString {
    val builder = AnnotatedString.Builder()

    fun appendNode(curr: Node) {
        when (curr) {
            is TextNode -> {
                builder.append(curr.text())
            }
            is Element -> {
                val tag = curr.tagName().lowercase()
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
                } else if (tag == "code") {
                    builder.pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Gray.copy(alpha = 0.2f)
                        )
                    )
                }

                curr.childNodes().forEach { appendNode(it) }

                if (isLink) {
                    builder.pop()
                    builder.pop()
                } else if (tag == "b" || tag == "strong" || tag == "i" || tag == "em" || tag == "code") {
                    builder.pop()
                }
            }
        }
    }

    appendNode(node)
    return builder.toAnnotatedString()
}
