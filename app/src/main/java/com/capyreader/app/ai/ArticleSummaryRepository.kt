package com.capyreader.app.ai

import android.content.Context
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class ArticleSummaryRepository(context: Context) {
    private val cacheDir = File(context.cacheDir, "ai_summaries").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val memoryCache = LruCache<String, String>(MAX_MEMORY_CACHE_ENTRIES)

    suspend fun get(articleId: String): String? = withContext(Dispatchers.IO) {
        val inMemory = memoryCache.get(articleId)
        if (inMemory != null) {
            return@withContext inMemory
        }

        val file = cacheFile(articleId)
        if (file.exists()) {
            try {
                val text = file.readText()
                if (text.isNotBlank()) {
                    memoryCache.put(articleId, text)
                    return@withContext text
                }
            } catch (e: Exception) {
                file.delete()
            }
        }
        null
    }

    suspend fun put(articleId: String, summary: String) = withContext(Dispatchers.IO) {
        memoryCache.put(articleId, summary)
        try {
            cacheFile(articleId).writeText(summary)
        } catch (_: Exception) {
        }
    }

    suspend fun remove(articleId: String) = withContext(Dispatchers.IO) {
        memoryCache.remove(articleId)
        try {
            cacheFile(articleId).delete()
        } catch (_: Exception) {
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        memoryCache.evictAll()
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    private fun cacheFile(articleId: String): File {
        val hash = sha256(articleId)
        return File(cacheDir, "$hash.txt")
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MAX_MEMORY_CACHE_ENTRIES = 50
    }
}
