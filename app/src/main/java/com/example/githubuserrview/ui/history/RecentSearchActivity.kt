package com.example.githubuserrview.ui.history

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.R
import com.example.githubuserrview.databinding.ActivityRecentSearchBinding
import com.example.githubuserrview.settings.RecentSearchPreferences
import com.example.githubuserrview.settings.AppThemeManager
import com.example.githubuserrview.settings.appDataStore
import com.example.githubuserrview.ui.common.AppHeader
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.main.SearchActivity

class RecentSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecentSearchBinding
    private lateinit var adapter: RecentSearchAdapter
    private lateinit var viewModel: RecentSearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivityRecentSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppHeader.apply(
            this,
            R.string.recent_searches,
            R.string.header_recent_subtitle,
            showBack = true
        )

        adapter = RecentSearchAdapter { query ->
            AppNavigator.open(this, SearchActivity.createIntent(this, query))
        }

        binding.recyclerViewRecent.apply {
            layoutManager = LinearLayoutManager(this@RecentSearchActivity)
            setHasFixedSize(true)
            adapter = this@RecentSearchActivity.adapter
        }

        viewModel = ViewModelProvider(
            this,
            RecentSearchViewModelFactory(
                RecentSearchPreferences.getInstance(appDataStore)
            )
        )[RecentSearchViewModel::class.java]

        viewModel.recentSearches.observe(this) { searches ->
            adapter.submitList(searches)
            binding.tvEmptyRecent.visibility = if (searches.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnClearRecent.setOnClickListener {
            viewModel.clearHistory()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        AppNavigator.finish(this)
        return true
    }
}
