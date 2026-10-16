package com.example.buylog.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class MissedPaymentCandidate(
    val dateEpochDay: Long,
    val startHour: Int,
    val endHour: Int,
    val probabilityPercent: Int,
)

object MissedPaymentEvaluator {
    fun evaluate(
        paidAtValues: List<Long>,
        now: Instant,
        zoneId: ZoneId,
        alreadyHandledToday: Boolean,
    ): MissedPaymentCandidate? {
        if (alreadyHandledToday) return null
        val localNow = now.atZone(zoneId)
        val today = localNow.toLocalDate()
        val report = PaymentPatternAnalyzer.analyze(
            paidAtValues = paidAtValues,
            now = now,
            zoneId = zoneId,
            minimumObservedDays = MINIMUM_OBSERVED_DAYS,
            minimumProbabilityPercent = MINIMUM_PROBABILITY_PERCENT,
        )
        val patterns = report.weekdays.firstOrNull { it.dayOfWeek == localNow.dayOfWeek }?.patterns.orEmpty()
        val todayPayments = paidAtValues.map { Instant.ofEpochMilli(it).atZone(zoneId) }
            .filter { it.toLocalDate() == today }

        return patterns.mapNotNull { pattern ->
            val slotStart = today.atStartOfDay(zoneId).plusHours(pattern.startHour.toLong())
            val slotEnd = today.atStartOfDay(zoneId).plusHours(pattern.endHour.toLong())
            val elapsed = Duration.between(slotEnd, localNow)
            val isCheckWindow = !elapsed.isNegative && elapsed <= MAX_CHECK_DELAY
            val hasPayment = todayPayments.any { !it.isBefore(slotStart) && it.isBefore(slotEnd) }
            if (!isCheckWindow || hasPayment) null else {
                MissedPaymentCandidate(
                    dateEpochDay = today.toEpochDay(),
                    startHour = pattern.startHour,
                    endHour = pattern.endHour,
                    probabilityPercent = pattern.probabilityPercent,
                )
            }
        }.maxWithOrNull(compareBy<MissedPaymentCandidate> { it.probabilityPercent }.thenBy { it.startHour })
    }

    private const val MINIMUM_OBSERVED_DAYS = 6
    private const val MINIMUM_PROBABILITY_PERCENT = 70
    private val MAX_CHECK_DELAY = Duration.ofHours(2)
}
