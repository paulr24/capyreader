package com.capyreader.app.ui.articles.detail

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capyreader.app.R
import com.capyreader.app.preferences.AppPreferences
import com.capyreader.app.ui.LocalTimeFormats
import com.capyreader.app.ui.articles.displayFeedName
import com.capyreader.app.ui.collectChangesWithDefault
import com.jocmp.capy.Article
import com.jocmp.capy.articles.FontOption
import com.jocmp.capy.articles.TextAlignment
import org.koin.compose.koinInject

@Composable
fun ArticleHeader(
    article: Article,
    onOpenLink: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    appPreferences: AppPreferences = koinInject(),
) {
    val context = LocalContext.current
    val timeFormats = LocalTimeFormats.current

    val titleFontSize by appPreferences.readerOptions.titleFontSize.collectChangesWithDefault()
    val titleTextAlignment by appPreferences.readerOptions.titleTextAlignment.collectChangesWithDefault()
    val titleFollowsBodyFont by appPreferences.readerOptions.titleFollowsBodyFont.collectChangesWithDefault()
    val fontFamilyOption by appPreferences.readerOptions.fontFamily.collectChangesWithDefault()

    val displayTitle = article.title.ifBlank { article.displayFeedName(context) }
    val bylineText = article.byline(context, timeFormats)
    val feedNameText = article.displayFeedName(context)

    val textAlign = when (titleTextAlignment) {
        TextAlignment.CENTER -> TextAlign.Center
        else -> TextAlign.Start
    }
    val alignment = when (titleTextAlignment) {
        TextAlignment.CENTER -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }

    val titleFont: FontFamily? = remember(titleFollowsBodyFont, fontFamilyOption) {
        if (titleFollowsBodyFont) {
            findFont(fontFamilyOption)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            horizontalAlignment = alignment,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = displayTitle,
                fontSize = titleFontSize.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = titleFont,
                color = MaterialTheme.colorScheme.primary,
                textAlign = textAlign,
                lineHeight = (titleFontSize * 1.25f).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = {
                            val targetUrl = article.url?.toString() ?: article.siteURL
                            if (!targetUrl.isNullOrBlank()) {
                                onOpenLink(Uri.parse(targetUrl))
                            }
                        }
                    )
            )

            if (bylineText.isNotBlank()) {
                Text(
                    text = bylineText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (article.title.isNotBlank() && feedNameText.isNotBlank()) {
                Text(
                    text = feedNameText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
