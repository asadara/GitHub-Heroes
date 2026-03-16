package com.example.githubuserrview.ui.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.githubuserrview.R
import com.example.githubuserrview.auth.GithubAuthRepository
import com.example.githubuserrview.data.model.GithubBranch
import com.example.githubuserrview.data.model.GithubCommit
import com.example.githubuserrview.data.model.GithubContributor
import com.example.githubuserrview.data.model.GithubIssue
import com.example.githubuserrview.data.model.GithubReadme
import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.databinding.ActivityRepositoryDetailBinding
import com.example.githubuserrview.settings.AppThemeManager
import com.example.githubuserrview.ui.common.AppHeader
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.common.SyncStatusFormatter
import com.example.githubuserrview.ui.detail.ResultActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class RepositoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRepositoryDetailBinding
    private val githubAuthRepository by lazy { GithubAuthRepository(this) }
    private var currentRepository: GithubRepo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivityRepositoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppHeader.apply(
            this,
            R.string.repo_detail_title,
            R.string.repo_detail_subtitle,
            showBack = true
        )

        val owner = intent.getStringExtra(EXTRA_OWNER).orEmpty()
        val repositoryName = intent.getStringExtra(EXTRA_REPOSITORY_NAME).orEmpty()
        val fullName = intent.getStringExtra(EXTRA_FULL_NAME).orEmpty()

        binding.btnRepoOpenGithub.setOnClickListener {
            openExternalUrl(currentRepository?.htmlUrl)
        }
        binding.btnRepoOpenHomepage.setOnClickListener {
            openExternalUrl(currentRepository?.homepage)
        }
        binding.btnRepoOpenOwnerProfile.setOnClickListener {
            currentRepository?.owner?.login?.takeIf { it.isNotBlank() }?.let { ownerLogin ->
                AppNavigator.open(
                    this,
                    ResultActivity.createIntent(this, ownerLogin)
                )
            }
        }
        binding.btnRepoRefresh.setOnClickListener {
            loadRepository(owner, repositoryName, fullName, showRefreshToast = true)
        }

        setRefreshEnabled(false)
        loadRepository(owner, repositoryName, fullName, showRefreshToast = false)
    }

    private fun loadRepository(
        owner: String,
        repositoryName: String,
        fullName: String,
        showRefreshToast: Boolean
    ) {
        if (owner.isBlank() || repositoryName.isBlank()) {
            showToast(getString(R.string.repo_detail_error_missing))
            AppNavigator.finish(this)
            return
        }

        showLoading(true)
        setRefreshEnabled(false)
        lifecycleScope.launch {
            val cachedSnapshot = githubAuthRepository.getCachedRepositorySnapshot(fullName)
            cachedSnapshot?.let { snapshot ->
                bindRepository(snapshot.repository)
                renderRepositoryState(
                    source = SyncSource.CACHE,
                    lastSyncEpochMs = snapshot.syncedAtEpochMs
                )
            }

            val detailDeferred = async {
                githubAuthRepository.getRepositoryDetail(owner, repositoryName)
            }
            val readmeDeferred = async {
                githubAuthRepository.getRepositoryReadme(owner, repositoryName)
            }
            val commitsDeferred = async {
                githubAuthRepository.getRecentCommits(owner, repositoryName)
            }
            val branchesDeferred = async {
                githubAuthRepository.getRepositoryBranches(owner, repositoryName)
            }
            val contributorsDeferred = async {
                githubAuthRepository.getRepositoryContributors(owner, repositoryName)
            }
            val issuesDeferred = async {
                githubAuthRepository.getRepositoryIssues(owner, repositoryName)
            }

            when (val result = detailDeferred.await()) {
                is NetworkResult.Success -> {
                    bindRepository(result.data)
                    renderRepositoryState(
                        source = SyncSource.LIVE,
                        lastSyncEpochMs = System.currentTimeMillis()
                    )
                }
                is NetworkResult.Error -> {
                    if (currentRepository == null) {
                        showToast(result.message)
                    } else {
                        renderRepositoryState(
                            source = SyncSource.CACHE_ERROR,
                            lastSyncEpochMs = cachedSnapshot?.syncedAtEpochMs
                        )
                    }
                }
            }

            when (val readmeResult = readmeDeferred.await()) {
                is NetworkResult.Success -> bindReadme(readmeResult.data)
                is NetworkResult.Error -> bindReadme(null)
            }

            when (val commitsResult = commitsDeferred.await()) {
                is NetworkResult.Success -> bindCommits(commitsResult.data)
                is NetworkResult.Error -> bindCommits(emptyList())
            }

            when (val branchesResult = branchesDeferred.await()) {
                is NetworkResult.Success -> bindBranches(branchesResult.data)
                is NetworkResult.Error -> bindBranches(emptyList())
            }

            when (val contributorsResult = contributorsDeferred.await()) {
                is NetworkResult.Success -> bindContributors(contributorsResult.data)
                is NetworkResult.Error -> bindContributors(emptyList())
            }

            when (val issuesResult = issuesDeferred.await()) {
                is NetworkResult.Success -> bindIssues(issuesResult.data)
                is NetworkResult.Error -> bindIssues(emptyList())
            }

            if (showRefreshToast && currentRepository != null) {
                showToast(getString(R.string.repo_detail_refreshed))
            }
            showLoading(false)
            setRefreshEnabled(currentRepository != null)
        }
    }

    private fun bindRepository(repository: GithubRepo) {
        currentRepository = repository

        Glide.with(this)
            .load(repository.owner.avatarUrl)
            .centerCrop()
            .into(binding.ivRepoOwnerAvatar)

        binding.tvRepoOwnerLogin.text = getString(
            R.string.detail_username_format,
            repository.owner.login
        )
        binding.tvRepoDetailName.text = repository.name
        binding.tvRepoDetailFullName.text = repository.fullName
        binding.tvRepoDetailDescription.text = repository.description
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_repo_description_fallback)
        binding.tvRepoDetailVisibility.text = getString(
            R.string.repo_detail_visibility,
            if (repository.isPrivate) getString(R.string.repo_detail_private)
            else getString(R.string.repo_detail_public)
        )
        binding.tvRepoDetailOverview.text = getString(
            R.string.repo_detail_overview,
            repository.language ?: getString(R.string.detail_unknown_value),
            repository.defaultBranch ?: getString(R.string.detail_unknown_value),
            repository.license?.name ?: getString(R.string.detail_unknown_value),
            repository.size ?: 0
        )
        binding.tvRepoDetailStats.text = getString(
            R.string.repo_detail_stats,
            repository.stargazersCount,
            repository.forksCount,
            repository.watchersCount,
            repository.openIssuesCount
        )
        binding.tvRepoDetailLinks.text = getString(
            R.string.repo_detail_links,
            repository.htmlUrl,
            repository.homepage?.takeIf { it.isNotBlank() }
                ?: getString(R.string.detail_unknown_value)
        )
        binding.tvRepoDetailUpdated.text = getString(
            R.string.repo_detail_updated,
            formatUpdatedAt(repository.updatedAt)
        )
        binding.btnRepoOpenOwnerProfile.text = getString(
            R.string.repo_detail_open_owner_profile,
            repository.owner.login
        )
        binding.btnRepoOpenHomepage.isEnabled = !repository.homepage.isNullOrBlank()
        binding.btnRepoOpenHomepage.alpha = if (repository.homepage.isNullOrBlank()) 0.55f else 1f
        setRefreshEnabled(true)
    }

    private fun bindReadme(readme: GithubReadme?) {
        val content = readme?.content
            ?.takeIf { !it.isBlank() }
            ?.let(::decodeReadme)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        binding.tvRepoReadme.text = content?.take(1600)
            ?: getString(R.string.repo_detail_readme_empty)
    }

    private fun bindCommits(commits: List<GithubCommit>) {
        binding.tvRepoCommits.text = if (commits.isEmpty()) {
            getString(R.string.repo_detail_commits_empty)
        } else {
            commits.joinToString("\n\n") { commit ->
                val author = commit.commit.author?.name ?: getString(R.string.detail_unknown_value)
                val date = formatUpdatedAt(commit.commit.author?.date)
                val message = commit.commit.message.lineSequence().firstOrNull().orEmpty()
                "• $message\n$author · $date"
            }
        }
    }

    private fun bindBranches(branches: List<GithubBranch>) {
        populateChipGroup(
            chipGroup = binding.chipGroupRepoBranches,
            labels = branches.map { branch ->
                if (branch.isProtected) {
                    getString(R.string.repo_detail_branch_protected, branch.name)
                } else {
                    branch.name
                }
            },
            emptyLabel = getString(R.string.repo_detail_branches_empty)
        )
    }

    private fun bindContributors(contributors: List<GithubContributor>) {
        populateChipGroup(
            chipGroup = binding.chipGroupRepoContributors,
            labels = contributors.map { contributor ->
                getString(
                    R.string.repo_detail_contributor_label,
                    contributor.login,
                    contributor.contributions
                )
            },
            emptyLabel = getString(R.string.repo_detail_contributors_empty)
        )
    }

    private fun bindIssues(issues: List<GithubIssue>) {
        binding.tvRepoIssues.text = if (issues.isEmpty()) {
            getString(R.string.repo_detail_issues_empty)
        } else {
            buildString {
                appendLine(getString(R.string.repo_detail_issue_header, issues.size))
                appendLine()
                append(
                    issues.joinToString("\n\n") { issue ->
                        getString(
                            R.string.repo_detail_issue_line,
                            issue.number,
                            issue.title,
                            issue.user?.login ?: getString(R.string.detail_unknown_value),
                            issue.comments
                        )
                    }
                )
            }.trim()
        }
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

    private fun showLoading(isLoading: Boolean) {
        binding.repoDetailLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun renderRepositoryState(source: SyncSource, lastSyncEpochMs: Long?) {
        binding.tvRepoDetailState.text = when (source) {
            SyncSource.LIVE -> lastSyncEpochMs?.let {
                getString(
                    R.string.repo_detail_state_live_updated,
                    SyncStatusFormatter.formatTimestamp(it)
                )
            } ?: getString(R.string.repo_detail_state_live)
            SyncSource.CACHE -> lastSyncEpochMs?.let {
                getString(
                    R.string.repo_detail_state_cache_updated,
                    SyncStatusFormatter.formatTimestamp(it)
                )
            } ?: getString(R.string.repo_detail_state_cache)
            SyncSource.CACHE_ERROR -> getString(R.string.repo_detail_state_cache_error)
        }
    }

    private fun setRefreshEnabled(isEnabled: Boolean) {
        binding.btnRepoRefresh.isEnabled = isEnabled
        binding.btnRepoOpenGithub.isEnabled = isEnabled && currentRepository != null
        binding.btnRepoOpenOwnerProfile.isEnabled = isEnabled && currentRepository != null
    }

    private fun formatUpdatedAt(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) {
            return getString(R.string.detail_joined_unknown)
        }

        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            parser.parse(rawDate)?.let(formatter::format) ?: getString(R.string.detail_joined_unknown)
        } catch (_: Exception) {
            getString(R.string.detail_joined_unknown)
        }
    }

    private fun decodeReadme(encodedContent: String): String {
        return try {
            val normalized = encodedContent.replace("\n", "")
            String(Base64.decode(normalized, Base64.DEFAULT))
        } catch (_: Exception) {
            encodedContent
        }
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

    override fun onSupportNavigateUp(): Boolean {
        AppNavigator.finish(this)
        return true
    }

    companion object {
        private const val EXTRA_OWNER = "extra_owner"
        private const val EXTRA_REPOSITORY_NAME = "extra_repository_name"
        private const val EXTRA_FULL_NAME = "extra_full_name"

        fun createIntent(
            context: Context,
            owner: String,
            repositoryName: String,
            fullName: String
        ): Intent {
            return Intent(context, RepositoryDetailActivity::class.java).apply {
                putExtra(EXTRA_OWNER, owner)
                putExtra(EXTRA_REPOSITORY_NAME, repositoryName)
                putExtra(EXTRA_FULL_NAME, fullName)
            }
        }
    }

    private enum class SyncSource {
        LIVE,
        CACHE,
        CACHE_ERROR
    }
}
