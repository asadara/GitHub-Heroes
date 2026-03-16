package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubIssue(
    @field:SerializedName("number")
    val number: Int,

    @field:SerializedName("title")
    val title: String,

    @field:SerializedName("html_url")
    val htmlUrl: String,

    @field:SerializedName("comments")
    val comments: Int,

    @field:SerializedName("user")
    val user: GithubIssueUser?,

    @field:SerializedName("pull_request")
    val pullRequest: GithubIssuePullRequest?
)

data class GithubIssueUser(
    @field:SerializedName("login")
    val login: String?
)

data class GithubIssuePullRequest(
    @field:SerializedName("url")
    val url: String?
)
