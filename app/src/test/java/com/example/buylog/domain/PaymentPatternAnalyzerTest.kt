package com.example.buylog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class PaymentPatternAnalyzerTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val now = LocalDateTime.of(2026, 10, 16, 12, 0).atZone(zone).toInstant()

    @Test
    fun `six of eight monday lunch payments become seventy five percent pattern`() {
        val mondayLunches = listOf(7, 14, 21, 28).map { septemberDay ->
            millis(2026, 9, septemberDay, 12)
        } + listOf(5, 12).map { octoberDay ->
            millis(2026, 10, octoberDay, 12)
        }
        val report = PaymentPatternAnalyzer.analyze(
            paidAtValues = mondayLunches + listOf(millis(2026, 8, 24, 18), millis(2026, 8, 31, 18)),
            now = now,
            zoneId = zone,
        )

        val monday = report.weekdays.single { it.dayOfWeek == DayOfWeek.MONDAY }
        val lunch = monday.patterns.single { it.startHour == 12 }
        assertEquals(8, monday.observedDays)
        assertEquals(6, lunch.occurrenceDays)
        assertEquals(75, lunch.probabilityPercent)
    }

    @Test
    fun `multiple payments in same slot on same day count once`() {
        val mondays = listOf(7, 14, 21, 28).flatMap { day ->
            listOf(millis(2026, 9, day, 12), millis(2026, 9, day, 13))
        }
        val endOfSeptember = LocalDateTime.of(2026, 10, 1, 0, 0).atZone(zone).toInstant()
        val report = PaymentPatternAnalyzer.analyze(mondays, endOfSeptember, zone, lookbackDays = 40)

        val pattern = report.weekdays.single { it.dayOfWeek == DayOfWeek.MONDAY }.patterns.single()
        assertEquals(4, pattern.occurrenceDays)
        assertEquals(100, pattern.probabilityPercent)
    }

    @Test
    fun `fewer than four observed weekdays are not treated as a pattern`() {
        val values = listOf(28).map { millis(2026, 9, it, 12) } +
            listOf(5, 12).map { millis(2026, 10, it, 12) }
        val report = PaymentPatternAnalyzer.analyze(values, now, zone)

        assertFalse(report.hasSufficientData)
        assertTrue(report.weekdays.isEmpty())
    }

    @Test
    fun `payments from today are excluded because the day is incomplete`() {
        val todayOnly = listOf(millis(2026, 10, 16, 12))
        val report = PaymentPatternAnalyzer.analyze(todayOnly, now, zone)

        assertFalse(report.hasSufficientData)
        assertEquals(null, report.from)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int): Long =
        LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toInstant().toEpochMilli()
}
