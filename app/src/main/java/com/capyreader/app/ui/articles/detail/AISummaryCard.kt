package com.capyreader.app.ui.articles.detail

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.capyreader.app.R
import com.capyreader.app.ai.AIAudioService
import com.capyreader.app.ai.AISummarizerService
import com.capyreader.app.ai.ArticleSummaryRepository
import com.capyreader.app.common.AudioEnclosure
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.ui.components.LocalSnackbarHost
import com.capyreader.app.ui.components.buildCopyToClipboard
import com.jocmp.capy.Article
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.koin.compose.koinInject

@Composable
fun AISummaryCard(
    article: Article,
    modifier: Modifier = Modifier,
    summaryTrigger: Long = 0L,
    onNavigateToSettings: () -> Unit = {},
    onSelectAudio: (AudioEnclosure) -> Unit = {},
    appPreferences: AppPreferences = koinInject(),
    aiService: AISummarizerService = koinInject(),
    aiAudioService: AIAudioService = koinInject(),
    summaryRepository: ArticleSummaryRepository = koinInject(),
) {
    val aiOptions = appPreferences.aiOptions
    val isConfigured = remember { aiOptions.isConfigured() }
    val isEnabled = remember { aiOptions.enabled.get() }

    if (!isEnabled) {
        return
    }

    val modelName = remember { aiOptions.currentModel() }
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current

    var isExpanded by rememberSaveable(article.id) { mutableStateOf(true) }
    var isGenerating by rememberSaveable(article.id) { mutableStateOf(false) }
    var isAudioLoading by rememberSaveable(article.id) { mutableStateOf(false) }
    var summaryText by rememberSaveable(article.id) { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable(article.id) { mutableStateOf<String?>(null) }
    var showAudioMenu by remember { mutableStateOf(false) }

    fun playAudioForText(textToPlay: String, title: String) {
        if (textToPlay.isBlank() || isAudioLoading) return

        isAudioLoading = true
        scope.launch {
            try {
                val result = aiAudioService.generateAudio(textToPlay)
                result.fold(
                    onSuccess = { audioFile ->
                        onSelectAudio(
                            AudioEnclosure(
                                url = Uri.fromFile(audioFile).toString(),
                                title = title,
                                feedName = article.title,
                                durationSeconds = null,
                                artworkUrl = null
                            )
                        )
                    },
                    onFailure = { err ->
                        snackbarHost.showSnackbar(err.message ?: "Failed to generate audio")
                    }
                )
            } finally {
                isAudioLoading = false
            }
        }
    }

    fun generateSummary(forceRefresh: Boolean = false) {
        if (!isConfigured) return

        isGenerating = true
        errorMessage = null

        scope.launch {
            if (!forceRefresh) {
                val cached = summaryRepository.get(article.id)
                if (cached != null) {
                    summaryText = cached
                    isGenerating = false
                    return@launch
                }
            }

            summaryText = ""
            try {
                aiService.summarizeStream(article).collect { chunk ->
                    summaryText = (summaryText ?: "") + chunk
                }
                summaryText?.let {
                    if (it.isNotBlank()) {
                        summaryRepository.put(article.id, it)
                    }
                }
            } catch (e: Exception) {
                if (summaryText.isNullOrBlank()) {
                    errorMessage = e.message.orEmpty().ifBlank { "Unknown error occurred" }
                } else {
                    scope.launch {
                        snackbarHost.showSnackbar(e.message.orEmpty().ifBlank { "Error while streaming summary" })
                    }
                }
            } finally {
                isGenerating = false
            }
        }
    }

    var isVisible by rememberSaveable(article.id) {
        mutableStateOf(aiOptions.autoSummarize.get())
    }

    LaunchedEffect(article.id) {
        val cached = summaryRepository.get(article.id)
        if (cached != null) {
            summaryText = cached
        } else if (aiOptions.autoSummarize.get()) {
            isVisible = true
            generateSummary(forceRefresh = false)
        }
    }

    LaunchedEffect(summaryTrigger) {
        if (summaryTrigger > 0L) {
            isVisible = true
            isExpanded = true
            generateSummary(forceRefresh = false)
        }
    }

    if (!isVisible) {
        return
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.ai_summary_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = modelName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Headphones Audio Button
                    if (isConfigured) {
                        IconButton(
                            onClick = {
                                if (summaryText.isNullOrBlank()) {
                                    val raw = article.content.ifBlank { article.summary }
                                    val clean = Jsoup.parse(raw).text()
                                    playAudioForText(clean, "Article: ${article.title}")
                                } else {
                                    showAudioMenu = true
                                }
                            }
                        ) {
                            if (isAudioLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Headphones,
                                    contentDescription = stringResource(R.string.ai_audio_listen_summary),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showAudioMenu,
                                onDismissRequest = { showAudioMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.ai_audio_listen_summary)) },
                                    enabled = !summaryText.isNullOrBlank(),
                                    onClick = {
                                        showAudioMenu = false
                                        val text = summaryText
                                        if (!text.isNullOrBlank()) {
                                            playAudioForText(text, "Summary: ${article.title}")
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.ai_audio_listen_article)) },
                                    onClick = {
                                        showAudioMenu = false
                                        val raw = article.content.ifBlank { article.summary }
                                        val clean = Jsoup.parse(raw).text()
                                        playAudioForText(clean, article.title)
                                    }
                                )
                            }
                        }
                    }

                    // Collapse / Expand toggle
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) {
                                stringResource(R.string.ai_summary_collapse)
                            } else {
                                stringResource(R.string.ai_summary_expand)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Close / Dismiss button
                    IconButton(
                        onClick = { isVisible = false }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.ai_summary_close),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    when {
                        !isConfigured -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_summary_not_configured_message),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = onNavigateToSettings) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.ai_summary_open_settings))
                                }
                            }
                        }

                        errorMessage != null && summaryText.isNullOrBlank() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { generateSummary(forceRefresh = true) }) {
                                        Text(stringResource(R.string.ai_summary_regenerate))
                                    }
                                    OutlinedButton(onClick = onNavigateToSettings) {
                                        Text(stringResource(R.string.ai_summary_open_settings))
                                    }
                                }
                            }
                        }

                        !summaryText.isNullOrBlank() -> {
                            val text = summaryText.orEmpty()
                            val copyToClipboard = buildCopyToClipboard(text)
                            val copiedMessage = stringResource(R.string.ai_summary_copied)

                            SelectionContainer {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isGenerating) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = stringResource(R.string.ai_summary_loading, modelName),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                OutlinedButton(
                                    onClick = { generateSummary(forceRefresh = true) },
                                    enabled = !isGenerating
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_summary_regenerate))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        copyToClipboard()
                                        scope.launch {
                                            snackbarHost.showSnackbar(copiedMessage)
                                        }
                                    },
                                    enabled = !isGenerating
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_summary_copy))
                                }
                            }
                        }

                        isGenerating -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.ai_summary_loading, modelName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            // Not yet generated
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { generateSummary(forceRefresh = false) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.ai_summary_generate_button))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
