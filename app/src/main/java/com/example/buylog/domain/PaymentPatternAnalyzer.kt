package com.example.buylog.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TimePattern(
    val startHour: Int,
    val endHour: Int,
    val occurrenceDays: Int,
    val probabilityPercent: Int,
)

data class WeekdayPattern(
    val dayOfWeek: DayOfWeek,
    val observedDays: Int,
    val patterns: List<TimePattern>,
)

data class PaymentPatternReport(
    val from: LocalDate?,
    val through: LocalDate?,
    val weekdays: List<WeekdayPattern>,
    val minimumObservedDays: Int,
) {
    val hasSufficientData: Boolean = weekdays.isNotEmpty()
}

object PaymentPatternAnalyzer {
    fun analyze(
        paidAtValues: List<Long>,
        now: Instant,
        zoneId: ZoneId,
        lookbackDays: Long = 56,
        minimumObservedDays: Int = 4,
        minimumProbabilityPercent: Int = 50,
    ): PaymentPatternReport {
        require(lookbackDays > 0)
        require(minimumObservedDays > 0)
        require(minimumProbabilityPercent in 1..100)

        val today = now.atZone(zoneId).toLocalDate()
        val completedPayments = paidAtValues
            .map { Instant.ofEpochMilli(it).atZone(zoneId) }
            .filter { it.toLocalDate().isBefore(today) }
        val earliestDate = completedPayments.minOfOrNull { it.toLocalDate() }
            ?: return PaymentPatternReport(null, null, emptyList(), minimumObservedDays)
        val from = maxOf(today.minusDays(lookbackDays), earliestDate)
        if (!from.isBefore(today)) {
            return PaymentPatternReport(null, null, emptyList(), minimumObservedDays)
        }

        val dates = generateSequence(from) { current -> current.plusDays(1).takeIf { it.isBefore(today) } }.toList()
        val paymentDaysBySlot = completedPayments
            .filter { !it.toLocalDate().isBefore(from) }
            .groupBy { it.dayOfWeek to (it.hour / SLOT_HOURS) * SLOT_HOURS }
            .mapValues { (_, values) -> values.map { it.toLocalDate() }.toSet().size }

        val weekdays = DayOfWeek.entries.mapNotNull { dayOfWeek ->
            val observedDays = dates.count { it.dayOfWeek == dayOfWeek }
            if (observedDays < minimumObservedDays) return@mapNotNull null
            val patterns = (0 until 24 step SLOT_HOURS).mapNotNull { startHour ->
                val occurrenceDays = paymentDaysBySlot[dayOfWeek to startHour] ?: 0
                val probability = occurrenceDays * 100 / observedDays
                if (occurrenceDays < 2 || probability < minimumProbabilityPercent) null else {
                    TimePattern(startHour, startHour + SLOT_HOURS, occurrenceDays, probability)
                }
            }.sortedWith(compareByDescending<TimePattern> { it.probabilityPercent }.thenBy { it.startHour })
                .take(MAX_PATTERNS_PER_DAY)
            WeekdayPattern(dayOfWeek, observedDays, patterns)
        }

        return PaymentPatternReport(
            from = from,
            through = today.minusDays(1),
            weekdays = weekdays,
            minimumObservedDays = minimumObservedDays,
        )
    }

    private const val SLOT_HOURS = 2
    private const val MAX_PATTERNS_PER_DAY = 3
}
