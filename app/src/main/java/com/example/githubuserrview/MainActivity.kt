package com.example.githubuserrview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.adapter.PackageAdapter
import com.example.githubuserrview.api.Package
import com.example.githubuserrview.databinding.ActivityMainBinding
import com.example.githubuserrview.data.repository.GithubRepositoryProvider
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
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.history.RecentSearchActivity
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel
    private val profiles: ArrayList<Package> by lazy { listGitHub }
    private val githubRepository by lazy { GithubRepositoryProvider.getInstance() }

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

        val communityPicks = sortCommunityPicks(profiles)
        setupHomeContent(communityPicks)
        showCommunityLoading(true)
        loadCreatorSpotlight()
        setupQuickActions()
        showListGh(communityPicks)
        BottomNavHelper.setup(this, binding.bottomNav.bottomNav, R.id.nav_home)
    }

    private val listGitHub: ArrayList<Package>
        @SuppressLint("Recycle")
        get() {
            val dataId = resources.getStringArray(R.array.username)
            val dataNama = resources.getStringArray(R.array.surename)
            val dataFoto = resources.obtainTypedArray(R.array.avatar)
            val dataLokasi = resources.getStringArray(R.array.location)
            val dataRepository = resources.getIntArray(R.array.repository)
            val dataCompany = resources.getStringArray(R.array.company)
            val dataFollowers = resources.getIntArray(R.array.followers)
            val dataFollowing = resources.getIntArray(R.array.following)
            val dataDeskripsi = resources.getStringArray(R.array.description)

            val listgh = ArrayList<Package>()

            for (i in dataId.indices) {
                val gh = Package(
                    dataId[i],
                    dataNama[i],
                    dataFoto.getResourceId(i, -1),
                    dataLokasi[i],
                    dataRepository[i],
                    dataCompany[i],
                    dataFollowers[i],
                    dataFollowing[i],
                    dataDeskripsi[i]
                )
                listgh.add(gh)
            }
            return listgh
        }

    private fun setupHomeContent(items: List<Package>) {
        val totalRepositories = items.sumOf { it.repository }
        val totalFollowers = items.sumOf { it.followers }

        binding.tvHomeBadge.text = getString(R.string.home_spotlight_label_creator)
        binding.tvHomeSpotlightName.text = SPOTLIGHT_USERNAME
        binding.tvHomeSpotlightMeta.text = getString(R.string.home_spotlight_loading)
        binding.tvHomeSpotlightSubtitle.text = getString(R.string.home_spotlight_fallback_subtitle)
        binding.ivHomeSpotlightAvatar.setImageResource(R.drawable.ic_icongithub)
        binding.tvHomeProfilesValue.text = items.size.toString()
        binding.tvHomeRepositoriesValue.text = String.format("%,d", totalRepositories)
        binding.tvHomeFollowersValue.text = String.format("%,d", totalFollowers)

        binding.btnHomeSpotlight.setOnClickListener {
            AppNavigator.open(this, ResultActivity.createIntent(this, SPOTLIGHT_USERNAME))
        }
    }

    private fun loadCreatorSpotlight() {
        lifecycleScope.launch {
            when (val result = githubRepository.getUserDetail(SPOTLIGHT_USERNAME)) {
                is NetworkResult.Success -> bindCreatorSpotlight(result.data)
                is NetworkResult.Error -> showCreatorFallback()
            }
        }
    }

    private fun bindCreatorSpotlight(user: DetailUserResponse) {
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
        binding.tvHomeSpotlightName.text = SPOTLIGHT_USERNAME
        binding.tvHomeSpotlightMeta.text = getString(R.string.home_spotlight_fallback_meta)
        binding.tvHomeSpotlightSubtitle.text = getString(R.string.home_spotlight_fallback_subtitle)
    }

    private fun sortCommunityPicks(items: List<Package>): List<Package> {
        return items.sortedWith(
            compareByDescending<Package> { it.followers }
                .thenByDescending { it.repository }
                .thenBy { it.username.lowercase() }
        )
    }

    private fun setupQuickActions() {
        binding.btnHomeFavorites.setOnClickListener {
            AppNavigator.open(this, Intent(this, MyFavorites::class.java))
        }
        binding.btnHomeRecent.setOnClickListener {
            AppNavigator.open(this, Intent(this, RecentSearchActivity::class.java))
        }
    }

    private fun showSelectedItem(gh: Package) {
        AppNavigator.open(this, ResultActivity.createIntent(this, gh.username))
    }

    private fun showListGh(items: List<Package>) {
        showCommunityLoading(false)
        binding.tvHomeEmpty.visibility =
            if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerView.visibility =
            if (items.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

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
            adapter = PackageAdapter(items, ::showSelectedItem)
        }
    }

    private fun showCommunityLoading(isLoading: Boolean) {
        binding.lottieHomeLoading.visibility =
            if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        if (isLoading) {
            binding.recyclerView.visibility = android.view.View.GONE
            binding.tvHomeEmpty.visibility = android.view.View.GONE
        }
    }

    companion object {
        private const val SPOTLIGHT_USERNAME = "asadara"
    }
}
