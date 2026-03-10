package com.example.githubuserrview.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.MyFavorites
import com.example.githubuserrview.R
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.data.repository.GithubRepositoryProvider
import com.example.githubuserrview.databinding.ActivitySearchBinding
import com.example.githubuserrview.settings.RecentSearchPreferences
import com.example.githubuserrview.settings.appDataStore
import com.example.githubuserrview.ui.history.RecentSearchActivity
import com.example.githubuserrview.ui.detail.ResultActivity

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: UserAdapter

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.bar_title_search)

        adapter = UserAdapter()
        adapter.setOnItemClickCallback(object : UserAdapter.OnItemClickCallback{
            override fun onItemClicked(data: User) {
                openResult(data)
            }
        })
        viewModel = ViewModelProvider(
            this,
            SearchViewModelFactory(
                GithubRepositoryProvider.getInstance(),
                RecentSearchPreferences.getInstance(appDataStore)
            )
        )[SearchViewModel::class.java]

        binding.apply {
            recyclerViewRetro.layoutManager = LinearLayoutManager(this@SearchActivity)
            recyclerViewRetro.setHasFixedSize(true)
            recyclerViewRetro.adapter = adapter
            lottieNotFound.visibility = View.GONE
        }

        viewModel.getSearchUser().observe(this) {
            val users = it ?: arrayListOf()
            adapter.setList(users)
            showEmptyState(users.isEmpty())
        }

        viewModel.getLoadingState().observe(this) {
            showLoading(it)
        }

        viewModel.getErrorMessage().observe(this) {
            if (!it.isNullOrBlank()) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                showEmptyState(true)
            }
        }

        //saya rasa orientasi terbaik untuk design apk ini adalah orientasi portrait, so i made this one
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        searchUser()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        menu.findItem(R.id.search).isVisible = false
        menu.findItem(R.id.switch_theme).isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
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

    private fun searchUser() {
        binding.apply {
            val query = intent.getStringExtra(EXTRA_USER)?.trim()

            if (query.isNullOrBlank()) {
                searchFor.text = getString(R.string.search_result_for)
                showLoading(false)
                showEmptyState(true)
                Toast.makeText(
                    this@SearchActivity,
                    getString(R.string.search_error_empty_query),
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            searchFor.text = getString(R.string.tv_search_for, query)
            showEmptyState(false)
            viewModel.setSearchUsers(query)
        }
    }

    private fun openResult(data: User) {
        startActivity(
            ResultActivity.createIntent(
                this,
                data.login,
                data.id,
                data.avatar_url
            )
        )
    }

    private fun showLoading(isLoading: Boolean) {
        binding.lottie.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.lottieNotFound.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewRetro.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    companion object {
        const val EXTRA_USER = "extra_user"

        fun createIntent(context: Context, query: String?): Intent {
            return Intent(context, SearchActivity::class.java).apply {
                putExtra(EXTRA_USER, query)
            }
        }
    }
}
