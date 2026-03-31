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
import com.example.githubuserrview.data.model.GithubBranch
import com.example.githubuserrview.data.model.GithubContributor
import com.example.githubuserrview.data.model.GithubIssue
import com.example.githubuserrview.data.local.CachedGithubProfile
import com.example.githubuserrview.data.local.CachedGithubRepo
import com.example.githubuserrview.data.local.UserDatabase
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.response.DetailUserResponse
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GithubAuthRepository(
    context: Context,
    private val authStore: GithubAuthStore = GithubAuthStore(context),
    private val httpClient: OkHttpClient = OkHttpClient()
) {

    private val gson = Gson()
    private val cacheDao = UserDatabase.getDatabase(context).githubCacheDao()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class CachedProfileSnapshot(
        val profile: DetailUserResponse,
        val syncedAtEpochMs: Long
    )

    data class CachedRepositoriesSnapshot(
        val repositories: List<GithubRepo>,
        val syncedAtEpochMs: Long?
    )

    data class CachedRepositorySnapshot(
        val repository: GithubRepo,
        val syncedAtEpochMs: Long
    )

    data class GithubUserSocialState(
        val isSelf: Boolean,
        val isFollowing: Boolean
    )

    data class GithubRepositorySocialState(
        val isStarred: Boolean,
        val isWatching: Boolean
    )

    fun getSession(): GithubAuthSession? = authStore.getSession()

    suspend fun getCachedProfile(): DetailUserResponse? = withContext(Dispatchers.IO) {
        val login = authStore.getSession()?.login ?: return@withContext null
        cacheDao.getProfile(login)?.toDetailUserResponse()
    }

    suspend fun getCachedProfileSnapshot(): CachedProfileSnapshot? = withContext(Dispatchers.IO) {
        val login = authStore.getSession()?.login ?: return@withContext null
        cacheDao.getProfile(login)?.let { cachedProfile ->
            CachedProfileSnapshot(
                profile = cachedProfile.toDetailUserResponse(),
                syncedAtEpochMs = cachedProfile.updatedAtEpochMs
            )
        }
    }

    suspend fun getCachedRepositories(): List<GithubRepo> = withContext(Dispatchers.IO) {
        val login = authStore.getSession()?.login ?: return@withContext emptyList()
        cacheDao.getRepositoriesForOwner(login).map(CachedGithubRepo::toGithubRepo)
    }

    suspend fun getCachedRepositoriesSnapshot(): CachedRepositoriesSnapshot = withContext(Dispatchers.IO) {
        val login = authStore.getSession()?.login ?: return@withContext CachedRepositoriesSnapshot(
            repositories = emptyList(),
            syncedAtEpochMs = null
        )
        val cachedRepositories = cacheDao.getRepositoriesForOwner(login)
        CachedRepositoriesSnapshot(
            repositories = cachedRepositories.map(CachedGithubRepo::toGithubRepo),
            syncedAtEpochMs = cachedRepositories.maxOfOrNull { it.updatedAtEpochMs }
        )
    }

    suspend fun getCachedRepository(fullName: String): GithubRepo? = withContext(Dispatchers.IO) {
        cacheDao.getRepository(fullName)?.toGithubRepo()
    }

    suspend fun getCachedRepositorySnapshot(fullName: String): CachedRepositorySnapshot? =
        withContext(Dispatchers.IO) {
            cacheDao.getRepository(fullName)?.let { cachedRepository ->
                CachedRepositorySnapshot(
                    repository = cachedRepository.toGithubRepo(),
                    syncedAtEpochMs = cachedRepository.updatedAtEpochMs
                )
            }
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

    suspend fun getRepositoryBranches(
        owner: String,
        repositoryName: String
    ): NetworkResult<List<GithubBranch>> = withContext(Dispatchers.IO) {
        val accessToken = authStore.getSession()?.accessToken

        try {
            val response = ApiConfig.getApiService(accessToken)
                .getRepositoryBranches(owner, repositoryName)
                .execute()

            if (response.isSuccessful) {
                NetworkResult.Success(response.body().orEmpty())
            } else {
                NetworkResult.Error("Gagal memuat branch repository. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat branch repository.")
        }
    }

    suspend fun getRepositoryContributors(
        owner: String,
        repositoryName: String
    ): NetworkResult<List<GithubContributor>> = withContext(Dispatchers.IO) {
        val accessToken = authStore.getSession()?.accessToken

        try {
            val response = ApiConfig.getApiService(accessToken)
                .getRepositoryContributors(owner, repositoryName)
                .execute()

            if (response.isSuccessful) {
                NetworkResult.Success(response.body().orEmpty())
            } else {
                NetworkResult.Error("Gagal memuat contributor repository. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat contributor repository.")
        }
    }

    suspend fun getRepositoryIssues(
        owner: String,
        repositoryName: String
    ): NetworkResult<List<GithubIssue>> = withContext(Dispatchers.IO) {
        val accessToken = authStore.getSession()?.accessToken

        try {
            val response = ApiConfig.getApiService(accessToken)
                .getRepositoryIssues(owner, repositoryName)
                .execute()

            if (response.isSuccessful) {
                val issues = response.body().orEmpty()
                    .filter { it.pullRequest == null }
                NetworkResult.Success(issues)
            } else {
                NetworkResult.Error("Gagal memuat issue repository. (${response.code()})")
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memuat issue repository.")
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

    suspend fun getUserSocialState(username: String): NetworkResult<GithubUserSocialState> =
        withContext(Dispatchers.IO) {
            val currentSession = authStore.getSession()
                ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")
            val normalizedUsername = username.trim()
            if (normalizedUsername.isBlank()) {
                return@withContext NetworkResult.Error("Username GitHub tidak valid.")
            }

            if (normalizedUsername.equals(currentSession.login, ignoreCase = true)) {
                return@withContext NetworkResult.Success(
                    GithubUserSocialState(
                        isSelf = true,
                        isFollowing = false
                    )
                )
            }

            val request = newAuthorizedRequestBuilder(
                currentSession.accessToken,
                "$GITHUB_API_BASE/user/following/$normalizedUsername"
            ).get().build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    when (response.code) {
                        204 -> NetworkResult.Success(
                            GithubUserSocialState(
                                isSelf = false,
                                isFollowing = true
                            )
                        )
                        404 -> NetworkResult.Success(
                            GithubUserSocialState(
                                isSelf = false,
                                isFollowing = false
                            )
                        )
                        401 -> {
                            authStore.clearSession()
                            NetworkResult.Error("Sesi GitHub berakhir. Silakan login ulang.")
                        }
                        else -> NetworkResult.Error(
                            "Gagal memeriksa status follow GitHub. (${response.code})"
                        )
                    }
                }
            } catch (error: Exception) {
                NetworkResult.Error(error.localizedMessage ?: "Gagal memeriksa status follow GitHub.")
            }
        }

    suspend fun setUserFollowing(username: String, shouldFollow: Boolean): NetworkResult<Unit> =
        withContext(Dispatchers.IO) {
            val currentSession = authStore.getSession()
                ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")
            val normalizedUsername = username.trim()
            if (normalizedUsername.isBlank()) {
                return@withContext NetworkResult.Error("Username GitHub tidak valid.")
            }

            if (normalizedUsername.equals(currentSession.login, ignoreCase = true)) {
                return@withContext NetworkResult.Error("Akun sendiri tidak perlu di-follow.")
            }

            val requestBuilder = newAuthorizedRequestBuilder(
                currentSession.accessToken,
                "$GITHUB_API_BASE/user/following/$normalizedUsername"
            )
            val request = if (shouldFollow) {
                requestBuilder.put(EMPTY_REQUEST_BODY)
            } else {
                requestBuilder.delete()
            }.build()

            executeUnitRequest(
                request = request,
                successCodes = setOf(204),
                defaultError = if (shouldFollow) {
                    "Gagal mengikuti user GitHub."
                } else {
                    "Gagal membatalkan follow user GitHub."
                }
            )
        }

    suspend fun getRepositorySocialState(
        owner: String,
        repositoryName: String
    ): NetworkResult<GithubRepositorySocialState> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")
        val normalizedOwner = owner.trim()
        val normalizedRepo = repositoryName.trim()
        if (normalizedOwner.isBlank() || normalizedRepo.isBlank()) {
            return@withContext NetworkResult.Error("Repository GitHub tidak valid.")
        }

        val starredRequest = newAuthorizedRequestBuilder(
            currentSession.accessToken,
            "$GITHUB_API_BASE/user/starred/$normalizedOwner/$normalizedRepo"
        ).get().build()
        val watchingRequest = newAuthorizedRequestBuilder(
            currentSession.accessToken,
            "$GITHUB_API_BASE/repos/$normalizedOwner/$normalizedRepo/subscription"
        ).get().build()

        try {
            val isStarred = httpClient.newCall(starredRequest).execute().use { response ->
                when (response.code) {
                    204 -> true
                    404 -> false
                    401 -> {
                        authStore.clearSession()
                        return@withContext NetworkResult.Error("Sesi GitHub berakhir. Silakan login ulang.")
                    }
                    else -> return@withContext NetworkResult.Error(
                        "Gagal memeriksa status star repository. (${response.code})"
                    )
                }
            }

            val isWatching = httpClient.newCall(watchingRequest).execute().use { response ->
                when (response.code) {
                    200 -> {
                        val body = response.body?.string().orEmpty()
                        val subscription = gson.fromJson(
                            body,
                            GithubRepositorySubscriptionResponse::class.java
                        )
                        subscription?.subscribed == true && subscription.ignored != true
                    }
                    404 -> false
                    401 -> {
                        authStore.clearSession()
                        return@withContext NetworkResult.Error("Sesi GitHub berakhir. Silakan login ulang.")
                    }
                    else -> return@withContext NetworkResult.Error(
                        "Gagal memeriksa status watch repository. (${response.code})"
                    )
                }
            }

            NetworkResult.Success(
                GithubRepositorySocialState(
                    isStarred = isStarred,
                    isWatching = isWatching
                )
            )
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: "Gagal memeriksa status sosial repository.")
        }
    }

    suspend fun setRepositoryStarred(
        owner: String,
        repositoryName: String,
        shouldStar: Boolean
    ): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")
        val normalizedOwner = owner.trim()
        val normalizedRepo = repositoryName.trim()
        if (normalizedOwner.isBlank() || normalizedRepo.isBlank()) {
            return@withContext NetworkResult.Error("Repository GitHub tidak valid.")
        }

        val requestBuilder = newAuthorizedRequestBuilder(
            currentSession.accessToken,
            "$GITHUB_API_BASE/user/starred/$normalizedOwner/$normalizedRepo"
        )
        val request = if (shouldStar) {
            requestBuilder.put(EMPTY_REQUEST_BODY)
        } else {
            requestBuilder.delete()
        }.build()

        executeUnitRequest(
            request = request,
            successCodes = setOf(204),
            defaultError = if (shouldStar) {
                "Gagal menandai repository dengan star."
            } else {
                "Gagal menghapus star repository."
            }
        )
    }

    suspend fun setRepositoryWatching(
        owner: String,
        repositoryName: String,
        shouldWatch: Boolean
    ): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")
        val normalizedOwner = owner.trim()
        val normalizedRepo = repositoryName.trim()
        if (normalizedOwner.isBlank() || normalizedRepo.isBlank()) {
            return@withContext NetworkResult.Error("Repository GitHub tidak valid.")
        }

        val requestBuilder = newAuthorizedRequestBuilder(
            currentSession.accessToken,
            "$GITHUB_API_BASE/repos/$normalizedOwner/$normalizedRepo/subscription"
        )
        val request = if (shouldWatch) {
            val requestBody = gson.toJson(
                mapOf(
                    "subscribed" to true,
                    "ignored" to false
                )
            ).toRequestBody(jsonMediaType)
            requestBuilder.put(requestBody)
        } else {
            requestBuilder.delete()
        }.build()

        executeUnitRequest(
            request = request,
            successCodes = if (shouldWatch) setOf(200) else setOf(204),
            defaultError = if (shouldWatch) {
                "Gagal mengaktifkan watch repository."
            } else {
                "Gagal menonaktifkan watch repository."
            }
        )
    }

    suspend fun createIssueComment(
        owner: String,
        repositoryName: String,
        issueNumber: Int,
        body: String
    ): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")
        val normalizedOwner = owner.trim()
        val normalizedRepo = repositoryName.trim()
        val normalizedBody = body.trim()
        if (normalizedOwner.isBlank() || normalizedRepo.isBlank() || issueNumber <= 0) {
            return@withContext NetworkResult.Error("Issue GitHub tidak valid.")
        }
        if (normalizedBody.isBlank()) {
            return@withContext NetworkResult.Error("Isi comment tidak boleh kosong.")
        }

        val requestBody = gson.toJson(mapOf("body" to normalizedBody)).toRequestBody(jsonMediaType)
        val request = newAuthorizedRequestBuilder(
            currentSession.accessToken,
            "$GITHUB_API_BASE/repos/$normalizedOwner/$normalizedRepo/issues/$issueNumber/comments"
        ).post(requestBody).build()

        executeUnitRequest(
            request = request,
            successCodes = setOf(201),
            defaultError = "Gagal mengirim comment ke issue."
        )
    }

    suspend fun addIssueReaction(
        owner: String,
        repositoryName: String,
        issueNumber: Int,
        content: String
    ): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        val currentSession = authStore.getSession()
            ?: return@withContext NetworkResult.Error("Belum ada akun GitHub yang tersinkron.")
        val normalizedOwner = owner.trim()
        val normalizedRepo = repositoryName.trim()
        val normalizedContent = content.trim()
        if (normalizedOwner.isBlank() || normalizedRepo.isBlank() || issueNumber <= 0) {
            return@withContext NetworkResult.Error("Issue GitHub tidak valid.")
        }
        if (normalizedContent.isBlank()) {
            return@withContext NetworkResult.Error("Reaction GitHub tidak valid.")
        }

        val requestBody = gson.toJson(mapOf("content" to normalizedContent)).toRequestBody(jsonMediaType)
        val request = newAuthorizedRequestBuilder(
            currentSession.accessToken,
            "$GITHUB_API_BASE/repos/$normalizedOwner/$normalizedRepo/issues/$issueNumber/reactions"
        ).post(requestBody).build()

        executeUnitRequest(
            request = request,
            successCodes = setOf(200, 201),
            defaultError = "Gagal mengirim reaction ke issue."
        )
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

    private fun newAuthorizedRequestBuilder(accessToken: String, url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $accessToken")
            .header("User-Agent", "GitHubUserRview")
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
    }

    private fun executeUnitRequest(
        request: Request,
        successCodes: Set<Int>,
        defaultError: String
    ): NetworkResult<Unit> {
        return try {
            httpClient.newCall(request).execute().use { response ->
                when {
                    successCodes.contains(response.code) -> NetworkResult.Success(Unit)
                    response.code == 401 -> {
                        authStore.clearSession()
                        NetworkResult.Error("Sesi GitHub berakhir. Silakan login ulang.")
                    }
                    response.code == 403 -> NetworkResult.Error(
                        "$defaultError Scope GitHub Anda belum cukup atau resource dibatasi. (${response.code})"
                    )
                    else -> NetworkResult.Error("$defaultError (${response.code})")
                }
            }
        } catch (error: Exception) {
            NetworkResult.Error(error.localizedMessage ?: defaultError)
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

private data class GithubRepositorySubscriptionResponse(
    @SerializedName("subscribed")
    val subscribed: Boolean?,
    @SerializedName("ignored")
    val ignored: Boolean?
)

private const val GITHUB_API_BASE = "https://api.github.com"
private const val GITHUB_API_VERSION = "2022-11-28"
private val EMPTY_REQUEST_BODY = ByteArray(0).toRequestBody(null)
