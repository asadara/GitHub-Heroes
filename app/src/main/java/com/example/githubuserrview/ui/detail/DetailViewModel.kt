package com.example.githubuserrview.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.githubuserrview.data.local.FavoriteUser
import com.example.githubuserrview.data.local.FavoriteUserDao
import com.example.githubuserrview.data.local.UserDatabase
import com.example.githubuserrview.data.repository.GithubRepositoryProvider
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.response.DetailUserResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao: FavoriteUserDao = UserDatabase.getDatabase(application).favoriteUserDao()
    private val repository = GithubRepositoryProvider.getInstance()

    private val detailUser = MutableLiveData<DetailUserResponse?>()
    private val isLoading = MutableLiveData(false)
    private val errorMessage = MutableLiveData<String?>()

    fun loadUserDetail(username: String) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null

            when (val result = repository.getUserDetail(username)) {
                is NetworkResult.Success -> detailUser.value = result.data
                is NetworkResult.Error -> {
                    detailUser.value = null
                    errorMessage.value = result.message
                }
            }

            isLoading.value = false
        }
    }

    fun getDetailUser(): LiveData<DetailUserResponse?> = detailUser

    fun getLoadingState(): LiveData<Boolean> = isLoading

    fun getErrorMessage(): LiveData<String?> = errorMessage

    fun addToFavorite(username: String, id: Int, avatarUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            userDao.addToFavorite(FavoriteUser(username, id, avatarUrl))
        }
    }

    suspend fun checkUser(id: Int): Int = userDao.checkUser(id)

    fun removeFromFavorite(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            userDao.removeFromFavorite(id)
        }
    }
}
