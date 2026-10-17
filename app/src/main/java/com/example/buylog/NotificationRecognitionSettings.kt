package com.example.buylog

import android.content.Context
import androidx.core.content.edit

object NotificationRecognitionSettings {
    private const val preferencesName = "notification_recognition"
    private const val selectedPackagesKey = "selected_packages"

    fun getSelectedPackages(context: Context): Set<String> =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getStringSet(selectedPackagesKey, emptySet())
            ?.toSet()
            .orEmpty()

    fun isSelected(context: Context, packageName: String): Boolean =
        packageName in getSelectedPackages(context)

    fun setSelected(context: Context, packageName: String, selected: Boolean) {
        val packages = getSelectedPackages(context).toMutableSet()
        if (selected) packages += packageName else packages -= packageName
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit { putStringSet(selectedPackagesKey, packages) }
    }
}
