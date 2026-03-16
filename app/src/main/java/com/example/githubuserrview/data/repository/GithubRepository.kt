package com.example.githubuserrview.data.repository

import com.example.githubuserrview.api.ApiConfig
import com.example.githubuserrview.api.ApiService
import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.response.DetailUserResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GithubRepository(
    private val apiService: ApiService = ApiConfig.getApiService()
) {

    suspend fun searchUsers(
        query: String,
        page: Int,
        perPage: Int,
        sort: String? = null,
        order: String? = null
    ): NetworkResult<SearchUsersPage> {
        return runRequest(
            call = apiService.getSearchUsers(query, page, perPage, sort, order),
            successBody = { response ->
                response.body()?.let {
                    SearchUsersPage(
                        totalCount = it.totalCount,
                        items = it.items,
                        page = page,
                        perPage = perPage
                    )
                }
            },
            defaultError = "Gagal memuat hasil pencarian."
        )
    }

    suspend fun getUserDetail(username: String): NetworkResult<DetailUserResponse> {
        return runRequest(
            call = apiService.getDetailUser(username),
            successBody = { response -> response.body() },
            defaultError = "Gagal memuat detail pengguna."
        )
    }

    suspend fun getFollowers(username: String): NetworkResult<List<User>> {
        return runRequest(
            call = apiService.getUserFollowers(username),
            successBody = { response -> response.body() ?: emptyList() },
            defaultError = "Gagal memuat followers."
        )
    }

    suspend fun getFollowing(username: String): NetworkResult<List<User>> {
        return runRequest(
            call = apiService.getUserFollowing(username),
            successBody = { response -> response.body() ?: emptyList() },
            defaultError = "Gagal memuat following."
        )
    }

    suspend fun getPublicRepositories(username: String): NetworkResult<List<GithubRepo>> {
        return runRequest(
            call = apiService.getPublicUserRepos(username),
            successBody = { response -> response.body() ?: emptyList() },
            defaultError = "Gagal memuat repository publik."
        )
    }

    private suspend fun <T, R> runRequest(
        call: Call<T>,
        successBody: (Response<T>) -> R?,
        defaultError: String
    ): NetworkResult<R> {
        return try {
            val response = call.awaitResponse()
            if (response.isSuccessful) {
                val body = successBody(response)
                if (body != null) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Error(defaultError)
                }
            } else {
                NetworkResult.Error(mapHttpError(response.code(), defaultError))
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: defaultError)
        }
    }

    private fun mapHttpError(code: Int, defaultError: String): String {
        return when (code) {
            403 -> "GitHub API sedang dibatasi. Coba lagi sebentar."
            404 -> "Data GitHub tidak ditemukan."
            else -> "$defaultError ($code)"
        }
    }
}

private suspend fun <T> Call<T>.awaitResponse(): Response<T> =
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })

        continuation.invokeOnCancellation { cancel() }
    }
