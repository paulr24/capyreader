package com.jocmp.capy.persistence

import com.jocmp.capy.DislikedArticle
import com.jocmp.capy.common.TimeHelpers.nowUTC
import com.jocmp.capy.common.toDateTimeFromSeconds
import com.jocmp.capy.db.Database
import java.time.ZonedDateTime

class DislikedArticleRecords(
    private val database: Database
) {
    fun add(id: String, feedID: String, articleTitle: String, createdAt: ZonedDateTime = nowUTC()) {
        database.dislikedArticlesQueries.add(
            id = id,
            feedID = feedID,
            articleTitle = articleTitle,
            createdAt = createdAt.toEpochSecond()
        )
    }

    fun delete(id: String) {
        database.dislikedArticlesQueries.deleteByID(id = id)
    }

    fun allActive(since: ZonedDateTime = nowUTC().minusDays(7)): List<DislikedArticle> {
        return database.dislikedArticlesQueries
            .allActive(sinceEpochSeconds = since.toEpochSecond()) { id, feedID, articleTitle, createdAt ->
                DislikedArticle(
                    id = id,
                    feedID = feedID,
                    articleTitle = articleTitle,
                    createdAt = toDateTimeFromSeconds(createdAt)
                )
            }
            .executeAsList()
    }

    fun all(): List<DislikedArticle> {
        return database.dislikedArticlesQueries
            .all { id, feedID, articleTitle, createdAt ->
                DislikedArticle(
                    id = id,
                    feedID = feedID,
                    articleTitle = articleTitle,
                    createdAt = toDateTimeFromSeconds(createdAt)
                )
            }
            .executeAsList()
    }

    fun deleteOlderThan(cutoff: ZonedDateTime = nowUTC().minusDays(30)) {
        database.dislikedArticlesQueries.deleteOlderThan(cutoffEpochSeconds = cutoff.toEpochSecond())
    }
}
