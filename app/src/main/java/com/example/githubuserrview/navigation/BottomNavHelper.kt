package com.example.githubuserrview.navigation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.githubuserrview.MainActivity
import com.example.githubuserrview.MyFavorites
import com.example.githubuserrview.R
import com.example.githubuserrview.settings.ActiveProfileStore
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.main.SearchActivity
import com.example.githubuserrview.ui.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavHelper {

    fun setup(
        activity: AppCompatActivity,
        bottomNav: BottomNavigationView,
        selectedItemId: Int
    ) {
        bottomNav.selectedItemId = selectedItemId
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(activity, MainActivity::class.java)
                R.id.nav_profile -> ResultActivity.createIntent(
                    activity,
                    ActiveProfileStore.get(activity)
                )
                R.id.nav_favorites -> Intent(activity, MyFavorites::class.java)
                R.id.nav_search -> SearchActivity.createIntent(activity, null)
                R.id.nav_settings -> Intent(activity, SettingsActivity::class.java)
                else -> null
            }

            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (intent != null) {
                AppNavigator.switchTab(activity, intent)
                true
            } else {
                false
            }
        }
    }
}
