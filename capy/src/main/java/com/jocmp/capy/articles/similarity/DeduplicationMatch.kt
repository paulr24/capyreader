package com.jocmp.capy.articles.similarity

import kotlinx.serialization.Serializable

@Serializable
data class DeduplicationMatch(
    val keptArticleId: String,
    val keptArticleTitle: String,
    val keptFeedTitle: String,
    val duplicateArticleId: String,
    val duplicateArticleTitle: String,
    val duplicateFeedTitle: String,
    val similarityPercentage: Int,
    val timestamp: Long = System.currentTimeMillis() / 1000L,
)

data class DeduplicationResult(
    val duplicateIDs: List<String>,
    val matches: List<DeduplicationMatch>,
)
