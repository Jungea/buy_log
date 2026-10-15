package com.example.buylog.domain

import com.example.buylog.data.PaymentEntity
import com.example.buylog.data.PaymentMethodEntity
import com.example.buylog.data.PaymentWithMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentSummaryCalculatorTest {
    private val method = PaymentMethodEntity(id = 1, name = "테스트 카드", createdAt = 0)

    @Test
    fun `total includes every payment`() {
        val payments = listOf(
            payment(amount = 10_000),
            payment(amount = 3_000),
            payment(amount = 2_000),
        )

        assertEquals(
            PaymentSummary(totalAmount = 15_000),
            PaymentSummaryCalculator.calculate(payments),
        )
    }

    @Test
    fun `empty payments produce zero summary`() {
        assertEquals(PaymentSummary(), PaymentSummaryCalculator.calculate(emptyList()))
    }

    private fun payment(amount: Long) = PaymentWithMethod(
        payment = PaymentEntity(
            id = amount,
            amount = amount,
            merchant = "사용처",
            paidAt = 0,
            paymentMethodId = method.id,
            memo = "",
            createdAt = 0,
            updatedAt = 0,
        ),
        paymentMethod = method,
    )
}
