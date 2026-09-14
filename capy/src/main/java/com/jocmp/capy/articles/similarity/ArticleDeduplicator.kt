package com.jocmp.capy.articles.similarity

import kotlin.math.abs

object ArticleDeduplicator {

    /**
     * Finds duplicate article IDs that should be marked as read.
     *
     * @param candidates Unread, non-starred articles sorted by publishedAt DESC.
     * @param preferredFeedIDs Ordered list of preferred feed IDs (index 0 has highest priority).
     * @param bypassKeywords Keywords that prevent matching if found in the title.
     * @param similarityThreshold Minimum similarity score [0.0..1.0] to consider articles duplicates.
     * @param timeWindowSeconds Maximum time difference in seconds between two articles.
     * @return List of article IDs to mark as read.
     */
    fun findDuplicates(
        candidates: List<ArticleCandidate>,
        preferredFeedIDs: List<String> = emptyList(),
        bypassKeywords: Set<String> = emptySet(),
        similarityThreshold: Float = 0.90f,
        timeWindowSeconds: Long = 48 * 3600L,
    ): List<String> {
        if (candidates.size < 2) return emptyList()

        // 1. Filter out articles matching bypass keywords
        val eligibleCandidates = candidates.filter { candidate ->
            !ArticleSimilarity.matchesBypassKeywords(candidate.title, bypassKeywords)
        }

        if (eligibleCandidates.size < 2) return emptyList()

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

            // Winner is sorted[0]; all others are duplicates to mark as read
            for (k in 1 until sorted.size) {
                duplicatesToMarkRead.add(sorted[k].id)
            }
        }

        return duplicatesToMarkRead
    }
}
