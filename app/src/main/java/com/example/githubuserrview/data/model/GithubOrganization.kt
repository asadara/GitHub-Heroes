package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubOrganization(
    @field:SerializedName("id")
    val id: Long,

    @field:SerializedName("login")
    val login: String,

    @field:SerializedName("description")
    val description: String?,

    @field:SerializedName("avatar_url")
    val avatarUrl: String,

    @field:SerializedName("html_url")
    val htmlUrl: String?
)
