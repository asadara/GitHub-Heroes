package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubRepoSearchResponse(
    @SerializedName("total_count")
    val totalCount: Int,
    val items: List<GithubRepo>
)
