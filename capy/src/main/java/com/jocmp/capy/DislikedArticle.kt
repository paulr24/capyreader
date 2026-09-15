package com.jocmp.capy

import java.time.ZonedDateTime

data class DislikedArticle(
    val id: String,
    val feedID: String,
    val articleTitle: String,
    val createdAt: ZonedDateTime,
)
