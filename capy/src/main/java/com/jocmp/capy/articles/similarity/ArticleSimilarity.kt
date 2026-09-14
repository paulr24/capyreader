package com.jocmp.capy.articles.similarity

object ArticleSimilarity {

    private val STOP_WORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
        "below", "between", "both", "but", "by", "can't", "cannot", "could", "couldn't",
        "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
        "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
        "have", "haven't", "having", "he", "her", "here", "hers", "herself", "him",
        "himself", "his", "how", "i", "if", "in", "into", "is", "isn't", "it", "it's",
        "its", "itself", "just", "me", "more", "most", "my", "myself", "no", "nor",
        "not", "now", "of", "off", "on", "once", "only", "or", "other", "ought",
        "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should",
        "shouldn't", "so", "some", "such", "than", "that", "the", "their", "theirs",
        "them", "themselves", "then", "there", "these", "they", "this", "those",
        "through", "to", "too", "under", "until", "up", "very", "was", "wasn't",
        "we", "were", "weren't", "what", "when", "where", "which", "while", "who",
        "whom", "why", "with", "won't", "would", "wouldn't", "you", "your", "yours",
        "yourself", "yourselves"
    )

    private val BRAND_SUFFIX_REGEX = Regex("""(?:\s+[-–—|•/]\s+[^–—|-|•/]+$)|(?:^\[[^\]]+\]\s*)""", RegexOption.IGNORE_CASE)
    private val NON_ALPHANUMERIC_REGEX = Regex("""[^a-z0-9\s]""")
    private val MULTI_SPACE_REGEX = Regex("""\s+""")

    /**
     * Calculates the overall similarity between two article titles.
     * Returns a float in range [0.0, 1.0].
     */
    fun calculateTitleSimilarity(titleA: String, titleB: String): Float {
        if (titleA.isBlank() || titleB.isBlank()) return 0.0f
        if (titleA.equals(titleB, ignoreCase = true)) return 1.0f

        val normA = normalize(titleA)
        val normB = normalize(titleB)
        if (normA == normB) return 1.0f

        val score1 = compareNormalized(normA, normB)

        // Also test with outlet/brand suffixes stripped
        val strippedA = normalize(stripBrand(titleA))
        val strippedB = normalize(stripBrand(titleB))
        val score2 = if (strippedA != normA || strippedB != normB) {
            compareNormalized(strippedA, strippedB)
        } else {
            0.0f
        }

        return maxOf(score1, score2)
    }

    /**
     * Checks if the given title contains any of the bypass keywords (case-insensitive).
     */
    fun matchesBypassKeywords(title: String, bypassKeywords: Set<String>): Boolean {
        if (bypassKeywords.isEmpty() || title.isBlank()) return false
        val lowerTitle = title.lowercase()
        return bypassKeywords.any { keyword ->
            val trimmed = keyword.trim().lowercase()
            trimmed.isNotEmpty() && (
                lowerTitle.contains(trimmed) ||
                Regex("""\b${Regex.escape(trimmed)}\b""").containsMatchIn(lowerTitle)
            )
        }
    }

    private fun compareNormalized(a: String, b: String): Float {
        if (a.isEmpty() || b.isEmpty()) return 0.0f
        if (a == b) return 1.0f

        val trigramScore = trigramDice(a, b)
        val tokenScore = tokenDice(a, b)

        return maxOf(trigramScore, tokenScore)
    }

    /**
     * Sørensen-Dice coefficient on character 3-grams.
     */
    fun trigramDice(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.length < 3 || s2.length < 3) {
            return bigramDice(s1, s2)
        }

        val tg1 = (0..s1.length - 3).map { s1.substring(it, it + 3) }.toSet()
        val tg2 = (0..s2.length - 3).map { s2.substring(it, it + 3) }.toSet()

        val total = tg1.size + tg2.size
        if (total == 0) return 0.0f

        val intersection = tg1.count { tg2.contains(it) }
        return (2.0f * intersection) / total
    }

    private fun bigramDice(s1: String, s2: String): Float {
        if (s1.length < 2 || s2.length < 2) return if (s1 == s2) 1.0f else 0.0f
        val bg1 = (0..s1.length - 2).map { s1.substring(it, it + 2) }.toSet()
        val bg2 = (0..s2.length - 2).map { s2.substring(it, it + 2) }.toSet()
        val total = bg1.size + bg2.size
        if (total == 0) return 0.0f
        val intersection = bg1.count { bg2.contains(it) }
        return (2.0f * intersection) / total
    }

    /**
     * Sørensen-Dice coefficient on unique non-stop words.
     */
    fun tokenDice(s1: String, s2: String): Float {
        val tokens1 = tokenize(s1)
        val tokens2 = tokenize(s2)

        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0f

        val total = tokens1.size + tokens2.size
        val intersection = tokens1.count { tokens2.contains(it) }
        return (2.0f * intersection) / total
    }

    fun normalize(text: String): String {
        return text.lowercase()
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    fun stripBrand(text: String): String {
        return text.replace(BRAND_SUFFIX_REGEX, "").trim()
    }

    private fun tokenize(text: String): Set<String> {
        return text.split(" ")
            .map { it.trim() }
            .filter { it.length > 1 && !STOP_WORDS.contains(it) }
            .toSet()
    }
}
