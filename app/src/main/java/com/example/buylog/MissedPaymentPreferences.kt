package com.example.buylog

import android.content.Context
import androidx.core.content.edit

object MissedPaymentPreferences {
    private const val PREFERENCES = "missed_payment_preferences"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ENABLED, enabled)
        }
        if (enabled) MissedPaymentScheduler.schedule(context) else MissedPaymentScheduler.cancel(context)
    }
}
