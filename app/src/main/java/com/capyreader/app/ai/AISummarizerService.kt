package com.capyreader.app.ai

import com.capyreader.app.preferences.AIOptions
import com.capyreader.app.preferences.AIProvider
import com.capyreader.app.preferences.AppPreferences
import com.jocmp.capy.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class AISummarizerService(
    private val httpClient: OkHttpClient,
    private val appPreferences: AppPreferences,
) {
    private val aiOptions: AIOptions
        get() = appPreferences.aiOptions

    fun summarizeStream(article: Article): Flow<String> = flow {
        val provider = aiOptions.provider.get()
        val rawContent = article.content.ifBlank { article.summary }
        val cleanContent = extractCleanText(rawContent)

        if (cleanContent.isBlank()) {
            throw IllegalStateException("Article content is empty")
        }

        val prompt = buildPrompt(
            template = aiOptions.promptTemplate.get(),
            title = article.title,
            content = cleanContent,
        )

        when (provider) {
            AIProvider.GEMINI -> streamGemini(
                prompt = prompt,
                model = aiOptions.geminiModel.get(),
                apiKey = aiOptions.geminiApiKey.get()
            ) { chunk -> emit(chunk) }

            AIProvider.OPENAI_COMPATIBLE -> streamOpenAI(
                prompt = prompt,
                endpoint = aiOptions.openAiEndpoint.get(),
                model = aiOptions.openAiModel.get(),
                apiKey = aiOptions.openAiApiKey.get()
            ) { chunk -> emit(chunk) }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun summarize(article: Article): Result<String> = withContext(Dispatchers.IO) {
        try {
            val provider = aiOptions.provider.get()
            val rawContent = article.content.ifBlank { article.summary }
            val cleanContent = extractCleanText(rawContent)

            if (cleanContent.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Article content is empty"))
            }

            val prompt = buildPrompt(
                template = aiOptions.promptTemplate.get(),
                title = article.title,
                content = cleanContent,
            )

            val summary = when (provider) {
                AIProvider.GEMINI -> callGemini(
                    prompt = prompt,
                    model = aiOptions.geminiModel.get(),
                    apiKey = aiOptions.geminiApiKey.get()
                )
                AIProvider.OPENAI_COMPATIBLE -> callOpenAI(
                    prompt = prompt,
                    endpoint = aiOptions.openAiEndpoint.get(),
                    model = aiOptions.openAiModel.get(),
                    apiKey = aiOptions.openAiApiKey.get()
                )
            }

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val provider = aiOptions.provider.get()
            val testPrompt = "Please respond with 'Connected successfully' to confirm connectivity."

            val result = when (provider) {
                AIProvider.GEMINI -> callGemini(
                    prompt = testPrompt,
                    model = aiOptions.geminiModel.get(),
                    apiKey = aiOptions.geminiApiKey.get()
                )
                AIProvider.OPENAI_COMPATIBLE -> callOpenAI(
                    prompt = testPrompt,
                    endpoint = aiOptions.openAiEndpoint.get(),
                    model = aiOptions.openAiModel.get(),
                    apiKey = aiOptions.openAiApiKey.get()
                )
            }

            Result.success(result.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractCleanText(html: String): String {
        val parsed = Jsoup.parse(html).text()
        return if (parsed.length > MAX_ARTICLE_TEXT_LENGTH) {
            parsed.take(MAX_ARTICLE_TEXT_LENGTH) + "…"
        } else {
            parsed
        }
    }

    private fun buildPrompt(template: String, title: String, content: String): String {
        return if (template.contains("%title%") || template.contains("%content%")) {
            template
                .replace("%title%", title)
                .replace("%content%", content)
        } else {
            "$template\n\nTitle: $title\n\n$content"
        }
    }

    private fun buildGeminiRequestBody(prompt: String, model: String = ""): String {
        val partObject = JSONObject().apply {
            put("text", prompt)
        }
        val partsArray = JSONArray().apply {
            put(partObject)
        }
        val contentObject = JSONObject().apply {
            put("parts", partsArray)
        }
        val contentsArray = JSONArray().apply {
            put(contentObject)
        }

        val generationConfig = JSONObject().apply {
            put("temperature", 0.3)
            put("maxOutputTokens", 800)
            if (model.isBlank() || isThinkingSupported(model)) {
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingBudget", 0)
                })
            }
        }

        return JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", generationConfig)
        }.toString()
    }

    private fun isThinkingSupported(model: String): Boolean {
        return !model.contains("1.5") && !model.contains("1.0")
    }

    private inline fun streamGemini(
        prompt: String,
        model: String,
        apiKey: String,
        onChunk: (String) -> Unit,
    ) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key is not configured")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"
        val requestBody = buildGeminiRequestBody(prompt, model).toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            throw IOException(parseErrorMessage(errBody, "HTTP ${response.code}: ${response.message}"))
        }

        response.body?.source()?.let { source ->
            val reader = BufferedReader(InputStreamReader(source.inputStream(), Charsets.UTF_8))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                if (currentLine.startsWith("data: ")) {
                    val jsonStr = currentLine.removePrefix("data: ").trim()
                    if (jsonStr.isNotBlank() && jsonStr != "[DONE]") {
                        try {
                            val json = JSONObject(jsonStr)
                            val candidates = json.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    val text = parts.getJSONObject(0).optString("text")
                                    if (text.isNotEmpty()) {
                                        onChunk(text)
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    private inline fun streamOpenAI(
        prompt: String,
        endpoint: String,
        model: String,
        apiKey: String,
        onChunk: (String) -> Unit,
    ) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API key is not configured")
        }

        val baseUrl = endpoint.trim().trimEnd('/')
        val url = if (baseUrl.endsWith("/chat/completions")) {
            baseUrl
        } else {
            "$baseUrl/chat/completions"
        }

        val messageObject = JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        }
        val messagesArray = JSONArray().apply {
            put(messageObject)
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("temperature", 0.3)
            put("max_tokens", 800)
            put("stream", true)
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            throw IOException(parseErrorMessage(errBody, "HTTP ${response.code}: ${response.message}"))
        }

        response.body?.source()?.let { source ->
            val reader = BufferedReader(InputStreamReader(source.inputStream(), Charsets.UTF_8))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: break
                if (currentLine.startsWith("data: ")) {
                    val jsonStr = currentLine.removePrefix("data: ").trim()
                    if (jsonStr == "[DONE]") break
                    if (jsonStr.isNotBlank()) {
                        try {
                            val json = JSONObject(jsonStr)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val text = delta?.optString("content").orEmpty()
                                if (text.isNotEmpty()) {
                                    onChunk(text)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
    }

    private fun callGemini(prompt: String, model: String, apiKey: String): String {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key is not configured")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val requestBody = buildGeminiRequestBody(prompt, model).toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMessage = parseErrorMessage(responseBody, defaultMessage = "HTTP ${response.code}: ${response.message}")
            throw IOException(errorMessage)
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val text = parts.getJSONObject(0).optString("text")
                if (text.isNotBlank()) {
                    return text
                }
            }
        }

        throw IOException("No summary content generated by Gemini")
    }

    private fun callOpenAI(
        prompt: String,
        endpoint: String,
        model: String,
        apiKey: String,
    ): String {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API key is not configured")
        }

        val baseUrl = endpoint.trim().trimEnd('/')
        val url = if (baseUrl.endsWith("/chat/completions")) {
            baseUrl
        } else {
            "$baseUrl/chat/completions"
        }

        val messageObject = JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        }
        val messagesArray = JSONArray().apply {
            put(messageObject)
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("temperature", 0.3)
            put("max_tokens", 800)
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMessage = parseErrorMessage(responseBody, defaultMessage = "HTTP ${response.code}: ${response.message}")
            throw IOException(errorMessage)
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val choice = choices.getJSONObject(0)
            val message = choice.optJSONObject("message")
            val text = message?.optString("content").orEmpty()
            if (text.isNotBlank()) {
                return text
            }
        }

        throw IOException("No summary content generated by AI service")
    }

    private fun parseErrorMessage(responseBody: String, defaultMessage: String): String {
        return try {
            val json = JSONObject(responseBody)
            if (json.has("error")) {
                val errorObj = json.optJSONObject("error")
                errorObj?.optString("message") ?: json.optString("error")
            } else {
                defaultMessage
            }
        } catch (_: Exception) {
            defaultMessage
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val MAX_ARTICLE_TEXT_LENGTH = 10000
    }
}
