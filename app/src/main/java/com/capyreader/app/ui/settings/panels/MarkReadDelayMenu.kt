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
import com.capyreader.app.preferences.MarkReadDelay
import com.capyreader.app.ui.fixtures.PreviewKoinApplication
import com.capyreader.app.ui.settings.PreferenceSelect

@Composable
fun MarkReadDelayMenu(
    markReadDelay: MarkReadDelay,
    updateMarkReadDelay: (MarkReadDelay) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp),
    ) {
        PreferenceSelect(
            selected = markReadDelay,
            update = { updateMarkReadDelay(it) },
            options = MarkReadDelay.entries,
            label = R.string.settings_mark_read_delay_title,
            optionText = { stringResource(it.translationKey) }
        )
    }
}

@Preview
@Composable
fun MarkReadDelayMenuPreview() {
    PreviewKoinApplication {
        Surface {
            MarkReadDelayMenu(
                markReadDelay = MarkReadDelay.default,
                updateMarkReadDelay = {}
            )
        }
    }
}
