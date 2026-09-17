package com.capyreader.desktop.ui.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capyreader.desktop.model.DesktopAccountState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import com.jocmp.capy.Article
import org.jsoup.Jsoup
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun DesktopArticleList(
    state: DesktopAccountState,
    showSidebarToggle: Boolean = false,
    onToggleSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val articles by state.articles.collectAsState()
    val selectedArticle by state.selectedArticle.collectAsState()
    val searchQuery by state.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar & Header
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSidebarToggle) {
                        IconButton(
                            onClick = onToggleSidebar,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Toggle sidebar feeds",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { state.setSearchQuery(it) },
                        placeholder = { Text("Search articles...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    state.setSearchQuery("")
                                    focusManager.clearFocus()
                                    state.setTextInputActive(false)
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { state.setTextInputActive(it.isFocused) },
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                state.setTextInputActive(false)
                            },
                            onDone = {
                                focusManager.clearFocus()
                                state.setTextInputActive(false)
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${articles.size} articles",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = { state.markAllRead() },
                        enabled = articles.any { !it.read }
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark all read", fontSize = 12.sp)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            if (articles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching articles" else "No articles found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(articles, key = { it.id }) { article ->
                        ArticleItemRow(
                            article = article,
                            isSelected = selectedArticle?.id == article.id,
                            onClick = {
                                focusManager.clearFocus()
                                state.setTextInputActive(false)
                                state.selectArticle(article)
                            },
                            onToggleStar = { state.toggleStarred(article) },
                            onToggleRead = { state.toggleRead(article) },
                            onDislike = { state.dislikeArticle(article) }
                        )
                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleItemRow(
    article: Article,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleRead: () -> Unit,
    onDislike: () -> Unit
) {
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }

    val snippet = androidx.compose.runtime.remember(article.content) {
        try {
            val text = Jsoup.parse(article.content).text()
            if (text.length > 120) text.take(120) + "…" else text
        } catch (_: Exception) {
            article.summary
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Feed Name & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Unread dot
                    if (!article.read) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = article.feedName.ifBlank { "Feed" },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatRelativeTime(article.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = article.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (!article.read) FontWeight.Bold else FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Icons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dislike / auto-mute button
                IconButton(
                    onClick = onDislike,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Dislike & filter similar",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Star toggle
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (article.starred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (article.starred) "Starred" else "Star",
                        modifier = Modifier.size(16.dp),
                        tint = if (article.starred) Color(0xFFFBC02D) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(date: java.time.ZonedDateTime?): String {
    if (date == null) return ""
    val now = java.time.ZonedDateTime.now()
    val minutes = ChronoUnit.MINUTES.between(date, now)
    if (minutes < 1) return "Just now"
    if (minutes < 60) return "${minutes}m"
    val hours = ChronoUnit.HOURS.between(date, now)
    if (hours < 24) return "${hours}h"
    val days = ChronoUnit.DAYS.between(date, now)
    if (days < 7) return "${days}d"
    return date.format(DateTimeFormatter.ofPattern("MMM d"))
}
