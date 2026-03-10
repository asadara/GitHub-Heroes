package com.example.githubuserrview.ui.detail

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.githubuserrview.R
import com.example.githubuserrview.adapter.SectionsPagerAdapter
import com.example.githubuserrview.databinding.ActivityResultBinding
import com.example.githubuserrview.response.DetailUserResponse
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var viewModel: DetailViewModel
    private var currentUser: DetailUserResponse? = null
    private var isFavorite = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = intent.getStringExtra(EXTRA_USERNAME)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.bar_title_detail)

        viewModel = ViewModelProvider(this)[DetailViewModel::class.java]

        viewModel.getDetailUser().observe(this) { user ->
            if (user != null) {
                bindUserDetail(user)
            }
        }
        viewModel.getLoadingState().observe(this, ::showLoading)
        viewModel.getErrorMessage().observe(this) {
            if (!it.isNullOrBlank()) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        binding.toggleFav.setOnClickListener {
            val userDetail = currentUser ?: return@setOnClickListener

            isFavorite = !isFavorite
            if (isFavorite) {
                viewModel.addToFavorite(userDetail.login, userDetail.id, userDetail.avatarUrl)
            } else {
                viewModel.removeFromFavorite(userDetail.id)
            }
            binding.toggleFav.isChecked = isFavorite
        }

        val sectionsPagerAdapter = SectionsPagerAdapter(this, username.toString())
        val viewPager: ViewPager2 = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = resources.getString(TAB_TITLES[position])
        }.attach()

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (!username.isNullOrBlank()) {
            viewModel.loadUserDetail(username)
        } else {
            showLoading(false)
            Toast.makeText(this, R.string.search_error_empty_query, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun bindUserDetail(user: DetailUserResponse) {
        val joinedAt = formatGithubDate(user.createdAt)
        val profileUrl = user.htmlUrl
        val blogUrl = user.blog?.takeIf { it.isNotBlank() }
        val aboutText = user.bio?.takeIf { it.isNotBlank() }
            ?: getString(
                R.string.detail_about_fallback,
                user.login,
                user.type.lowercase(Locale.getDefault())
            )

        currentUser = user
        supportActionBar?.subtitle = user.login

        binding.apply {
            tvProfileBadge.text = resolveInsightTitle(user)
            tvDetailName.text = user.name ?: user.login
            tvDetailId.text = getString(R.string.detail_username_format, user.login)
            tvProfileType.text = getString(R.string.detail_type_joined, user.type, joinedAt)
            tvStatFollowersValue.text = String.format(Locale.getDefault(), "%,d", user.followers)
            tvStatFollowingValue.text = String.format(Locale.getDefault(), "%,d", user.following)
            tvStatReposValue.text = String.format(Locale.getDefault(), "%,d", user.publicRepos)
            tvStatGistsValue.text = String.format(Locale.getDefault(), "%,d", user.publicGists)
            tvDetailProfile.text = getString(
                R.string.detail_overview_body,
                user.company ?: getString(R.string.detail_unknown_value),
                user.location ?: getString(R.string.detail_unknown_value),
                user.publicRepos,
                user.publicGists
            )
            tvProfileSummary.text = buildSummary(user, aboutText)
            tvInsightTitle.text = resolveInsightTitle(user)
            tvInsightBody.text = buildInsightBody(user)
            tvWeb.text = getString(R.string.detail_links_profile, profileUrl)
            tvBlog.text = getString(
                R.string.detail_links_blog,
                blogUrl ?: getString(R.string.detail_blog_unavailable)
            )
            tvOrganizationsUrl.text = getString(R.string.detail_links_orgs, user.organizationsUrl)

            btnOpenBlog.isEnabled = !blogUrl.isNullOrBlank()
            btnOpenBlog.alpha = if (blogUrl.isNullOrBlank()) 0.55f else 1f

            Glide.with(this@ResultActivity)
                .load(user.avatarUrl)
                .centerCrop()
                .into(ivDetailPhoto)
        }

        updateFavoriteState(user.id)
        setupActions(user, blogUrl)
    }

    private fun setupActions(user: DetailUserResponse, blogUrl: String?) {
        val sharePackage = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                """
                GitHub User: ${user.login}
                Name: ${user.name ?: user.login}
                Type: ${user.type}
                Location: ${user.location ?: getString(R.string.detail_unknown_value)}
                Company: ${user.company ?: getString(R.string.detail_unknown_value)}
                Followers: ${user.followers}
                Following: ${user.following}
                Repositories: ${user.publicRepos}
                Gists: ${user.publicGists}
                Profile: ${user.htmlUrl}
                Blog: ${blogUrl ?: getString(R.string.detail_blog_unavailable)}
                """.trimIndent()
            )
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.bar_title_detail))
        }

        binding.btnShare.setOnClickListener {
            startActivity(Intent.createChooser(sharePackage, "Share profile via"))
        }
        binding.btnOpenProfile.setOnClickListener {
            openExternalUrl(user.htmlUrl)
        }
        binding.btnOpenBlog.setOnClickListener {
            if (blogUrl.isNullOrBlank()) {
                Toast.makeText(this, R.string.detail_blog_unavailable, Toast.LENGTH_SHORT).show()
            } else {
                openExternalUrl(blogUrl)
            }
        }
        binding.tvWeb.setOnClickListener { openExternalUrl(user.htmlUrl) }
        binding.tvBlog.setOnClickListener {
            if (!blogUrl.isNullOrBlank()) {
                openExternalUrl(blogUrl)
            }
        }
    }

    private fun buildSummary(user: DetailUserResponse, aboutText: String): String {
        val extras = mutableListOf(
            getString(R.string.detail_network_value, user.followers, user.following)
        )

        user.twitterUsername?.takeIf { it.isNotBlank() }?.let {
            extras.add(getString(R.string.detail_twitter_value, it))
        }

        return buildString {
            append(aboutText)
            append("\n\n")
            append(extras.joinToString("\n"))
        }
    }

    private fun resolveInsightTitle(user: DetailUserResponse): String {
        return when {
            user.followers >= 10_000 -> getString(R.string.detail_signal_high_reach)
            user.publicRepos >= 75 -> getString(R.string.detail_signal_prolific)
            else -> getString(R.string.detail_signal_emerging)
        }
    }

    private fun buildInsightBody(user: DetailUserResponse): String {
        val ratio = if (user.following == 0) {
            user.followers.toDouble()
        } else {
            user.followers.toDouble() / user.following.toDouble()
        }

        return getString(
            R.string.detail_insight_body,
            resolveInsightTitle(user).lowercase(Locale.getDefault()),
            String.format(Locale.getDefault(), "%.1f", ratio),
            user.publicRepos,
            user.publicGists
        )
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
            val parsedDate = parser.parse(rawDate)
            parsedDate?.let { formatter.format(it) } ?: getString(R.string.detail_joined_unknown)
        } catch (_: Exception) {
            getString(R.string.detail_joined_unknown)
        }
    }

    private fun openExternalUrl(rawUrl: String) {
        val normalizedUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.detail_open_link_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.lottie.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun updateFavoriteState(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val count = viewModel.checkUser(id)
            withContext(Dispatchers.Main) {
                isFavorite = count > 0
                binding.toggleFav.isChecked = isFavorite
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_URL = "extra_url"

        fun createIntent(context: Context, username: String): Intent {
            return Intent(context, ResultActivity::class.java).apply {
                putExtra(EXTRA_USERNAME, username)
            }
        }

        fun createIntent(
            context: Context,
            username: String,
            id: Int,
            avatarUrl: String
        ): Intent {
            return Intent(context, ResultActivity::class.java).apply {
                putExtra(EXTRA_USERNAME, username)
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_URL, avatarUrl)
            }
        }

        @StringRes
        private val TAB_TITLES = intArrayOf(
            R.string.tab_text_1,
            R.string.tab_text_2
        )
    }
}
