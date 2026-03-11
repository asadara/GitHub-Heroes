package com.example.githubuserrview.settings

import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.example.githubuserrview.R

enum class AppThemePalette(
    val storageValue: String,
    @StyleRes val themeRes: Int,
    @StringRes val labelRes: Int
) {
    FOREST("forest", R.style.Theme_GitHubUserRview_Forest, R.string.theme_palette_forest),
    OCEAN("ocean", R.style.Theme_GitHubUserRview_Ocean, R.string.theme_palette_ocean),
    TERRACOTTA(
        "terracotta",
        R.style.Theme_GitHubUserRview_Terracotta,
        R.string.theme_palette_terracotta
    ),
    SLATE("slate", R.style.Theme_GitHubUserRview_Slate, R.string.theme_palette_slate);

    companion object {
        fun fromStorage(value: String?): AppThemePalette {
            return values().firstOrNull { it.storageValue == value } ?: FOREST
        }
    }
}
