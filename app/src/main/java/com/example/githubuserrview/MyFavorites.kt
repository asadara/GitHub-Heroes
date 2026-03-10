package com.example.githubuserrview

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.githubuserrview.data.local.FavoriteUser
import com.example.githubuserrview.data.model.User
import com.example.githubuserrview.databinding.ActivityMyFavoritesBinding
import com.example.githubuserrview.model.FavoriteViewModel
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.main.UserAdapter

class MyFavorites : AppCompatActivity() {

    private lateinit var binding: ActivityMyFavoritesBinding
    private lateinit var adapter: UserAdapter
    private lateinit var viewModel: FavoriteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = getString(R.string.bar_title_fav)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = UserAdapter()
        viewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]
        adapter.setOnItemClickCallback(object : UserAdapter.OnItemClickCallback{
            override fun onItemClicked(data: User) {
                openResult(data)
            }
        })
        binding.apply {
            rvFavorite.setHasFixedSize(true)
            rvFavorite.layoutManager =
                if (applicationContext.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    GridLayoutManager(this@MyFavorites, 2)
                } else {
                    LinearLayoutManager(this@MyFavorites)
                }
            rvFavorite.adapter = adapter
        }

        viewModel.getFavoriteUser().observe(this) {
            adapter.setList(mapList(it))
        }

    }

    private fun mapList(users: List<FavoriteUser>): ArrayList<User> {
        val listUser = ArrayList<User>()
        for (user in users){
            val userMapped = User(
                user.login,
                user.id,
                user.avatar_url
            )
            listUser.addAll(listOf(userMapped))
        }
        return listUser
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
