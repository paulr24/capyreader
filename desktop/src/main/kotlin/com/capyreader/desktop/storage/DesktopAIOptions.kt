package com.capyreader.desktop.storage

import com.jocmp.capy.preferences.Preference
import com.jocmp.capy.preferences.PreferenceStore
import com.jocmp.capy.preferences.getEnum

enum class DesktopAIProvider {
    GEMINI,
    OPENAI_COMPATIBLE;

    companion object {
        val default = GEMINI
    }
}

object DesktopGeminiModels {
    const val FLASH_3_5_LITE = "gemini-3.5-flash-lite"
    const val FLASH_3_7 = "gemini-3.7-flash"
    const val FLASH_2_5_LITE = "gemini-2.5-flash-lite"
    const val FLASH_2_5 = "gemini-2.5-flash"

    val default = FLASH_3_5_LITE

    val presets = listOf(
        FLASH_3_5_LITE to "Gemini 3.5 Flash Lite (Fastest)",
        FLASH_3_7 to "Gemini 3.7 Flash (High Quality)",
        FLASH_2_5_LITE to "Gemini 2.5 Flash Lite",
        FLASH_2_5 to "Gemini 2.5 Flash",
    )
}

const val DEFAULT_DESKTOP_AI_PROMPT_TEMPLATE =
    "Summarize the following article concisely. Provide a 2-3 sentence overview followed by 3-5 bullet points highlighting the main takeaways.\n\nTitle: %title%\n\n%content%"

class DesktopAIOptions(private val preferenceStore: PreferenceStore) {
    val enabled: Preference<Boolean>
        get() = preferenceStore.getBoolean("ai_enabled", false)

    val provider: Preference<DesktopAIProvider>
        get() = preferenceStore.getEnum("ai_provider", DesktopAIProvider.default)

    val geminiApiKey: Preference<String>
        get() = preferenceStore.getString("ai_gemini_api_key", "")

    val geminiModel: Preference<String>
        get() = preferenceStore.getString("ai_gemini_model", DesktopGeminiModels.default)

    val openAiApiKey: Preference<String>
        get() = preferenceStore.getString("ai_openai_api_key", "")

    val openAiEndpoint: Preference<String>
        get() = preferenceStore.getString("ai_openai_endpoint", "https://api.openai.com/v1")

    val openAiModel: Preference<String>
        get() = preferenceStore.getString("ai_openai_model", "gpt-4o-mini")

    val promptTemplate: Preference<String>
        get() = preferenceStore.getString("ai_prompt_template", DEFAULT_DESKTOP_AI_PROMPT_TEMPLATE)

    val autoSummarize: Preference<Boolean>
        get() = preferenceStore.getBoolean("ai_auto_summarize", false)

    fun isConfigured(): Boolean {
        return when (provider.get()) {
            DesktopAIProvider.GEMINI -> geminiApiKey.get().isNotBlank()
            DesktopAIProvider.OPENAI_COMPATIBLE -> openAiApiKey.get().isNotBlank()
        }
    }

    fun currentModel(): String {
        return when (provider.get()) {
            DesktopAIProvider.GEMINI -> geminiModel.get()
            DesktopAIProvider.OPENAI_COMPATIBLE -> openAiModel.get()
        }
    }

    fun currentModelDisplayName(): String {
        val model = currentModel()
        return when (model) {
            DesktopGeminiModels.FLASH_3_5_LITE -> "Gemini 3.5 Flash Lite"
            DesktopGeminiModels.FLASH_3_7 -> "Gemini 3.7 Flash"
            DesktopGeminiModels.FLASH_2_5_LITE -> "Gemini 2.5 Flash Lite"
            DesktopGeminiModels.FLASH_2_5 -> "Gemini 2.5 Flash"
            "gpt-4o-mini" -> "GPT-4o Mini"
            "gpt-4o" -> "GPT-4o"
            else -> model
        }
    }
}
