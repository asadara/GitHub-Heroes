package com.example.githubuserrview.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.githubuserrview.R
import com.example.githubuserrview.data.repository.NetworkResult
import com.example.githubuserrview.settings.ActiveProfileStore
import com.example.githubuserrview.settings.AppThemeManager
import com.example.githubuserrview.ui.detail.ResultActivity
import com.example.githubuserrview.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class GithubAuthCallbackActivity : AppCompatActivity() {

    private val authRepository by lazy { GithubAuthRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.apply(this)
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            when (val result = authRepository.completeAuthorization(intent?.data)) {
                is NetworkResult.Success -> {
                    ActiveProfileStore.save(this@GithubAuthCallbackActivity, result.data.login)
                    Toast.makeText(
                        this@GithubAuthCallbackActivity,
                        getString(R.string.settings_sync_success, result.data.login),
                        Toast.LENGTH_SHORT
                    ).show()
                    openResult(result.data.login)
                }
                is NetworkResult.Error -> {
                    Toast.makeText(
                        this@GithubAuthCallbackActivity,
                        result.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    openSettings()
                }
            }
            finish()
        }
    }

    private fun openResult(username: String) {
        startActivity(
            ResultActivity.createIntent(this, username).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    private fun openSettings() {
        startActivity(
            Intent(this, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }
}
