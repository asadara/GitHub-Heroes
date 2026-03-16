package com.example.githubuserrview.auth

import android.content.Context

class GithubAuthStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun savePendingAuth(pendingAuth: GithubPendingAuth) {
        preferences.edit()
            .putString(KEY_PENDING_STATE, pendingAuth.state)
            .putString(KEY_PENDING_CODE_VERIFIER, pendingAuth.codeVerifier)
            .apply()
    }

    fun getPendingAuth(): GithubPendingAuth? {
        val state = preferences.getString(KEY_PENDING_STATE, null)
        val codeVerifier = preferences.getString(KEY_PENDING_CODE_VERIFIER, null)

        if (state.isNullOrBlank() || codeVerifier.isNullOrBlank()) {
            return null
        }

        return GithubPendingAuth(
            state = state,
            codeVerifier = codeVerifier
        )
    }

    fun clearPendingAuth() {
        preferences.edit()
            .remove(KEY_PENDING_STATE)
            .remove(KEY_PENDING_CODE_VERIFIER)
            .apply()
    }

    fun saveSession(session: GithubAuthSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_TOKEN_TYPE, session.tokenType)
            .putString(KEY_SCOPE, session.scope)
            .putString(KEY_LOGIN, session.login)
            .putString(KEY_NAME, session.name)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_AVATAR_URL, session.avatarUrl)
            .putString(KEY_HTML_URL, session.htmlUrl)
            .apply()
    }

    fun getSession(): GithubAuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
        val tokenType = preferences.getString(KEY_TOKEN_TYPE, null)
        val scope = preferences.getString(KEY_SCOPE, null)
        val login = preferences.getString(KEY_LOGIN, null)
        val avatarUrl = preferences.getString(KEY_AVATAR_URL, null)
        val htmlUrl = preferences.getString(KEY_HTML_URL, null)

        if (
            accessToken.isNullOrBlank() ||
            tokenType.isNullOrBlank() ||
            scope.isNullOrBlank() ||
            login.isNullOrBlank() ||
            avatarUrl.isNullOrBlank() ||
            htmlUrl.isNullOrBlank()
        ) {
            return null
        }

        return GithubAuthSession(
            accessToken = accessToken,
            tokenType = tokenType,
            scope = scope,
            login = login,
            name = preferences.getString(KEY_NAME, null),
            email = preferences.getString(KEY_EMAIL, null),
            avatarUrl = avatarUrl,
            htmlUrl = htmlUrl
        )
    }

    fun clearSession() {
        preferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_TOKEN_TYPE)
            .remove(KEY_SCOPE)
            .remove(KEY_LOGIN)
            .remove(KEY_NAME)
            .remove(KEY_EMAIL)
            .remove(KEY_AVATAR_URL)
            .remove(KEY_HTML_URL)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "github_auth_store"
        private const val KEY_PENDING_STATE = "pending_state"
        private const val KEY_PENDING_CODE_VERIFIER = "pending_code_verifier"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_SCOPE = "scope"
        private const val KEY_LOGIN = "login"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_HTML_URL = "html_url"
    }
}
