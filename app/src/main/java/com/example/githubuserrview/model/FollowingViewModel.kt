package com.example.githubuserrview.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.data.repository.GithubRepositoryProvider
import com.example.githubuserrview.data.repository.NetworkResult
import kotlinx.coroutines.launch

class FollowingViewModel : ViewModel() {
    private val repository = GithubRepositoryProvider.getInstance()

    private val listFollowing = MutableLiveData<ArrayList<User>>()
    private val isLoading = MutableLiveData(false)
    private val errorMessage = MutableLiveData<String?>()

    fun setListFollowing(username: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            when (val result = repository.getFollowing(username)) {
                is NetworkResult.Success -> {
                    listFollowing.value = ArrayList(result.data)
                }
                is NetworkResult.Error -> {
                    listFollowing.value = arrayListOf()
                    errorMessage.value = result.message
                }
            }

            isLoading.value = false
        }
    }

    fun getListFollowing(): LiveData<ArrayList<User>> = listFollowing

    fun getLoadingState(): LiveData<Boolean> = isLoading

    fun getErrorMessage(): LiveData<String?> = errorMessage
}
