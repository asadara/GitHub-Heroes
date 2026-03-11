package com.example.githubuserrview.settings

import android.content.Context

object ActiveProfileStore {

    private const val PREF_NAME = "active_profile_store"
    private const val KEY_USERNAME = "active_username"
    const val DEFAULT_USERNAME = "asadara"

    fun save(context: Context, username: String) {
        if (username.isBlank()) return

        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, username.trim())
            .apply()
    }

    fun get(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, DEFAULT_USERNAME)
            .orEmpty()
            .ifBlank { DEFAULT_USERNAME }
    }
}
