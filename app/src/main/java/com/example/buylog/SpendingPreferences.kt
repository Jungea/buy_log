package com.example.buylog

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.time.YearMonth

object SpendingPreferences {
    private const val preferencesName = "spending_preferences"
    private const val paydayKey = "payday"
    private const val moveWeekendPaydayKey = "move_weekend_payday_to_previous_weekday"
    private const val paydayOverridesKey = "payday_overrides"

    fun getPayday(context: Context): Int = context
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        .getInt(paydayKey, 25)

    fun setPayday(context: Context, payday: Int) {
        require(payday in 1..31)
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit { putInt(paydayKey, payday) }
    }

    fun getMoveWeekendPayday(context: Context): Boolean = context
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        .getBoolean(moveWeekendPaydayKey, true)

    fun setMoveWeekendPayday(context: Context, enabled: Boolean) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit { putBoolean(moveWeekendPaydayKey, enabled) }
    }

    fun getPaydayOverrides(context: Context): Map<YearMonth, LocalDate> = context
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        .getStringSet(paydayOverridesKey, emptySet())
        .orEmpty()
        .mapNotNull { stored ->
            val parts = stored.split('|', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            runCatching { YearMonth.parse(parts[0]) to LocalDate.parse(parts[1]) }.getOrNull()
        }
        .toMap()

    fun setPaydayOverride(context: Context, month: YearMonth, date: LocalDate) {
        require(YearMonth.from(date) == month) { "지정일은 선택한 달 안에 있어야 합니다." }
        val updated = getPaydayOverrides(context).toMutableMap().apply { put(month, date) }
        savePaydayOverrides(context, updated)
    }

    fun removePaydayOverride(context: Context, month: YearMonth) {
        val updated = getPaydayOverrides(context).toMutableMap().apply { remove(month) }
        savePaydayOverrides(context, updated)
    }

    private fun savePaydayOverrides(context: Context, overrides: Map<YearMonth, LocalDate>) {
        val stored = overrides.mapTo(mutableSetOf()) { (month, date) -> "$month|$date" }
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit { putStringSet(paydayOverridesKey, stored) }
    }
}
