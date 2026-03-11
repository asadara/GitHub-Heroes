package com.example.githubuserrview.ui.common

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.githubuserrview.R

object AppNavigator {

    fun open(activity: AppCompatActivity, intent: Intent) {
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.screen_enter, R.anim.screen_exit)
    }

    fun switchTab(activity: AppCompatActivity, intent: Intent) {
        activity.startActivity(intent)
        activity.overridePendingTransition(R.anim.tab_enter, R.anim.tab_exit)
    }

    fun finish(activity: AppCompatActivity) {
        activity.finish()
        activity.overridePendingTransition(R.anim.screen_pop_enter, R.anim.screen_pop_exit)
    }
}
