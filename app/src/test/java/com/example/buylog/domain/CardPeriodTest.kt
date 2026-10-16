package com.example.buylog.domain

import com.example.buylog.data.PaymentEntity
import com.example.buylog.data.PaymentMethodEntity
import com.example.buylog.data.PaymentWithMethod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CardPeriodTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val method = PaymentMethodEntity(id = 1, name = "카드", createdAt = 0, isCard = true)

    @Test
    fun `start day 15 creates previous 15 through current 14`() {
        val now = LocalDateTime.of(2026, 10, 10, 12, 0).atZone(zone).toInstant()
        assertEquals("9/15~10/14", CardPeriodCalculator.containing(now, 15, 0, zone).label)
    }

    @Test
    fun `card performance excludes only manually excluded payment`() {
        val result = CardPerformanceCalculator.calculate(
            listOf(payment(200_000, true), payment(50_000, false)),
            target = 300_000,
        )
        assertEquals(250_000, result.total)
        assertEquals(200_000, result.included)
        assertEquals(100_000, result.remaining)
        assertEquals(66, result.progressPercent)
    }

    private fun payment(amount: Long, included: Boolean) = PaymentWithMethod(
        PaymentEntity(
            id = amount, amount = amount, merchant = "사용처", paidAt = 0,
            paymentMethodId = 1, memo = "", createdAt = 0, updatedAt = 0,
            performanceIncluded = included,
        ),
        method,
        emptyList(),
    )
}
