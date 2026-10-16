package com.example.buylog

import android.content.Context
import androidx.core.content.edit

object SpendingPreferences {
    private const val preferencesName = "spending_preferences"
    private const val paydayKey = "payday"

    fun getPayday(context: Context): Int = context
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        .getInt(paydayKey, 25)

    fun setPayday(context: Context, payday: Int) {
        require(payday in 1..31)
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit { putInt(paydayKey, payday) }
    }
}
