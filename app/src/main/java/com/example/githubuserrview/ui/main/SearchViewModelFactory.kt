package com.example.githubuserrview.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.githubuserrview.data.repository.GithubRepository
import com.example.githubuserrview.settings.RecentSearchPreferences

class SearchViewModelFactory(
    private val repository: GithubRepository,
    private val recentSearchPreferences: RecentSearchPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(repository, recentSearchPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
