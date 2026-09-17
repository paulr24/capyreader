package com.capyreader.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.capyreader.desktop.model.DesktopAccountState
import com.capyreader.desktop.ui.articles.DesktopArticleList
import com.capyreader.desktop.ui.articles.DesktopArticleReader
import com.capyreader.desktop.ui.dialogs.DesktopAddFeedDialog
import com.capyreader.desktop.ui.dialogs.DesktopLoginDialog
import com.capyreader.desktop.ui.dialogs.DesktopSettingsDialog
import com.capyreader.desktop.ui.sidebar.DesktopSidebar
import com.capyreader.desktop.ui.theme.DesktopTheme
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

@Composable
fun DesktopApp(
    onRegisterKeyHandler: (((KeyEvent) -> Boolean)?) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val state = remember { DesktopAccountState(scope = scope) }
    val currentAccount by state.currentAccount.collectAsState()
    val errorMessage by state.errorMessage.collectAsState()

    var showLoginDialog by remember { mutableStateOf(false) }
    var showAddFeedDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var userSidebarOpen by remember { mutableStateOf<Boolean?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

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

    val themeMode by state.themeMode.collectAsState()
    val fontFamily by state.fontFamily.collectAsState()
    val accentColor by state.accentColor.collectAsState()

    DesktopTheme(
        themeMode = themeMode,
        fontFamily = fontFamily,
        accentColorHex = accentColor
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenWidth = maxWidth
            val isWide = screenWidth >= 1020.dp
            val isMedium = screenWidth in 720.dp..<1020.dp
            val isNarrow = screenWidth < 720.dp

            val selectedArticle by state.selectedArticle.collectAsState()

            // Determine if sidebar should be visible
            val showSidebar = when {
                isNarrow -> (userSidebarOpen == true) && (selectedArticle == null)
                isMedium -> userSidebarOpen ?: false
                else -> userSidebarOpen ?: true
            }

            val keyHandler: (KeyEvent) -> Boolean = remember(
                state,
                isWide,
                isNarrow,
                userSidebarOpen,
                showLoginDialog,
                showAddFeedDialog,
                showSettingsDialog
            ) {
                { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        false
                    } else if (showLoginDialog || showAddFeedDialog || showSettingsDialog) {
                        false
                    } else if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
                        false
                    } else if (state.isTextInputActive.value) {
                        if (event.key == Key.Escape) {
                            state.setTextInputActive(false)
                            focusManager.clearFocus()
                            true
                        } else {
                            false
                        }
                    } else {
                        val articles = state.articles.value
                        val selected = state.selectedArticle.value
                        val currentIndex = articles.indexOfFirst { it.id == selected?.id }

                        when (event.key) {
                            Key.J, Key.DirectionDown -> {
                                if (currentIndex in 0 until articles.size - 1) {
                                    state.selectArticle(articles[currentIndex + 1])
                                    true
                                } else if (articles.isNotEmpty() && currentIndex == -1) {
                                    state.selectArticle(articles[0])
                                    true
                                } else false
                            }
                            Key.K, Key.DirectionUp -> {
                                if (currentIndex > 0) {
                                    state.selectArticle(articles[currentIndex - 1])
                                    true
                                } else if (articles.isNotEmpty() && currentIndex == -1) {
                                    state.selectArticle(articles[0])
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
                            Key.A, Key.X -> {
                                selected?.let { article ->
                                    if (state.preferences.aiOptions.enabled.get()) {
                                        state.triggerAISummary(article.id)
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Enable AI Summarization in Settings to generate summaries.")
                                        }
                                    }
                                }
                                true
                            }
                            Key.F, Key.W -> {
                                selected?.let { state.toggleFullContent(it) }
                                true
                            }
                            Key.Backslash -> {
                                val currentlyOpen = userSidebarOpen ?: isWide
                                userSidebarOpen = !currentlyOpen
                                true
                            }
                            Key.Escape -> {
                                if (isNarrow && selected != null) {
                                    state.clearSelectedArticle()
                                    true
                                } else false
                            }
                            Key.D -> {
                                scope.launch {
                                    state.deduplicateNow { count ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (count > 0) "Deduplication complete: cleaned up $count duplicate articles."
                                                else "Deduplication complete: no duplicate articles found."
                                            )
                                        }
                                    }
                                }
                                true
                            }
                            Key.R -> {
                                state.syncAndRefresh()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Refreshing feeds...")
                                }
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
                    }
                }
            }

            DisposableEffect(keyHandler) {
                onRegisterKeyHandler(keyHandler)
                onDispose {
                    onRegisterKeyHandler(null)
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isNarrow) {
                    // NARROW SCREEN (Mobile / Compact):
                    // If an article is open, collapse panes 1 & 2 completely, giving 100% width to ArticleReader!
                    if (selectedArticle != null) {
                        DesktopArticleReader(
                            state = state,
                            showBackButton = true,
                            onBack = { state.clearSelectedArticle() },
                            showSidebarToggle = false,
                            onOpenSettings = { showSettingsDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (showSidebar) {
                        // Narrow screen showing sidebar
                        DesktopSidebar(
                            state = state,
                            onOpenAddFeed = { showAddFeedDialog = true },
                            onOpenLogin = { showLoginDialog = true },
                            onOpenSettings = { showSettingsDialog = true },
                            onCloseSidebar = { userSidebarOpen = false }
                        )
                    } else {
                        // Narrow screen showing article list
                        DesktopArticleList(
                            state = state,
                            showSidebarToggle = true,
                            onToggleSidebar = { userSidebarOpen = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // WIDE & MEDIUM SCREENS
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Pane 1: Sidebar (if visible)
                        if (showSidebar) {
                            val sidebarWidth = if (isMedium) 250.dp else 280.dp
                            Box(modifier = Modifier.width(sidebarWidth).fillMaxHeight()) {
                                DesktopSidebar(
                                    state = state,
                                    onOpenAddFeed = { showAddFeedDialog = true },
                                    onOpenLogin = { showLoginDialog = true },
                                    onOpenSettings = { showSettingsDialog = true }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                        }

                        // Pane 2: Article List
                        val listWidth = when {
                            !showSidebar && isWide -> 380.dp
                            !showSidebar -> 330.dp
                            isMedium -> 300.dp
                            else -> 350.dp
                        }

                        DesktopArticleList(
                            state = state,
                            showSidebarToggle = !showSidebar,
                            onToggleSidebar = { userSidebarOpen = true },
                            modifier = Modifier.width(listWidth).fillMaxHeight()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        )

                        // Pane 3: Article Reader
                        DesktopArticleReader(
                            state = state,
                            showBackButton = false,
                            showSidebarToggle = !showSidebar,
                            onToggleSidebar = { userSidebarOpen = !showSidebar },
                            onOpenSettings = { showSettingsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
}

