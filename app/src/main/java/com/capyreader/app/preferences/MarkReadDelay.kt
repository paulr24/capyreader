package com.capyreader.app.preferences

import com.capyreader.app.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class MarkReadDelay {
    IMMEDIATELY,
    SECONDS_5,
    SECONDS_10,
    SECONDS_15;

    val duration: Duration
        get() = when (this) {
            IMMEDIATELY -> Duration.ZERO
            SECONDS_5 -> 5.seconds
            SECONDS_10 -> 10.seconds
            SECONDS_15 -> 15.seconds
        }

    val translationKey: Int
        get() = when (this) {
            IMMEDIATELY -> R.string.settings_mark_read_delay_immediately
            SECONDS_5 -> R.string.settings_mark_read_delay_5_seconds
            SECONDS_10 -> R.string.settings_mark_read_delay_10_seconds
            SECONDS_15 -> R.string.settings_mark_read_delay_15_seconds
        }

    companion object {
        val default = IMMEDIATELY
    }
}
