package com.example.githubuserrview.auth

data class GithubAuthSession(
    val accessToken: String,
    val tokenType: String,
    val scope: String,
    val login: String,
    val name: String?,
    val email: String?,
    val avatarUrl: String,
    val htmlUrl: String
)

data class GithubPendingAuth(
    val state: String,
    val codeVerifier: String
)
