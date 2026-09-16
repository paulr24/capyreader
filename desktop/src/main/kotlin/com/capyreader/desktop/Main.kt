package com.capyreader.desktop

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
        Window(
            onCloseRequest = ::exitApplication,
            title = "Capy Reader",
            icon = painterResource("icon.png"),
            state = WindowState(width = 1200.dp, height = 800.dp)
        ) {
            DesktopApp()
        }
    }
}
