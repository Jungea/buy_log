package com.example.buylog.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class MonthRangeTest {
    @Test
    fun `month range uses the supplied local timezone`() {
        val zone = ZoneId.of("Asia/Seoul")
        val range = MonthRange.containing(
            instant = Instant.parse("2026-10-15T03:00:00Z"),
            zoneId = zone,
        )

        assertEquals(
            ZonedDateTime.parse("2026-10-01T00:00:00+09:00[Asia/Seoul]").toInstant().toEpochMilli(),
            range.fromInclusive,
        )
        assertEquals(
            ZonedDateTime.parse("2026-11-01T00:00:00+09:00[Asia/Seoul]").toInstant().toEpochMilli(),
            range.toExclusive,
        )
    }

    @Test
    fun `month end is exclusive`() {
        val zone = ZoneId.of("UTC")
        val october = MonthRange.containing(Instant.parse("2026-10-31T23:59:59Z"), zone)
        val november = MonthRange.containing(Instant.parse("2026-11-01T00:00:00Z"), zone)

        assertEquals(october.toExclusive, november.fromInclusive)
    }
}
