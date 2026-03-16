package com.example.githubuserrview.ui.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.example.githubuserrview.R
import com.example.githubuserrview.auth.GithubAuthRepository

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
            setDisplayShowCustomEnabled(true)
        }

        val iconColor = TypedValue().also { typedValue ->
            activity.theme.resolveAttribute(R.attr.appTextPrimary, typedValue, true)
        }.data

        val githubIcon = AppCompatResources.getDrawable(
            activity,
            R.drawable.ic_icongithub
        )?.mutate()?.also { drawable ->
            DrawableCompat.setTint(drawable, iconColor)
        }
        val iconSize = (activity.resources.displayMetrics.density * 42).toInt()

        val iconView = ImageView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                iconSize,
                iconSize
            )
            setImageDrawable(githubIcon)
            contentDescription = activity.getString(R.string.header_open_github)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(0, 0, 0, 0)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                openGithub(activity)
            }
        }

        activity.supportActionBar?.customView = iconView
        activity.supportActionBar?.setCustomView(
            iconView,
            ActionBar.LayoutParams(
                iconSize,
                iconSize,
                Gravity.END or Gravity.CENTER_VERTICAL
            )
        )
    }

    private const val GITHUB_HOME_URL = "https://github.com"

    private fun openGithub(activity: AppCompatActivity) {
        val githubUrl = GithubAuthRepository(activity)
            .getSession()
            ?.htmlUrl
            ?: GITHUB_HOME_URL

        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                activity,
                R.string.open_link_error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
