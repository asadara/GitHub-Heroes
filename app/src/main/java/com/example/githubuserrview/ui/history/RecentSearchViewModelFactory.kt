package com.example.githubuserrview.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.githubuserrview.settings.RecentSearchPreferences

class RecentSearchViewModelFactory(
    private val recentSearchPreferences: RecentSearchPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecentSearchViewModel::class.java)) {
            return RecentSearchViewModel(recentSearchPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
