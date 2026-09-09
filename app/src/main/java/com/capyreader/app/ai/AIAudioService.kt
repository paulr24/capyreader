package com.capyreader.app.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import com.capyreader.app.preferences.AIAudioProvider
import com.capyreader.app.preferences.AIOptions
import com.capyreader.app.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume

class AIAudioService(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val appPreferences: AppPreferences,
) {
    private val aiOptions: AIOptions
        get() = appPreferences.aiOptions

    private val audioDir: File by lazy {
        File(context.cacheDir, "ai_audio").apply {
            if (!exists()) mkdirs()
        }
    }

    private var samplePlayer: MediaPlayer? = null
    private val _isSamplePlaying = MutableStateFlow(false)
    val isSamplePlaying: StateFlow<Boolean> = _isSamplePlaying.asStateFlow()

    suspend fun generateAudio(text: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanText = text.trim()
            if (cleanText.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Text is empty"))
            }

            val provider = aiOptions.audioProvider.get()
            val cacheKey = buildCacheKey(cleanText, provider)
            val cachedFile = File(audioDir, "$cacheKey.wav")
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return@withContext Result.success(cachedFile)
            }

            val resultFile = when (provider) {
                AIAudioProvider.GEMINI -> generateGeminiSpeech(cleanText, cachedFile)
                AIAudioProvider.SYSTEM -> generateSystemSpeech(cleanText, cachedFile)
                AIAudioProvider.OPENAI -> generateOpenAISpeech(cleanText, File(audioDir, "$cacheKey.mp3"))
            }

            Result.success(resultFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun playSample(voiceName: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            stopSample()

            val sampleText = "Hello! I am ready to read your articles and summaries."
            val provider = aiOptions.audioProvider.get()
            val voice = voiceName ?: when (provider) {
                AIAudioProvider.GEMINI -> aiOptions.geminiVoice.get()
                AIAudioProvider.OPENAI -> aiOptions.openAiVoice.get()
                AIAudioProvider.SYSTEM -> "system"
            }

            val cacheKey = "sample_${provider.name.lowercase()}_$voice"
            val sampleFile = File(audioDir, "$cacheKey.wav")

            if (!sampleFile.exists() || sampleFile.length() == 0L) {
                when (provider) {
                    AIAudioProvider.GEMINI -> generateGeminiSpeech(sampleText, sampleFile, voiceOverride = voice)
                    AIAudioProvider.SYSTEM -> generateSystemSpeech(sampleText, sampleFile)
                    AIAudioProvider.OPENAI -> generateOpenAISpeech(sampleText, File(audioDir, "$cacheKey.mp3"), voiceOverride = voice)
                }
            }

            val targetPlayFile = if (sampleFile.exists()) sampleFile else File(audioDir, "$cacheKey.mp3")

            withContext(Dispatchers.Main) {
                samplePlayer?.release()
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(targetPlayFile.absolutePath)
                    setOnCompletionListener {
                        _isSamplePlaying.value = false
                    }
                    setOnErrorListener { _, _, _ ->
                        _isSamplePlaying.value = false
                        false
                    }
                    prepare()
                    start()
                }
                samplePlayer = player
                _isSamplePlaying.value = true
            }

            Result.success(Unit)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _isSamplePlaying.value = false
            }
            Result.failure(e)
        }
    }

    fun stopSample() {
        try {
            samplePlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            samplePlayer = null
            _isSamplePlaying.value = false
        }
    }

    private fun generateGeminiSpeech(
        text: String,
        outputFile: File,
        voiceOverride: String? = null
    ): File {
        val apiKey = aiOptions.geminiApiKey.get()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key is not configured")
        }

        val model = aiOptions.geminiAudioModel.get().ifBlank { "gemini-3.1-flash-tts-preview" }
        val voice = voiceOverride ?: aiOptions.geminiVoice.get().ifBlank { "Kore" }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val partObj = JSONObject().apply {
            put("text", "Read the following text aloud clearly and naturally:\n\n$text")
        }
        val contentsArr = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply { put(partObj) })
            })
        }

        val prebuiltVoiceConfig = JSONObject().apply {
            put("voiceName", voice)
        }
        val voiceConfig = JSONObject().apply {
            put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        }
        val speechConfig = JSONObject().apply {
            put("voiceConfig", voiceConfig)
        }

        val generationConfig = JSONObject().apply {
            put("responseModalities", JSONArray().apply { put("AUDIO") })
            put("speechConfig", speechConfig)
        }

        val requestJson = JSONObject().apply {
            put("contents", contentsArr)
            put("generationConfig", generationConfig)
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = parseErrorMessage(responseBody, "HTTP ${response.code}: ${response.message}")
            throw IOException(errorMsg)
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                val inlineData = parts.getJSONObject(0).optJSONObject("inlineData")
                val base64Data = inlineData?.optString("data").orEmpty()
                if (base64Data.isNotBlank()) {
                    val rawAudioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    writeAudioBytesWithWavHeader(rawAudioBytes, outputFile)
                    return outputFile
                }
            }
        }

        throw IOException("No audio data generated by Gemini")
    }

    private fun writeAudioBytesWithWavHeader(bytes: ByteArray, file: File) {
        FileOutputStream(file).use { fos ->
            if (bytes.size >= 4 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte()
            ) {
                fos.write(bytes)
            } else {
                val wavHeader = createWavHeader(pcmDataSize = bytes.size, sampleRate = 24000)
                fos.write(wavHeader)
                fos.write(bytes)
            }
        }
    }

    private suspend fun generateSystemSpeech(text: String, outputFile: File): File {
        return suspendCancellableCoroutine { continuation ->
            var tts: TextToSpeech? = null
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    try {
                        tts.language = Locale.getDefault()
                        val utteranceId = "system_tts_${System.currentTimeMillis()}"

                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}

                            override fun onDone(utteranceId: String?) {
                                tts?.shutdown()
                                continuation.resume(outputFile)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                tts?.shutdown()
                                continuation.resumeWith(Result.failure(IOException("System TTS error")))
                            }
                        })

                        val params = Bundle()
                        val result = tts.synthesizeToFile(text, params, outputFile, utteranceId)
                        if (result != TextToSpeech.SUCCESS) {
                            tts.shutdown()
                            continuation.resumeWith(Result.failure(IOException("Failed to synthesize system audio")))
                        }
                    } catch (e: Exception) {
                        tts.shutdown()
                        continuation.resumeWith(Result.failure(e))
                    }
                } else {
                    tts?.shutdown()
                    continuation.resumeWith(Result.failure(IOException("System TextToSpeech initialization failed")))
                }
            }

            continuation.invokeOnCancellation {
                tts?.shutdown()
            }
        }
    }

    private fun generateOpenAISpeech(
        text: String,
        outputFile: File,
        voiceOverride: String? = null
    ): File {
        val apiKey = aiOptions.openAiApiKey.get()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("API key is not configured")
        }

        val baseUrl = aiOptions.openAiEndpoint.get().trim().trimEnd('/')
        val url = if (baseUrl.endsWith("/audio/speech")) baseUrl else "$baseUrl/audio/speech"
        val voice = voiceOverride ?: aiOptions.openAiVoice.get().ifBlank { "alloy" }

        val requestJson = JSONObject().apply {
            put("model", "tts-1")
            put("input", text)
            put("voice", voice)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string().orEmpty()
            throw IOException(parseErrorMessage(errBody, "HTTP ${response.code}: ${response.message}"))
        }

        response.body?.byteStream()?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Empty audio response body")

        return outputFile
    }

    private fun createWavHeader(
        pcmDataSize: Int,
        sampleRate: Int = 24000,
        channels: Short = 1,
        bitsPerSample: Short = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = pcmDataSize + 36
        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalDataLen)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channels)
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(bitsPerSample)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmDataSize)

        return header
    }

    private fun buildCacheKey(text: String, provider: AIAudioProvider): String {
        val voice = when (provider) {
            AIAudioProvider.GEMINI -> "${aiOptions.geminiAudioModel.get()}_${aiOptions.geminiVoice.get()}"
            AIAudioProvider.OPENAI -> "tts-1_${aiOptions.openAiVoice.get()}"
            AIAudioProvider.SYSTEM -> "system"
        }
        val input = "${provider.name}_${voice}_$text"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
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

    fun clearCache() {
        try {
            audioDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
