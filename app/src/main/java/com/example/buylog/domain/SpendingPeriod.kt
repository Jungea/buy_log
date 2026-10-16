package com.example.buylog.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class SpendingPeriod(val fromInclusive: Long, val toExclusive: Long, val label: String)

object SpendingPeriodCalculator {
    fun calendarMonth(anchor: YearMonth, zoneId: ZoneId): SpendingPeriod = SpendingPeriod(
        anchor.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
        anchor.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
        "${anchor.year}년 ${anchor.monthValue}월",
    )

    fun salaryCycle(instant: Instant, payday: Int, offset: Long, zoneId: ZoneId): SpendingPeriod {
        require(payday in 1..31)
        val today = instant.atZone(zoneId).toLocalDate()
        var anchor = YearMonth.from(today)
        if (today.isBefore(paydayDate(anchor, payday))) anchor = anchor.minusMonths(1)
        anchor = anchor.plusMonths(offset)
        val start = paydayDate(anchor, payday)
        val end = paydayDate(anchor.plusMonths(1), payday)
        return SpendingPeriod(
            start.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            end.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            "${start.monthValue}/${start.dayOfMonth}~${end.minusDays(1).monthValue}/${end.minusDays(1).dayOfMonth}",
        )
    }

    private fun paydayDate(month: YearMonth, payday: Int): LocalDate = month.atDay(payday.coerceAtMost(month.lengthOfMonth()))
}
