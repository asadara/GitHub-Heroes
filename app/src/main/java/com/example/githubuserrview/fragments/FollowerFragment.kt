package com.example.githubuserrview.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.R
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.databinding.FragmentFollowerBinding
import com.example.githubuserrview.model.FollowerViewModel
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.main.UserAdapter

class FollowerFragment : Fragment(R.layout.fragment_follower) {

    private var _binding: FragmentFollowerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FollowerViewModel
    private lateinit var adapter: UserAdapter
    private lateinit var username2: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        username2 = args?.getString(ResultActivity.EXTRA_USERNAME)
            ?: args?.getString(USER_NAME)
            ?: ""
        _binding = FragmentFollowerBinding.bind(view)

        adapter = UserAdapter()
        adapter.setOnItemClickCallback(object : UserAdapter.OnItemClickCallback {
            override fun onItemClicked(data: User) {
                openResult(data)
            }
        })
        binding.apply {
            recyclerViewFollowers.setHasFixedSize(true)
            recyclerViewFollowers.layoutManager = LinearLayoutManager(activity)
            recyclerViewFollowers.adapter = adapter
        }
        viewModel = ViewModelProvider(this,ViewModelProvider.NewInstanceFactory())[FollowerViewModel::class.java]
        viewModel.setListFollowers(username2)
        viewModel.getListFollowers().observe(viewLifecycleOwner) {
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
        AppNavigator.open(
            requireActivity() as AppCompatActivity,
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
            FollowerFragment().apply {
                arguments = Bundle().apply {
                    putString(USER_NAME, username)
                }
            }
    }

}
