package com.example.githubuserrview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.adapter.PackageAdapter
import com.example.githubuserrview.api.Package
import com.example.githubuserrview.databinding.ActivityMainBinding
import com.example.githubuserrview.model.MainViewModel
import com.example.githubuserrview.model.ViewModelFactory
import com.example.githubuserrview.navigation.BottomNavHelper
import com.example.githubuserrview.settings.SettingPreferences
import com.example.githubuserrview.settings.appDataStore
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.history.RecentSearchActivity
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel
    private val profiles: ArrayList<Package> by lazy { listGitHub }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_GitHubUserRview)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.bar_title_info)
        supportActionBar?.subtitle = getString(R.string.home_toolbar_subtitle)

        val pref = SettingPreferences.getInstance(appDataStore)
        mainViewModel = ViewModelProvider(this, ViewModelFactory(pref))[MainViewModel::class.java]
        mainViewModel.getThemeSettings().observe(this) { isDarkModeActive: Boolean ->
            AppCompatDelegate.setDefaultNightMode(
                if (isDarkModeActive) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        setupHomeContent(profiles)
        setupQuickActions()
        showListGh(profiles)
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
        val spotlight = selectSpotlight(items) ?: return
        val totalRepositories = items.sumOf { it.repository }
        val totalFollowers = items.sumOf { it.followers }

        binding.tvHomeBadge.text = getString(R.string.home_spotlight_label_balanced)
        binding.tvHomeSpotlightName.text = spotlight.surename
        binding.tvHomeSpotlightMeta.text = getString(
            R.string.home_spotlight_meta,
            spotlight.followers,
            spotlight.repository
        )
        binding.tvHomeSpotlightSubtitle.text = getString(
            R.string.home_card_meta,
            spotlight.company,
            spotlight.location
        )
        binding.tvHomeProfilesValue.text = items.size.toString()
        binding.tvHomeRepositoriesValue.text = String.format("%,d", totalRepositories)
        binding.tvHomeFollowersValue.text = String.format("%,d", totalFollowers)

        binding.btnHomeSpotlight.setOnClickListener {
            startActivity(ResultActivity.createIntent(this, spotlight.username))
        }
    }

    private fun selectSpotlight(items: List<Package>): Package? {
        if (items.isEmpty()) return null

        val maxFollowers = items.maxOf { it.followers }.coerceAtLeast(1)
        val maxRepositories = items.maxOf { it.repository }.coerceAtLeast(1)

        return items.maxByOrNull { item ->
            val followerScore = item.followers.toDouble() / maxFollowers.toDouble()
            val repositoryScore = item.repository.toDouble() / maxRepositories.toDouble()
            (followerScore * 0.7) + (repositoryScore * 0.3)
        }
    }

    private fun setupQuickActions() {
        binding.btnHomeFavorites.setOnClickListener {
            startActivity(Intent(this, MyFavorites::class.java))
        }
        binding.btnHomeRecent.setOnClickListener {
            startActivity(Intent(this, RecentSearchActivity::class.java))
        }
    }

    private fun showSelectedItem(gh: Package) {
        startActivity(ResultActivity.createIntent(this, gh.username))
    }

    private fun showListGh(items: List<Package>) {
        binding.recyclerView.apply {
            layoutManager = if (
                applicationContext.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            ) {
                GridLayoutManager(this@MainActivity, 2)
            } else {
                LinearLayoutManager(this@MainActivity)
            }
            setHasFixedSize(true)
            adapter = PackageAdapter(items, ::showSelectedItem)
        }
    }
}
