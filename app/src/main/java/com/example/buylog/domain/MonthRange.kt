package com.example.buylog.domain

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class MonthRange(
    val fromInclusive: Long,
    val toExclusive: Long,
) {
    companion object {
        fun containing(
            instant: Instant,
            zoneId: ZoneId,
        ): MonthRange {
            val month = YearMonth.from(instant.atZone(zoneId))
            return MonthRange(
                fromInclusive = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                toExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            )
        }
    }
}
