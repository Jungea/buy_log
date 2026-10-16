package com.example.buylog.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object CardPeriodCalculator {
    fun containing(instant: Instant, startDay: Int, offset: Long, zoneId: ZoneId): SpendingPeriod {
        require(startDay in 1..31)
        val today = instant.atZone(zoneId).toLocalDate()
        var month = YearMonth.from(today)
        if (today.isBefore(day(month, startDay))) month = month.minusMonths(1)
        month = month.plusMonths(offset)
        val start = day(month, startDay)
        val end = day(month.plusMonths(1), startDay)
        return SpendingPeriod(
            start.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            end.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            "${start.monthValue}/${start.dayOfMonth}~${end.minusDays(1).monthValue}/${end.minusDays(1).dayOfMonth}",
        )
    }

    private fun day(month: YearMonth, value: Int): LocalDate = month.atDay(value.coerceAtMost(month.lengthOfMonth()))
}

data class CardPerformance(val total: Long, val included: Long, val target: Long) {
    val remaining: Long = (target - included).coerceAtLeast(0)
    val progressPercent: Long = if (target > 0) (included * 100 / target).coerceAtMost(100) else 0
}

object CardPerformanceCalculator {
    fun calculate(payments: List<com.example.buylog.data.PaymentWithMethod>, target: Long): CardPerformance = CardPerformance(
        total = payments.sumOf { it.payment.amount },
        included = payments.filter { it.payment.performanceIncluded }.sumOf { it.payment.amount },
        target = target,
    )
}
