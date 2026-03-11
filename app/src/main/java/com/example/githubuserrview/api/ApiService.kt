package com.example.githubuserrview.api

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
        @Query("per_page") perPage: Int
    ): Call<UserResponse>

    @GET("users/{username}")
    fun getDetailUser(@Path("username") username: String): Call<DetailUserResponse>

    @GET("users/{username}/followers")
    fun getUserFollowers(@Path("username") username: String): Call<ArrayList<User>>

    @GET("users/{username}/following")
    fun getUserFollowing(@Path("username") username: String): Call<ArrayList<User>>
}
