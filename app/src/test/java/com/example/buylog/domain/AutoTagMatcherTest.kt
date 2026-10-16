package com.example.buylog.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoTagMatcherTest {
    @Test
    fun `merchant phrase adds matching tags without case sensitivity`() {
        val result = AutoTagMatcher.matchingTagIds(
            merchant = "PG_TEST 테스트몰",
            memo = "",
            patterns = listOf(
                AutoTagPattern("pg_test", 1),
                AutoTagPattern("테스트", 2),
                AutoTagPattern("식사", 3),
            ),
        )

        assertEquals(setOf(1L, 2L), result)
    }

    @Test
    fun `memo phrase adds tag and duplicate tag ids are collapsed`() {
        val result = AutoTagMatcher.matchingTagIds(
            merchant = "온라인몰",
            memo = "업무 테스트 결제",
            patterns = listOf(
                AutoTagPattern("업무", 7),
                AutoTagPattern("테스트", 7),
            ),
        )

        assertEquals(setOf(7L), result)
    }

    @Test
    fun `blank phrase never matches`() {
        val result = AutoTagMatcher.matchingTagIds("가맹점", "메모", listOf(AutoTagPattern(" ", 9)))

        assertEquals(emptySet<Long>(), result)
    }
}
