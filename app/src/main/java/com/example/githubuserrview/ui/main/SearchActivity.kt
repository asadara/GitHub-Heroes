package com.example.githubuserrview.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.R
import com.example.githubuserrview.api.ApiConfig
import com.example.githubuserrview.auth.GithubAuthRepository
import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.data.repository.GithubRepository
import com.example.githubuserrview.databinding.ActivitySearchBinding
import com.example.githubuserrview.navigation.BottomNavHelper
import com.example.githubuserrview.settings.AppThemeManager
import com.example.githubuserrview.settings.RecentSearchPreferences
import com.example.githubuserrview.settings.appDataStore
import com.example.githubuserrview.ui.common.AppHeader
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.history.RecentSearchActivity
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.profile.ProfileRepoAdapter
import com.example.githubuserrview.ui.profile.RepositoryDetailActivity

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var viewModel: SearchViewModel
    private lateinit var userAdapter: UserAdapter
    private lateinit var repositoryAdapter: ProfileRepoAdapter

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppHeader.apply(
            this,
            R.string.bar_title_search,
            R.string.header_search_subtitle
        )

        userAdapter = UserAdapter()
        userAdapter.setOnItemClickCallback(object : UserAdapter.OnItemClickCallback{
            override fun onItemClicked(data: User) {
                openResult(data)
            }
        })
        repositoryAdapter = ProfileRepoAdapter(::openRepository)
        val githubRepository = GithubRepository(
            ApiConfig.getApiService(GithubAuthRepository(this).getSession()?.accessToken)
        )
        viewModel = ViewModelProvider(
            this,
            SearchViewModelFactory(
                githubRepository,
                RecentSearchPreferences.getInstance(appDataStore)
            )
        )[SearchViewModel::class.java]

        binding.apply {
            recyclerViewRetro.layoutManager = LinearLayoutManager(this@SearchActivity)
            recyclerViewRetro.setHasFixedSize(true)
            recyclerViewRetro.adapter = userAdapter
            lottieNotFound.visibility = View.GONE
            layoutSearchPagination.visibility = View.GONE
            btnRecentSearches.setOnClickListener {
                AppNavigator.open(
                    this@SearchActivity,
                    Intent(this@SearchActivity, RecentSearchActivity::class.java)
                )
            }
            btnSearch.setOnClickListener {
                submitSearch()
            }
            searchModeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                val mode = when (checkedId) {
                    R.id.btn_mode_repositories -> SearchMode.REPOSITORIES
                    else -> SearchMode.USERS
                }
                viewModel.setSearchMode(mode)
            }
            btnPrevPage.setOnClickListener {
                viewModel.loadPreviousPage()
            }
            btnNextPage.setOnClickListener {
                viewModel.loadNextPage()
            }
            etSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    submitSearch()
                    true
                } else {
                    false
                }
            }
        }

        viewModel.getSearchUser().observe(this) {
            val users = it ?: arrayListOf()
            if (viewModel.getSearchMode().value == SearchMode.USERS) {
                userAdapter.setList(users)
                showEmptyState(users.isEmpty())
            }
        }

        viewModel.getSearchRepositories().observe(this) {
            if (viewModel.getSearchMode().value == SearchMode.REPOSITORIES) {
                repositoryAdapter.submitList(it ?: emptyList())
                showEmptyState(it.isNullOrEmpty())
            }
        }

        viewModel.getPaginationState().observe(this, ::renderPagination)

        viewModel.getSearchMode().observe(this, ::renderSearchMode)

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

        BottomNavHelper.setup(this, binding.bottomNav.bottomNav, R.id.nav_search)
        searchUser()
    }

    private fun searchUser() {
        val query = intent.getStringExtra(EXTRA_USER)?.trim()

        binding.etSearch.setText(query.orEmpty())

        if (query.isNullOrBlank()) {
            updateSearchTitle("")
            binding.tvSearchResultMeta.text = getString(R.string.search_result_idle_meta)
            showLoading(false)
            showEmptyState(false)
            renderPagination(SearchPaginationState())
            userAdapter.setList(arrayListOf())
            repositoryAdapter.submitList(emptyList())
            return
        }

        runSearch(query)
    }

    private fun submitSearch() {
        val query = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            updateSearchTitle("")
            binding.tvSearchResultMeta.text = getString(R.string.search_result_idle_meta)
            showLoading(false)
            showEmptyState(false)
            Toast.makeText(
                this,
                getString(R.string.search_error_empty_query),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        hideKeyboard()
        runSearch(query)
    }

    private fun runSearch(query: String) {
        updateSearchTitle(query)
        binding.tvSearchResultMeta.text = getString(R.string.search_result_loading_meta)
        showEmptyState(false)
        viewModel.setSearchQuery(query)
    }

    private fun openResult(data: User) {
        AppNavigator.open(
            this,
            ResultActivity.createIntent(
                this,
                data.login,
                data.id,
                data.avatar_url
            )
        )
    }

    private fun openRepository(repository: GithubRepo) {
        AppNavigator.open(
            this,
            RepositoryDetailActivity.createIntent(
                this,
                repository.owner.login,
                repository.name,
                repository.fullName
            )
        )
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.cardSearchLoading.visibility = View.VISIBLE
            binding.cardSearchLoading.bringToFront()
            binding.recyclerViewRetro.visibility = View.INVISIBLE
            binding.layoutSearchPagination.visibility = View.INVISIBLE
            return
        }

        binding.cardSearchLoading.visibility = View.GONE
        binding.recyclerViewRetro.visibility =
            if (binding.lottieNotFound.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        renderPagination(viewModel.getPaginationState().value ?: SearchPaginationState())
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.lottieNotFound.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewRetro.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun renderSearchMode(mode: SearchMode) {
        binding.recyclerViewRetro.adapter =
            if (mode == SearchMode.USERS) userAdapter else repositoryAdapter
        binding.searchInputLayout.hint = getString(
            if (mode == SearchMode.USERS) {
                R.string.search_input_hint_users
            } else {
                R.string.search_input_hint_repositories
            }
        )
        updateSearchTitle(binding.etSearch.text?.toString()?.trim().orEmpty())
        val queryIsBlank = binding.etSearch.text?.toString()?.trim().isNullOrEmpty()
        val hasItems = if (mode == SearchMode.USERS) {
            userAdapter.itemCount > 0
        } else {
            repositoryAdapter.itemCount > 0
        }
        showEmptyState(!queryIsBlank && !binding.cardSearchLoading.isShown && !hasItems)
        binding.btnModeUsers.alpha = if (mode == SearchMode.USERS) 1f else 0.72f
        binding.btnModeRepositories.alpha = if (mode == SearchMode.REPOSITORIES) 1f else 0.72f
    }

    private fun updateSearchTitle(query: String) {
        val mode = viewModel.getSearchMode().value ?: SearchMode.USERS
        binding.searchFor.text = if (query.isBlank()) {
            getString(
                if (mode == SearchMode.USERS) {
                    R.string.search_idle_title_users
                } else {
                    R.string.search_idle_title_repositories
                }
            )
        } else {
            getString(
                if (mode == SearchMode.USERS) {
                    R.string.search_result_users_for
                } else {
                    R.string.search_result_repositories_for
                },
                query
            )
        }
    }

    private fun renderPagination(state: SearchPaginationState) {
        val startIndex = if (state.totalCount == 0 || state.shownCount == 0) {
            0
        } else {
            ((state.currentPage - 1) * state.pageSize) + 1
        }
        val endIndex = if (state.totalCount == 0 || state.shownCount == 0) {
            0
        } else {
            startIndex + state.shownCount - 1
        }

        binding.tvSearchResultMeta.text = if (state.totalCount == 0) {
            getString(R.string.search_result_empty_meta)
        } else {
            getString(R.string.search_result_meta_format, state.totalCount, startIndex, endIndex)
        }

        binding.layoutSearchPagination.visibility =
            if (state.totalCount > state.pageSize) View.VISIBLE else View.GONE
        binding.btnPrevPage.isEnabled = state.hasPreviousPage
        binding.btnNextPage.isEnabled = state.hasNextPage
        binding.btnPrevPage.alpha = if (state.hasPreviousPage) 1f else 0.55f
        binding.btnNextPage.alpha = if (state.hasNextPage) 1f else 0.55f
        binding.tvPageIndicator.text = getString(
            R.string.search_page_indicator,
            state.currentPage
        )
    }

    private fun hideKeyboard() {
        binding.etSearch.clearFocus()
        binding.etSearch.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
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
