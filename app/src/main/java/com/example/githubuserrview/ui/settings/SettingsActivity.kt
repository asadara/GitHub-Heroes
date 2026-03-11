package com.example.githubuserrview.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.githubuserrview.R
import com.example.githubuserrview.databinding.ActivitySettingsBinding
import com.example.githubuserrview.model.MainViewModel
import com.example.githubuserrview.model.ViewModelFactory
import com.example.githubuserrview.navigation.BottomNavHelper
import com.example.githubuserrview.settings.RecentSearchPreferences
import com.example.githubuserrview.settings.SettingPreferences
import com.example.githubuserrview.settings.AppThemeManager
import com.example.githubuserrview.settings.AppThemePalette
import com.example.githubuserrview.settings.appDataStore
import com.example.githubuserrview.ui.common.AppHeader
import com.example.githubuserrview.ui.common.AppNavigator
import com.example.githubuserrview.ui.history.RecentSearchActivity
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var recentSearchPreferences: RecentSearchPreferences
    private var isUpdatingSwitch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppHeader.apply(
            this,
            R.string.settings_title,
            R.string.header_settings_subtitle
        )

        mainViewModel = ViewModelProvider(
            this,
            ViewModelFactory(SettingPreferences.getInstance(appDataStore))
        )[MainViewModel::class.java]
        recentSearchPreferences = RecentSearchPreferences.getInstance(appDataStore)
        bindPaletteSelection()

        mainViewModel.getThemeSettings().observe(this) { isDarkModeActive ->
            AppCompatDelegate.setDefaultNightMode(
                if (isDarkModeActive) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            isUpdatingSwitch = true
            binding.switchTheme.isChecked = isDarkModeActive
            binding.tvThemeStatus.text = getString(
                if (isDarkModeActive) R.string.settings_theme_status_on
                else R.string.settings_theme_status_off
            )
            isUpdatingSwitch = false
        }

        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingSwitch) {
                mainViewModel.saveThemeSetting(isChecked)
            }
        }
        binding.btnOpenHistory.setOnClickListener {
            AppNavigator.open(this, android.content.Intent(this, RecentSearchActivity::class.java))
        }
        binding.btnClearHistory.setOnClickListener {
            lifecycleScope.launch {
                recentSearchPreferences.clear()
                android.widget.Toast.makeText(
                    this@SettingsActivity,
                    R.string.settings_history_cleared,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        BottomNavHelper.setup(this, binding.bottomNav.bottomNav, R.id.nav_settings)
    }

    private fun bindPaletteSelection() {
        val currentPalette = AppThemeManager.getPalette(this)
        binding.radioGroupPalette.check(
            when (currentPalette) {
                AppThemePalette.FOREST -> binding.radioPaletteForest.id
                AppThemePalette.OCEAN -> binding.radioPaletteOcean.id
                AppThemePalette.TERRACOTTA -> binding.radioPaletteTerracotta.id
                AppThemePalette.SLATE -> binding.radioPaletteSlate.id
            }
        )

        binding.radioGroupPalette.setOnCheckedChangeListener { _, checkedId ->
            val selectedPalette = when (checkedId) {
                binding.radioPaletteOcean.id -> AppThemePalette.OCEAN
                binding.radioPaletteTerracotta.id -> AppThemePalette.TERRACOTTA
                binding.radioPaletteSlate.id -> AppThemePalette.SLATE
                else -> AppThemePalette.FOREST
            }

            if (selectedPalette != AppThemeManager.getPalette(this)) {
                AppThemeManager.savePalette(this, selectedPalette)
                recreate()
            }
        }
    }
}
