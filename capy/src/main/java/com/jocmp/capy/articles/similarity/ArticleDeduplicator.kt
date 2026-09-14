package com.jocmp.capy.articles.similarity

import kotlin.math.abs

object ArticleDeduplicator {

    /**
     * Finds duplicate article IDs that should be marked as read along with match details.
     *
     * @param candidates Unread, non-starred articles sorted by publishedAt DESC.
     * @param preferredFeedIDs Ordered list of preferred feed IDs (index 0 has highest priority).
     * @param bypassKeywords Keywords that prevent matching if found in the title.
     * @param similarityThreshold Minimum similarity score [0.0..1.0] to consider articles duplicates.
     * @param timeWindowSeconds Maximum time difference in seconds between two articles.
     * @return DeduplicationResult containing duplicate IDs and match details.
     */
    fun findDuplicates(
        candidates: List<ArticleCandidate>,
        preferredFeedIDs: List<String> = emptyList(),
        bypassKeywords: Set<String> = emptySet(),
        similarityThreshold: Float = 0.90f,
        timeWindowSeconds: Long = 48 * 3600L,
    ): DeduplicationResult {
        if (candidates.size < 2) return DeduplicationResult(emptyList(), emptyList())

        // 1. Filter out articles matching bypass keywords
        val eligibleCandidates = candidates.filter { candidate ->
            !ArticleSimilarity.matchesBypassKeywords(candidate.title, bypassKeywords)
        }

        if (eligibleCandidates.size < 2) return DeduplicationResult(emptyList(), emptyList())

        val n = eligibleCandidates.size
        val parent = IntArray(n) { it }

        fun find(i: Int): Int {
            var root = i
            while (root != parent[root]) {
                root = parent[root]
            }
            var curr = i
            while (curr != root) {
                val next = parent[curr]
                parent[curr] = root
                curr = next
            }
            return root
        }

        fun union(i: Int, j: Int) {
            val rootI = find(i)
            val rootJ = find(j)
            if (rootI != rootJ) {
                parent[rootI] = rootJ
            }
        }

        // 2. Pairwise comparison within the time window
        for (i in 0 until n) {
            val a = eligibleCandidates[i]
            for (j in i + 1 until n) {
                val b = eligibleCandidates[j]

                // Since candidates are sorted descending by publication date:
                val timeDiff = a.publishedAt - b.publishedAt
                if (timeDiff > timeWindowSeconds) {
                    // All subsequent candidates will have even older publishedAt
                    break
                }

                // If published in reverse order somehow, check absolute diff:
                if (abs(timeDiff) > timeWindowSeconds) {
                    continue
                }

                // Articles from the exact same feed should not deduplicate each other
                if (a.feedID == b.feedID) {
                    continue
                }

                // Fast length filter: if lengths diverge widely, they cannot meet high threshold
                val lenA = a.title.length
                val lenB = b.title.length
                if (lenA > 0 && lenB > 0) {
                    val minLen = minOf(lenA, lenB).toFloat()
                    val maxLen = maxOf(lenA, lenB).toFloat()
                    if (minLen / maxLen < (similarityThreshold - 0.25f)) {
                        continue
                    }
                }

                val similarity = ArticleSimilarity.calculateTitleSimilarity(a.title, b.title)
                if (similarity >= similarityThreshold) {
                    union(i, j)
                }
            }
        }

        // 3. Group by connected components (clusters)
        val clusters = mutableMapOf<Int, MutableList<ArticleCandidate>>()
        for (i in 0 until n) {
            val root = find(i)
            clusters.getOrPut(root) { mutableListOf() }.add(eligibleCandidates[i])
        }

        // 4. Resolve each cluster: keep the highest priority/newest, mark the rest read
        val duplicatesToMarkRead = mutableListOf<String>()
        val matches = mutableListOf<DeduplicationMatch>()

        fun feedPriority(feedID: String): Int {
            val index = preferredFeedIDs.indexOf(feedID)
            return if (index >= 0) {
                preferredFeedIDs.size - index
            } else {
                0
            }
        }

        for ((_, cluster) in clusters) {
            if (cluster.size <= 1) continue

            // Sort cluster to determine winner:
            // 1. Highest feed priority first
            // 2. Newest publishedAt first
            val sorted = cluster.sortedWith(
                compareByDescending<ArticleCandidate> { feedPriority(it.feedID) }
                    .thenByDescending { it.publishedAt }
            )

            val winner = sorted[0]
            for (k in 1 until sorted.size) {
                val duplicate = sorted[k]
                duplicatesToMarkRead.add(duplicate.id)
                val simPercent = (ArticleSimilarity.calculateTitleSimilarity(winner.title, duplicate.title) * 100).toInt()
                matches.add(
                    DeduplicationMatch(
                        keptArticleId = winner.id,
                        keptArticleTitle = winner.title,
                        keptFeedTitle = winner.feedTitle,
                        duplicateArticleId = duplicate.id,
                        duplicateArticleTitle = duplicate.title,
                        duplicateFeedTitle = duplicate.feedTitle,
                        similarityPercentage = simPercent,
                    )
                )
            }
        }

        return DeduplicationResult(
            duplicateIDs = duplicatesToMarkRead,
            matches = matches,
        )
    }
}
