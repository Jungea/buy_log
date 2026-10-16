package com.example.buylog.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PaymentNotificationParserTest {
    private val zoneId = ZoneId.of("Asia/Seoul")
    private val parser = PaymentNotificationParser(zoneId)
    private val receivedAt = LocalDateTime.of(2026, 10, 16, 12, 35)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `clear approval notification creates payment candidate`() {
        val result = parser.parse(
            event(
                title = "[신한카드] 승인",
                content = "스타벅스 12,000원 10/16 12:31",
                rawText = "[신한카드] 승인\n스타벅스 12,000원 10/16 12:31",
            ),
        )

        assertEquals(PaymentParseStatus.CANDIDATE, result.status)
        assertEquals(12_000L, result.candidate?.amount)
        assertEquals("스타벅스", result.candidate?.merchant)
        assertEquals("신한카드", result.candidate?.paymentMethod)
        assertEquals(
            LocalDateTime.of(2026, 10, 16, 12, 31).atZone(zoneId).toInstant().toEpochMilli(),
            result.candidate?.paidAt,
        )
    }

    @Test
    fun `time only notification uses received date`() {
        val result = parser.parse(
            event(
                title = "삼성카드 승인",
                content = "배달의민족 8,500원 12:30",
                rawText = "삼성카드 승인\n배달의민족 8,500원 12:30",
            ),
        )

        assertEquals(PaymentParseStatus.CANDIDATE, result.status)
        assertEquals(
            LocalDateTime.of(2026, 10, 16, 12, 30).atZone(zoneId).toInstant().toEpochMilli(),
            result.candidate?.paidAt,
        )
    }

    @Test
    fun `approval without won amount needs review`() {
        val result = parser.parse(
            event(
                title = "카드 승인",
                content = "스타벅스 USD 10",
                rawText = "카드 승인\n스타벅스 USD 10",
            ),
        )

        assertEquals(PaymentParseStatus.NEEDS_REVIEW, result.status)
        assertNull(result.candidate)
    }

    @Test
    fun `cancel notification does not create candidate`() {
        val result = parser.parse(
            event(
                title = "신한카드 승인취소",
                content = "스타벅스 12,000원",
                rawText = "신한카드 승인취소\n스타벅스 12,000원",
            ),
        )

        assertEquals(PaymentParseStatus.NOT_PAYMENT, result.status)
        assertNull(result.candidate)
    }

    @Test
    fun `ordinary notification does not create candidate`() {
        val result = parser.parse(
            event(
                title = "배송 안내",
                content = "상품이 출발했습니다.",
                rawText = "배송 안내\n상품이 출발했습니다.",
            ),
        )

        assertEquals(PaymentParseStatus.NOT_PAYMENT, result.status)
        assertNull(result.candidate)
    }

    @Test
    fun `generic point usage notification is not treated as payment`() {
        val result = parser.parse(
            event(
                title = "포인트 안내",
                content = "포인트 사용 가능 금액 12,000원",
                rawText = "포인트 안내\n포인트 사용 가능 금액 12,000원",
            ),
        )

        assertEquals(PaymentParseStatus.NOT_PAYMENT, result.status)
        assertNull(result.candidate)
    }

    private fun event(
        title: String,
        content: String,
        rawText: String,
    ) = PaymentSourceEvent(
        sourceType = "APP_NOTIFICATION",
        packageName = "com.example.card",
        appName = "카드 앱",
        title = title,
        content = content,
        rawText = rawText,
        receivedAt = receivedAt,
    )
}
