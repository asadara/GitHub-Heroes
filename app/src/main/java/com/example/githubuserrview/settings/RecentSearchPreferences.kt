package com.example.githubuserrview.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecentSearchPreferences private constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun getRecentSearches(): Flow<List<String>> {
        return dataStore.data.map { preferences ->
            preferences[RECENT_SEARCHES_KEY]
                ?.split(SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
    }

    suspend fun saveSearch(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return

        dataStore.edit { preferences ->
            val current = preferences[RECENT_SEARCHES_KEY]
                ?.split(SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: mutableListOf()

            current.remove(normalized)
            current.add(0, normalized)

            preferences[RECENT_SEARCHES_KEY] = current
                .take(MAX_ITEMS)
                .joinToString(SEPARATOR)
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(RECENT_SEARCHES_KEY)
        }
    }

    companion object {
        private const val MAX_ITEMS = 10
        private const val SEPARATOR = "\n"
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")

        @Volatile
        private var INSTANCE: RecentSearchPreferences? = null

        fun getInstance(dataStore: DataStore<Preferences>): RecentSearchPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = RecentSearchPreferences(dataStore)
                INSTANCE = instance
                instance
            }
        }
    }
}
