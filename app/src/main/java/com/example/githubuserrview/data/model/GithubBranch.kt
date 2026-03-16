package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubBranch(
    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("protected")
    val isProtected: Boolean
)
