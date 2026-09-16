package com.capyreader.desktop.ui.articles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capyreader.desktop.ai.DesktopChatMessage
import com.capyreader.desktop.model.DesktopAccountState
import com.capyreader.desktop.ui.components.DesktopMarkdownFormatter
import com.jocmp.capy.Article
import kotlinx.coroutines.launch
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun DesktopAISummaryCard(
    article: Article,
    state: DesktopAccountState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aiOptions = state.preferences.aiOptions
    val isEnabled = aiOptions.enabled.get()
    val isConfigured = aiOptions.isConfigured()
    val modelName = aiOptions.currentModelDisplayName()
    val scope = rememberCoroutineScope()

    var isVisible by remember(article.id) { mutableStateOf(true) }
    var isExpanded by remember(article.id) { mutableStateOf(true) }
    var isGenerating by remember(article.id) { mutableStateOf(false) }
    var summaryText by remember(article.id) { mutableStateOf<String?>(null) }
    var errorMessage by remember(article.id) { mutableStateOf<String?>(null) }
    var copiedFeedback by remember(article.id) { mutableStateOf(false) }

    // Chat / Q&A state
    var isChatOpen by remember(article.id) { mutableStateOf(false) }
    var chatMessages by remember(article.id) { mutableStateOf<List<DesktopChatMessage>>(emptyList()) }
    var currentQuestion by remember(article.id) { mutableStateOf("") }
    var isQuestionStreaming by remember(article.id) { mutableStateOf(false) }

    fun generateSummary(forceRefresh: Boolean = false) {
        if (!isConfigured) return
        isGenerating = true
        errorMessage = null

        scope.launch {
            if (!forceRefresh) {
                val cached = state.summaryRepository.get(article.id)
                if (cached != null) {
                    summaryText = cached
                    isGenerating = false
                    return@launch
                }
            }

            summaryText = ""
            try {
                state.aiService.summarizeStream(article).collect { chunk ->
                    summaryText = (summaryText ?: "") + chunk
                }
                summaryText?.let {
                    if (it.isNotBlank()) {
                        state.summaryRepository.put(article.id, it)
                    }
                }
            } catch (e: Exception) {
                if (summaryText.isNullOrBlank()) {
                    errorMessage = e.message ?: "Failed to generate summary"
                }
            } finally {
                isGenerating = false
            }
        }
    }

    fun askQuestion() {
        val q = currentQuestion.trim()
        if (q.isBlank() || isQuestionStreaming) return

        val userMessage = DesktopChatMessage(text = q, isUser = true)
        val updatedHistory = chatMessages + userMessage
        chatMessages = updatedHistory
        currentQuestion = ""
        isQuestionStreaming = true

        val assistantMessage = DesktopChatMessage(text = "", isUser = false)
        chatMessages = updatedHistory + assistantMessage

        scope.launch {
            try {
                state.aiService.askQuestionStream(
                    article = article,
                    question = q,
                    history = updatedHistory
                ).collect { chunk ->
                    val last = chatMessages.lastOrNull()
                    if (last != null && !last.isUser) {
                        val updatedLast = last.copy(text = last.text + chunk)
                        chatMessages = chatMessages.dropLast(1) + updatedLast
                    }
                }
            } catch (e: Exception) {
                val last = chatMessages.lastOrNull()
                if (last != null && !last.isUser) {
                    val updatedLast = last.copy(text = last.text + "\n[Error: ${e.message}]")
                    chatMessages = chatMessages.dropLast(1) + updatedLast
                }
            } finally {
                isQuestionStreaming = false
            }
        }
    }

    LaunchedEffect(article.id) {
        val cached = state.summaryRepository.get(article.id)
        if (cached != null) {
            summaryText = cached
        } else if (isEnabled && isConfigured && aiOptions.autoSummarize.get()) {
            generateSummary(forceRefresh = false)
        }
    }

    if (!isVisible) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isEnabled && isConfigured) {
                        Text(
                            text = "• $modelName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { isVisible = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    when {
                        !isEnabled -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AI Summarization is disabled. Enable it in Settings to summarize articles instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = onOpenSettings,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Settings", fontSize = 12.sp)
                                }
                            }
                        }

                        !isConfigured -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "API Key not configured. Add your free Google Gemini API key to start summarizing.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = onOpenSettings,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Configure Key", fontSize = 12.sp)
                                }
                            }
                        }

                        errorMessage != null && summaryText.isNullOrBlank() -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { generateSummary(forceRefresh = true) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Retry", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = onOpenSettings,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Check Settings", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        !summaryText.isNullOrBlank() -> {
                            SelectionContainer {
                                Text(
                                    text = DesktopMarkdownFormatter.parseMarkdown(summaryText!!),
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: Ask Question, Regenerate, Copy
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isGenerating) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Generating...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { isChatOpen = !isChatOpen },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QuestionAnswer,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val count = chatMessages.count { it.isUser }
                                    Text(if (count > 0) "Q&A ($count)" else "Ask Question", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { generateSummary(forceRefresh = true) },
                                    enabled = !isGenerating,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Regenerate", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        summaryText?.let { text ->
                                            try {
                                                val selection = StringSelection(text)
                                                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                                                copiedFeedback = true
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (copiedFeedback) "Copied!" else "Copy", fontSize = 12.sp)
                                }
                            }

                            // Interactive Article Q&A Panel
                            if (isChatOpen) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Ask About This Article",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (chatMessages.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            chatMessages.forEach { msg ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (msg.isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = if (msg.isUser) "You" else modelName,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                                        )
                                                        SelectionContainer {
                                                            Text(
                                                                text = DesktopMarkdownFormatter.parseMarkdown(msg.text.ifBlank { "..." }),
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = currentQuestion,
                                            onValueChange = { currentQuestion = it },
                                            placeholder = { Text("Ask a question about this article...", fontSize = 12.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            enabled = !isQuestionStreaming
                                        )

                                        Button(
                                            onClick = { askQuestion() },
                                            enabled = currentQuestion.isNotBlank() && !isQuestionStreaming,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            if (isQuestionStreaming) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "Send",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        isGenerating -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Summarizing with $modelName...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Generate a concise 3-point summary using $modelName.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = { generateSummary(forceRefresh = false) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Summarize Article", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
