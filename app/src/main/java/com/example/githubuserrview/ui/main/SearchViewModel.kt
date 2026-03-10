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

class SearchViewModel(
    private val repository: GithubRepository,
    private val recentSearchPreferences: RecentSearchPreferences
) : ViewModel() {

    private val listUsers = MutableLiveData<ArrayList<User>>()
    private val isLoading = MutableLiveData(false)
    private val errorMessage = MutableLiveData<String?>()

    fun setSearchUsers(query: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            recentSearchPreferences.saveSearch(query)

            when (val result = repository.searchUsers(query)) {
                is NetworkResult.Success -> {
                    listUsers.value = ArrayList(result.data)
                }
                is NetworkResult.Error -> {
                    listUsers.value = arrayListOf()
                    errorMessage.value = result.message
                }
            }

            isLoading.value = false
        }
    }

    fun getSearchUser(): LiveData<ArrayList<User>> = listUsers

    fun getLoadingState(): LiveData<Boolean> = isLoading

    fun getErrorMessage(): LiveData<String?> = errorMessage
}
