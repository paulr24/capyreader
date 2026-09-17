package com.capyreader.desktop.articles

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object DesktopArticleExtractor {

    private val UNWANTED_TAGS = listOf(
        "script", "style", "noscript", "template", "svg", "canvas",
        "form", "button", "input", "select", "textarea",
        "nav", "header", "footer", "aside", "dialog"
    )

    private val CLUTTER_SELECTORS = listOf(
        "[class*='cookie' i]", "[id*='cookie' i]",
        "[class*='consent' i]", "[id*='consent' i]",
        "[class*='banner' i]", "[id*='banner' i]",
        "[class*='modal' i]", "[id*='modal' i]",
        "[class*='popup' i]", "[id*='popup' i]",
        "[class*='newsletter' i]", "[id*='newsletter' i]",
        "[class*='subscribe' i]", "[id*='subscribe' i]",
        "[class*='social' i]", "[id*='social' i]",
        "[class*='share' i]", "[id*='share' i]",
        "[class*='comment' i]", "[id*='comment' i]",
        "[class*='disqus' i]", "[id*='disqus' i]",
        "[class*='sidebar' i]", "[id*='sidebar' i]",
        "[class*='side-bar' i]", "[id*='side-bar' i]",
        "[class*='ad-' i]", "[class*='ad_' i]", "[class*='advert' i]",
        "[class*='login' i]", "[class*='signin' i]", "[class*='sign-in' i]",
        "[class*='burger' i]", "[id*='burger' i]",
        "[class*='menu' i]", "[id*='menu' i]",
        "[class*='breadcrumb' i]", "[id*='breadcrumb' i]",
        "[class*='related' i]", "[id*='related' i]",
        "[class*='recommend' i]", "[id*='recommend' i]",
        "[aria-hidden='true']", "[hidden]"
    )

    private val ARTICLE_CONTAINER_CANDIDATES = listOf(
        "[itemprop='articleBody']",
        ".article-body",
        ".article__body",
        ".article-content",
        ".article__content",
        ".entry-content",
        ".entry-body",
        ".post-content",
        ".post-body",
        ".story-content",
        ".story-body",
        ".content-body",
        ".content__body",
        ".caas-body",
        "#article-body",
        "#article-content",
        "#content-body",
        "#story-body",
        "article",
        "main",
        "[role='main']"
    )

    fun extract(html: String, articleUrl: String? = null): String {
        if (html.isBlank()) return ""

        val doc = Jsoup.parse(html, articleUrl ?: "")

        // If it's already an RSS feed snippet (short and no html/head/body structure)
        val isFullPage = html.contains("<!DOCTYPE", ignoreCase = true) ||
                html.contains("<html", ignoreCase = true) ||
                html.contains("<head", ignoreCase = true) ||
                html.contains("<body", ignoreCase = true)

        if (!isFullPage) {
            // Just clean scripts/styles
            doc.select("script, style, noscript, form, button, nav, header, footer").remove()
            return doc.body().html()
        }

        // Extract lead image from meta tags
        val leadImageUrl = doc.selectFirst("meta[property='og:image']")?.attr("abs:content")
            ?: doc.selectFirst("meta[name='twitter:image']")?.attr("abs:content")

        // 1. Remove unwanted tags
        UNWANTED_TAGS.forEach { tag ->
            doc.select(tag).remove()
        }

        // 2. Remove common clutter elements
        for (selector in CLUTTER_SELECTORS) {
            val elements = doc.select(selector)
            for (el in elements) {
                val tag = el.tagName().lowercase()
                if (tag in listOf("body", "html", "article", "main")) continue

                val classAndId = "${el.className()} ${el.id()}".lowercase()
                val isMainArticleContainer = classAndId.contains("article-body") ||
                        classAndId.contains("article__body") ||
                        classAndId.contains("article-content") ||
                        classAndId.contains("entry-content") ||
                        classAndId.contains("post-content") ||
                        classAndId.contains("story-content")

                if (!isMainArticleContainer) {
                    el.remove()
                }
            }
        }

        // 3. Find candidate container
        var container: Element? = null

        for (candidate in ARTICLE_CONTAINER_CANDIDATES) {
            val el = doc.selectFirst(candidate)
            if (el != null) {
                val pCount = el.select("p").size
                val textLen = el.text().length
                if (pCount >= 2 || textLen >= 200) {
                    container = el
                    break
                }
            }
        }

        // 4. Fallback to scoring candidate containers
        if (container == null) {
            var bestScore = 0
            val candidates = doc.select("div, section, article, main")
            for (el in candidates) {
                val pCount = el.select("> p, > div > p").size
                val text = el.text()
                val textLen = text.length
                if (textLen < 150) continue

                val linkLen = el.select("a").text().length
                val linkDensity = if (textLen > 0) linkLen.toDouble() / textLen else 1.0
                if (linkDensity > 0.45) continue // Skip navbars or link lists

                val classAndId = "${el.className()} ${el.id()}".lowercase()
                var score = (pCount * 25) + minOf(textLen / 50, 100)
                if (classAndId.contains("article") || classAndId.contains("content") ||
                    classAndId.contains("body") || classAndId.contains("post") ||
                    classAndId.contains("story")) {
                    score += 50
                }
                if (classAndId.contains("nav") || classAndId.contains("menu") ||
                    classAndId.contains("footer") || classAndId.contains("sidebar")) {
                    score -= 60
                }

                if (score > bestScore) {
                    bestScore = score
                    container = el
                }
            }
        }

        val target = container ?: doc.body()

        // Remove residual internal clutter inside container
        target.select(
            ".share, .social, .related, .author-bio, .newsletter, .subscribe, " +
            ".tags, .categories, .advertisement, .ad, .comments, .disqus"
        ).forEach { el ->
            if (el.select("p").size <= 1) {
                el.remove()
            }
        }

        // Prepend lead image if not already present in the article
        if (!leadImageUrl.isNullOrBlank()) {
            val existingImages = target.select("img").map { it.attr("src") }
            val hasLeadImage = existingImages.any { it.contains(leadImageUrl) || leadImageUrl.contains(it) }
            if (!hasLeadImage && target.select("img").isEmpty()) {
                target.prepend("<p><img src=\"$leadImageUrl\" alt=\"\" /></p>")
            }
        }

        return target.html()
    }
}
