package com.example.buylog

import android.content.Context
import androidx.core.content.edit

object NotificationCaptureSettings {
    private const val preferencesName = "notification_capture"
    private const val captureEnabledKey = "capture_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getBoolean(captureEnabledKey, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit { putBoolean(captureEnabledKey, enabled) }
    }
}
