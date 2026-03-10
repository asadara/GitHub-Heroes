package com.example.githubuserrview.data.repository

object GithubRepositoryProvider {
    @Volatile
    private var instance: GithubRepository? = null

    fun getInstance(): GithubRepository {
        return instance ?: synchronized(this) {
            instance ?: GithubRepository().also { instance = it }
        }
    }
}
