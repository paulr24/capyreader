package com.capyreader.app.preferences

import com.jocmp.capy.preferences.Preference
import com.jocmp.capy.preferences.PreferenceStore
import com.jocmp.capy.preferences.getEnum

enum class AIProvider {
    GEMINI,
    OPENAI_COMPATIBLE;

    companion object {
        val default = GEMINI
    }
}

object GeminiModels {
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

enum class AIAudioProvider {
    GEMINI,
    SYSTEM,
    OPENAI;

    companion object {
        val default = GEMINI
    }
}

object GeminiAudioModels {
    const val FLASH_3_1_TTS = "gemini-3.1-flash-tts-preview"
    const val PRO_2_5_TTS = "gemini-2.5-pro-preview-tts"
    const val FLASH_2_5_TTS = "gemini-2.5-flash-preview-tts"

    val default = FLASH_3_1_TTS

    val presets = listOf(
        FLASH_3_1_TTS to "Gemini 3.1 Flash TTS (Latest)",
        PRO_2_5_TTS to "Gemini 2.5 Pro TTS (High Quality)",
        FLASH_2_5_TTS to "Gemini 2.5 Flash TTS (Fast)",
    )
}

object GeminiVoices {
    const val KORE = "Kore"
    const val PUCK = "Puck"
    const val AOEDE = "Aoede"
    const val CHARON = "Charon"
    const val FENRIR = "Fenrir"

    val default = KORE

    val presets = listOf(
        KORE to "Kore (Warm Female)",
        PUCK to "Puck (Engaging Male)",
        AOEDE to "Aoede (Melodic Female)",
        CHARON to "Charon (Deep Male)",
        FENRIR to "Fenrir (Crisp Male)",
    )
}

object OpenAIVoices {
    const val ALLOY = "alloy"
    const val ECHO = "echo"
    const val FABLE = "fable"
    const val ONYX = "onyx"
    const val NOVA = "nova"
    const val SHIMMER = "shimmer"

    val default = ALLOY

    val presets = listOf(
        ALLOY to "Alloy (Neutral)",
        ECHO to "Echo (Warm)",
        FABLE to "Fable (Expressive British)",
        ONYX to "Onyx (Deep Authoritative)",
        NOVA to "Nova (Energetic)",
        SHIMMER to "Shimmer (Clear Melodic)",
    )
}

const val DEFAULT_AI_PROMPT_TEMPLATE =
    "Summarize the following article concisely. Provide a 2-3 sentence overview followed by 3-5 bullet points highlighting the main takeaways.\n\nTitle: %title%\n\n%content%"

class AIOptions(private val preferenceStore: PreferenceStore) {
    val enabled: Preference<Boolean>
        get() = preferenceStore.getBoolean("ai_enabled", false)

    val provider: Preference<AIProvider>
        get() = preferenceStore.getEnum("ai_provider", AIProvider.default)

    val geminiApiKey: Preference<String>
        get() = preferenceStore.getString("ai_gemini_api_key", "")

    val geminiModel: Preference<String>
        get() = preferenceStore.getString("ai_gemini_model", GeminiModels.default)

    val openAiApiKey: Preference<String>
        get() = preferenceStore.getString("ai_openai_api_key", "")

    val openAiEndpoint: Preference<String>
        get() = preferenceStore.getString("ai_openai_endpoint", "https://api.openai.com/v1")

    val openAiModel: Preference<String>
        get() = preferenceStore.getString("ai_openai_model", "gpt-4o-mini")

    val promptTemplate: Preference<String>
        get() = preferenceStore.getString("ai_prompt_template", DEFAULT_AI_PROMPT_TEMPLATE)

    val autoSummarize: Preference<Boolean>
        get() = preferenceStore.getBoolean("ai_auto_summarize", false)

    val audioProvider: Preference<AIAudioProvider>
        get() = preferenceStore.getEnum("ai_audio_provider", AIAudioProvider.default)

    val geminiAudioModel: Preference<String>
        get() = preferenceStore.getString("ai_gemini_audio_model", GeminiAudioModels.default)

    val geminiVoice: Preference<String>
        get() = preferenceStore.getString("ai_gemini_voice", GeminiVoices.default)

    val openAiVoice: Preference<String>
        get() = preferenceStore.getString("ai_openai_voice", OpenAIVoices.default)

    fun isConfigured(): Boolean {
        return when (provider.get()) {
            AIProvider.GEMINI -> geminiApiKey.get().isNotBlank()
            AIProvider.OPENAI_COMPATIBLE -> openAiApiKey.get().isNotBlank()
        }
    }

    fun currentModel(): String {
        return when (provider.get()) {
            AIProvider.GEMINI -> geminiModel.get()
            AIProvider.OPENAI_COMPATIBLE -> openAiModel.get()
        }
    }
}
