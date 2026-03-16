package com.example.githubuserrview.api

import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.data.model.GithubRepoSearchResponse
import com.example.githubuserrview.data.model.GithubEmail
import com.example.githubuserrview.data.model.GithubOrganization
import com.example.githubuserrview.data.model.GithubReadme
import com.example.githubuserrview.data.model.GithubCommit
import com.example.githubuserrview.data.model.GithubBranch
import com.example.githubuserrview.data.model.GithubContributor
import com.example.githubuserrview.data.model.GithubIssue
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.data.model.UserResponse
import com.example.githubuserrview.response.DetailUserResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

//interface retrofit
interface ApiService {
    @GET("search/users?")
    fun getSearchUsers(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null
    ): Call<UserResponse>

    @GET("search/repositories?")
    fun getSearchRepositories(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null
    ): Call<GithubRepoSearchResponse>

    @GET("users/{username}")
    fun getDetailUser(@Path("username") username: String): Call<DetailUserResponse>

    @GET("users/{username}/followers")
    fun getUserFollowers(@Path("username") username: String): Call<ArrayList<User>>

    @GET("users/{username}/following")
    fun getUserFollowing(@Path("username") username: String): Call<ArrayList<User>>

    @GET("users/{username}/repos")
    fun getPublicUserRepos(
        @Path("username") username: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100
    ): Call<List<GithubRepo>>

    @GET("users/{username}/starred")
    fun getPublicUserStarredRepos(
        @Path("username") username: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100
    ): Call<List<GithubRepo>>

    @GET("user")
    fun getAuthenticatedUser(): Call<DetailUserResponse>

    @GET("user/repos")
    fun getAuthenticatedUserRepos(
        @Query("visibility") visibility: String = "public",
        @Query("affiliation") affiliation: String = "owner",
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100
    ): Call<List<GithubRepo>>

    @GET("repos/{owner}/{repo}")
    fun getRepositoryDetail(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<GithubRepo>

    @GET("repos/{owner}/{repo}/readme")
    fun getRepositoryReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<GithubReadme>

    @GET("repos/{owner}/{repo}/commits")
    fun getRepositoryCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 5
    ): Call<List<GithubCommit>>

    @GET("repos/{owner}/{repo}/branches")
    fun getRepositoryBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 8
    ): Call<List<GithubBranch>>

    @GET("repos/{owner}/{repo}/contributors")
    fun getRepositoryContributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 8
    ): Call<List<GithubContributor>>

    @GET("repos/{owner}/{repo}/issues")
    fun getRepositoryIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 5
    ): Call<List<GithubIssue>>

    @GET("user/orgs")
    fun getAuthenticatedUserOrganizations(
        @Query("per_page") perPage: Int = 100
    ): Call<List<GithubOrganization>>

    @GET("users/{username}/orgs")
    fun getPublicUserOrganizations(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 100
    ): Call<List<GithubOrganization>>

    @GET("user/emails")
    fun getAuthenticatedUserEmails(
        @Query("per_page") perPage: Int = 100
    ): Call<List<GithubEmail>>
}
