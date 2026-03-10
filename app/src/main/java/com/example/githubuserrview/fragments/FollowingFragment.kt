package com.example.githubuserrview.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.R
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.databinding.FragmentFollowingBinding
import com.example.githubuserrview.model.FollowingViewModel
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.main.UserAdapter

class FollowingFragment : Fragment(R.layout.fragment_following) {

    private var _binding: FragmentFollowingBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FollowingViewModel
    private lateinit var adapter: UserAdapter
    private lateinit var username2: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments
        username2 = args?.getString(ResultActivity.EXTRA_USERNAME)
            ?: args?.getString(USER_NAME)
            ?: ""
        _binding = FragmentFollowingBinding.bind(view)

        adapter = UserAdapter()
        adapter.setOnItemClickCallback(object : UserAdapter.OnItemClickCallback {
            override fun onItemClicked(data: User) {
                openResult(data)
            }
        })
        binding.apply {
            recyclerViewFollowing.setHasFixedSize(true)
            recyclerViewFollowing.layoutManager = LinearLayoutManager(activity)
            recyclerViewFollowing.adapter = adapter
        }
        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())[FollowingViewModel::class.java]
        viewModel.setListFollowing(username2)
        viewModel.getListFollowing().observe(viewLifecycleOwner) {
            if (it != null){
                adapter.setList(it)
            }
        }
        viewModel.getLoadingState().observe(viewLifecycleOwner, ::showLoading)
        viewModel.getErrorMessage().observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank()) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun openResult(data: User) {
        startActivity(
            ResultActivity.createIntent(
                requireContext(),
                data.login,
                data.id,
                data.avatar_url
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
            FollowingFragment().apply {
                arguments = Bundle().apply {
                    putString(USER_NAME, username)
                }
            }
    }

}
