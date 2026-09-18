package com.capyreader.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.capyreader.desktop.ui.DesktopApp

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        System.err.println("Uncaught exception on thread ${thread.name}: ${throwable.message}")
        throwable.printStackTrace()
    }
    application {
        var keyEventHandler by remember { mutableStateOf<((KeyEvent) -> Boolean)?>(null) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Capy Reader",
            icon = painterResource("icon.png"),
            state = WindowState(width = 1200.dp, height = 800.dp),
            onPreviewKeyEvent = { event ->
                keyEventHandler?.invoke(event) ?: false
            }
        ) {
            DesktopApp(
                window = window,
                onRegisterKeyHandler = { handler ->
                    keyEventHandler = handler
                }
            )
        }
    }
}
