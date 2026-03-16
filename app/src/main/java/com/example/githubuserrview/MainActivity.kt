package com.example.githubuserrview

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.adapter.HomeCommunityAdapter
import com.example.githubuserrview.api.ApiConfig
import com.example.githubuserrview.auth.GithubAuthRepository
import com.example.githubuserrview.databinding.ActivityMainBinding
import com.example.githubuserrview.data.repository.GithubRepository
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.model.MainViewModel
import com.example.githubuserrview.model.ViewModelFactory
import com.example.githubuserrview.navigation.BottomNavHelper
import com.example.githubuserrview.response.DetailUserResponse
import com.example.githubuserrview.settings.SettingPreferences
import com.example.githubuserrview.settings.AppThemeManager
import com.example.githubuserrview.settings.appDataStore
import com.example.githubuserrview.ui.common.AppHeader
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.common.SyncStatusFormatter
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.history.RecentSearchActivity
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide
import android.view.View

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel
    private val githubAuthRepository by lazy { GithubAuthRepository(this) }
    private var spotlightUsername: String = ""
    private var lastHomeRefreshEpochMs: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppHeader.apply(
            this,
            R.string.bar_title_info,
            R.string.header_home_subtitle
        )

        val pref = SettingPreferences.getInstance(appDataStore)
        mainViewModel = ViewModelProvider(this, ViewModelFactory(pref))[MainViewModel::class.java]
        mainViewModel.getThemeSettings().observe(this) { isDarkModeActive: Boolean ->
            AppCompatDelegate.setDefaultNightMode(
                if (isDarkModeActive) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        setupHomeContent(emptyList())
        showCommunityLoading(true)
        loadHomeData()
        setupQuickActions()
        BottomNavHelper.setup(this, binding.bottomNav.bottomNav, R.id.nav_home)
    }

    private fun setupHomeContent(items: List<DetailUserResponse>) {
        val totalRepositories = items.sumOf { it.publicRepos }
        val totalFollowers = items.sumOf { it.followers }

        binding.tvHomeBadge.text = getString(R.string.home_spotlight_label_creator)
        binding.tvHomeSpotlightName.text = getString(R.string.home_spotlight_loading_name)
        binding.tvHomeSpotlightMeta.text = getString(R.string.home_spotlight_loading)
        binding.tvHomeSpotlightSubtitle.text = getString(R.string.home_spotlight_loading_subtitle)
        binding.ivHomeSpotlightAvatar.setImageResource(R.drawable.ic_icongithub)
        binding.tvHomeProfilesValue.text = items.size.toString()
        binding.tvHomeRepositoriesValue.text = String.format("%,d", totalRepositories)
        binding.tvHomeFollowersValue.text = String.format("%,d", totalFollowers)
        binding.btnHomeSpotlight.isEnabled = false
        binding.tvHomeLiveState.text = getString(R.string.home_status_loading)

        binding.btnHomeSpotlight.setOnClickListener {
            if (spotlightUsername.isNotBlank()) {
                AppNavigator.open(this, ResultActivity.createIntent(this, spotlightUsername))
            }
        }
    }

    private fun loadHomeData() {
        setHomeRefreshing(true)
        binding.tvHomeLiveState.text = getString(R.string.home_status_loading)
        lifecycleScope.launch {
            val githubRepository = GithubRepository(
                ApiConfig.getApiService(githubAuthRepository.getSession()?.accessToken)
            )
            when (
                val searchResult = githubRepository.searchUsers(
                    query = HOME_DISCOVERY_QUERY,
                    page = 1,
                    perPage = HOME_COMMUNITY_LIMIT,
                    sort = "followers",
                    order = "desc"
                )
            ) {
                is NetworkResult.Success -> {
                    val detailedUsers = searchResult.data.items.map { user ->
                        async { githubRepository.getUserDetail(user.login) }
                    }.mapNotNull { deferred ->
                        when (val detailResult = deferred.await()) {
                            is NetworkResult.Success -> detailResult.data
                            is NetworkResult.Error -> null
                        }
                    }

                    val communityPicks = sortCommunityPicks(detailedUsers)
                    setupHomeContent(communityPicks)
                    if (communityPicks.isNotEmpty()) {
                        bindCreatorSpotlight(communityPicks.first())
                    } else {
                        showCreatorFallback()
                    }
                    binding.tvHomeEmpty.text = getString(R.string.home_community_empty)
                    lastHomeRefreshEpochMs = System.currentTimeMillis()
                    binding.tvHomeLiveState.text = getString(
                        R.string.home_status_updated,
                        SyncStatusFormatter.formatTimestamp(lastHomeRefreshEpochMs!!)
                    )
                    showListGh(communityPicks)
                }
                is NetworkResult.Error -> {
                    showCreatorFallback()
                    binding.tvHomeEmpty.text = getString(R.string.home_status_error)
                    binding.tvHomeLiveState.text = getString(R.string.home_status_error)
                    showListGh(emptyList())
                }
            }
            setHomeRefreshing(false)
        }
    }

    private fun bindCreatorSpotlight(user: DetailUserResponse) {
        spotlightUsername = user.login
        binding.btnHomeSpotlight.isEnabled = true
        Glide.with(this)
            .load(user.avatarUrl)
            .centerCrop()
            .into(binding.ivHomeSpotlightAvatar)
        binding.tvHomeSpotlightName.text = user.name ?: user.login
        binding.tvHomeSpotlightMeta.text = getString(
            R.string.home_spotlight_meta,
            user.followers,
            user.publicRepos
        )
        binding.tvHomeSpotlightSubtitle.text = getString(
            R.string.home_card_meta,
            user.company ?: getString(R.string.detail_unknown_value),
            user.location ?: getString(R.string.detail_unknown_value)
        )
    }

    private fun showCreatorFallback() {
        binding.ivHomeSpotlightAvatar.setImageResource(R.drawable.ic_icongithub)
        binding.tvHomeSpotlightName.text = getString(R.string.home_spotlight_fallback_name)
        binding.tvHomeSpotlightMeta.text = getString(R.string.home_spotlight_fallback_meta)
        binding.tvHomeSpotlightSubtitle.text = getString(R.string.home_spotlight_fallback_subtitle)
        spotlightUsername = ""
        binding.btnHomeSpotlight.isEnabled = false
    }

    private fun sortCommunityPicks(items: List<DetailUserResponse>): List<DetailUserResponse> {
        return items.sortedWith(
            compareByDescending<DetailUserResponse> { it.followers }
                .thenByDescending { it.publicRepos }
                .thenBy { it.login.lowercase() }
        )
    }

    private fun setupQuickActions() {
        binding.btnHomeFavorites.setOnClickListener {
            AppNavigator.open(this, Intent(this, MyFavorites::class.java))
        }
        binding.btnHomeRecent.setOnClickListener {
            AppNavigator.open(this, Intent(this, RecentSearchActivity::class.java))
        }
        binding.btnHomeRefresh.setOnClickListener {
            loadHomeData()
        }
    }

    private fun showSelectedItem(user: DetailUserResponse) {
        AppNavigator.open(this, ResultActivity.createIntent(this, user.login))
    }

    private fun showListGh(items: List<DetailUserResponse>) {
        showCommunityLoading(false)
        binding.tvHomeEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE

        binding.recyclerView.apply {
            layoutManager = if (
                applicationContext.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            ) {
                GridLayoutManager(this@MainActivity, 2)
            } else {
                LinearLayoutManager(this@MainActivity)
            }
            isNestedScrollingEnabled = false
            setHasFixedSize(true)
            adapter = HomeCommunityAdapter(items, ::showSelectedItem)
        }
    }

    private fun showCommunityLoading(isLoading: Boolean) {
        binding.lottieHomeLoading.visibility =
            if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.recyclerView.visibility = View.GONE
            binding.tvHomeEmpty.visibility = View.GONE
        }
    }

    private fun setHomeRefreshing(isRefreshing: Boolean) {
        binding.btnHomeRefresh.isEnabled = !isRefreshing
        binding.btnHomeRefresh.alpha = if (isRefreshing) 0.6f else 1f
    }

    companion object {
        private const val HOME_DISCOVERY_QUERY = "type:user followers:>1000 repos:>10"
        private const val HOME_COMMUNITY_LIMIT = 8
    }
}
