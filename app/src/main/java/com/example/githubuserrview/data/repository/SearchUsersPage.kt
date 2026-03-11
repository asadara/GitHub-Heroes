package com.example.githubuserrview.data.repository

import com.example.githubuserrview.data.model.User

data class SearchUsersPage(
    val totalCount: Int,
    val items: List<User>,
    val page: Int,
    val perPage: Int
)
