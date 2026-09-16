package com.capyreader.desktop.model

import com.capyreader.desktop.storage.DesktopDatabaseProvider
import com.capyreader.desktop.storage.DesktopPaths
import com.capyreader.desktop.storage.DesktopPreferenceStoreProvider
import com.capyreader.desktop.storage.DesktopPreferences
import com.jocmp.capy.Account
import com.jocmp.capy.AccountManager
import com.jocmp.capy.Article
import com.jocmp.capy.ArticleFilter
import com.jocmp.capy.ArticleStatus
import com.jocmp.capy.Feed
import com.jocmp.capy.FeedPriority
import com.jocmp.capy.Folder
import com.jocmp.capy.accounts.Credentials
import com.jocmp.capy.accounts.FaviconPolicy
import com.jocmp.capy.accounts.Source
import com.jocmp.capy.articles.SortOrder
import com.jocmp.capy.common.TimeHelpers
import com.jocmp.capy.persistence.ArticleRecords
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.Locale

class DesktopAccountState(
    val preferences: DesktopPreferences = DesktopPreferences(),
    private val scope: CoroutineScope
) {
    val databaseProvider = DesktopDatabaseProvider()
    val preferenceStoreProvider = DesktopPreferenceStoreProvider()

    val accountManager = AccountManager(
        rootFolder = DesktopPaths.accountsDir.toURI(),
        cacheDirectory = DesktopPaths.cacheDir.toURI(),
        databaseProvider = databaseProvider,
        preferenceStoreProvider = preferenceStoreProvider,
        faviconPolicy = FaviconPolicy { true },
        userAgent = { "CapyReaderDesktop/1.0" },
        acceptLanguage = Locale.getDefault().toLanguageTag(),
    )

    private val _currentAccount = MutableStateFlow<Account?>(null)
    val currentAccount = _currentAccount.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles = _articles.asStateFlow()

    private val _selectedArticle = MutableStateFlow<Article?>(null)
    val selectedArticle = _selectedArticle.asStateFlow()

    private val _filter = MutableStateFlow<ArticleFilter>(preferences.filter.get())
    val filter = _filter.asStateFlow()

    private val _status = MutableStateFlow<ArticleStatus>(preferences.articleStatus.get())
    val status = _status.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _feeds = MutableStateFlow<List<Feed>>(emptyList())
    val feeds = _feeds.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders = _folders.asStateFlow()

    private val _unreadCount = MutableStateFlow(0L)
    val unreadCount = _unreadCount.asStateFlow()

    init {
        val savedAccountID = preferences.accountID.get()
        if (savedAccountID.isNotBlank()) {
            val account = accountManager.findByID(savedAccountID)
            if (account != null) {
                setAccount(account)
            }
        }
    }

    private fun setAccount(account: Account) {
        _currentAccount.value = account
        preferences.accountID.set(account.id)

        // Observe feeds and folders
        scope.launch {
            account.allFeeds.collect { feedsList ->
                _feeds.value = feedsList
            }
        }
        scope.launch {
            account.folders.collect { foldersList ->
                _folders.value = foldersList
            }
        }
        scope.launch {
            account.countUnread(ArticleFilter.default(), null).collect { count ->
                _unreadCount.value = count
            }
        }

        refreshArticles()
    }

    fun setFilter(newFilter: ArticleFilter) {
        _filter.value = newFilter
        preferences.filter.set(newFilter)
        refreshArticles()
    }

    fun setStatus(newStatus: ArticleStatus) {
        _status.value = newStatus
        preferences.articleStatus.set(newStatus)
        refreshArticles()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        refreshArticles()
    }

    fun refreshArticles() {
        val account = _currentAccount.value ?: return
        val currentFilter = _filter.value.withStatus(_status.value)
        val query = _searchQuery.value.ifBlank { null }
        val sort = preferences.sortOrder.get()
        val since = TimeHelpers.nowUTC().toOffsetDateTime()

        scope.launch(Dispatchers.IO) {
            val articleRecords = ArticleRecords(account.database)
            val list: List<Article> = try {
                when (currentFilter) {
                    is ArticleFilter.Articles -> articleRecords.byStatus.all(
                        status = currentFilter.articleStatus,
                        query = query,
                        limit = 300,
                        offset = 0,
                        sortOrder = sort,
                        since = since
                    ).executeAsList()

                    is ArticleFilter.Feeds -> articleRecords.byFeed.all(
                        feedIDs = listOf(currentFilter.feedID),
                        status = currentFilter.feedStatus,
                        query = query,
                        since = since,
                        limit = 300,
                        sortOrder = sort,
                        offset = 0,
                        priority = FeedPriority.FEED,
                    ).executeAsList()

                    is ArticleFilter.Folders -> {
                        val folderFeedIDs = account.database.taggingsQueries
                            .findFeedIDs(folderTitle = currentFilter.folderTitle)
                            .executeAsList()
                        articleRecords.byFeed.all(
                            feedIDs = folderFeedIDs,
                            status = currentFilter.folderStatus,
                            query = query,
                            since = since,
                            limit = 300,
                            sortOrder = sort,
                            offset = 0,
                            priority = FeedPriority.CATEGORY,
                        ).executeAsList()
                    }

                    is ArticleFilter.SavedSearches -> articleRecords.bySavedSearch.all(
                        savedSearchID = currentFilter.savedSearchID,
                        status = currentFilter.savedSearchStatus,
                        query = query,
                        since = since,
                        limit = 300,
                        sortOrder = sort,
                        offset = 0,
                    ).executeAsList()

                    is ArticleFilter.Today -> articleRecords.byToday.all(
                        status = currentFilter.todayStatus,
                        query = query,
                        since = since,
                        limit = 300,
                        sortOrder = sort,
                        offset = 0,
                    ).executeAsList()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                emptyList()
            }

            _articles.value = list

            val currentSelected = _selectedArticle.value
            if (currentSelected != null) {
                _selectedArticle.value = list.firstOrNull { it.id == currentSelected.id } ?: list.firstOrNull()
            } else if (list.isNotEmpty()) {
                selectArticle(list.first())
            }
        }
    }

    fun selectArticle(article: Article) {
        _selectedArticle.value = article
        if (!article.read) {
            val account = _currentAccount.value ?: return
            scope.launch(Dispatchers.IO) {
                account.markRead(article.id)
                // update local state
                _articles.value = _articles.value.map {
                    if (it.id == article.id) it.copy(read = true) else it
                }
                if (_selectedArticle.value?.id == article.id) {
                    _selectedArticle.value = article.copy(read = true)
                }
            }
        }
    }

    fun toggleStarred(article: Article) {
        val account = _currentAccount.value ?: return
        scope.launch(Dispatchers.IO) {
            val newStarred = !article.starred
            if (newStarred) {
                account.addStar(article.id)
            } else {
                account.removeStar(article.id)
            }
            _articles.value = _articles.value.map {
                if (it.id == article.id) it.copy(starred = newStarred) else it
            }
            if (_selectedArticle.value?.id == article.id) {
                _selectedArticle.value = article.copy(starred = newStarred)
            }
        }
    }

    fun toggleRead(article: Article) {
        val account = _currentAccount.value ?: return
        scope.launch(Dispatchers.IO) {
            val newRead = !article.read
            if (newRead) {
                account.markRead(article.id)
            } else {
                account.markUnread(article.id)
            }
            _articles.value = _articles.value.map {
                if (it.id == article.id) it.copy(read = newRead) else it
            }
            if (_selectedArticle.value?.id == article.id) {
                _selectedArticle.value = article.copy(read = newRead)
            }
        }
    }

    fun dislikeArticle(article: Article) {
        val account = _currentAccount.value ?: return
        scope.launch(Dispatchers.IO) {
            account.dislikeArticle(articleID = article.id, feedID = article.feedID, title = article.title)
            _articles.value = _articles.value.filter { it.id != article.id }
            if (_selectedArticle.value?.id == article.id) {
                _selectedArticle.value = _articles.value.firstOrNull()
            }
        }
    }

    fun markAllRead() {
        val account = _currentAccount.value ?: return
        val unreadIDs = _articles.value.filter { !it.read }.map { it.id }
        if (unreadIDs.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            account.markAllRead(unreadIDs)
            refreshArticles()
        }
    }

    fun syncAndRefresh() {
        val account = _currentAccount.value ?: return
        if (_isRefreshing.value) return

        scope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                account.refresh(_filter.value)
                refreshArticles()
            } catch (e: Throwable) {
                _errorMessage.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun login(
        source: Source,
        username: String,
        password: String,
        url: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val verified = Credentials.from(
                    source = source,
                    username = username,
                    password = password,
                    url = url,
                ).verify().getOrThrow()

                val id = accountManager.createAccount(
                    username = verified.username,
                    password = verified.secret,
                    url = verified.url,
                    source = verified.source,
                )

                val account = accountManager.findByID(id) ?: error("Failed to load created account")
                withContext(Dispatchers.Main) {
                    setAccount(account)
                    onSuccess()
                }
                syncAndRefresh()
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Authentication failed")
                }
            }
        }
    }

    fun addFeed(
        url: String,
        folderTitle: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val account = _currentAccount.value ?: return
        scope.launch(Dispatchers.IO) {
            try {
                account.addFeed(
                    url = url,
                    folderTitles = folderTitle?.let { listOf(it) }
                )
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
                syncAndRefresh()
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Failed to add feed")
                }
            }
        }
    }

    fun logout() {
        preferences.accountID.set("")
        _currentAccount.value = null
        _articles.value = emptyList()
        _selectedArticle.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
