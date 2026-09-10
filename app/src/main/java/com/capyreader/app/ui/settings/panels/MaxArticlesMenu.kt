package com.capyreader.app.ui.settings.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.capyreader.app.R
import com.capyreader.app.ui.fixtures.PreviewKoinApplication
import com.capyreader.app.ui.settings.PreferenceSelect
import com.jocmp.capy.accounts.MaxArticles

@Composable
fun MaxArticlesMenu(
    maxArticles: MaxArticles,
    updateMaxArticles: (MaxArticles) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        PreferenceSelect(
            selected = maxArticles,
            update = { updateMaxArticles(it) },
            options = MaxArticles.entries,
            label = R.string.settings_max_unread_articles_title,
            optionText = { translationKey(it) }
        )
    }
}

@Composable
private fun translationKey(maxArticles: MaxArticles): String {
    val resource = when (maxArticles) {
        MaxArticles.LIMIT_2500 -> R.string.settings_max_unread_articles_2500
        MaxArticles.LIMIT_5000 -> R.string.settings_max_unread_articles_5000
        MaxArticles.LIMIT_10000 -> R.string.settings_max_unread_articles_10000
        MaxArticles.LIMIT_25000 -> R.string.settings_max_unread_articles_25000
        MaxArticles.UNLIMITED -> R.string.settings_max_unread_articles_unlimited
    }

    return stringResource(resource)
}

@Preview
@Composable
fun MaxArticlesMenuPreview() {
    PreviewKoinApplication {
        Surface {
            MaxArticlesMenu(
                maxArticles = MaxArticles.LIMIT_5000,
                updateMaxArticles = {}
            )
        }
    }
}
