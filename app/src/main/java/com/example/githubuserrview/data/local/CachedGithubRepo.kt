package com.example.githubuserrview.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_github_repo",
    indices = [Index(value = ["ownerLogin"])]
)
data class CachedGithubRepo(
    @PrimaryKey
    val fullName: String,
    val repoId: Long,
    val ownerLogin: String,
    val ownerAvatarUrl: String?,
    val name: String,
    val description: String?,
    val htmlUrl: String,
    val homepage: String?,
    val language: String?,
    val defaultBranch: String?,
    val size: Int?,
    val stargazersCount: Int,
    val forksCount: Int,
    val watchersCount: Int,
    val openIssuesCount: Int,
    val licenseName: String?,
    val updatedAt: String?,
    val isPrivate: Boolean,
    val updatedAtEpochMs: Long
)
