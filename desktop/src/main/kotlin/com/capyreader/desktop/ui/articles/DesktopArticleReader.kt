package com.capyreader.desktop.ui.articles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import com.jocmp.capy.Article
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capyreader.desktop.model.DesktopAccountState
import com.capyreader.desktop.ui.components.HtmlArticleView
import java.awt.Desktop
import java.net.URI
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DesktopArticleReader(
    state: DesktopAccountState,
    onOpenSettings: () -> Unit = {},
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    showSidebarToggle: Boolean = false,
    onToggleSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedArticle by state.selectedArticle.collectAsState()
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        if (selectedArticle == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select an article to read",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val article = selectedArticle!!

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (showBackButton) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to article list",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        if (showSidebarToggle) {
                            IconButton(onClick = onToggleSidebar) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Toggle sidebar feeds",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Column {
                            Text(
                                text = article.feedName.ifBlank { "Feed" },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            val formattedDate = article.publishedAt.format(
                                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mark unread/read
                        IconButton(onClick = { state.toggleRead(article) }) {
                            Icon(
                                imageVector = if (article.read) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead,
                                contentDescription = if (article.read) "Mark unread" else "Mark read",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Star
                        IconButton(onClick = { state.toggleStarred(article) }) {
                            Icon(
                                imageVector = if (article.starred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Star",
                                tint = if (article.starred) Color(0xFFFBC02D) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Load / Extract Full Content (Hotkey: F)
                        IconButton(
                            onClick = { state.toggleFullContent(article) },
                            enabled = article.fullContent != Article.FullContentState.LOADING
                        ) {
                            when (article.fullContent) {
                                Article.FullContentState.LOADING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Article.FullContentState.LOADED -> {
                                    Icon(
                                        imageVector = Icons.Default.Article,
                                        contentDescription = "Full content loaded (Press F to revert)",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Article.FullContentState.ERROR -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Article,
                                        contentDescription = "Failed to load full content (Click to retry, F)",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Article.FullContentState.NONE -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Article,
                                        contentDescription = "Load full article content (F)",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Dislike / filter
                        IconButton(onClick = { state.dislikeArticle(article) }) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "Dislike & filter similar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Open in external browser
                        if (article.url != null) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        Desktop.getDesktop().browse(URI.create(article.url.toString()))
                                    } catch (_: Exception) {}
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open in Browser", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Scrollable Article Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 48.dp, vertical = 24.dp)
                ) {
                    // Headline
                    Text(
                        text = article.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 36.sp
                    )

                    if (!article.author.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "By ${article.author}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Content Status Banner
                    when (article.fullContent) {
                        Article.FullContentState.LOADING -> {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Extracting full article content from web...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Article.FullContentState.LOADED -> {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Article,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Full content extracted from web",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = "Revert to summary (F)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                            .clickable { state.toggleFullContent(article) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Article.FullContentState.ERROR -> {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Could not extract full web content (page may require login or block scrapers)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Retry (F)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                            .clickable { state.toggleFullContent(article) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Article.FullContentState.NONE -> {
                            // Normal feed summary content
                        }
                    }

                    // AI Summary Card
                    DesktopAISummaryCard(
                        article = article,
                        state = state,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Article HTML Content
                    HtmlArticleView(
                        html = article.content,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}
