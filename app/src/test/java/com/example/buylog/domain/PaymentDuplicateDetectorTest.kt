package com.example.buylog.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentDuplicateDetectorTest {
    private val candidate = PaymentCandidate(
        amount = 12_000,
        merchant = "스타벅스 강남점",
        paidAt = 1_000_000,
        paymentMethod = "신한카드",
        sourceType = "SMS_NOTIFICATION",
        sourcePackage = "com.google.android.apps.messaging",
    )

    @Test
    fun `different source for same payment links automatically`() {
        val decision = PaymentDuplicateDetector.decide(candidate, listOf(existing(setOf("APP_NOTIFICATION"))))

        assertEquals(DuplicateDecision.LINK_HIGH, decision.first)
        assertEquals(1L, decision.second)
    }

    @Test
    fun `same source match requires confirmation`() {
        val decision = PaymentDuplicateDetector.decide(candidate, listOf(existing(setOf("SMS_NOTIFICATION"))))

        assertEquals(DuplicateDecision.REVIEW_MEDIUM, decision.first)
        assertEquals(1L, decision.second)
    }

    @Test
    fun `manual payment match requires confirmation`() {
        val decision = PaymentDuplicateDetector.decide(candidate, listOf(existing(emptySet())))

        assertEquals(DuplicateDecision.REVIEW_MEDIUM, decision.first)
    }

    @Test
    fun `different merchant or time registers a new payment`() {
        val differentMerchant = existing(setOf("APP_NOTIFICATION")).copy(merchant = "편의점")
        val differentTime = existing(setOf("APP_NOTIFICATION")).copy(paidAt = 1_000_000 + 5 * 60 * 1000 + 1)

        assertEquals(DuplicateDecision.REGISTER_NEW, PaymentDuplicateDetector.decide(candidate, listOf(differentMerchant)).first)
        assertEquals(DuplicateDecision.REGISTER_NEW, PaymentDuplicateDetector.decide(candidate, listOf(differentTime)).first)
    }

    private fun existing(sourceTypes: Set<String>) = ExistingPaymentForDuplicate(
        paymentId = 1,
        amount = 12_000,
        merchant = "스타벅스 강남점",
        paidAt = 1_000_000,
        linkedSourceTypes = sourceTypes,
    )
}
