package com.example.buylog.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class PaymentParseStatus {
    CANDIDATE,
    NEEDS_REVIEW,
    NOT_PAYMENT,
    UNPROCESSED,
}

data class PaymentSourceEvent(
    val sourceType: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val rawText: String,
    val receivedAt: Long,
)

data class PaymentCandidate(
    val amount: Long,
    val merchant: String,
    val paidAt: Long,
    val paymentMethod: String?,
    val sourceType: String,
    val sourcePackage: String,
)

data class PaymentParseResult(
    val status: PaymentParseStatus,
    val candidate: PaymentCandidate? = null,
    val message: String,
)

object PaymentDetector {
    private val strongPaymentKeywords = listOf("승인", "결제완료", "결제 완료")
    private val excludedKeywords = listOf("승인취소", "승인 취소", "결제취소", "결제 취소", "환불")
    private val paymentMethodHint = Regex("""카드|페이""")

    fun isPaymentNotification(rawText: String): Boolean {
        val normalized = rawText.replace("\n", " ").trim()
        if (excludedKeywords.any(normalized::contains)) return false
        return strongPaymentKeywords.any(normalized::contains) ||
            (normalized.contains("사용") && paymentMethodHint.containsMatchIn(normalized))
    }
}

class PaymentNotificationParser(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun parse(event: PaymentSourceEvent): PaymentParseResult {
        if (!PaymentDetector.isPaymentNotification(event.rawText)) {
            return PaymentParseResult(
                status = PaymentParseStatus.NOT_PAYMENT,
                message = "명확한 결제 승인 알림이 아닙니다.",
            )
        }

        val amountMatch = amountRegex.find(event.rawText)
            ?: return PaymentParseResult(
                status = PaymentParseStatus.NEEDS_REVIEW,
                message = "원화 금액을 찾지 못했습니다.",
            )
        val amount = amountMatch.groupValues[1].replace(",", "").toLongOrNull()
        if (amount == null || amount <= 0) {
            return PaymentParseResult(
                status = PaymentParseStatus.NEEDS_REVIEW,
                message = "유효한 결제 금액을 확인할 수 없습니다.",
            )
        }

        val paymentMethod = paymentMethodRegex.find(event.rawText)?.groupValues?.get(1)
        val merchant = extractMerchant(event, amountMatch.value, paymentMethod)
            ?: return PaymentParseResult(
                status = PaymentParseStatus.NEEDS_REVIEW,
                message = "가맹점을 확정할 수 없습니다.",
            )
        val paidAt = extractPaidAt(event.rawText, event.receivedAt)

        return PaymentParseResult(
            status = PaymentParseStatus.CANDIDATE,
            candidate = PaymentCandidate(
                amount = amount,
                merchant = merchant,
                paidAt = paidAt,
                paymentMethod = paymentMethod,
                sourceType = event.sourceType,
                sourcePackage = event.packageName,
            ),
            message = if (paymentMethod == null) {
                "결제수단은 확인이 필요합니다."
            } else {
                "결제 후보를 생성했습니다."
            },
        )
    }

    private fun extractMerchant(
        event: PaymentSourceEvent,
        amountText: String,
        paymentMethod: String?,
    ): String? {
        val inputs = listOf(event.content, event.rawText).filter(String::isNotBlank)
        return inputs.firstNotNullOfOrNull { input ->
            var normalized = input
                .replace(amountText, " ")
                .replace(fullDateTimeRegex, " ")
                .replace(shortDateTimeRegex, " ")
                .replace(koreanDateTimeRegex, " ")
                .replace(timeRegex, " ")
            if (paymentMethod != null) normalized = normalized.replace(paymentMethod, " ")
            val cleaned = normalized
                .replace(paymentWordsRegex, " ")
                .replace(fieldWordsRegex, " ")
                .replace(maskedNumberRegex, " ")
                .replace(bracketLabelRegex, " ")
                .lineSequence()
                .map { line -> line.replace(whitespaceRegex, " ").trim(' ', '-', ':', '·') }
                .filter { it.length in 2..40 }
                .firstOrNull { line -> line.any(Char::isLetter) }
            cleaned
        }
    }

    private fun extractPaidAt(rawText: String, receivedAt: Long): Long {
        val received = Instant.ofEpochMilli(receivedAt).atZone(zoneId)

        fullDateTimeRegex.find(rawText)?.let { match ->
            val values = match.groupValues
            return localDateTimeOrNull(
                values[1].toInt(),
                values[2].toInt(),
                values[3].toInt(),
                values[4].toInt(),
                values[5].toInt(),
            )?.atZone(zoneId)?.toInstant()?.toEpochMilli() ?: receivedAt
        }

        val shortMatch = shortDateTimeRegex.find(rawText) ?: koreanDateTimeRegex.find(rawText)
        shortMatch?.let { match ->
            val values = match.groupValues
            val month = values[1].toInt()
            val day = values[2].toInt()
            val hour = values[3].toInt()
            val minute = values[4].toInt()
            val candidates = (received.year - 1..received.year + 1).mapNotNull { year ->
                localDateTimeOrNull(year, month, day, hour, minute)
            }
            return candidates.minByOrNull { candidate ->
                kotlin.math.abs(
                    ChronoUnit.MINUTES.between(received.toLocalDateTime(), candidate),
                )
            }?.atZone(zoneId)?.toInstant()?.toEpochMilli() ?: receivedAt
        }

        timeRegex.find(rawText)?.let { match ->
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            val time = runCatching { LocalTime.of(hour, minute) }.getOrNull() ?: return receivedAt
            var candidate = LocalDateTime.of(received.toLocalDate(), time)
            if (candidate.isAfter(received.toLocalDateTime().plusHours(2))) {
                candidate = candidate.minusDays(1)
            }
            return candidate.atZone(zoneId).toInstant().toEpochMilli()
        }

        return receivedAt
    }

    private fun localDateTimeOrNull(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): LocalDateTime? = runCatching {
        LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute))
    }.getOrNull()

    private companion object {
        val amountRegex = Regex("""(?:₩\s*)?([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\s*원""")
        val paymentMethodRegex = Regex("""\[?([가-힣A-Za-z0-9]{2,20}(?:카드|페이))\]?""")
        val fullDateTimeRegex = Regex("""(20\d{2})[./-](\d{1,2})[./-](\d{1,2})\s+(\d{1,2}):(\d{2})""")
        val shortDateTimeRegex = Regex("""(?<!\d)(\d{1,2})[./-](\d{1,2})\s+(\d{1,2}):(\d{2})""")
        val koreanDateTimeRegex = Regex("""(\d{1,2})월\s*(\d{1,2})일\s+(\d{1,2}):(\d{2})""")
        val timeRegex = Regex("""(?<!\d)(\d{1,2}):(\d{2})(?!\d)""")
        val paymentWordsRegex = Regex("""승인|결제완료|결제 완료|결제|사용""")
        val fieldWordsRegex = Regex("""금액|일시|가맹점|이용처|사용처|누적|잔액""")
        val maskedNumberRegex = Regex("""(?<!\p{L})[0-9*#-]{3,}(?!\p{L})""")
        val bracketLabelRegex = Regex("""\[[^]]{1,30}]""")
        val whitespaceRegex = Regex("""\s+""")
    }
}
