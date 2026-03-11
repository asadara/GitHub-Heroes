package com.example.githubuserrview.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.githubuserrview.settings.ActiveProfileStore
import com.example.githubuserrview.settings.AppThemeManager
import com.example.githubuserrview.ui.detail.ResultActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        startActivity(
            ResultActivity.createIntent(
                this,
                ActiveProfileStore.get(this)
            )
        )
        finish()
    }
}
