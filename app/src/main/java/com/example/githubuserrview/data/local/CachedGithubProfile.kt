package com.example.githubuserrview.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_github_profile")
data class CachedGithubProfile(
    @PrimaryKey
    val login: String,
    val name: String?,
    val bio: String?,
    val company: String?,
    val location: String?,
    val email: String?,
    val avatarUrl: String,
    val htmlUrl: String,
    val createdAt: String?,
    val type: String,
    val followers: Int,
    val following: Int,
    val publicRepos: Int,
    val publicGists: Int,
    val twitterUsername: String?,
    val updatedAtEpochMs: Long
)
