package com.example.githubuserrview.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GithubCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: CachedGithubProfile)

    @Query("SELECT * FROM cached_github_profile WHERE login = :login LIMIT 1")
    suspend fun getProfile(login: String): CachedGithubProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRepositories(repositories: List<CachedGithubRepo>)

    @Query("DELETE FROM cached_github_repo WHERE ownerLogin = :ownerLogin")
    suspend fun deleteRepositoriesForOwner(ownerLogin: String)

    @Query("SELECT * FROM cached_github_repo WHERE ownerLogin = :ownerLogin ORDER BY updatedAtEpochMs DESC, name ASC")
    suspend fun getRepositoriesForOwner(ownerLogin: String): List<CachedGithubRepo>

    @Query("SELECT * FROM cached_github_repo WHERE fullName = :fullName LIMIT 1")
    suspend fun getRepository(fullName: String): CachedGithubRepo?
}
