package com.example.buylog.domain

import java.time.Instant
import java.time.DayOfWeek
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

    fun salaryCycle(
        instant: Instant,
        payday: Int,
        offset: Long,
        zoneId: ZoneId,
        moveWeekendToPreviousWeekday: Boolean,
        paydayOverrides: Map<YearMonth, LocalDate>,
    ): SpendingPeriod {
        require(payday in 1..31)
        val today = instant.atZone(zoneId).toLocalDate()
        var anchor = YearMonth.from(today)
        val currentPayday = paydayDate(anchor, payday, moveWeekendToPreviousWeekday, paydayOverrides)
        val nextPayday = paydayDate(anchor.plusMonths(1), payday, moveWeekendToPreviousWeekday, paydayOverrides)
        anchor = when {
            today.isBefore(currentPayday) -> anchor.minusMonths(1)
            !today.isBefore(nextPayday) -> anchor.plusMonths(1)
            else -> anchor
        }
        anchor = anchor.plusMonths(offset)
        val start = paydayDate(anchor, payday, moveWeekendToPreviousWeekday, paydayOverrides)
        val end = paydayDate(anchor.plusMonths(1), payday, moveWeekendToPreviousWeekday, paydayOverrides)
        return SpendingPeriod(
            start.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            end.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            "${start.monthValue}/${start.dayOfMonth}~${end.minusDays(1).monthValue}/${end.minusDays(1).dayOfMonth}",
        )
    }

    private fun paydayDate(
        month: YearMonth,
        payday: Int,
        moveWeekendToPreviousWeekday: Boolean,
        paydayOverrides: Map<YearMonth, LocalDate>,
    ): LocalDate {
        paydayOverrides[month]?.let { return it }
        val date = month.atDay(payday.coerceAtMost(month.lengthOfMonth()))
        if (!moveWeekendToPreviousWeekday) return date
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> date.minusDays(1)
            DayOfWeek.SUNDAY -> date.minusDays(2)
            else -> date
        }
    }
}
