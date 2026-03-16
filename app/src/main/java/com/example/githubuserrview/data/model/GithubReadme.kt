package com.example.githubuserrview.data.model

import com.google.gson.annotations.SerializedName

data class GithubReadme(
    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("path")
    val path: String,

    @field:SerializedName("content")
    val content: String?,

    @field:SerializedName("encoding")
    val encoding: String?,

    @field:SerializedName("html_url")
    val htmlUrl: String?,

    @field:SerializedName("download_url")
    val downloadUrl: String?
)
