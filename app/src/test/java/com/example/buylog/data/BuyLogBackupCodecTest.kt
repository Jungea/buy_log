package com.example.buylog.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BuyLogBackupCodecTest {
    @Test
    fun `round trip preserves every data type and special text`() {
        val snapshot = BuyLogBackupSnapshot(
            paymentMethods = listOf(PaymentMethodEntity(1, "신한\t카드", 10, true, "신한카드", 15, 300_000)),
            payments = listOf(PaymentEntity(2, 12_000, "테스트몰\n강남점", 20, 1, "메모🙂", 21, 22, false)),
            notificationEvents = listOf(
                NotificationEventEntity(
                    id = 3, sourceType = "APP_NOTIFICATION", packageName = "com.example.bank", appName = "은행", title = "승인",
                    content = "12,000원", rawText = "원문\n전체", receivedAt = 30, createdAt = 31, parseStatus = "CANDIDATE",
                    parsedAmount = 12_000, parsedMerchant = "테스트몰\n강남점", parsedPaidAt = 20, parsedPaymentMethod = "신한\t카드",
                    parseMessage = "", registrationStatus = "REGISTERED", duplicateConfidence = "LOW", paymentId = 2,
                ),
            ),
            tags = listOf(TagEntity(4, "업무 테스트", true, 40)),
            paymentTags = listOf(PaymentTagCrossRef(2, 4)),
            autoTagRules = listOf(AutoTagRuleEntity(5, "테스트몰", 4, 50)),
            missedPaymentChecks = listOf(MissedPaymentCheckEntity(6, 20_000, 12, 14, 75, "NO_PAYMENT", 60, 61)),
        )

        assertEquals(snapshot, BuyLogBackupCodec.decode(BuyLogBackupCodec.encode(snapshot)))
    }

    @Test
    fun `invalid payment method reference is rejected before restore`() {
        val invalid = BuyLogBackupSnapshot(
            payments = listOf(PaymentEntity(1, 1_000, "가맹점", 1, 99, "", 1, 1)),
        )

        assertThrows(IllegalArgumentException::class.java) {
            BuyLogBackupCodec.decode(BuyLogBackupCodec.encode(invalid))
        }
    }

    @Test
    fun `unknown format is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BuyLogBackupCodec.decode("NOT_A_BUYLOG_BACKUP")
        }
    }
}
