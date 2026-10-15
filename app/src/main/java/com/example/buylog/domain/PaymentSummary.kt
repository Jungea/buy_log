package com.example.buylog.domain

import com.example.buylog.data.PaymentWithMethod

data class PaymentSummary(
    val totalAmount: Long = 0,
)

object PaymentSummaryCalculator {
    fun calculate(payments: List<PaymentWithMethod>): PaymentSummary = PaymentSummary(
        totalAmount = payments.sumOf { it.payment.amount },
    )
}
