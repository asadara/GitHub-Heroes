package com.example.githubuserrview.settings

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

object AppThemeManager {

    private const val PREF_NAME = "app_theme_palette"
    private const val KEY_PALETTE = "selected_palette"

    fun apply(activity: AppCompatActivity) {
        activity.setTheme(resolveThemeRes(activity))
    }

    fun resolveThemeRes(context: Context): Int {
        return getPalette(context).themeRes
    }

    fun getPalette(context: Context): AppThemePalette {
        val value = context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PALETTE, AppThemePalette.FOREST.storageValue)
        return AppThemePalette.fromStorage(value)
    }

    fun savePalette(context: Context, palette: AppThemePalette) {
        context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PALETTE, palette.storageValue)
            .apply()
    }
}
