package com.example.githubuserrview.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.R
import com.example.githubuserrview.api.ApiConfig
import com.example.githubuserrview.auth.GithubAuthRepository
import com.example.githubuserrview.data.model.GithubRepo
import com.example.githubuserrview.data.repository.GithubRepository
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.databinding.FragmentStarredRepositoriesBinding
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.profile.ProfileRepoAdapter
import com.example.githubuserrview.ui.profile.RepositoryDetailActivity
import kotlinx.coroutines.launch

class StarredRepositoriesFragment : Fragment(R.layout.fragment_starred_repositories) {

    private var _binding: FragmentStarredRepositoriesBinding? = null
    private val binding get() = _binding!!
    private val githubRepository by lazy {
        GithubRepository(
            ApiConfig.getApiService(GithubAuthRepository(requireContext()).getSession()?.accessToken)
        )
    }
    private val adapter by lazy { ProfileRepoAdapter(::openRepository) }
    private lateinit var username: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStarredRepositoriesBinding.bind(view)
        username = arguments?.getString(ResultActivity.EXTRA_USERNAME)
            ?: arguments?.getString(USER_NAME)
            ?: ""

        binding.recyclerViewStarred.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StarredRepositoriesFragment.adapter
        }

        loadStarredRepositories()
    }

    private fun loadStarredRepositories() {
        if (username.isBlank()) {
            renderRepositories(emptyList())
            return
        }

        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = githubRepository.getPublicStarredRepositories(username)) {
                is NetworkResult.Success -> renderRepositories(result.data)
                is NetworkResult.Error -> {
                    renderRepositories(emptyList())
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
            showLoading(false)
        }
    }

    private fun renderRepositories(repositories: List<GithubRepo>) {
        adapter.submitList(repositories)
        binding.tvStarredEmpty.visibility = if (repositories.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewStarred.visibility = if (repositories.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun openRepository(repository: GithubRepo) {
        AppNavigator.open(
            requireActivity() as AppCompatActivity,
            RepositoryDetailActivity.createIntent(
                context = requireContext(),
                owner = repository.owner.login,
                repositoryName = repository.name,
                fullName = repository.fullName
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val USER_NAME = "user_name"

        @JvmStatic
        fun userName(username: String) =
            StarredRepositoriesFragment().apply {
                arguments = Bundle().apply {
                    putString(USER_NAME, username)
                }
            }
    }
}
