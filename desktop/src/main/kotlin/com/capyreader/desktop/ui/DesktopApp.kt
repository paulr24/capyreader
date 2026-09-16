package com.capyreader.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.capyreader.desktop.model.DesktopAccountState
import com.capyreader.desktop.ui.articles.DesktopArticleList
import com.capyreader.desktop.ui.articles.DesktopArticleReader
import com.capyreader.desktop.ui.dialogs.DesktopAddFeedDialog
import com.capyreader.desktop.ui.dialogs.DesktopLoginDialog
import com.capyreader.desktop.ui.dialogs.DesktopSettingsDialog
import com.capyreader.desktop.ui.sidebar.DesktopSidebar
import com.capyreader.desktop.ui.theme.DesktopTheme
import java.awt.Desktop
import java.net.URI

@Composable
fun DesktopApp() {
    val scope = rememberCoroutineScope()
    val state = remember { DesktopAccountState(scope = scope) }
    val currentAccount by state.currentAccount.collectAsState()
    val errorMessage by state.errorMessage.collectAsState()

    var showLoginDialog by remember { mutableStateOf(false) }
    var showAddFeedDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(currentAccount) {
        if (currentAccount == null && !state.preferences.isLoggedIn) {
            showLoginDialog = true
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            state.clearError()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val themeMode by state.preferences.themeMode.changes().collectAsState(state.preferences.themeMode.get())
    val fontFamily by state.preferences.fontFamily.changes().collectAsState(state.preferences.fontFamily.get())

    DesktopTheme(
        themeMode = themeMode,
        fontFamily = fontFamily
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        val articles = state.articles.value
                        val selected = state.selectedArticle.value
                        val currentIndex = articles.indexOfFirst { it.id == selected?.id }

                        when (event.key) {
                            Key.J, Key.DirectionDown -> {
                                if (currentIndex in 0 until articles.size - 1) {
                                    state.selectArticle(articles[currentIndex + 1])
                                    true
                                } else false
                            }
                            Key.K, Key.DirectionUp -> {
                                if (currentIndex > 0) {
                                    state.selectArticle(articles[currentIndex - 1])
                                    true
                                } else false
                            }
                            Key.M -> {
                                selected?.let { state.toggleRead(it) }
                                true
                            }
                            Key.S -> {
                                selected?.let { state.toggleStarred(it) }
                                true
                            }
                            Key.D -> {
                                selected?.let { state.dislikeArticle(it) }
                                true
                            }
                            Key.R -> {
                                state.syncAndRefresh()
                                true
                            }
                            Key.O -> {
                                selected?.url?.let {
                                    try {
                                        Desktop.getDesktop().browse(URI.create(it.toString()))
                                    } catch (_: Exception) {}
                                }
                                true
                            }
                            Key.Comma -> {
                                showSettingsDialog = true
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left pane: Sidebar
                DesktopSidebar(
                    state = state,
                    onOpenAddFeed = { showAddFeedDialog = true },
                    onOpenLogin = { showLoginDialog = true },
                    onOpenSettings = { showSettingsDialog = true }
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )

                // Middle pane: Article List
                DesktopArticleList(
                    state = state
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )

                // Right pane: Article Reader
                DesktopArticleReader(
                    state = state,
                    onOpenSettings = { showSettingsDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (showLoginDialog) {
                DesktopLoginDialog(
                    state = state,
                    onDismissRequest = { showLoginDialog = false }
                )
            }

            if (showAddFeedDialog) {
                DesktopAddFeedDialog(
                    state = state,
                    onDismissRequest = { showAddFeedDialog = false }
                )
            }

            if (showSettingsDialog) {
                DesktopSettingsDialog(
                    state = state,
                    onDismissRequest = { showSettingsDialog = false },
                    onOpenLogin = {
                        showSettingsDialog = false
                        showLoginDialog = true
                    }
                )
            }
        }
    }
}
