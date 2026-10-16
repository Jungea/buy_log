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
        val period = SpendingPeriodCalculator.salaryCycle(instant, 25, 0, zone, false, emptyMap())
        assertEquals("9/25~10/24", period.label)
    }

    @Test
    fun `payday 31 clamps to month end`() {
        val instant = LocalDateTime.of(2026, 3, 15, 12, 0).atZone(zone).toInstant()
        val period = SpendingPeriodCalculator.salaryCycle(instant, 31, 0, zone, false, emptyMap())
        assertEquals("2/28~3/30", period.label)
    }

    @Test
    fun `sunday payday moves cycle start to previous friday`() {
        val instant = LocalDateTime.of(2026, 10, 24, 12, 0).atZone(zone).toInstant()
        val period = SpendingPeriodCalculator.salaryCycle(instant, 25, 0, zone, true, emptyMap())
        assertEquals("10/23~11/24", period.label)
    }

    @Test
    fun `saturday payday moves cycle start to previous friday`() {
        val instant = LocalDateTime.of(2026, 4, 25, 12, 0).atZone(zone).toInstant()
        val period = SpendingPeriodCalculator.salaryCycle(instant, 25, 0, zone, true, emptyMap())
        assertEquals("4/24~5/24", period.label)
    }

    @Test
    fun `day one payday moved into previous month still selects newest cycle`() {
        val instant = LocalDateTime.of(2025, 5, 31, 12, 0).atZone(zone).toInstant()
        val period = SpendingPeriodCalculator.salaryCycle(instant, 1, 0, zone, true, emptyMap())
        assertEquals("5/30~6/30", period.label)
    }

    @Test
    fun `monthly override takes priority over weekend adjustment`() {
        val instant = LocalDateTime.of(2026, 10, 22, 12, 0).atZone(zone).toInstant()
        val overrides = mapOf(java.time.YearMonth.of(2026, 10) to java.time.LocalDate.of(2026, 10, 22))
        val period = SpendingPeriodCalculator.salaryCycle(instant, 25, 0, zone, true, overrides)
        assertEquals("10/22~11/24", period.label)
    }
}
