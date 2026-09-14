package com.capyreader.app.ui.articles.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.capyreader.app.R
import com.capyreader.app.ai.AIChatMessage
import com.capyreader.app.ai.AISummarizerService
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.ui.components.LocalSnackbarHost
import com.capyreader.app.ui.components.MarkdownFormatter
import com.capyreader.app.ui.components.buildCopyToClipboard
import com.jocmp.capy.Article
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleChatBottomSheet(
    article: Article,
    onDismissRequest: () -> Unit,
    messages: List<AIChatMessage>,
    onMessagesChanged: (List<AIChatMessage>) -> Unit,
    articleFont: FontFamily? = null,
    appPreferences: AppPreferences = koinInject(),
    aiService: AISummarizerService = koinInject(),
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current
    val modelName = remember { appPreferences.aiOptions.currentModelDisplayName() }

    var inputText by remember { mutableStateOf("") }
    var isAnswering by remember { mutableStateOf(false) }
    var currentStreamingAnswer by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    fun sendQuestion(questionText: String) {
        val trimmed = questionText.trim()
        if (trimmed.isBlank() || isAnswering) return

        val userMessage = AIChatMessage(
            isUser = true,
            text = trimmed,
        )
        val historyBeforeThisQuestion = messages
        val updatedMessagesWithUser = messages + userMessage
        onMessagesChanged(updatedMessagesWithUser)
        inputText = ""
        isAnswering = true
        currentStreamingAnswer = ""

        scope.launch {
            try {
                aiService.askQuestionStream(
                    article = article,
                    question = trimmed,
                    history = historyBeforeThisQuestion,
                ).collect { chunk ->
                    currentStreamingAnswer += chunk
                }

                if (currentStreamingAnswer.isNotBlank()) {
                    val aiMessage = AIChatMessage(
                        isUser = false,
                        text = currentStreamingAnswer,
                    )
                    onMessagesChanged(updatedMessagesWithUser + aiMessage)
                }
            } catch (e: Exception) {
                val errorMessage = AIChatMessage(
                    isUser = false,
                    text = "Error: " + (e.message ?: "Failed to get an answer"),
                )
                onMessagesChanged(updatedMessagesWithUser + errorMessage)
            } finally {
                currentStreamingAnswer = ""
                isAnswering = false
            }
        }
    }

    LaunchedEffect(messages.size, currentStreamingAnswer) {
        val totalCount = messages.size + if (isAnswering && currentStreamingAnswer.isNotBlank()) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QuestionAnswer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.ai_chat_bottom_sheet_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = modelName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = { onMessagesChanged(emptyList()) },
                            enabled = !isAnswering
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.ai_chat_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.ai_summary_close),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Chat Messages / Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && !isAnswering) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.ai_chat_empty_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // Starter quick chips
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val starters = listOf(
                                "What are the main takeaways?",
                                "Who is mentioned?",
                                "What is the context?",
                            )
                            starters.forEach { starter ->
                                AssistChip(
                                    onClick = { sendQuestion(starter) },
                                    label = { Text(starter, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = messages, key = { it.id }) { msg ->
                            if (msg.isUser) {
                                UserMessageItem(message = msg)
                            } else {
                                AssistantMessageItem(
                                    message = msg,
                                    articleFont = articleFont,
                                    onCopied = {
                                        scope.launch {
                                            snackbarHost.showSnackbar(it)
                                        }
                                    }
                                )
                            }
                        }

                        // Live streaming bubble if generating
                        if (isAnswering && currentStreamingAnswer.isNotBlank()) {
                            item(key = "streaming_answer") {
                                AssistantMessageItem(
                                    message = AIChatMessage(isUser = false, text = currentStreamingAnswer),
                                    articleFont = articleFont,
                                    isStreaming = true,
                                    onCopied = {}
                                )
                            }
                        } else if (isAnswering && currentStreamingAnswer.isBlank()) {
                            item(key = "thinking_indicator") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = stringResource(R.string.ai_chat_answering),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.ai_chat_input_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 120.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { sendQuestion(inputText) }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    singleLine = false,
                )

                IconButton(
                    onClick = { sendQuestion(inputText) },
                    enabled = inputText.isNotBlank() && !isAnswering,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (inputText.isNotBlank() && !isAnswering) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.ai_chat_send),
                        tint = if (inputText.isNotBlank() && !isAnswering) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageItem(message: AIChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 4.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            SelectionContainer {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun AssistantMessageItem(
    message: AIChatMessage,
    articleFont: FontFamily? = null,
    isStreaming: Boolean = false,
    onCopied: (String) -> Unit = {},
) {
    val formattedText = remember(message.text) {
        MarkdownFormatter.parseMarkdown(message.text)
    }
    val copyToClipboard = buildCopyToClipboard(formattedText.text)
    val copiedMessage = stringResource(R.string.ai_chat_copied)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = formattedText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = articleFont
                        ),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.35f,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isStreaming) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                copyToClipboard()
                                onCopied(copiedMessage)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
