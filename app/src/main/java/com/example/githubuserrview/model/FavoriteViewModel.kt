package com.example.githubuserrview.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.githubuserrview.data.local.FavoriteUser
import com.example.githubuserrview.data.local.FavoriteUserDao
import com.example.githubuserrview.data.local.UserDatabase

class FavoriteViewModel(application: Application): AndroidViewModel(application) {

    private val userDao: FavoriteUserDao
    private val userDb: UserDatabase = UserDatabase.getDatabase(application)

    init {
        userDao = userDb.favoriteUserDao()
    }

    fun getFavoriteUser(): LiveData<List<FavoriteUser>> {
        return userDao.getFavoriteUser()
    }
}
