package com.example.githubuserrview.ui.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.caverock.androidsvg.SVG
import com.example.githubuserrview.BuildConfig
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RepositoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRepositoryDetailBinding
    private val githubAuthRepository by lazy { GithubAuthRepository(this) }
    private val previewImageClient by lazy { OkHttpClient() }
    private var currentRepository: GithubRepo? = null
    private var previewLoadJob: Job? = null
    private var debugPreviewMode = DebugPreviewMode.LIVE_REPO

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
            openExternalUrl(resolveActiveHomepageUrl(currentRepository?.homepage))
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
        if (BuildConfig.DEBUG) {
            binding.btnRepoRefresh.setOnLongClickListener {
                advanceDebugPreviewMode()
                true
            }
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
        val activeHomepageUrl = resolveActiveHomepageUrl(repository.homepage)
        val normalizedHomepageUrl = normalizeExternalUrl(activeHomepageUrl)
        val previewImageUrl = extractPreviewImageUrl(activeHomepageUrl)

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
            normalizedHomepageUrl
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
        bindPreviewImage(previewImageUrl)
        binding.btnRepoOpenHomepage.text = getString(
            if (previewImageUrl != null) {
                R.string.repo_detail_open_preview_image
            } else {
                R.string.repo_detail_open_homepage
            }
        )
        binding.btnRepoOpenHomepage.isEnabled = normalizedHomepageUrl != null
        binding.btnRepoOpenHomepage.alpha = if (normalizedHomepageUrl == null) 0.55f else 1f
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

    private fun bindPreviewImage(previewImageUrl: String?) {
        val hasPreview = !previewImageUrl.isNullOrBlank()

        binding.tvRepoPreviewTitle.visibility = if (hasPreview) View.VISIBLE else View.GONE
        binding.cardRepoPreview.visibility = if (hasPreview) View.VISIBLE else View.GONE

        previewLoadJob?.cancel()

        if (!hasPreview) {
            binding.ivRepoPreview.setLayerType(View.LAYER_TYPE_NONE, null)
            binding.ivRepoPreview.setImageDrawable(null)
            Glide.with(this).clear(binding.ivRepoPreview)
            return
        }

        if (isSvgUrl(previewImageUrl)) {
            Glide.with(this).clear(binding.ivRepoPreview)
            loadSvgPreview(previewImageUrl.orEmpty())
        } else {
            binding.ivRepoPreview.setLayerType(View.LAYER_TYPE_NONE, null)
            Glide.with(this)
                .load(previewImageUrl)
                .centerCrop()
                .into(binding.ivRepoPreview)
        }
    }

    private fun extractPreviewImageUrl(rawUrl: String?): String? {
        val normalizedUrl = normalizeExternalUrl(rawUrl) ?: return null
        val path = Uri.parse(normalizedUrl).encodedPath.orEmpty().lowercase(Locale.US)
        return if (IMAGE_EXTENSIONS.any(path::endsWith)) normalizedUrl else null
    }

    private fun resolveActiveHomepageUrl(rawUrl: String?): String? {
        return when (debugPreviewMode) {
            DebugPreviewMode.LIVE_REPO -> rawUrl
            DebugPreviewMode.SAMPLE_PNG -> DEBUG_SAMPLE_PNG_URL
            DebugPreviewMode.SAMPLE_SVG -> DEBUG_SAMPLE_SVG_URL
        }
    }

    private fun advanceDebugPreviewMode() {
        debugPreviewMode = when (debugPreviewMode) {
            DebugPreviewMode.LIVE_REPO -> DebugPreviewMode.SAMPLE_PNG
            DebugPreviewMode.SAMPLE_PNG -> DebugPreviewMode.SAMPLE_SVG
            DebugPreviewMode.SAMPLE_SVG -> DebugPreviewMode.LIVE_REPO
        }

        currentRepository?.let(::bindRepository)
        showToast(
            getString(
                when (debugPreviewMode) {
                    DebugPreviewMode.LIVE_REPO -> R.string.repo_detail_debug_preview_live
                    DebugPreviewMode.SAMPLE_PNG -> R.string.repo_detail_debug_preview_png
                    DebugPreviewMode.SAMPLE_SVG -> R.string.repo_detail_debug_preview_svg
                }
            )
        )
    }

    private fun isSvgUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) {
            return false
        }
        val path = Uri.parse(url).encodedPath.orEmpty().lowercase(Locale.US)
        return path.endsWith(SVG_EXTENSION)
    }

    private fun loadSvgPreview(previewImageUrl: String) {
        binding.ivRepoPreview.setImageDrawable(null)
        binding.ivRepoPreview.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        previewLoadJob = lifecycleScope.launch {
            val drawable = withContext(Dispatchers.IO) {
                fetchSvgDrawable(previewImageUrl)
            }

            if (
                !isFinishing &&
                !isDestroyed &&
                resolveActiveHomepageUrl(currentRepository?.homepage)?.let(::normalizeExternalUrl) == previewImageUrl
            ) {
                if (drawable != null) {
                    binding.ivRepoPreview.setImageDrawable(drawable)
                } else {
                    binding.cardRepoPreview.visibility = View.GONE
                    binding.tvRepoPreviewTitle.visibility = View.GONE
                }
            }
        }
    }

    private fun fetchSvgDrawable(previewImageUrl: String): PictureDrawable? {
        return try {
            val request = Request.Builder()
                .url(previewImageUrl)
                .build()
            previewImageClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }

                val svg = response.body?.byteStream()?.use(SVG::getFromInputStream) ?: return null
                PictureDrawable(svg.renderToPicture())
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeExternalUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) {
            return null
        }

        return if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
    }

    private fun openExternalUrl(rawUrl: String?) {
        val normalizedUrl = normalizeExternalUrl(rawUrl)
        if (normalizedUrl == null) {
            showToast(getString(R.string.detail_open_link_error))
            return
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

    override fun onDestroy() {
        previewLoadJob?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_OWNER = "extra_owner"
        private const val EXTRA_REPOSITORY_NAME = "extra_repository_name"
        private const val EXTRA_FULL_NAME = "extra_full_name"
        private const val SVG_EXTENSION = ".svg"
        private const val DEBUG_SAMPLE_PNG_URL =
            "https://upload.wikimedia.org/wikipedia/commons/f/f6/Kaohsiung_Location_Map.png"
        private const val DEBUG_SAMPLE_SVG_URL =
            "https://upload.wikimedia.org/wikipedia/commons/4/4a/Commons-logo.svg"
        private val IMAGE_EXTENSIONS = listOf(
            ".png",
            ".jpg",
            ".jpeg",
            ".webp",
            ".gif",
            ".bmp",
            ".avif",
            SVG_EXTENSION
        )

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

    private enum class DebugPreviewMode {
        LIVE_REPO,
        SAMPLE_PNG,
        SAMPLE_SVG
    }

    private enum class SyncSource {
        LIVE,
        CACHE,
        CACHE_ERROR
    }
}
