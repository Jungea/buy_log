package com.example.buylog

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import com.example.buylog.domain.NotificationPaymentMethodResolver
import com.example.buylog.domain.NotificationPaymentRule

object NotificationRecognitionSettings {
    private const val preferencesName = "notification_recognition"
    private const val selectedPackagesKey = "selected_packages"
    private const val defaultMethodPrefix = "default_method_"
    private const val rulesPrefix = "rules_"

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

    fun getDefaultPaymentMethodId(context: Context, packageName: String): Long? =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getLong(defaultMethodPrefix + packageName, -1L)
            .takeIf { it > 0 }

    fun setDefaultPaymentMethodId(context: Context, packageName: String, methodId: Long?) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit {
            if (methodId == null) remove(defaultMethodPrefix + packageName)
            else putLong(defaultMethodPrefix + packageName, methodId)
        }
    }

    fun getRules(context: Context, packageName: String): List<NotificationPaymentRule> =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getStringSet(rulesPrefix + packageName, emptySet())
            .orEmpty()
            .mapNotNull { encoded ->
                val separator = encoded.lastIndexOf(':')
                if (separator <= 0) return@mapNotNull null
                val phrase = runCatching {
                    String(Base64.decode(encoded.substring(0, separator), Base64.NO_WRAP))
                }.getOrNull()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val methodId = encoded.substring(separator + 1).toLongOrNull()
                    ?.takeIf { it > 0 } ?: return@mapNotNull null
                NotificationPaymentRule(phrase, methodId)
            }
            .sortedByDescending { it.phrase.length }

    fun setRules(context: Context, packageName: String, rules: List<NotificationPaymentRule>) {
        val encoded = rules.map { rule ->
            val phrase = Base64.encodeToString(rule.phrase.toByteArray(), Base64.NO_WRAP)
            "$phrase:${rule.paymentMethodId}"
        }.toSet()
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit { putStringSet(rulesPrefix + packageName, encoded) }
    }

    fun resolvePaymentMethodId(context: Context, packageName: String, rawText: String): Long? =
        NotificationPaymentMethodResolver.resolve(
            rawText = rawText,
            rules = getRules(context, packageName),
            defaultPaymentMethodId = getDefaultPaymentMethodId(context, packageName),
        )
}
