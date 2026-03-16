package com.example.githubuserrview.data.repository

import com.example.githubuserrview.data.model.GithubRepo

data class SearchRepositoriesPage(
    val totalCount: Int,
    val items: List<GithubRepo>,
    val page: Int,
    val perPage: Int
)
