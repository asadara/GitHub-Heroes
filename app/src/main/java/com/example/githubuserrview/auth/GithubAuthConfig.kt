package com.example.githubuserrview.auth

import android.net.Uri
import android.util.Base64
import com.example.githubuserrview.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom

object GithubAuthConfig {

    private val secureRandom = SecureRandom()
    private val scopeDelimiter = Regex("[,\\s]+")
    private val socialScopes = listOf(
        "read:user",
        "user:email",
        "user:follow",
        "notifications",
        "public_repo"
    )

    val clientId: String
        get() = BuildConfig.GITHUB_CLIENT_ID

    val clientSecret: String
        get() = BuildConfig.GITHUB_CLIENT_SECRET

    val redirectUri: String
        get() = buildString {
            append(BuildConfig.GITHUB_REDIRECT_SCHEME)
            append("://")
            append(BuildConfig.GITHUB_REDIRECT_HOST)
            append(BuildConfig.GITHUB_REDIRECT_PATH)
        }

    val scopes: String
        get() = BuildConfig.GITHUB_AUTH_SCOPES

    val requiredSocialScopes: List<String>
        get() = socialScopes

    val isConfigured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    fun normalizeScopeString(rawScope: String?): String {
        return parseScopeSet(rawScope).joinToString(" ")
    }

    fun missingSocialScopes(rawScope: String?): List<String> {
        val grantedScopes = parseScopeSet(rawScope).toSet()
        return socialScopes.filterNot(grantedScopes::contains)
    }

    fun hasSocialScopeAccess(rawScope: String?): Boolean {
        return missingSocialScopes(rawScope).isEmpty()
    }

    fun createState(): String = randomUrlSafeValue(24)

    fun createCodeVerifier(): String = randomUrlSafeValue(48)

    fun buildAuthorizationUri(state: String, codeVerifier: String): Uri {
        return Uri.parse("https://github.com/login/oauth/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scopes)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", buildCodeChallenge(codeVerifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("allow_signup", "false")
            .build()
    }

    private fun buildCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray())
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun randomUrlSafeValue(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun parseScopeSet(rawScope: String?): List<String> {
        return rawScope
            .orEmpty()
            .split(scopeDelimiter)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
    }
}
