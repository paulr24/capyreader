package com.capyreader.app.preferences

import com.jocmp.capy.preferences.Preference
import com.jocmp.capy.preferences.PreferenceStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIOptionsTest {
    private val testStore = SimpleTestPreferenceStore()
    private val options = AIOptions(testStore)

    @Test
    fun defaultValues() {
        assertFalse(options.enabled.get())
        assertEquals(AIProvider.GEMINI, options.provider.get())
        assertEquals(GeminiModels.FLASH_3_5_LITE, options.geminiModel.get())
        assertEquals("", options.geminiApiKey.get())
        assertEquals("https://api.openai.com/v1", options.openAiEndpoint.get())
        assertEquals("gpt-4o-mini", options.openAiModel.get())
        assertEquals("", options.openAiApiKey.get())
        assertFalse(options.autoSummarize.get())
        assertFalse(options.isConfigured())
        assertEquals(GeminiModels.FLASH_3_5_LITE, options.currentModel())
        assertEquals(AIAudioProvider.GEMINI, options.audioProvider.get())
        assertEquals(GeminiAudioModels.FLASH_TTS_3_1, options.geminiAudioModel.get())
        assertEquals(GeminiVoices.KORE, options.geminiVoice.get())
        assertEquals(OpenAIVoices.ALLOY, options.openAiVoice.get())
    }

    @Test
    fun isConfigured_gemini() {
        options.provider.set(AIProvider.GEMINI)
        assertFalse(options.isConfigured())

        options.geminiApiKey.set("test-key")
        assertTrue(options.isConfigured())
    }

    @Test
    fun isConfigured_openAi() {
        options.provider.set(AIProvider.OPENAI_COMPATIBLE)
        assertFalse(options.isConfigured())

        options.openAiApiKey.set("test-openai-key")
        assertTrue(options.isConfigured())
    }

    @Test
    fun currentModel_reflectsActiveProvider() {
        options.provider.set(AIProvider.GEMINI)
        options.geminiModel.set("gemini-3.7-flash")
        assertEquals("gemini-3.7-flash", options.currentModel())

        options.provider.set(AIProvider.OPENAI_COMPATIBLE)
        options.openAiModel.set("llama-3.3-70b")
        assertEquals("llama-3.3-70b", options.currentModel())
    }

    private class SimpleTestPreferenceStore : PreferenceStore {
        val map = mutableMapOf<String, Any>()

        override fun getString(key: String, defaultValue: String): Preference<String> =
            SimpleTestPreference(key, defaultValue, map)

        override fun getLong(key: String, defaultValue: Long): Preference<Long> =
            SimpleTestPreference(key, defaultValue, map)

        override fun getInt(key: String, defaultValue: Int): Preference<Int> =
            SimpleTestPreference(key, defaultValue, map)

        override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
            SimpleTestPreference(key, defaultValue, map)

        override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
            SimpleTestPreference(key, defaultValue, map)

        override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
            SimpleTestPreference(key, defaultValue, map)

        override fun <T> getObject(
            key: String,
            defaultValue: T,
            serializer: (T) -> String,
            deserializer: (String) -> T
        ): Preference<T> =
            SimpleTestPreference(key, defaultValue, map)

        override fun clearAll() {
            map.clear()
        }
    }

    private class SimpleTestPreference<T>(
        private val key: String,
        private val defaultValue: T,
        private val map: MutableMap<String, Any>
    ) : Preference<T> {
        @Suppress("UNCHECKED_CAST")
        override fun get(): T = map[key] as? T ?: defaultValue

        override fun set(value: T) {
            map[key] = value as Any
        }

        override val state: Flow<T>
            get() = flowOf(get())

        override fun delete() {
            map.remove(key)
        }

        override val isSet: Boolean
            get() = map.containsKey(key)
    }
}
