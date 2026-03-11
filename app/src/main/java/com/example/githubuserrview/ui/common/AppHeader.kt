package com.example.githubuserrview.ui.common

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity

object AppHeader {

    fun apply(
        activity: AppCompatActivity,
        @StringRes titleRes: Int,
        @StringRes subtitleRes: Int? = null,
        showBack: Boolean = false
    ) {
        activity.supportActionBar?.apply {
            title = activity.getString(titleRes)
            subtitle = subtitleRes?.let(activity::getString)
            setDisplayHomeAsUpEnabled(showBack)
            setHomeButtonEnabled(showBack)
            setDisplayShowHomeEnabled(showBack)
            elevation = 0f
        }
    }
}
