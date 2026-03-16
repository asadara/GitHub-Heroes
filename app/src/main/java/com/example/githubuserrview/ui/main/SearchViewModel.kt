package com.example.githubuserrview.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.data.repository.GithubRepository
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.settings.RecentSearchPreferences
import kotlinx.coroutines.launch

data class SearchPaginationState(
    val totalCount: Int = 0,
    val currentPage: Int = 1,
    val pageSize: Int = 20,
    val shownCount: Int = 0,
    val hasPreviousPage: Boolean = false,
    val hasNextPage: Boolean = false
)

enum class SearchMode {
    USERS,
    REPOSITORIES
}

class SearchViewModel(
    private val repository: GithubRepository,
    private val recentSearchPreferences: RecentSearchPreferences
) : ViewModel() {

    private val listUsers = MutableLiveData<ArrayList<User>>()
    private val listRepositories = MutableLiveData<List<GithubRepo>>(emptyList())
    private val isLoading = MutableLiveData(false)
    private val errorMessage = MutableLiveData<String?>()
    private val paginationState = MutableLiveData(SearchPaginationState())
    private val searchMode = MutableLiveData(SearchMode.USERS)
    private var currentQuery: String = ""
    private var currentPage: Int = 1
    private var currentMode: SearchMode = SearchMode.USERS

    fun setSearchQuery(query: String) {
        currentQuery = query.trim()
        currentPage = 1
        loadPage(saveHistory = true)
    }

    fun setSearchMode(mode: SearchMode) {
        if (currentMode == mode) return

        currentMode = mode
        searchMode.value = mode
        currentPage = 1
        errorMessage.value = null

        if (currentQuery.isBlank()) {
            listUsers.value = arrayListOf()
            listRepositories.value = emptyList()
            paginationState.value = SearchPaginationState()
            return
        }

        loadPage(saveHistory = false)
    }

    fun loadNextPage() {
        val state = paginationState.value ?: return
        if (currentQuery.isBlank() || !state.hasNextPage) return
        currentPage += 1
        loadPage(saveHistory = false)
    }

    fun loadPreviousPage() {
        val state = paginationState.value ?: return
        if (currentQuery.isBlank() || !state.hasPreviousPage) return
        currentPage -= 1
        loadPage(saveHistory = false)
    }

    private fun loadPage(saveHistory: Boolean) {
        if (currentQuery.isBlank()) {
            listUsers.value = arrayListOf()
            paginationState.value = SearchPaginationState()
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            if (saveHistory) {
                recentSearchPreferences.saveSearch(currentQuery)
            }

            when (currentMode) {
                SearchMode.USERS -> {
                    when (val result = repository.searchUsers(currentQuery, currentPage, PAGE_SIZE)) {
                        is NetworkResult.Success -> {
                            listUsers.value = ArrayList(result.data.items)
                            listRepositories.value = emptyList()
                            paginationState.value = buildPaginationState(
                                totalCount = result.data.totalCount,
                                shownCount = result.data.items.size
                            )
                        }
                        is NetworkResult.Error -> {
                            listUsers.value = arrayListOf()
                            listRepositories.value = emptyList()
                            errorMessage.value = result.message
                            paginationState.value = SearchPaginationState(
                                totalCount = 0,
                                currentPage = currentPage,
                                pageSize = PAGE_SIZE
                            )
                        }
                    }
                }
                SearchMode.REPOSITORIES -> {
                    when (val result = repository.searchRepositories(currentQuery, currentPage, PAGE_SIZE)) {
                        is NetworkResult.Success -> {
                            listRepositories.value = result.data.items
                            listUsers.value = arrayListOf()
                            paginationState.value = buildPaginationState(
                                totalCount = result.data.totalCount,
                                shownCount = result.data.items.size
                            )
                        }
                        is NetworkResult.Error -> {
                            listRepositories.value = emptyList()
                            listUsers.value = arrayListOf()
                            errorMessage.value = result.message
                            paginationState.value = SearchPaginationState(
                                totalCount = 0,
                                currentPage = currentPage,
                                pageSize = PAGE_SIZE
                            )
                        }
                    }
                }
            }

            isLoading.value = false
        }
    }

    private fun buildPaginationState(totalCount: Int, shownCount: Int): SearchPaginationState {
        val visibleUntil = (currentPage * PAGE_SIZE).coerceAtMost(totalCount)
        return SearchPaginationState(
            totalCount = totalCount,
            currentPage = currentPage,
            pageSize = PAGE_SIZE,
            shownCount = shownCount,
            hasPreviousPage = currentPage > 1,
            hasNextPage = shownCount == PAGE_SIZE && visibleUntil < totalCount
        )
    }

    fun getSearchUser(): LiveData<ArrayList<User>> = listUsers

    fun getSearchRepositories(): LiveData<List<GithubRepo>> = listRepositories

    fun getLoadingState(): LiveData<Boolean> = isLoading

    fun getErrorMessage(): LiveData<String?> = errorMessage

    fun getPaginationState(): LiveData<SearchPaginationState> = paginationState

    fun getSearchMode(): LiveData<SearchMode> = searchMode

    companion object {
        private const val PAGE_SIZE = 20
    }
}
