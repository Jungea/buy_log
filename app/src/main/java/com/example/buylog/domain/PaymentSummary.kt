package com.example.buylog.domain

import com.example.buylog.data.PaymentWithMethod

data class PaymentSummary(
    val totalAmount: Long = 0,
    val actualSpending: Long = 0,
    val excludedAmount: Long = 0,
)

object PaymentSummaryCalculator {
    fun calculate(payments: List<PaymentWithMethod>): PaymentSummary {
        val total = payments.sumOf { it.payment.amount }
        val excluded = payments.filter { item -> item.tags.any { it.excludeFromSpending } }.sumOf { it.payment.amount }
        return PaymentSummary(totalAmount = total, actualSpending = total - excluded, excludedAmount = excluded)
    }
}
