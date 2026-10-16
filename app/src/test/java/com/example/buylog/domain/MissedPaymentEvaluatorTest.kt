package com.example.buylog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MissedPaymentEvaluatorTest {
    private val zone = ZoneId.of("Asia/Seoul")

    @Test
    fun `missing high probability slot is detected after it ends`() {
        val historicalLunches = previousWeekdays(LocalDate.of(2026, 10, 16), count = 8)
            .map { it.atTime(12, 30).atZone(zone).toInstant().toEpochMilli() }
        val now = LocalDateTime.of(2026, 10, 16, 14, 30).atZone(zone).toInstant()

        val candidate = MissedPaymentEvaluator.evaluate(historicalLunches, now, zone, false)

        requireNotNull(candidate)
        assertEquals(12, candidate.startHour)
        assertEquals(14, candidate.endHour)
        assertEquals(100, candidate.probabilityPercent)
    }

    @Test
    fun `today payment in expected slot suppresses candidate`() {
        val day = LocalDate.of(2026, 10, 16)
        val values = previousWeekdays(day, 8).map {
            it.atTime(12, 30).atZone(zone).toInstant().toEpochMilli()
        } + day.atTime(13, 0).atZone(zone).toInstant().toEpochMilli()
        val now = day.atTime(14, 30).atZone(zone).toInstant()

        assertNull(MissedPaymentEvaluator.evaluate(values, now, zone, false))
    }

    @Test
    fun `old slot and handled day do not notify again`() {
        val day = LocalDate.of(2026, 10, 16)
        val values = previousWeekdays(day, 8).map {
            it.atTime(12, 30).atZone(zone).toInstant().toEpochMilli()
        }

        assertNull(MissedPaymentEvaluator.evaluate(values, day.atTime(17, 0).atZone(zone).toInstant(), zone, false))
        assertNull(MissedPaymentEvaluator.evaluate(values, day.atTime(14, 30).atZone(zone).toInstant(), zone, true))
    }

    @Test
    fun `probability below seventy percent stays silent`() {
        val day = LocalDate.of(2026, 10, 16)
        val dates = previousWeekdays(day, 8)
        val values = dates.take(5).map {
            it.atTime(12, 30).atZone(zone).toInstant().toEpochMilli()
        } + dates.drop(5).map {
            it.atTime(18, 30).atZone(zone).toInstant().toEpochMilli()
        }

        assertNull(MissedPaymentEvaluator.evaluate(values, day.atTime(14, 30).atZone(zone).toInstant(), zone, false))
    }

    private fun previousWeekdays(today: LocalDate, count: Int): List<LocalDate> =
        generateSequence(today.minusWeeks(1)) { it.minusWeeks(1) }.take(count).toList()
}
