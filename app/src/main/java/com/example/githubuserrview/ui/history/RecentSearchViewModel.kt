package com.example.githubuserrview.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.githubuserrview.settings.RecentSearchPreferences
import kotlinx.coroutines.launch

class RecentSearchViewModel(
    private val recentSearchPreferences: RecentSearchPreferences
) : ViewModel() {

    val recentSearches: LiveData<List<String>> = recentSearchPreferences
        .getRecentSearches()
        .asLiveData()

    fun clearHistory() {
        viewModelScope.launch {
            recentSearchPreferences.clear()
        }
    }
}
