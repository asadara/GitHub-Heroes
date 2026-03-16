package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubContributor(
    @field:SerializedName("login")
    val login: String,

    @field:SerializedName("contributions")
    val contributions: Int
)
