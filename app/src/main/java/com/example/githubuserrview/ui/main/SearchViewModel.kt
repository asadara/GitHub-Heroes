package com.example.githubuserrview.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class SearchViewModel(
    private val repository: GithubRepository,
    private val recentSearchPreferences: RecentSearchPreferences
) : ViewModel() {

    private val listUsers = MutableLiveData<ArrayList<User>>()
    private val isLoading = MutableLiveData(false)
    private val errorMessage = MutableLiveData<String?>()
    private val paginationState = MutableLiveData(SearchPaginationState())
    private var currentQuery: String = ""
    private var currentPage: Int = 1

    fun setSearchUsers(query: String) {
        currentQuery = query.trim()
        currentPage = 1
        loadPage(saveHistory = true)
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

            when (val result = repository.searchUsers(currentQuery, currentPage, PAGE_SIZE)) {
                is NetworkResult.Success -> {
                    listUsers.value = ArrayList(result.data.items)
                    val shownCount = result.data.items.size
                    val totalCount = result.data.totalCount
                    val visibleUntil = (currentPage * PAGE_SIZE).coerceAtMost(totalCount)
                    paginationState.value = SearchPaginationState(
                        totalCount = totalCount,
                        currentPage = currentPage,
                        pageSize = PAGE_SIZE,
                        shownCount = shownCount,
                        hasPreviousPage = currentPage > 1,
                        hasNextPage = shownCount == PAGE_SIZE && visibleUntil < totalCount
                    )
                }
                is NetworkResult.Error -> {
                    listUsers.value = arrayListOf()
                    errorMessage.value = result.message
                    paginationState.value = SearchPaginationState(
                        totalCount = 0,
                        currentPage = currentPage,
                        pageSize = PAGE_SIZE
                    )
                }
            }

            isLoading.value = false
        }
    }

    fun getSearchUser(): LiveData<ArrayList<User>> = listUsers

    fun getLoadingState(): LiveData<Boolean> = isLoading

    fun getErrorMessage(): LiveData<String?> = errorMessage

    fun getPaginationState(): LiveData<SearchPaginationState> = paginationState

    companion object {
        private const val PAGE_SIZE = 20
    }
}
