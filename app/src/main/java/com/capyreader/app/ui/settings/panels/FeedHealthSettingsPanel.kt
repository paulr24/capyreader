package com.capyreader.app.ui.settings.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capyreader.app.R
import com.capyreader.app.ui.components.FormSection
import com.capyreader.app.ui.components.LocalSnackbarHost
import com.jocmp.capy.stats.FeedHealthStats
import com.jocmp.capy.stats.PruneReason
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun FeedHealthSettingsPanel(
    viewModel: FeedHealthSettingsViewModel = koinViewModel(),
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val sortedFeeds by viewModel.sortedFeeds.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val pendingUnsubscribe by viewModel.feedPendingUnsubscribe.collectAsStateWithLifecycle()

    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    FeedHealthSettingsPanelView(
        overview = overview,
        recommendations = recommendations,
        sortedFeeds = sortedFeeds,
        sortOrder = sortOrder,
        isLoading = isLoading,
        onSelectSortOrder = viewModel::updateSortOrder,
        onRequestUnsubscribe = viewModel::requestUnsubscribe,
    )

    if (pendingUnsubscribe != null) {
        val feed = pendingUnsubscribe ?: return
        AlertDialog(
            onDismissRequest = viewModel::cancelUnsubscribe,
            title = {
                Text(stringResource(R.string.settings_feed_health_unsubscribe_confirm_title, feed.title))
            },
            text = {
                Text(stringResource(R.string.settings_feed_health_unsubscribe_confirm_message))
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelUnsubscribe) {
                    Text(stringResource(R.string.feed_form_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmUnsubscribe { unsubscribedTitle ->
                            scope.launch {
                                snackbarHost.showSnackbar(
                                    context.getString(
                                        R.string.settings_feed_health_unsubscribe_success,
                                        unsubscribedTitle
                                    )
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.settings_feed_health_unsubscribe))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedHealthSettingsPanelView(
    overview: FeedHealthOverview,
    recommendations: List<FeedHealthStats>,
    sortedFeeds: List<FeedHealthStats>,
    sortOrder: FeedSortOrder,
    isLoading: Boolean,
    onSelectSortOrder: (FeedSortOrder) -> Unit,
    onRequestUnsubscribe: (FeedHealthStats) -> Unit,
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Overview Card
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.settings_feed_health_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBox(
                        label = stringResource(R.string.settings_feed_health_overview_total_feeds),
                        value = "${overview.totalFeeds}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = stringResource(R.string.settings_feed_health_overview_prune_candidates),
                        value = "${overview.pruneCandidatesCount}",
                        highlight = overview.pruneCandidatesCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = stringResource(R.string.settings_feed_health_overview_read_rate),
                        value = "${(overview.overallReadRate * 100).toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = stringResource(R.string.settings_feed_health_overview_weekly_volume),
                        value = String.format(Locale.getDefault(), "%.0f", overview.totalWeeklyVolume),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recommendations Section (Prune Candidates)
        FormSection(
            title = "${stringResource(R.string.settings_feed_health_recommendations_title)} (${recommendations.size})"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (recommendations.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_feed_health_recommendations_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    recommendations.forEach { feed ->
                        RecommendationCard(
                            feed = feed,
                            onUnsubscribe = { onRequestUnsubscribe(feed) }
                        )
                    }
                }
            }
        }

        // All Feeds Section with Sorting Chips
        FormSection(
            title = "${stringResource(R.string.settings_feed_health_all_feeds_title)} (${sortedFeeds.size})"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Filter / Sorting Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortOrder == FeedSortOrder.LOWEST_READ_PERCENTAGE,
                        onClick = { onSelectSortOrder(FeedSortOrder.LOWEST_READ_PERCENTAGE) },
                        label = { Text(stringResource(R.string.settings_feed_health_sort_lowest_read)) }
                    )
                    FilterChip(
                        selected = sortOrder == FeedSortOrder.HIGHEST_VOLUME,
                        onClick = { onSelectSortOrder(FeedSortOrder.HIGHEST_VOLUME) },
                        label = { Text(stringResource(R.string.settings_feed_health_sort_highest_volume)) }
                    )
                    FilterChip(
                        selected = sortOrder == FeedSortOrder.MOST_DORMANT,
                        onClick = { onSelectSortOrder(FeedSortOrder.MOST_DORMANT) },
                        label = { Text(stringResource(R.string.settings_feed_health_sort_dormant)) }
                    )
                    FilterChip(
                        selected = sortOrder == FeedSortOrder.HIGHEST_READ_PERCENTAGE,
                        onClick = { onSelectSortOrder(FeedSortOrder.HIGHEST_READ_PERCENTAGE) },
                        label = { Text(stringResource(R.string.settings_feed_health_sort_highest_read)) }
                    )
                    FilterChip(
                        selected = sortOrder == FeedSortOrder.ALPHABETICAL,
                        onClick = { onSelectSortOrder(FeedSortOrder.ALPHABETICAL) },
                        label = { Text(stringResource(R.string.settings_feed_health_sort_name)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Feeds List
                sortedFeeds.forEachIndexed { index, feed ->
                    FeedHealthItem(
                        feed = feed,
                        onUnsubscribe = { onRequestUnsubscribe(feed) }
                    )
                    if (index < sortedFeeds.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecommendationCard(
    feed: FeedHealthStats,
    onUnsubscribe: () -> Unit,
) {
    val reason = feed.pruneReason()
    val reasonText = when (reason) {
        PruneReason.DEAD_FEED -> {
            val days = feed.daysSinceLastPost() ?: 180L
            stringResource(R.string.settings_feed_health_reason_dead, days)
        }
        PruneReason.DORMANT -> {
            val days = feed.daysSinceLastRead()
            if (days != null) {
                stringResource(R.string.settings_feed_health_reason_dormant, days)
            } else {
                stringResource(R.string.settings_feed_health_reason_dormant_never)
            }
        }
        PruneReason.FIREHOSE_LOW_READ -> {
            stringResource(R.string.settings_feed_health_reason_firehose, feed.readPercentage, feed.weeklyVolume)
        }
        null -> ""
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feed.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = reasonText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onUnsubscribe,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.settings_feed_health_unsubscribe))
                }
            }
        }
    }
}

@Composable
private fun FeedHealthItem(
    feed: FeedHealthStats,
    onUnsubscribe: () -> Unit,
) {
    val lastReadText = when (val days = feed.daysSinceLastRead()) {
        null -> stringResource(R.string.settings_feed_health_last_read_never)
        0L -> stringResource(R.string.settings_feed_health_last_read_today)
        else -> stringResource(R.string.settings_feed_health_last_read_days, days)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feed.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Progress bar and rate label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { feed.readRate.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .width(90.dp)
                        .height(6.dp),
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = stringResource(R.string.settings_feed_health_read_rate_label, feed.readPercentage),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (feed.readRate < 0.15f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle details
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_feed_health_volume_label, feed.weeklyVolume),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = lastReadText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = stringResource(R.string.settings_feed_health_articles_count, feed.totalArticles),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextButton(
            onClick = onUnsubscribe,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.settings_feed_health_unsubscribe))
        }
    }
}
