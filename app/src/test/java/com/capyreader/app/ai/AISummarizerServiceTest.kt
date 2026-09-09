package com.capyreader.app.ai

import com.capyreader.app.preferences.AIOptions
import com.capyreader.app.preferences.AIProvider
import com.capyreader.app.preferences.AppPreferences
import com.jocmp.capy.Article
import com.jocmp.capy.preferences.Preference
import com.jocmp.capy.preferences.PreferenceStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.time.ZonedDateTime

class AISummarizerServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var httpClient: OkHttpClient
    private lateinit var testStore: SimpleTestPreferenceStore
    private lateinit var appPreferences: AppPreferences
    private lateinit var service: AISummarizerService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        httpClient = OkHttpClient.Builder().build()
        testStore = SimpleTestPreferenceStore()

        val aiOptions = AIOptions(testStore)
        appPreferences = mockk {
            every { this@mockk.aiOptions } returns aiOptions
        }

        service = AISummarizerService(httpClient, appPreferences)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun summarize_openAiCompatible_parsesResponse() = runTest {
        val baseUrl = server.url("/v1").toString()
        val options = appPreferences.aiOptions
        options.provider.set(AIProvider.OPENAI_COMPATIBLE)
        options.openAiEndpoint.set(baseUrl)
        options.openAiApiKey.set("test-key")
        options.openAiModel.set("test-model")

        val responseJson = """
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "This is the generated AI summary."
                  }
                }
              ]
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseJson)
        )

        val article = createArticle(contentHTML = "<p>Here is some HTML article content.</p>")
        val result = service.summarize(article)

        assertTrue(result.isSuccess)
        assertEquals("This is the generated AI summary.", result.getOrNull())

        val recordedRequest = server.takeRequest()
        assertEquals("Bearer test-key", recordedRequest.headers["Authorization"])
        assertTrue(recordedRequest.body.readUtf8().contains("Here is some HTML article content."))
    }

    @Test
    fun summarize_emptyContent_returnsFailure() = runTest {
        val article = createArticle(contentHTML = "")
        val result = service.summarize(article)

        assertTrue(result.isFailure)
    }

    private fun createArticle(contentHTML: String): Article {
        return Article(
            id = "test-article-1",
            feedID = "feed-1",
            title = "Test Article Title",
            author = "Author",
            contentHTML = contentHTML,
            url = URL("https://example.com/article-1"),
            summary = "",
            imageURL = null,
            updatedAt = ZonedDateTime.now(),
            publishedAt = ZonedDateTime.now(),
            read = false,
            starred = false,
        )
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

        override val state = flowOf(get())

        override fun delete() {
            map.remove(key)
        }

        override val isSet: Boolean
            get() = map.containsKey(key)
    }
}
