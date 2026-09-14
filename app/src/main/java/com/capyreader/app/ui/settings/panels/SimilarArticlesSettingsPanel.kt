package com.capyreader.app.ui.settings.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capyreader.app.R
import com.capyreader.app.common.RowItem
import com.capyreader.app.ui.components.DialogCard
import com.capyreader.app.ui.components.FormSection
import com.capyreader.app.ui.components.LocalSnackbarHost
import com.capyreader.app.ui.components.TextSwitch
import com.capyreader.app.ui.settings.PreferenceSelect
import com.jocmp.capy.Feed
import com.jocmp.capy.articles.similarity.DeduplicationMatch
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SimilarArticlesSettingsPanel(
    viewModel: SimilarArticlesSettingsViewModel = koinViewModel(),
) {
    val preferredFeeds by viewModel.preferredFeeds.collectAsStateWithLifecycle()
    val availableFeeds by viewModel.availableFeeds.collectAsStateWithLifecycle()
    val bypassWords by viewModel.bypassWords.collectAsStateWithLifecycle()
    val recentMatches by viewModel.recentMatches.collectAsStateWithLifecycle()
    val snackbarHost = LocalSnackbarHost.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isMatchesDialogOpen by remember { mutableStateOf(false) }

    SimilarArticlesSettingsPanelView(
        enabled = viewModel.enabled,
        onUpdateEnabled = viewModel::updateEnabled,
        threshold = viewModel.threshold,
        onUpdateThreshold = viewModel::updateThreshold,
        timeWindowHours = viewModel.timeWindowHours,
        onUpdateTimeWindowHours = viewModel::updateTimeWindowHours,
        preferredFeeds = preferredFeeds,
        availableFeeds = availableFeeds,
        onAddPreferredFeed = viewModel::addPreferredFeed,
        onRemovePreferredFeed = viewModel::removePreferredFeed,
        onMovePreferredFeedUp = viewModel::movePreferredFeedUp,
        onMovePreferredFeedDown = viewModel::movePreferredFeedDown,
        bypassWords = bypassWords,
        onAddBypassWord = viewModel::addBypassKeyword,
        onRemoveBypassWord = viewModel::removeBypassKeyword,
        recentMatches = recentMatches,
        onClearRecentMatches = viewModel::clearRecentMatches,
        isMatchesDialogOpen = isMatchesDialogOpen,
        onSetMatchesDialogOpen = { isMatchesDialogOpen = it },
        isCleaningUp = viewModel.isCleaningUp,
        onCleanUpNow = {
            viewModel.cleanUpNow { result ->
                coroutineScope.launch {
                    val count = result.duplicateIDs.size
                    val message = if (count > 0) {
                        context.getString(R.string.settings_similar_articles_clean_up_success, count)
                    } else {
                        context.getString(R.string.settings_similar_articles_clean_up_success_zero)
                    }
                    snackbarHost.showSnackbar(message)
                }
                if (result.matches.isNotEmpty()) {
                    isMatchesDialogOpen = true
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimilarArticlesSettingsPanelView(
    enabled: Boolean,
    onUpdateEnabled: (Boolean) -> Unit,
    threshold: Int,
    onUpdateThreshold: (Int) -> Unit,
    timeWindowHours: Int,
    onUpdateTimeWindowHours: (Int) -> Unit,
    preferredFeeds: List<Feed>,
    availableFeeds: List<Feed>,
    onAddPreferredFeed: (Feed) -> Unit,
    onRemovePreferredFeed: (String) -> Unit,
    onMovePreferredFeedUp: (String) -> Unit,
    onMovePreferredFeedDown: (String) -> Unit,
    bypassWords: Set<String>,
    onAddBypassWord: (String) -> Unit,
    onRemoveBypassWord: (String) -> Unit,
    recentMatches: List<DeduplicationMatch>,
    onClearRecentMatches: () -> Unit,
    isMatchesDialogOpen: Boolean,
    onSetMatchesDialogOpen: (Boolean) -> Unit,
    isCleaningUp: Boolean,
    onCleanUpNow: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var isAddSourceDialogOpen by remember { mutableStateOf(false) }
    var newKeywordText by remember { mutableStateOf("") }

    val addKeywordAction = {
        if (newKeywordText.isNotBlank()) {
            onAddBypassWord(newKeywordText)
            newKeywordText = ""
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        FormSection(title = stringResource(R.string.settings_similar_articles_title)) {
            Column {
                RowItem {
                    TextSwitch(
                        checked = enabled,
                        onCheckedChange = onUpdateEnabled,
                        title = stringResource(R.string.settings_similar_articles_enable),
                        subtitle = stringResource(R.string.settings_similar_articles_enable_subtitle)
                    )
                }

                PreferenceSelect(
                    selected = threshold,
                    update = onUpdateThreshold,
                    options = listOf(75, 80, 85, 90, 95),
                    label = R.string.settings_similar_articles_threshold,
                    optionText = { "$it%" },
                    enabled = enabled,
                )

                PreferenceSelect(
                    selected = timeWindowHours,
                    update = onUpdateTimeWindowHours,
                    options = listOf(24, 48, 72, 168),
                    label = R.string.settings_similar_articles_time_window,
                    optionText = { hours ->
                        when (hours) {
                            24 -> stringResource(R.string.settings_similar_articles_time_window_24h)
                            48 -> stringResource(R.string.settings_similar_articles_time_window_48h)
                            72 -> stringResource(R.string.settings_similar_articles_time_window_72h)
                            else -> stringResource(R.string.settings_similar_articles_time_window_1w)
                        }
                    },
                    enabled = enabled,
                )
            }
        }

        // Source Priority
        FormSection(title = stringResource(R.string.settings_similar_articles_preferred_sources)) {
            Column {
                Text(
                    text = stringResource(R.string.settings_similar_articles_preferred_sources_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (preferredFeeds.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_similar_articles_preferred_sources_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                } else {
                    preferredFeeds.forEachIndexed { index, feed ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                            leadingContent = {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            headlineContent = {
                                Text(feed.title, fontWeight = FontWeight.Medium)
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onMovePreferredFeedUp(feed.id) },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowUp,
                                            contentDescription = "Move Up"
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMovePreferredFeedDown(feed.id) },
                                        enabled = index < preferredFeeds.size - 1
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Move Down"
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemovePreferredFeed(feed.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { isAddSourceDialogOpen = true },
                        enabled = availableFeeds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_similar_articles_add_preferred_source),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        // Bypass Keywords
        FormSection(title = stringResource(R.string.settings_similar_articles_bypass_keywords)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.settings_similar_articles_bypass_keywords_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    bypassWords.sorted().forEach { word ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(word) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemoveBypassWord(word) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove $word",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newKeywordText,
                        onValueChange = { newKeywordText = it },
                        label = { Text(stringResource(R.string.settings_similar_articles_add_bypass_keyword)) },
                        placeholder = { Text(stringResource(R.string.settings_similar_articles_bypass_keyword_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { addKeywordAction() }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = { addKeywordAction() },
                        enabled = newKeywordText.isNotBlank()
                    ) {
                        Text(stringResource(R.string.filters_add_keyword))
                    }
                }
            }
        }

        // Action: Clean Up Unread Articles Now & View Results
        FormSection(title = stringResource(R.string.settings_section_mark_all_as_read)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCleanUpNow,
                    enabled = !isCleaningUp && enabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCleaningUp) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(stringResource(R.string.settings_similar_articles_clean_up_running))
                    } else {
                        Text(stringResource(R.string.settings_similar_articles_clean_up_now))
                    }
                }

                OutlinedButton(
                    onClick = { onSetMatchesDialogOpen(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${stringResource(R.string.settings_similar_articles_view_matches)} (${recentMatches.size})")
                }
            }
        }
    }

    // Dialog for adding a source to priority list
    if (isAddSourceDialogOpen) {
        Dialog(onDismissRequest = { isAddSourceDialogOpen = false }) {
            DialogCard {
                Column(
                    modifier = Modifier
                        .heightIn(max = 450.dp)
                        .imePadding()
                ) {
                    Text(
                        text = stringResource(R.string.settings_similar_articles_choose_source),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .padding(top = 24.dp, bottom = 8.dp)
                            .padding(horizontal = 16.dp)
                    )
                    HorizontalDivider()
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(1f, fill = false)
                    ) {
                        availableFeeds.forEach { feed ->
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = CardDefaults.cardColors().containerColor),
                                headlineContent = { Text(feed.title) },
                                modifier = Modifier.clickable {
                                    onAddPreferredFeed(feed)
                                    isAddSourceDialogOpen = false
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        TextButton(onClick = { isAddSourceDialogOpen = false }) {
                            Text(stringResource(R.string.feed_form_cancel))
                        }
                    }
                }
            }
        }
    }

    // Dialog for viewing recent deduplication matches
    if (isMatchesDialogOpen) {
        Dialog(onDismissRequest = { onSetMatchesDialogOpen(false) }) {
            DialogCard {
                Column(
                    modifier = Modifier
                        .heightIn(max = 550.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_similar_articles_matches_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (recentMatches.isNotEmpty()) {
                            TextButton(onClick = onClearRecentMatches) {
                                Text(stringResource(R.string.settings_similar_articles_clear_log))
                            }
                        }
                    }
                    HorizontalDivider()

                    if (recentMatches.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.settings_similar_articles_matches_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .weight(1f, fill = false)
                                .padding(vertical = 8.dp)
                        ) {
                            recentMatches.forEachIndexed { idx, match ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_similar_articles_match_kept),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (match.similarityPercentage > 0) {
                                            Text(
                                                text = stringResource(R.string.settings_similar_articles_match_similarity, match.similarityPercentage),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    Text(
                                        text = match.keptArticleTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (match.keptFeedTitle.isNotBlank()) {
                                        Text(
                                            text = match.keptFeedTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = stringResource(R.string.settings_similar_articles_match_removed),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = match.duplicateArticleTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (match.duplicateFeedTitle.isNotBlank()) {
                                        Text(
                                            text = match.duplicateFeedTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                if (idx < recentMatches.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        TextButton(onClick = { onSetMatchesDialogOpen(false) }) {
                            Text(stringResource(R.string.feed_form_cancel))
                        }
                    }
                }
            }
        }
    }
}
