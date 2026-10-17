package com.example.buylog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPaymentMethodResolverTest {
    @Test
    fun `more specific keyword wins`() {
        val result = NotificationPaymentMethodResolver.resolve(
            rawText = "국민 체크카드 승인 5,000원",
            rules = listOf(
                NotificationPaymentRule("카드", 1L),
                NotificationPaymentRule("체크카드", 2L),
            ),
            defaultPaymentMethodId = 3L,
        )

        assertEquals(2L, result)
    }

    @Test
    fun `default is used when no keyword matches`() {
        val result = NotificationPaymentMethodResolver.resolve(
            rawText = "출금 승인 5,000원",
            rules = listOf(NotificationPaymentRule("카드", 1L)),
            defaultPaymentMethodId = 3L,
        )

        assertEquals(3L, result)
    }

    @Test
    fun `missing rule and default returns null for unclassified fallback`() {
        assertNull(NotificationPaymentMethodResolver.resolve("승인", emptyList(), null))
    }
}
