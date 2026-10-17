package com.example.buylog.domain

data class NotificationPaymentRule(
    val phrase: String,
    val paymentMethodId: Long,
)

object NotificationPaymentMethodResolver {
    fun resolve(
        rawText: String,
        rules: List<NotificationPaymentRule>,
        defaultPaymentMethodId: Long?,
    ): Long? = rules
        .filter { it.phrase.isNotBlank() }
        .sortedByDescending { it.phrase.length }
        .firstOrNull { rawText.contains(it.phrase, ignoreCase = true) }
        ?.paymentMethodId
        ?: defaultPaymentMethodId
}
