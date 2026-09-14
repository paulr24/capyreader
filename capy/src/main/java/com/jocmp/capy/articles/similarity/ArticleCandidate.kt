package com.jocmp.capy.articles.similarity

data class ArticleCandidate(
    val id: String,
    val feedID: String,
    val title: String,
    val publishedAt: Long,
    val feedTitle: String = "",
)
