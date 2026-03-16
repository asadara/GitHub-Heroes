package com.example.githubuserrview.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SyncStatusFormatter {

    fun formatTimestamp(epochMs: Long): String {
        return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            .format(Date(epochMs))
    }
}
