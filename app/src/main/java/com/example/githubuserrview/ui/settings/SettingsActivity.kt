package com.example.githubuserrview.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.githubuserrview.R
import com.example.githubuserrview.auth.GithubAuthConfig
import com.example.githubuserrview.auth.GithubAuthRepository
import com.example.githubuserrview.auth.GithubAuthSession
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.databinding.ActivitySettingsBinding
import com.example.githubuserrview.model.MainViewModel
import com.example.githubuserrview.model.ViewModelFactory
import com.example.githubuserrview.navigation.BottomNavHelper
import com.example.githubuserrview.settings.ActiveProfileStore
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
    private val githubAuthRepository by lazy { GithubAuthRepository(this) }
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
        bindGithubSyncCard()
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

    override fun onResume() {
        super.onResume()
        renderGithubSyncState()
    }

    private fun bindGithubSyncCard() {
        binding.btnGithubConnect.setOnClickListener {
            lifecycleScope.launch {
                when (val result = githubAuthRepository.beginAuthorization()) {
                    is NetworkResult.Success -> openGithubAuthorization(result.data)
                    is NetworkResult.Error -> {
                        renderGithubSyncState()
                        showToast(result.message)
                    }
                }
            }
        }

        binding.btnGithubRefresh.setOnClickListener {
            lifecycleScope.launch {
                when (val result = githubAuthRepository.refreshSession()) {
                    is NetworkResult.Success -> {
                        ActiveProfileStore.save(this@SettingsActivity, result.data.login)
                        renderGithubSyncState()
                        showToast(getString(R.string.settings_sync_refresh_success))
                    }
                    is NetworkResult.Error -> {
                        renderGithubSyncState()
                        showToast(result.message)
                    }
                }
            }
        }

        binding.btnGithubDisconnect.setOnClickListener {
            githubAuthRepository.signOut()
            ActiveProfileStore.save(this, ActiveProfileStore.DEFAULT_USERNAME)
            renderGithubSyncState()
            showToast(getString(R.string.settings_sync_signed_out))
        }

        renderGithubSyncState()
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

    private fun renderGithubSyncState() {
        val session = githubAuthRepository.getSession()

        when {
            !GithubAuthConfig.isConfigured -> {
                binding.tvGithubSyncStatus.text = getString(R.string.settings_sync_status_not_configured)
                binding.tvGithubSyncMeta.text = getString(R.string.settings_sync_meta_not_configured)
                binding.btnGithubConnect.text = getString(R.string.settings_sync_connect)
                binding.btnGithubConnect.isEnabled = false
                binding.btnGithubRefresh.isEnabled = false
                binding.btnGithubDisconnect.isEnabled = false
            }
            session != null -> {
                val grantedScope = GithubAuthConfig.normalizeScopeString(session.scope)
                    .ifBlank { "read:user" }
                val missingScopes = GithubAuthConfig.missingSocialScopes(session.scope)
                val isSocialScopeReady = missingScopes.isEmpty()

                binding.tvGithubSyncStatus.text = getString(
                    if (isSocialScopeReady) {
                        R.string.settings_sync_status_connected
                    } else {
                        R.string.settings_sync_status_reauth_required
                    }
                )
                binding.tvGithubSyncMeta.text = if (isSocialScopeReady) {
                    getString(
                        R.string.settings_sync_connected_meta,
                        buildSessionSummary(session),
                        grantedScope
                    )
                } else {
                    getString(
                        R.string.settings_sync_upgrade_meta,
                        buildSessionSummary(session),
                        grantedScope,
                        missingScopes.joinToString(" ")
                    )
                }
                binding.btnGithubConnect.text = getString(R.string.settings_sync_reconnect)
                binding.btnGithubConnect.isEnabled = true
                binding.btnGithubRefresh.isEnabled = true
                binding.btnGithubDisconnect.isEnabled = true
            }
            else -> {
                binding.tvGithubSyncStatus.text = getString(R.string.settings_sync_status_disconnected)
                binding.tvGithubSyncMeta.text = getString(R.string.settings_sync_meta_disconnected)
                binding.btnGithubConnect.text = getString(R.string.settings_sync_connect)
                binding.btnGithubConnect.isEnabled = true
                binding.btnGithubRefresh.isEnabled = false
                binding.btnGithubDisconnect.isEnabled = false
            }
        }
    }

    private fun buildSessionSummary(session: GithubAuthSession): String {
        val summaryParts = mutableListOf<String>()
        val displayName = session.name?.takeIf {
            it.isNotBlank() && !it.equals(session.login, ignoreCase = true)
        }

        if (!displayName.isNullOrBlank()) {
            summaryParts.add(displayName)
        }

        summaryParts.add("@${session.login}")
        session.email?.takeIf { it.isNotBlank() }?.let(summaryParts::add)

        return summaryParts.joinToString("\n")
    }

    private fun openGithubAuthorization(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            showToast(getString(R.string.settings_sync_missing_browser))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
