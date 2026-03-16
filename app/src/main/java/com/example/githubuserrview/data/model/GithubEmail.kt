package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubEmail(
    @field:SerializedName("email")
    val email: String,

    @field:SerializedName("primary")
    val primary: Boolean,

    @field:SerializedName("verified")
    val verified: Boolean,

    @field:SerializedName("visibility")
    val visibility: String?
)
