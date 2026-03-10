package com.example.githubuserrview

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.adapter.PackageAdapter
import com.example.githubuserrview.api.Package
import com.example.githubuserrview.databinding.ActivityMainBinding
import com.example.githubuserrview.model.MainViewModel
import com.example.githubuserrview.model.ViewModelFactory
import com.example.githubuserrview.settings.SettingPreferences
import com.example.githubuserrview.settings.appDataStore
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.history.RecentSearchActivity
import com.example.githubuserrview.ui.main.SearchActivity
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel
    private var themeSwitch: SwitchCompat? = null
    private var isUpdatingThemeSwitch = false
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

            isUpdatingThemeSwitch = true
            themeSwitch?.isChecked = isDarkModeActive
            isUpdatingThemeSwitch = false
        }

        setupHomeContent(profiles)
        setupQuickActions()
        showListGh(profiles)
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
        val spotlight = items.maxByOrNull { it.followers } ?: return
        val totalRepositories = items.sumOf { it.repository }
        val totalFollowers = items.sumOf { it.followers }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search -> true
            R.id.favorites -> {
                startActivity(Intent(this, MyFavorites::class.java))
                true
            }
            R.id.recent_searches -> {
                startActivity(Intent(this, RecentSearchActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.search).actionView as SearchView
        val switchTheme = menu.findItem(R.id.switch_theme).actionView as SwitchCompat
        themeSwitch = switchTheme

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.queryHint = resources.getString(R.string.search_hint)

        isUpdatingThemeSwitch = true
        switchTheme.isChecked = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> switchTheme.isChecked
        }
        isUpdatingThemeSwitch = false

        switchTheme.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (!isUpdatingThemeSwitch) {
                mainViewModel.saveThemeSetting(isChecked)
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                startActivity(SearchActivity.createIntent(this@MainActivity, query))
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })
        return true
    }
}
