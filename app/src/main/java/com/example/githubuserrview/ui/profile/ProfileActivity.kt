package com.example.githubuserrview.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.githubuserrview.R
import com.example.githubuserrview.auth.GithubAuthRepository
import com.example.githubuserrview.data.model.GithubEmail
import com.example.githubuserrview.data.model.GithubOrganization
import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.databinding.ActivityProfileBinding
import com.example.githubuserrview.navigation.BottomNavHelper
import com.example.githubuserrview.response.DetailUserResponse
import com.example.githubuserrview.settings.ActiveProfileStore
import com.example.githubuserrview.settings.AppThemeManager
import com.example.githubuserrview.ui.common.AppHeader
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.history.RecentSearchActivity
import com.example.githubuserrview.ui.settings.SettingsActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val githubAuthRepository by lazy { GithubAuthRepository(this) }
    private val repoAdapter by lazy { ProfileRepoAdapter(::openRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppHeader.apply(
            this,
            R.string.profile_title,
            R.string.header_profile_subtitle
        )

        binding.recyclerProfileRepos.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            isNestedScrollingEnabled = false
            adapter = repoAdapter
        }

        binding.btnProfileOpenGithub.setOnClickListener {
            openExternalUrl(githubAuthRepository.getSession()?.htmlUrl)
        }
        binding.btnProfileOpenLive.setOnClickListener {
            val username = githubAuthRepository.getSession()?.login ?: ActiveProfileStore.get(this)
            AppNavigator.open(this, ResultActivity.createIntent(this, username))
        }
        binding.btnProfileRefresh.setOnClickListener {
            loadProfileContent(forceRefreshMessage = true)
        }
        binding.btnProfileOpenSettings.setOnClickListener {
            AppNavigator.open(this, Intent(this, SettingsActivity::class.java))
        }
        binding.btnProfileOpenRecentSearches.setOnClickListener {
            AppNavigator.open(this, Intent(this, RecentSearchActivity::class.java))
        }

        BottomNavHelper.setup(this, binding.bottomNav.bottomNav, R.id.nav_profile)

        loadProfileContent(forceRefreshMessage = false)
    }

    override fun onResume() {
        super.onResume()
        bindSessionSummary()
    }

    private fun loadProfileContent(forceRefreshMessage: Boolean) {
        bindSessionSummary()

        if (githubAuthRepository.getSession() == null) {
            showSignedOutState()
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            githubAuthRepository.getCachedProfile()?.let(::bindProfileDetail)
            val cachedRepos = githubAuthRepository.getCachedRepositories()
            if (cachedRepos.isNotEmpty()) {
                bindRepositories(cachedRepos)
            }

            val profileDeferred = async { githubAuthRepository.getAuthenticatedProfile() }
            val reposDeferred = async { githubAuthRepository.getOwnedPublicRepositories() }
            val organizationsDeferred = async { githubAuthRepository.getOrganizations() }
            val emailsDeferred = async { githubAuthRepository.getEmails() }

            val profileResult = profileDeferred.await()
            val repoResult = reposDeferred.await()
            val organizationsResult = organizationsDeferred.await()
            val emailsResult = emailsDeferred.await()

            showLoading(false)

            when (profileResult) {
                is NetworkResult.Success -> {
                    bindProfileDetail(profileResult.data)
                    ActiveProfileStore.save(this@ProfileActivity, profileResult.data.login)
                }
                is NetworkResult.Error -> {
                    showSignedOutState()
                    showToast(profileResult.message)
                    return@launch
                }
            }

            when (repoResult) {
                is NetworkResult.Success -> bindRepositories(repoResult.data)
                is NetworkResult.Error -> {
                    bindRepositories(emptyList())
                }
            }

            when (organizationsResult) {
                is NetworkResult.Success -> bindOrganizations(
                    organizationsResult.data,
                    getString(R.string.profile_orgs_hint)
                )
                is NetworkResult.Error -> bindOrganizations(
                    emptyList(),
                    organizationsResult.message
                )
            }

            when (emailsResult) {
                is NetworkResult.Success -> bindEmails(
                    emailsResult.data,
                    getString(R.string.profile_emails_hint)
                )
                is NetworkResult.Error -> bindEmails(
                    emptyList(),
                    emailsResult.message
                )
            }

            if (forceRefreshMessage) {
                showToast(getString(R.string.settings_sync_refresh_success))
            }
        }
    }

    private fun bindSessionSummary() {
        val session = githubAuthRepository.getSession()

        if (session == null) {
            binding.tvProfileSyncStatus.text = getString(R.string.profile_sync_status_disconnected)
            binding.tvProfileSyncMeta.text = getString(R.string.profile_sync_meta_disconnected)
            binding.btnProfileOpenGithub.isEnabled = false
            return
        }

        binding.tvProfileSyncStatus.text = getString(R.string.profile_sync_status_connected)
        binding.tvProfileSyncMeta.text = getString(
            R.string.profile_sync_meta_connected,
            session.name ?: session.login,
            session.scope.ifBlank { "read:user" }
        )
        binding.btnProfileOpenGithub.isEnabled = true
    }

    private fun bindProfileDetail(user: DetailUserResponse) {
        Glide.with(this)
            .load(user.avatarUrl)
            .centerCrop()
            .into(binding.ivProfileAvatar)

        binding.tvProfileName.text = user.name ?: user.login
        binding.tvProfileHandle.text = getString(R.string.detail_username_format, user.login)
        binding.tvProfileType.text = getString(
            R.string.detail_type_joined,
            user.type,
            formatGithubDate(user.createdAt)
        )
        binding.tvProfileBio.text = user.bio?.takeIf { it.isNotBlank() }
            ?: getString(
                R.string.detail_about_fallback,
                user.login,
                user.type.lowercase(Locale.getDefault())
            )
        binding.tvProfileOverview.text = getString(
            R.string.profile_overview_body,
            user.company ?: getString(R.string.detail_unknown_value),
            user.location ?: getString(R.string.detail_unknown_value),
            user.email ?: getString(R.string.detail_unknown_value)
        )
        binding.tvProfileFollowers.text = String.format(Locale.getDefault(), "%,d", user.followers)
        binding.tvProfileFollowing.text = String.format(Locale.getDefault(), "%,d", user.following)
        binding.tvProfileRepos.text = String.format(Locale.getDefault(), "%,d", user.publicRepos)
    }

    private fun bindRepositories(repositories: List<GithubRepo>) {
        repoAdapter.submitList(repositories)
        binding.tvProfileRepoEmpty.visibility =
            if (repositories.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerProfileRepos.visibility =
            if (repositories.isEmpty()) View.GONE else View.VISIBLE
        binding.tvProfileRepoSectionMeta.text = getString(
            R.string.profile_repo_count,
            repositories.size
        )
    }

    private fun bindOrganizations(
        organizations: List<GithubOrganization>,
        hintMessage: String
    ) {
        binding.tvProfileOrgsHint.text = if (organizations.isEmpty()) {
            hintMessage.ifBlank { getString(R.string.profile_orgs_empty) }
        } else {
            getString(R.string.profile_orgs_count, organizations.size)
        }

        populateChipGroup(
            chipGroup = binding.chipGroupProfileOrgs,
            labels = organizations.map { it.login },
            emptyLabel = getString(R.string.profile_orgs_empty)
        )
    }

    private fun bindEmails(
        emails: List<GithubEmail>,
        hintMessage: String
    ) {
        binding.tvProfileEmailsHint.text = if (emails.isEmpty()) {
            hintMessage.ifBlank { getString(R.string.profile_emails_empty) }
        } else {
            getString(R.string.profile_emails_count, emails.size)
        }

        populateChipGroup(
            chipGroup = binding.chipGroupProfileEmails,
            labels = emails.map(::buildEmailLabel),
            emptyLabel = getString(R.string.profile_emails_empty)
        )
    }

    private fun populateChipGroup(
        chipGroup: ChipGroup,
        labels: List<String>,
        emptyLabel: String
    ) {
        chipGroup.removeAllViews()

        val values = if (labels.isEmpty()) listOf(emptyLabel) else labels
        values.forEach { label ->
            chipGroup.addView(createInfoChip(label))
        }
    }

    private fun createInfoChip(label: String): Chip {
        return Chip(this).apply {
            text = label
            isCheckable = false
            isClickable = false
            isFocusable = false
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun buildEmailLabel(email: GithubEmail): String {
        val badges = mutableListOf<String>()
        if (email.primary) {
            badges += getString(R.string.profile_email_primary)
        }
        if (email.verified) {
            badges += getString(R.string.profile_email_verified)
        }
        badges += if (email.visibility == "public") {
            getString(R.string.profile_email_public)
        } else {
            getString(R.string.profile_email_private)
        }

        return "${email.email} (${badges.joinToString(", ")})"
    }

    private fun showSignedOutState() {
        binding.ivProfileAvatar.setImageResource(R.drawable.ic_icongithub)
        binding.tvProfileName.text = getString(R.string.profile_sync_status_disconnected)
        binding.tvProfileHandle.text = ""
        binding.tvProfileType.text = getString(R.string.profile_sync_meta_disconnected)
        binding.tvProfileBio.text = getString(R.string.profile_signed_out_body)
        binding.tvProfileOverview.text = getString(
            R.string.profile_overview_body,
            getString(R.string.detail_unknown_value),
            getString(R.string.detail_unknown_value),
            getString(R.string.detail_unknown_value)
        )
        binding.tvProfileFollowers.text = "0"
        binding.tvProfileFollowing.text = "0"
        binding.tvProfileRepos.text = "0"
        binding.tvProfileRepoSectionMeta.text = getString(R.string.profile_repo_count, 0)
        binding.tvProfileRepoEmpty.visibility = View.VISIBLE
        binding.recyclerProfileRepos.visibility = View.GONE
        repoAdapter.submitList(emptyList())
        binding.tvProfileOrgsHint.text = getString(R.string.profile_orgs_hint)
        binding.tvProfileEmailsHint.text = getString(R.string.profile_emails_hint)
        populateChipGroup(
            chipGroup = binding.chipGroupProfileOrgs,
            labels = emptyList(),
            emptyLabel = getString(R.string.profile_orgs_empty)
        )
        populateChipGroup(
            chipGroup = binding.chipGroupProfileEmails,
            labels = emptyList(),
            emptyLabel = getString(R.string.profile_emails_empty)
        )
        binding.btnProfileOpenGithub.isEnabled = false
    }

    private fun showLoading(isLoading: Boolean) {
        binding.profileLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun formatGithubDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) {
            return getString(R.string.detail_joined_unknown)
        }

        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            parser.parse(rawDate)?.let(formatter::format) ?: getString(R.string.detail_joined_unknown)
        } catch (_: Exception) {
            getString(R.string.detail_joined_unknown)
        }
    }

    private fun openRepository(repository: GithubRepo) {
        AppNavigator.open(
            this,
            RepositoryDetailActivity.createIntent(
                context = this,
                owner = repository.owner.login,
                repositoryName = repository.name,
                fullName = repository.fullName
            )
        )
    }

    private fun openExternalUrl(rawUrl: String?) {
        if (rawUrl.isNullOrBlank()) {
            showToast(getString(R.string.detail_open_link_error))
            return
        }

        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)))
        } catch (_: ActivityNotFoundException) {
            showToast(getString(R.string.detail_open_link_error))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
