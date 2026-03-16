package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubRepo(
    @field:SerializedName("id")
    val id: Long,

    @field:SerializedName("owner")
    val owner: GithubRepoOwner,

    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("full_name")
    val fullName: String,

    @field:SerializedName("description")
    val description: String?,

    @field:SerializedName("html_url")
    val htmlUrl: String,

    @field:SerializedName("homepage")
    val homepage: String?,

    @field:SerializedName("language")
    val language: String?,

    @field:SerializedName("default_branch")
    val defaultBranch: String?,

    @field:SerializedName("size")
    val size: Int?,

    @field:SerializedName("stargazers_count")
    val stargazersCount: Int,

    @field:SerializedName("forks_count")
    val forksCount: Int,

    @field:SerializedName("watchers_count")
    val watchersCount: Int,

    @field:SerializedName("open_issues_count")
    val openIssuesCount: Int,

    @field:SerializedName("license")
    val license: GithubRepoLicense?,

    @field:SerializedName("updated_at")
    val updatedAt: String?,

    @field:SerializedName("private")
    val isPrivate: Boolean
)

data class GithubRepoOwner(
    @field:SerializedName("login")
    val login: String,

    @field:SerializedName("avatar_url")
    val avatarUrl: String?
)

data class GithubRepoLicense(
    @field:SerializedName("key")
    val key: String?,

    @field:SerializedName("name")
    val name: String?
)
