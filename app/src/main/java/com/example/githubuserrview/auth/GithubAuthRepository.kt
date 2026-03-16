package com.example.githubuserrview.auth

import android.content.Context
import android.net.Uri
import com.example.githubuserrview.api.ApiConfig
import com.example.githubuserrview.data.model.GithubEmail
import com.example.githubuserrview.data.model.GithubOrganization
import com.example.githubuserrview.data.model.GithubReadme
import com.example.githubuserrview.data.model.GithubRepoLicense
import com.example.githubuserrview.data.model.GithubRepoOwner
import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.data.model.GithubCommit
import com.example.githubuserrview.data.local.CachedGithubProfile
import com.example.githubuserrview.data.local.CachedGithubRepo
import com.example.githubuserrview.data.local.UserDatabase
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.response.DetailUserResponse
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class GithubAuthRepository(
    context: Context,
    private val authStore: GithubAuthStore = GithubAuthStore(context),
    private val httpClient: OkHttpClient = OkHttpClient()
) {

    private val gson = Gson()
    private val cacheDao = UserDatabase.getDatabase(context).githubCacheDao()

    fun getSession(): GithubAuthSession? = authStore.getSession()

    suspend fun getCachedProfile(): DetailUserResponse? = withContext(Dispatchers.IO) {
        val login = authStore.getSession()?.login ?: return@withContext null
        cacheDao.getProfile(login)?.toDetailUserResponse()
    }

    suspend fun getCachedRepositories(): List<GithubRepo> = withContext(Dispatchers.IO) {
        val login = authStore.getSession()?.login ?: return@withContext emptyList()
        cacheDao.getRepositoriesForOwner(login).map(CachedGithubRepo::toGithubRepo)
    }

    suspend fun getCachedRepository(fullName: String): GithubRepo? = withContext(Dispatchers.IO) {
        cacheDao.getRepository(fullName)?.toGithubRepo()
    }

    suspend fun getAuthenticatedProfile(): NetworkResult<DetailUserResponse> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")

        val viewer = when (val viewerResult = fetchViewer(currentSession.accessToken)) {
            is NetworkResult.Success -> viewerResult.data
            is NetworkResult.Error -> {
                if (viewerResult.message.contains("401")) {
                    authStore.clearSession()
                }
                return@withContext NetworkResult.Error(viewerResult.message)
            }
        }

        authStore.saveSession(
            viewer.toSession(
                token = currentSession.accessToken,
                tokenType = currentSession.tokenType,
                scope = currentSession.scope
            )
        )
        cacheDao.upsertProfile(viewer.toCachedProfile())

        NetworkResult.Success(viewer)
    }

    suspend fun getOwnedPublicRepositories(): NetworkResult<List<GithubRepo>> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")

        try {
            val response = ApiConfig.getApiService(currentSession.accessToken)
                .getAuthenticatedUserRepos()
                .execute()

            if (response.isSuccessful) {
                val repositories = response.body().orEmpty()
                cacheDao.deleteRepositoriesForOwner(currentSession.login)
                if (repositories.isNotEmpty()) {
                    cacheDao.upsertRepositories(
                        repositories.map { it.toCachedRepo(ownerLogin = currentSession.login) }
                    )
                }
                NetworkResult.Success(repositories)
            } else {
                if (response.code() == 401) {
                    authStore.clearSession()
                }
                NetworkResult.Error("Gagal memuat repository GitHub. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat repository GitHub.")
        }
    }

    suspend fun getRepositoryDetail(
        owner: String,
        repositoryName: String
    ): NetworkResult<GithubRepo> = withContext(Dispatchers.IO) {
        val accessToken = authStore.getSession()?.accessToken

        try {
            val response = ApiConfig.getApiService(accessToken)
                .getRepositoryDetail(owner, repositoryName)
                .execute()

            if (response.isSuccessful) {
                val repository = response.body()
                if (repository != null) {
                    cacheDao.upsertRepositories(
                        listOf(repository.toCachedRepo(ownerLogin = repository.owner.login))
                    )
                    NetworkResult.Success(repository)
                } else {
                    NetworkResult.Error("Respons detail repository kosong.")
                }
            } else {
                NetworkResult.Error("Gagal memuat detail repository. (${response.code()})")
            }
        } catch (error: Exception) {
            val cached = cacheDao.getRepository("$owner/$repositoryName")?.toGithubRepo()
            if (cached != null) {
                NetworkResult.Success(cached)
            } else {
                NetworkResult.Error(error.localizedMessage ?: "Gagal memuat detail repository.")
            }
        }
    }

    suspend fun getRepositoryReadme(
        owner: String,
        repositoryName: String
    ): NetworkResult<GithubReadme> = withContext(Dispatchers.IO) {
        val accessToken = authStore.getSession()?.accessToken

        try {
            val response = ApiConfig.getApiService(accessToken)
                .getRepositoryReadme(owner, repositoryName)
                .execute()

            if (response.isSuccessful) {
                val readme = response.body()
                if (readme != null) {
                    NetworkResult.Success(readme)
                } else {
                    NetworkResult.Error("README repository tidak tersedia.")
                }
            } else {
                NetworkResult.Error("Gagal memuat README repository. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat README repository.")
        }
    }

    suspend fun getRecentCommits(
        owner: String,
        repositoryName: String
    ): NetworkResult<List<GithubCommit>> = withContext(Dispatchers.IO) {
        val accessToken = authStore.getSession()?.accessToken

        try {
            val response = ApiConfig.getApiService(accessToken)
                .getRepositoryCommits(owner, repositoryName)
                .execute()

            if (response.isSuccessful) {
                NetworkResult.Success(response.body().orEmpty())
            } else {
                NetworkResult.Error("Gagal memuat commit terbaru. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat commit terbaru.")
        }
    }

    suspend fun getOrganizations(): NetworkResult<List<GithubOrganization>> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")

        try {
            val authenticatedApi = ApiConfig.getApiService(currentSession.accessToken)
            val response = authenticatedApi
                .getAuthenticatedUserOrganizations()
                .execute()

            if (response.isSuccessful) {
                NetworkResult.Success(response.body().orEmpty())
            } else {
                if (response.code() == 401) {
                    authStore.clearSession()
                    return@withContext NetworkResult.Error("Sesi GitHub berakhir. Silakan login ulang.")
                }

                if (response.code() == 403) {
                    val publicResponse = authenticatedApi
                        .getPublicUserOrganizations(currentSession.login)
                        .execute()

                    return@withContext if (publicResponse.isSuccessful) {
                        NetworkResult.Success(publicResponse.body().orEmpty())
                    } else {
                        NetworkResult.Error(
                            "Gagal memuat organisasi publik GitHub. (${publicResponse.code()})"
                        )
                    }
                }

                NetworkResult.Error("Gagal memuat organisasi GitHub. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat organisasi GitHub.")
        }
    }

    suspend fun getEmails(): NetworkResult<List<GithubEmail>> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")

        try {
            val response = ApiConfig.getApiService(currentSession.accessToken)
                .getAuthenticatedUserEmails()
                .execute()

            if (response.isSuccessful) {
                NetworkResult.Success(response.body().orEmpty())
            } else {
                if (response.code() == 401) {
                    authStore.clearSession()
                }
                NetworkResult.Error("Gagal memuat email GitHub. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat email GitHub.")
        }
    }

    suspend fun beginAuthorization(): NetworkResult<Uri> {
        if (!GithubAuthConfig.isConfigured) {
            return NetworkResult.Error("GitHub OAuth belum dikonfigurasi.")
        }

        val state = GithubAuthConfig.createState()
        val codeVerifier = GithubAuthConfig.createCodeVerifier()
        authStore.savePendingAuth(GithubPendingAuth(state, codeVerifier))

        return NetworkResult.Success(
            GithubAuthConfig.buildAuthorizationUri(state, codeVerifier)
        )
    }

    suspend fun completeAuthorization(
        callbackUri: Uri?
    ): NetworkResult<GithubAuthSession> = withContext(Dispatchers.IO) {
        if (!GithubAuthConfig.isConfigured) {
            return@withContext NetworkResult.Error("GitHub OAuth belum dikonfigurasi.")
        }

        if (callbackUri == null) {
            return@withContext NetworkResult.Error("Callback GitHub tidak ditemukan.")
        }

        val callbackError = callbackUri.getQueryParameter("error")
        if (!callbackError.isNullOrBlank()) {
            authStore.clearPendingAuth()
            return@withContext NetworkResult.Error("Autorisasi GitHub dibatalkan: $callbackError")
        }

        val code = callbackUri.getQueryParameter("code")
        val returnedState = callbackUri.getQueryParameter("state")
        val pendingAuth = authStore.getPendingAuth()

        if (code.isNullOrBlank() || returnedState.isNullOrBlank() || pendingAuth == null) {
            authStore.clearPendingAuth()
            return@withContext NetworkResult.Error("Sesi login GitHub tidak valid.")
        }

        if (pendingAuth.state != returnedState) {
            authStore.clearPendingAuth()
            return@withContext NetworkResult.Error("State OAuth GitHub tidak cocok.")
        }

        val tokenResponse = when (val tokenResult = exchangeCodeForToken(code, pendingAuth)) {
            is NetworkResult.Success -> tokenResult.data
            is NetworkResult.Error -> {
                authStore.clearPendingAuth()
                return@withContext NetworkResult.Error(tokenResult.message)
            }
        }

        val viewer = when (val viewerResult = fetchViewer(tokenResponse.accessToken.orEmpty())) {
            is NetworkResult.Success -> viewerResult.data
            is NetworkResult.Error -> {
                authStore.clearPendingAuth()
                return@withContext NetworkResult.Error(viewerResult.message)
            }
        }

        val session = viewer.toSession(tokenResponse)
        authStore.saveSession(session)
        authStore.clearPendingAuth()
        cacheDao.upsertProfile(viewer.toCachedProfile())
        NetworkResult.Success(session)
    }

    suspend fun refreshSession(): NetworkResult<GithubAuthSession> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")

        val viewer = when (val viewerResult = fetchViewer(currentSession.accessToken)) {
            is NetworkResult.Success -> viewerResult.data
            is NetworkResult.Error -> {
                if (viewerResult.message.contains("401")) {
                    authStore.clearSession()
                }
                return@withContext NetworkResult.Error(viewerResult.message)
            }
        }

        val updatedSession = viewer.toSession(
            token = currentSession.accessToken,
            tokenType = currentSession.tokenType,
            scope = currentSession.scope
        )
        authStore.saveSession(updatedSession)
        NetworkResult.Success(updatedSession)
    }

    fun signOut() {
        authStore.clearPendingAuth()
        authStore.clearSession()
    }

    private fun exchangeCodeForToken(
        code: String,
        pendingAuth: GithubPendingAuth
    ): NetworkResult<GithubAccessTokenResponse> {
        val requestBody = FormBody.Builder()
            .add("client_id", GithubAuthConfig.clientId)
            .add("client_secret", GithubAuthConfig.clientSecret)
            .add("code", code)
            .add("redirect_uri", GithubAuthConfig.redirectUri)
            .add("state", pendingAuth.state)
            .add("code_verifier", pendingAuth.codeVerifier)
            .build()

        val request = Request.Builder()
            .url("https://github.com/login/oauth/access_token")
            .header("Accept", "application/json")
            .header("User-Agent", "GitHubUserRview")
            .post(requestBody)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return NetworkResult.Error("Gagal menukar kode GitHub. (${response.code})")
                }

                val tokenResponse = gson.fromJson(rawBody, GithubAccessTokenResponse::class.java)
                if (
                    tokenResponse == null ||
                    tokenResponse.accessToken.isNullOrBlank() ||
                    tokenResponse.tokenType.isNullOrBlank()
                ) {
                    val errorMessage = tokenResponse?.errorDescription
                        ?: "Respons token GitHub tidak valid."
                    NetworkResult.Error(errorMessage)
                } else {
                    NetworkResult.Success(tokenResponse)
                }
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal terhubung ke GitHub.")
        }
    }

    private fun fetchViewer(accessToken: String): NetworkResult<DetailUserResponse> {
        return try {
            val response = ApiConfig.getApiService(accessToken).getAuthenticatedUser().execute()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Error("Respons profil GitHub kosong.")
                }
            } else {
                NetworkResult.Error("Gagal memuat profil GitHub. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat profil GitHub.")
        }
    }

    private fun DetailUserResponse.toSession(tokenResponse: GithubAccessTokenResponse): GithubAuthSession {
        return toSession(
            token = tokenResponse.accessToken.orEmpty(),
            tokenType = tokenResponse.tokenType.orEmpty(),
            scope = tokenResponse.scope.orEmpty()
        )
    }

    private fun DetailUserResponse.toSession(
        token: String,
        tokenType: String,
        scope: String
    ): GithubAuthSession {
        return GithubAuthSession(
            accessToken = token,
            tokenType = tokenType,
            scope = scope,
            login = login,
            name = name,
            email = email,
            avatarUrl = avatarUrl,
            htmlUrl = htmlUrl
        )
    }

    private fun DetailUserResponse.toCachedProfile(): CachedGithubProfile {
        return CachedGithubProfile(
            login = login,
            name = name,
            bio = bio,
            company = company,
            location = location,
            email = email,
            avatarUrl = avatarUrl,
            htmlUrl = htmlUrl,
            createdAt = createdAt,
            type = type,
            followers = followers,
            following = following,
            publicRepos = publicRepos,
            publicGists = publicGists,
            twitterUsername = twitterUsername,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun GithubRepo.toCachedRepo(ownerLogin: String): CachedGithubRepo {
        return CachedGithubRepo(
            fullName = fullName,
            repoId = id,
            ownerLogin = ownerLogin,
            ownerAvatarUrl = owner.avatarUrl,
            name = name,
            description = description,
            htmlUrl = htmlUrl,
            homepage = homepage,
            language = language,
            defaultBranch = defaultBranch,
            size = size,
            stargazersCount = stargazersCount,
            forksCount = forksCount,
            watchersCount = watchersCount,
            openIssuesCount = openIssuesCount,
            licenseName = license?.name,
            updatedAt = updatedAt,
            isPrivate = isPrivate,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }
}

private fun CachedGithubProfile.toDetailUserResponse(): DetailUserResponse {
    return DetailUserResponse(
        bio = bio,
        blog = null,
        createdAt = createdAt,
        reposUrl = "",
        followingUrl = "",
        login = login,
        type = type,
        company = company,
        id = 0,
        publicRepos = publicRepos,
        publicGists = publicGists,
        email = email,
        organizationsUrl = "",
        followersUrl = "",
        url = "",
        followers = followers,
        avatarUrl = avatarUrl,
        htmlUrl = htmlUrl,
        following = following,
        name = name,
        location = location,
        twitterUsername = twitterUsername
    )
}

private fun CachedGithubRepo.toGithubRepo(): GithubRepo {
    return GithubRepo(
        id = repoId,
        owner = GithubRepoOwner(
            login = ownerLogin,
            avatarUrl = ownerAvatarUrl
        ),
        name = name,
        fullName = fullName,
        description = description,
        htmlUrl = htmlUrl,
        homepage = homepage,
        language = language,
        defaultBranch = defaultBranch,
        size = size,
        stargazersCount = stargazersCount,
        forksCount = forksCount,
        watchersCount = watchersCount,
        openIssuesCount = openIssuesCount,
        license = GithubRepoLicense(
            key = null,
            name = licenseName
        ),
        updatedAt = updatedAt,
        isPrivate = isPrivate
    )
}

private data class GithubAccessTokenResponse(
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("token_type")
    val tokenType: String?,
    @SerializedName("scope")
    val scope: String?,
    @SerializedName("error")
    val error: String?,
    @SerializedName("error_description")
    val errorDescription: String?
)
