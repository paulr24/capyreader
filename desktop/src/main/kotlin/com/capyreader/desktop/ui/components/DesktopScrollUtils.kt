package com.capyreader.desktop.ui.components

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent

/**
 * Intercepts desktop mouse wheel scroll events and scales them to a smooth,
 * gentle step (approx 1.5 to 2 lines per wheel notch), preventing aggressive jumps.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.gentleMouseScroll(
    scrollableState: ScrollableState,
    pixelsPerStep: Float = 36f
): Modifier = this.onPointerEvent(
    eventType = PointerEventType.Scroll,
    pass = PointerEventPass.Initial
) { event ->
    val deltaY = event.changes.sumOf { it.scrollDelta.y.toDouble() }.toFloat()
    if (deltaY != 0f) {
        scrollableState.dispatchRawDelta(deltaY * pixelsPerStep)
        event.changes.forEach { it.consume() }
    }
}
