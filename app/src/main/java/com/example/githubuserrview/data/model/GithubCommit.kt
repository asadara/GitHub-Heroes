package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubCommit(
    @field:SerializedName("sha")
    val sha: String,

    @field:SerializedName("html_url")
    val htmlUrl: String?,

    @field:SerializedName("commit")
    val commit: GithubCommitData
)

data class GithubCommitData(
    @field:SerializedName("message")
    val message: String,

    @field:SerializedName("author")
    val author: GithubCommitAuthor?
)

data class GithubCommitAuthor(
    @field:SerializedName("name")
    val name: String?,

    @field:SerializedName("date")
    val date: String?
)
