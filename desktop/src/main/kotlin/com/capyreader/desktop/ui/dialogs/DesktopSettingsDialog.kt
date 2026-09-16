package com.capyreader.desktop.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.capyreader.desktop.model.DesktopAccountState
import com.capyreader.desktop.storage.DesktopAIProvider
import com.capyreader.desktop.storage.DesktopGeminiModels
import com.jocmp.capy.accounts.AutoDelete
import com.jocmp.capy.accounts.MaxArticles
import com.jocmp.capy.accounts.Source
import com.jocmp.capy.articles.SortOrder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSettingsDialog(
    state: DesktopAccountState,
    onDismissRequest: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    val account by state.currentAccount.collectAsState()
    val preferences = account?.preferences

    var maxArticles by remember(account) {
        mutableStateOf(preferences?.maxArticles?.get() ?: MaxArticles.LIMIT_5000)
    }
    var autoDelete by remember(account) {
        mutableStateOf(preferences?.autoDelete?.get() ?: AutoDelete.EVERY_THREE_MONTHS)
    }
    var dedupEnabled by remember(account) {
        mutableStateOf(preferences?.deduplicationEnabled?.get() ?: true)
    }
    var dedupThreshold by remember(account) {
        mutableStateOf(preferences?.deduplicationThreshold?.get() ?: 90)
    }
    var dedupTimeWindow by remember(account) {
        mutableStateOf(preferences?.deduplicationTimeWindowHours?.get() ?: 48)
    }
    var sortOrder by remember {
        mutableStateOf(state.preferences.sortOrder.get())
    }

    var isPruning by remember { mutableStateOf(false) }
    var pruneStatusMessage by remember { mutableStateOf<String?>(null) }
    var isDeduplicating by remember { mutableStateOf(false) }
    var dedupStatusMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val aiOptions = state.preferences.aiOptions
    var aiEnabled by remember { mutableStateOf(aiOptions.enabled.get()) }
    var aiProvider by remember { mutableStateOf(aiOptions.provider.get()) }
    var geminiApiKey by remember { mutableStateOf(aiOptions.geminiApiKey.get()) }
    var geminiModel by remember { mutableStateOf(aiOptions.geminiModel.get()) }
    var openAiApiKey by remember { mutableStateOf(aiOptions.openAiApiKey.get()) }
    var openAiEndpoint by remember { mutableStateOf(aiOptions.openAiEndpoint.get()) }
    var openAiModel by remember { mutableStateOf(aiOptions.openAiModel.get()) }
    var autoSummarize by remember { mutableStateOf(aiOptions.autoSummarize.get()) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testConnectionResult by remember { mutableStateOf<String?>(null) }
    var clearCacheStatus by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 720.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // SECTION 1: STORAGE & SYNC LIMITS
                    SettingsSection(
                        title = "Storage & Article Limits",
                        icon = Icons.Default.Storage
                    ) {
                        // Max Unread Articles
                        val maxArticlesOptions = listOf(
                            MaxArticles.LIMIT_2500 to "2,500 articles",
                            MaxArticles.LIMIT_5000 to "5,000 articles (Default - Recommended)",
                            MaxArticles.LIMIT_10000 to "10,000 articles",
                            MaxArticles.LIMIT_25000 to "25,000 articles",
                            MaxArticles.UNLIMITED to "Unlimited (Not recommended)",
                        )

                        SettingsDropdown(
                            label = "Max Unread Articles",
                            subtitle = "Caps unread articles synced and stored to prevent slow performance and excessive downloads.",
                            selected = maxArticles,
                            options = maxArticlesOptions,
                            onSelect = {
                                maxArticles = it
                                state.updateMaxArticles(it)
                            },
                            enabled = account != null
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto-Delete Read Articles
                        val autoDeleteOptions = listOf(
                            AutoDelete.DISABLED to "Never",
                            AutoDelete.WEEKLY to "After 1 week",
                            AutoDelete.EVERY_TWO_WEEKS to "After 2 weeks",
                            AutoDelete.EVERY_MONTH to "After 1 month",
                            AutoDelete.EVERY_THREE_MONTHS to "After 3 months (Default)",
                        )

                        SettingsDropdown(
                            label = "Auto-Delete Read Articles",
                            subtitle = "Older read articles beyond this duration are automatically pruned from local storage.",
                            selected = autoDelete,
                            options = autoDeleteOptions,
                            onSelect = {
                                autoDelete = it
                                state.updateAutoDelete(it)
                            },
                            enabled = account != null
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Prune Local Storage Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Trim Local Database",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pruneStatusMessage ?: "Immediately remove excess unread articles beyond your configured limit.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (pruneStatusMessage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = {
                                    if (!isPruning && account != null) {
                                        isPruning = true
                                        pruneStatusMessage = "Pruning..."
                                        state.pruneExcessArticles {
                                            isPruning = false
                                            pruneStatusMessage = "Storage pruned to ${maxArticles.limit ?: "unlimited"} limit"
                                        }
                                    }
                                },
                                enabled = !isPruning && account != null,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isPruning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Prune Now")
                            }
                        }
                    }

                    // SECTION 2: ARTICLE DEDUPLICATION
                    SettingsSection(
                        title = "Article Deduplication",
                        icon = Icons.Default.FilterAlt
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Filter Duplicate Articles",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically detect and mark duplicate syndicated news stories as read across feeds.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = dedupEnabled,
                                onCheckedChange = {
                                    dedupEnabled = it
                                    state.updateDeduplicationEnabled(it)
                                },
                                enabled = account != null
                            )
                        }

                        if (dedupEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))

                            val thresholdOptions = listOf(
                                75 to "75% (More aggressive)",
                                80 to "80%",
                                85 to "85%",
                                90 to "90% (Default - Balanced)",
                                95 to "95% (Strict match)",
                            )

                            SettingsDropdown(
                                label = "Similarity Threshold",
                                subtitle = "Title and content similarity percentage required to identify duplicate stories.",
                                selected = dedupThreshold,
                                options = thresholdOptions,
                                onSelect = {
                                    dedupThreshold = it
                                    state.updateDeduplicationThreshold(it)
                                },
                                enabled = account != null
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val timeWindowOptions = listOf(
                                24 to "24 hours",
                                48 to "48 hours (Default)",
                                72 to "72 hours",
                                168 to "1 week",
                            )

                            SettingsDropdown(
                                label = "Time Window",
                                subtitle = "Only compare articles published within this time frame.",
                                selected = dedupTimeWindow,
                                options = timeWindowOptions,
                                onSelect = {
                                    dedupTimeWindow = it
                                    state.updateDeduplicationTimeWindowHours(it)
                                },
                                enabled = account != null
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Clean Up Duplicates Now",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = dedupStatusMessage ?: "Scan recent unread articles and mark duplicates as read.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (dedupStatusMessage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (!isDeduplicating && account != null) {
                                            isDeduplicating = true
                                            dedupStatusMessage = "Scanning..."
                                            state.deduplicateNow { count ->
                                                isDeduplicating = false
                                                dedupStatusMessage = if (count > 0) "Marked $count duplicate articles as read" else "No duplicates found"
                                            }
                                        }
                                    },
                                    enabled = !isDeduplicating && account != null,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isDeduplicating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Scan Now")
                                }
                            }
                        }
                    }

                    // SECTION 3: READING & DISPLAY
                    SettingsSection(
                        title = "Reading & Display",
                        icon = Icons.Default.Tune
                    ) {
                        val sortOrderOptions = listOf(
                            SortOrder.NEWEST_FIRST to "Newest First",
                            SortOrder.OLDEST_FIRST to "Oldest First",
                        )

                        SettingsDropdown(
                            label = "Article Sort Order",
                            subtitle = "Default order for displaying articles in your feed list.",
                            selected = sortOrder,
                            options = sortOrderOptions,
                            onSelect = {
                                sortOrder = it
                                state.updateSortOrder(it)
                            }
                        )
                    }

                    // SECTION 4: AI SUMMARIZATION
                    SettingsSection(
                        title = "AI Summarization & Q&A",
                        icon = Icons.Default.AutoAwesome
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable AI Summarization",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Summarize articles and ask interactive questions using Google Gemini or OpenAI.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = aiEnabled,
                                onCheckedChange = {
                                    aiEnabled = it
                                    aiOptions.enabled.set(it)
                                }
                            )
                        }

                        if (aiEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))

                            val providerOptions = listOf(
                                DesktopAIProvider.GEMINI to "Google Gemini (Recommended - Free Tier)",
                                DesktopAIProvider.OPENAI_COMPATIBLE to "OpenAI-Compatible Service",
                            )

                            SettingsDropdown(
                                label = "AI Provider",
                                selected = aiProvider,
                                options = providerOptions,
                                onSelect = {
                                    aiProvider = it
                                    aiOptions.provider.set(it)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (aiProvider == DesktopAIProvider.GEMINI) {
                                OutlinedTextField(
                                    value = geminiApiKey,
                                    onValueChange = {
                                        geminiApiKey = it
                                        aiOptions.geminiApiKey.set(it)
                                    },
                                    label = { Text("Gemini API Key") },
                                    placeholder = { Text("AIzaSy...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Text(
                                    text = "Get a free API key at aistudio.google.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                SettingsDropdown(
                                    label = "Gemini Model",
                                    subtitle = "Select model tier for speed and summary quality.",
                                    selected = geminiModel,
                                    options = DesktopGeminiModels.presets,
                                    onSelect = {
                                        geminiModel = it
                                        aiOptions.geminiModel.set(it)
                                    }
                                )
                            } else {
                                OutlinedTextField(
                                    value = openAiEndpoint,
                                    onValueChange = {
                                        openAiEndpoint = it
                                        aiOptions.openAiEndpoint.set(it)
                                    },
                                    label = { Text("Endpoint URL") },
                                    placeholder = { Text("https://api.openai.com/v1") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = openAiApiKey,
                                    onValueChange = {
                                        openAiApiKey = it
                                        aiOptions.openAiApiKey.set(it)
                                    },
                                    label = { Text("OpenAI API Key") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = openAiModel,
                                    onValueChange = {
                                        openAiModel = it
                                        aiOptions.openAiModel.set(it)
                                    },
                                    label = { Text("Model Name") },
                                    placeholder = { Text("gpt-4o-mini") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto-Summarize Articles",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Automatically generate a summary when opening an article.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = autoSummarize,
                                    onCheckedChange = {
                                        autoSummarize = it
                                        aiOptions.autoSummarize.set(it)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Test Connection",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (testConnectionResult != null) {
                                        Text(
                                            text = testConnectionResult!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (testConnectionResult!!.startsWith("Success") || testConnectionResult!!.contains("Connected")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (!isTestingConnection) {
                                            isTestingConnection = true
                                            testConnectionResult = "Connecting..."
                                            scope.launch {
                                                val res = state.aiService.testConnection()
                                                isTestingConnection = false
                                                testConnectionResult = res.fold(
                                                    onSuccess = { "Connected successfully!" },
                                                    onFailure = { err -> "Failed: ${err.message}" }
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isTestingConnection && aiOptions.isConfigured(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isTestingConnection) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Test Connection")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Clear Cached Summaries",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (clearCacheStatus != null) {
                                        Text(
                                            text = clearCacheStatus!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            state.summaryRepository.clearAll()
                                            clearCacheStatus = "Cache cleared"
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Clear Cache")
                                }
                            }
                        }
                    }

                    // SECTION 5: ACCOUNT DETAILS
                    SettingsSection(
                        title = "Account",
                        icon = Icons.Default.AccountCircle
                    ) {
                        if (account != null) {
                            val sourceName = sourceDisplayName(preferences?.source?.get() ?: Source.FRESHRSS)
                            val username = preferences?.username?.get().orEmpty()
                            val apiUrl = preferences?.url?.get().orEmpty()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AccountInfoRow(label = "Service", value = sourceName)
                                if (username.isNotBlank()) {
                                    AccountInfoRow(label = "Username", value = username)
                                }
                                if (apiUrl.isNotBlank()) {
                                    AccountInfoRow(label = "Server URL", value = apiUrl)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    state.logout()
                                    onDismissRequest()
                                    onOpenLogin()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out of Account")
                            }
                        } else {
                            Text(
                                text = "No account signed in.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onDismissRequest()
                                    onOpenLogin()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign In / Setup Account")
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Dialog Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(16.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsDropdown(
    label: String,
    subtitle: String? = null,
    selected: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.firstOrNull { it.first == selected }?.second ?: selected.toString()

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(8.dp),
                enabled = enabled
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (option, optLabel) ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(optLabel)
                                if (option == selected) {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun sourceDisplayName(source: Source): String {
    return when (source) {
        Source.FRESHRSS -> "FreshRSS"
        Source.READER -> "Google Reader API"
        Source.MINIFLUX -> "Miniflux"
        Source.MINIFLUX_TOKEN -> "Miniflux"
        Source.FEEDBIN -> "Feedbin"
        Source.LOCAL -> "Local RSS"
    }
}
