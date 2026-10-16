package com.example.buylog.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SpendingPeriodCalculatorTest {
    private val zone = ZoneId.of("Asia/Seoul")

    @Test
    fun `payday 25 creates cycle through next month 24`() {
        val instant = LocalDateTime.of(2026, 10, 16, 12, 0).atZone(zone).toInstant()
        val period = SpendingPeriodCalculator.salaryCycle(instant, 25, 0, zone)
        assertEquals("9/25~10/24", period.label)
    }

    @Test
    fun `payday 31 clamps to month end`() {
        val instant = LocalDateTime.of(2026, 3, 15, 12, 0).atZone(zone).toInstant()
        val period = SpendingPeriodCalculator.salaryCycle(instant, 31, 0, zone)
        assertEquals("2/28~3/30", period.label)
    }
}
