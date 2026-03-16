package com.example.githubuserrview.response

import com.google.gson.annotations.SerializedName

data class DetailUserResponse(

    @field:SerializedName("bio")
    val bio: String?,

    @field:SerializedName("blog")
    val blog: String?,

    @field:SerializedName("created_at")
    val createdAt: String?,

    @field:SerializedName("repos_url")
    val reposUrl: String,

    @field:SerializedName("following_url")
    val followingUrl: String,

    @field:SerializedName("login")
    val login: String,

    @field:SerializedName("type")
    val type: String,

    @field:SerializedName("company")
    val company: String?,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("public_repos")
    val publicRepos: Int,

    @field:SerializedName("public_gists")
    val publicGists: Int,

    @field:SerializedName("email")
    val email: String?,

    @field:SerializedName("organizations_url")
    val organizationsUrl: String,

    @field:SerializedName("followers_url")
    val followersUrl: String,

    @field:SerializedName("url")
    val url: String,

    @field:SerializedName("followers")
    val followers: Int,

    @field:SerializedName("avatar_url")
    val avatarUrl: String,

    @field:SerializedName("html_url")
    val htmlUrl: String,

    @field:SerializedName("following")
    val following: Int,

    @field:SerializedName("name")
    val name: String?,

    @field:SerializedName("location")
    val location: String?,

    @field:SerializedName("twitter_username")
    val twitterUsername: String?

)
