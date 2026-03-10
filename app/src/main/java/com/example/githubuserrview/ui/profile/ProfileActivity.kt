package com.example.githubuserrview.ui.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.githubuserrview.R
import com.example.githubuserrview.databinding.ActivityProfileBinding
import com.example.githubuserrview.navigation.BottomNavHelper
import com.example.githubuserrview.ui.history.RecentSearchActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.profile_title)

        binding.btnOpenRepository.setOnClickListener {
            openExternalUrl("https://github.com/asadara/GitHub-Heroes")
        }
        binding.btnOpenRecentSearches.setOnClickListener {
            startActivity(Intent(this, RecentSearchActivity::class.java))
        }

        BottomNavHelper.setup(this, binding.bottomNav.bottomNav, R.id.nav_profile)
    }

    private fun openExternalUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.open_link_error, Toast.LENGTH_SHORT).show()
        }
    }
}
