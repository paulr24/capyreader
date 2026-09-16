package com.capyreader.desktop.ai

import com.capyreader.desktop.storage.DesktopPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class DesktopArticleSummaryRepository(
    baseDir: File = DesktopPaths.cacheDir
) {
    private val cacheDir = File(baseDir, "ai_summaries").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val memoryCache = ConcurrentHashMap<String, String>()

    suspend fun get(articleId: String): String? = withContext(Dispatchers.IO) {
        val inMemory = memoryCache[articleId]
        if (inMemory != null) {
            return@withContext inMemory
        }

        val file = cacheFile(articleId)
        if (file.exists()) {
            try {
                val text = file.readText()
                if (text.isNotBlank()) {
                    memoryCache[articleId] = text
                    return@withContext text
                }
            } catch (e: Exception) {
                file.delete()
            }
        }
        null
    }

    suspend fun put(articleId: String, summary: String) = withContext(Dispatchers.IO) {
        memoryCache[articleId] = summary
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
        memoryCache.clear()
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
}
