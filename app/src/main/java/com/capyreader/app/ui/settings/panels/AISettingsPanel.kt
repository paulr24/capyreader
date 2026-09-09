package com.capyreader.app.ui.settings.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.capyreader.app.R
import com.capyreader.app.ai.AIAudioService
import com.capyreader.app.ai.AISummarizerService
import com.capyreader.app.ai.ArticleSummaryRepository
import com.capyreader.app.preferences.AIAudioProvider
import com.capyreader.app.preferences.AIProvider
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.preferences.DEFAULT_AI_PROMPT_TEMPLATE
import com.capyreader.app.preferences.GeminiAudioModels
import com.capyreader.app.preferences.GeminiModels
import com.capyreader.app.preferences.GeminiVoices
import com.capyreader.app.preferences.OpenAIVoices
import com.capyreader.app.ui.LocalLinkOpener
import com.capyreader.app.ui.collectChangesWithDefault
import com.capyreader.app.ui.components.FormSection
import com.capyreader.app.ui.components.LocalSnackbarHost
import com.capyreader.app.ui.components.TextSwitch
import com.capyreader.app.ui.settings.PreferenceSelect
import com.capyreader.app.ui.settings.SettingsPanelScaffold
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AISettingsPanel(
    appPreferences: AppPreferences = koinInject(),
    aiService: AISummarizerService = koinInject(),
    aiAudioService: AIAudioService = koinInject(),
    summaryRepository: ArticleSummaryRepository = koinInject(),
) {
    val aiOptions = appPreferences.aiOptions
    val enabled by aiOptions.enabled.collectChangesWithDefault()
    val provider by aiOptions.provider.collectChangesWithDefault()
    val geminiApiKey by aiOptions.geminiApiKey.collectChangesWithDefault()
    val geminiModel by aiOptions.geminiModel.collectChangesWithDefault()
    val openAiApiKey by aiOptions.openAiApiKey.collectChangesWithDefault()
    val openAiEndpoint by aiOptions.openAiEndpoint.collectChangesWithDefault()
    val openAiModel by aiOptions.openAiModel.collectChangesWithDefault()
    val promptTemplate by aiOptions.promptTemplate.collectChangesWithDefault()
    val autoSummarize by aiOptions.autoSummarize.collectChangesWithDefault()

    val audioProvider by aiOptions.audioProvider.collectChangesWithDefault()
    val geminiAudioModel by aiOptions.geminiAudioModel.collectChangesWithDefault()
    val geminiVoice by aiOptions.geminiVoice.collectChangesWithDefault()
    val openAiVoice by aiOptions.openAiVoice.collectChangesWithDefault()
    val isSamplePlaying by aiAudioService.isSamplePlaying.collectAsState()
    var isGeneratingSample by remember { mutableStateOf(false) }

    val linkOpener = LocalLinkOpener.current
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenAiKey by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf<Boolean?>(null) }

    val isCustomGeminiModel = remember(geminiModel) {
        GeminiModels.presets.none { it.first == geminiModel }
    }
    var customModelText by remember(geminiModel) {
        mutableStateOf(if (isCustomGeminiModel) geminiModel else "")
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
            FormSection(title = stringResource(R.string.ai_settings_section_general)) {
                TextSwitch(
                    onCheckedChange = { aiOptions.enabled.set(it) },
                    checked = enabled,
                    title = stringResource(R.string.ai_settings_enable_title),
                    subtitle = stringResource(R.string.ai_settings_enable_summary),
                )
                TextSwitch(
                    onCheckedChange = { aiOptions.autoSummarize.set(it) },
                    checked = autoSummarize,
                    title = stringResource(R.string.ai_settings_auto_summarize_title),
                    subtitle = stringResource(R.string.ai_settings_auto_summarize_summary),
                )
            }

            FormSection(title = stringResource(R.string.ai_settings_section_provider)) {
                PreferenceSelect(
                    selected = provider,
                    update = { aiOptions.provider.set(it) },
                    options = listOf(AIProvider.GEMINI, AIProvider.OPENAI_COMPATIBLE),
                    label = R.string.ai_settings_provider_title,
                    optionText = {
                        when (it) {
                            AIProvider.GEMINI -> stringResource(R.string.ai_provider_gemini)
                            AIProvider.OPENAI_COMPATIBLE -> stringResource(R.string.ai_provider_openai_compatible)
                        }
                    }
                )

                if (provider == AIProvider.GEMINI) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = geminiApiKey,
                            onValueChange = { aiOptions.geminiApiKey.set(it.trim()) },
                            label = { Text(stringResource(R.string.ai_settings_api_key_title)) },
                            placeholder = { Text(stringResource(R.string.ai_settings_api_key_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                    Icon(
                                        imageVector = if (showGeminiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        TextButton(
                            onClick = {
                                linkOpener.open("https://aistudio.google.com/app/apikey".toUri())
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                stringResource(R.string.ai_settings_get_gemini_key),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    val modelOptions = GeminiModels.presets.map { it.first } + listOf("CUSTOM")
                    val selectedOption = if (isCustomGeminiModel) "CUSTOM" else geminiModel

                    PreferenceSelect(
                        selected = selectedOption,
                        update = { option ->
                            if (option == "CUSTOM") {
                                aiOptions.geminiModel.set(customModelText.ifBlank { GeminiModels.default })
                            } else {
                                aiOptions.geminiModel.set(option)
                            }
                        },
                        options = modelOptions,
                        label = R.string.ai_settings_model_title,
                        optionText = { option ->
                            if (option == "CUSTOM") {
                                stringResource(R.string.ai_settings_custom_model)
                            } else {
                                GeminiModels.presets.firstOrNull { it.first == option }?.second ?: option
                            }
                        }
                    )

                    if (isCustomGeminiModel || selectedOption == "CUSTOM") {
                        OutlinedTextField(
                            value = customModelText,
                            onValueChange = {
                                customModelText = it.trim()
                                aiOptions.geminiModel.set(it.trim())
                            },
                            label = { Text(stringResource(R.string.ai_settings_custom_model)) },
                            placeholder = { Text(stringResource(R.string.ai_settings_custom_model_hint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = openAiEndpoint,
                            onValueChange = { aiOptions.openAiEndpoint.set(it.trim()) },
                            label = { Text(stringResource(R.string.ai_settings_endpoint_title)) },
                            placeholder = { Text(stringResource(R.string.ai_settings_endpoint_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = openAiApiKey,
                            onValueChange = { aiOptions.openAiApiKey.set(it.trim()) },
                            label = { Text(stringResource(R.string.ai_settings_api_key_title)) },
                            placeholder = { Text(stringResource(R.string.ai_settings_api_key_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showOpenAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                                    Icon(
                                        imageVector = if (showOpenAiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = openAiModel,
                            onValueChange = { aiOptions.openAiModel.set(it.trim()) },
                            label = { Text(stringResource(R.string.ai_settings_model_title)) },
                            placeholder = { Text("gpt-4o-mini") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            isTestingConnection = true
                            testResult = null
                            testSuccess = null
                            scope.launch {
                                val result = aiService.testConnection()
                                isTestingConnection = false
                                result.fold(
                                    onSuccess = {
                                        testSuccess = true
                                        testResult = it
                                    },
                                    onFailure = {
                                        testSuccess = false
                                        testResult = it.message.orEmpty()
                                    }
                                )
                            }
                        },
                        enabled = !isTestingConnection && aiOptions.isConfigured()
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.ai_settings_testing_connection))
                        } else {
                            Text(stringResource(R.string.ai_settings_test_connection))
                        }
                    }

                    if (testSuccess == true) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.ai_settings_test_success),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (testSuccess == false) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.ai_settings_test_failure, testResult.orEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            FormSection(title = stringResource(R.string.ai_settings_section_prompt)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = promptTemplate,
                        onValueChange = { aiOptions.promptTemplate.set(it) },
                        label = { Text(stringResource(R.string.ai_settings_prompt_title)) },
                        placeholder = { Text(stringResource(R.string.ai_settings_prompt_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 8,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        stringResource(R.string.ai_settings_prompt_variables_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { aiOptions.promptTemplate.set(DEFAULT_AI_PROMPT_TEMPLATE) }
                    ) {
                        Text(stringResource(R.string.ai_settings_prompt_reset))
                    }
                }
            }

            FormSection(title = stringResource(R.string.ai_settings_section_audio)) {
                PreferenceSelect(
                    selected = audioProvider,
                    update = { aiOptions.audioProvider.set(it) },
                    options = AIAudioProvider.entries,
                    label = R.string.ai_settings_audio_provider_title,
                    optionText = {
                        when (it) {
                            AIAudioProvider.GEMINI -> stringResource(R.string.ai_audio_provider_gemini)
                            AIAudioProvider.SYSTEM -> stringResource(R.string.ai_audio_provider_system)
                            AIAudioProvider.OPENAI -> stringResource(R.string.ai_audio_provider_openai)
                        }
                    }
                )

                if (audioProvider == AIAudioProvider.GEMINI) {
                    PreferenceSelect(
                        selected = geminiAudioModel,
                        update = { aiOptions.geminiAudioModel.set(it) },
                        options = GeminiAudioModels.presets.map { it.first },
                        label = R.string.ai_settings_audio_model_title,
                        optionText = { model ->
                            GeminiAudioModels.presets.firstOrNull { it.first == model }?.second ?: model
                        }
                    )

                    PreferenceSelect(
                        selected = geminiVoice,
                        update = { aiOptions.geminiVoice.set(it) },
                        options = GeminiVoices.presets.map { it.first },
                        label = R.string.ai_settings_voice_title,
                        optionText = { voice ->
                            GeminiVoices.presets.firstOrNull { it.first == voice }?.second ?: voice
                        }
                    )
                } else if (audioProvider == AIAudioProvider.OPENAI) {
                    PreferenceSelect(
                        selected = openAiVoice,
                        update = { aiOptions.openAiVoice.set(it) },
                        options = OpenAIVoices.presets.map { it.first },
                        label = R.string.ai_settings_voice_title,
                        optionText = { voice ->
                            OpenAIVoices.presets.firstOrNull { it.first == voice }?.second ?: voice
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isSamplePlaying) {
                                aiAudioService.stopSample()
                            } else {
                                isGeneratingSample = true
                                scope.launch {
                                    val res = aiAudioService.playSample()
                                    isGeneratingSample = false
                                    res.onFailure {
                                        snackbarHost.showSnackbar(it.message ?: "Failed to play voice sample")
                                    }
                                }
                            }
                        },
                        enabled = !isGeneratingSample
                    ) {
                        if (isGeneratingSample) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.ai_audio_generating))
                        } else {
                            Icon(
                                imageVector = if (isSamplePlaying) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                if (isSamplePlaying) {
                                    stringResource(R.string.ai_settings_stop_sample)
                                } else {
                                    stringResource(R.string.ai_settings_play_sample)
                                }
                            )
                        }
                    }
                }
            }

            FormSection(title = stringResource(R.string.ai_settings_section_cache)) {
                val clearSuccessMessage = stringResource(R.string.ai_settings_clear_cache_success)
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.ai_settings_clear_cache))
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.clickable {
                        scope.launch {
                            summaryRepository.clearAll()
                            aiAudioService.clearCache()
                            snackbarHost.showSnackbar(clearSuccessMessage)
                        }
                    }
                )
            }
        }
}
