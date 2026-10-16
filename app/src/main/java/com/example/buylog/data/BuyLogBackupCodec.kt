package com.example.buylog.data

import java.nio.charset.StandardCharsets
import java.util.Base64

data class BuyLogBackupSnapshot(
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val notificationEvents: List<NotificationEventEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val paymentTags: List<PaymentTagCrossRef> = emptyList(),
    val autoTagRules: List<AutoTagRuleEntity> = emptyList(),
    val missedPaymentChecks: List<MissedPaymentCheckEntity> = emptyList(),
)

object BuyLogBackupCodec {
    fun encode(snapshot: BuyLogBackupSnapshot): String = buildString {
        appendLine("BUYLOG\t1")
        snapshot.paymentMethods.forEach { value ->
            record("METHOD", value.id, text(value.name), value.createdAt, bool(value.isCard), text(value.cardCompany), value.periodStartDay, value.performanceTarget)
        }
        snapshot.payments.forEach { value ->
            record("PAYMENT", value.id, value.amount, text(value.merchant), value.paidAt, value.paymentMethodId, text(value.memo), value.createdAt, value.updatedAt, bool(value.performanceIncluded))
        }
        snapshot.notificationEvents.forEach { value ->
            record(
                "EVENT", value.id, text(value.sourceType), text(value.packageName), text(value.appName), text(value.title), text(value.content), text(value.rawText),
                value.receivedAt, value.createdAt, text(value.parseStatus), nullableLong(value.parsedAmount), nullableText(value.parsedMerchant), nullableLong(value.parsedPaidAt),
                nullableText(value.parsedPaymentMethod), text(value.parseMessage), text(value.registrationStatus), text(value.duplicateConfidence), nullableLong(value.paymentId), nullableLong(value.suggestedPaymentId),
            )
        }
        snapshot.tags.forEach { value ->
            record("TAG", value.id, text(value.name), bool(value.excludeFromSpending), value.createdAt)
        }
        snapshot.paymentTags.forEach { value -> record("PAYMENT_TAG", value.paymentId, value.tagId) }
        snapshot.autoTagRules.forEach { value -> record("AUTO_TAG", value.id, text(value.phrase), value.tagId, value.createdAt) }
        snapshot.missedPaymentChecks.forEach { value ->
            record("MISSED", value.id, value.dateEpochDay, value.startHour, value.endHour, value.probabilityPercent, text(value.status), value.createdAt, nullableLong(value.respondedAt))
        }
    }

    fun decode(content: String): BuyLogBackupSnapshot {
        val lines = content.lineSequence().filter(String::isNotBlank).toList()
        require(lines.firstOrNull() == "BUYLOG\t1") { "지원하지 않는 BuyLog 백업 파일입니다." }
        val methods = mutableListOf<PaymentMethodEntity>()
        val payments = mutableListOf<PaymentEntity>()
        val events = mutableListOf<NotificationEventEntity>()
        val tags = mutableListOf<TagEntity>()
        val paymentTags = mutableListOf<PaymentTagCrossRef>()
        val rules = mutableListOf<AutoTagRuleEntity>()
        val checks = mutableListOf<MissedPaymentCheckEntity>()

        lines.drop(1).forEachIndexed { index, line ->
            val values = line.split('\t')
            try {
                when (values.firstOrNull()) {
                    "METHOD" -> {
                        requireSize(values, 8)
                        methods += PaymentMethodEntity(values[1].long(), values[2].decoded(), values[3].long(), values[4].boolean(), values[5].decoded(), values[6].int(), values[7].long())
                    }
                    "PAYMENT" -> {
                        requireSize(values, 10)
                        payments += PaymentEntity(values[1].long(), values[2].long(), values[3].decoded(), values[4].long(), values[5].long(), values[6].decoded(), values[7].long(), values[8].long(), values[9].boolean())
                    }
                    "EVENT" -> {
                        requireSize(values, 20)
                        events += NotificationEventEntity(
                            id = values[1].long(), sourceType = values[2].decoded(), packageName = values[3].decoded(), appName = values[4].decoded(),
                            title = values[5].decoded(), content = values[6].decoded(), rawText = values[7].decoded(), receivedAt = values[8].long(), createdAt = values[9].long(),
                            parseStatus = values[10].decoded(), parsedAmount = values[11].nullableLong(), parsedMerchant = values[12].nullableDecoded(),
                            parsedPaidAt = values[13].nullableLong(), parsedPaymentMethod = values[14].nullableDecoded(), parseMessage = values[15].decoded(),
                            registrationStatus = values[16].decoded(), duplicateConfidence = values[17].decoded(), paymentId = values[18].nullableLong(), suggestedPaymentId = values[19].nullableLong(),
                        )
                    }
                    "TAG" -> {
                        requireSize(values, 5)
                        tags += TagEntity(values[1].long(), values[2].decoded(), values[3].boolean(), values[4].long())
                    }
                    "PAYMENT_TAG" -> {
                        requireSize(values, 3)
                        paymentTags += PaymentTagCrossRef(values[1].long(), values[2].long())
                    }
                    "AUTO_TAG" -> {
                        requireSize(values, 5)
                        rules += AutoTagRuleEntity(values[1].long(), values[2].decoded(), values[3].long(), values[4].long())
                    }
                    "MISSED" -> {
                        requireSize(values, 9)
                        checks += MissedPaymentCheckEntity(values[1].long(), values[2].long(), values[3].int(), values[4].int(), values[5].int(), values[6].decoded(), values[7].long(), values[8].nullableLong())
                    }
                    else -> error("알 수 없는 레코드입니다.")
                }
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("백업 파일 ${index + 2}번째 줄이 올바르지 않습니다.", error)
            }
        }
        return BuyLogBackupSnapshot(methods, payments, events, tags, paymentTags, rules, checks).also(::validate)
    }

    fun validate(snapshot: BuyLogBackupSnapshot) {
        requireUnique(snapshot.paymentMethods.map { it.id }, "결제수단")
        requireUnique(snapshot.payments.map { it.id }, "결제")
        requireUnique(snapshot.notificationEvents.map { it.id }, "알림 원문")
        requireUnique(snapshot.tags.map { it.id }, "태그")
        requireUnique(snapshot.autoTagRules.map { it.id }, "자동 태그 규칙")
        requireUnique(snapshot.missedPaymentChecks.map { it.id }, "누락 확인")
        requireUnique(snapshot.paymentTags.map { it.paymentId to it.tagId }, "결제 태그")
        requireUnique(snapshot.autoTagRules.map { it.phrase to it.tagId }, "자동 태그 규칙 조합")
        requireUnique(snapshot.missedPaymentChecks.map { it.dateEpochDay }, "누락 확인 날짜")
        requireUnique(snapshot.paymentMethods.map { it.name }, "결제수단 이름")
        requireUnique(snapshot.tags.map { it.name }, "태그 이름")
        require(snapshot.paymentMethods.all { it.id > 0 && it.name.isNotBlank() && it.periodStartDay in 1..31 && it.performanceTarget >= 0 }) { "결제수단 데이터가 올바르지 않습니다." }
        require(snapshot.payments.all { it.id > 0 && it.amount > 0 && it.merchant.isNotBlank() }) { "결제 데이터가 올바르지 않습니다." }
        require(snapshot.notificationEvents.all { it.id > 0 }) { "알림 원문 데이터가 올바르지 않습니다." }
        require(snapshot.tags.all { it.id > 0 && it.name.isNotBlank() }) { "태그 데이터가 올바르지 않습니다." }
        val methodIds = snapshot.paymentMethods.mapTo(mutableSetOf()) { it.id }
        val paymentIds = snapshot.payments.mapTo(mutableSetOf()) { it.id }
        val tagIds = snapshot.tags.mapTo(mutableSetOf()) { it.id }
        require(snapshot.payments.all { it.paymentMethodId in methodIds }) { "결제의 결제수단 참조가 올바르지 않습니다." }
        require(snapshot.paymentTags.all { it.paymentId in paymentIds && it.tagId in tagIds }) { "결제 태그 참조가 올바르지 않습니다." }
        require(snapshot.autoTagRules.all { it.id > 0 && it.phrase.isNotBlank() && it.tagId in tagIds }) { "자동 태그 규칙 참조가 올바르지 않습니다." }
        require(snapshot.notificationEvents.all { (it.paymentId == null || it.paymentId in paymentIds) && (it.suggestedPaymentId == null || it.suggestedPaymentId in paymentIds) }) { "알림 원문의 결제 참조가 올바르지 않습니다." }
        require(snapshot.missedPaymentChecks.all { it.id > 0 && it.startHour in 0..22 && it.endHour == it.startHour + 2 && it.probabilityPercent in 0..100 }) { "누락 확인 데이터가 올바르지 않습니다." }
    }

    private fun StringBuilder.record(vararg values: Any) {
        appendLine(values.joinToString("\t"))
    }

    private fun text(value: String): String = "!" + Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun nullableText(value: String?): String = value?.let(::text) ?: NULL
    private fun nullableLong(value: Long?): String = value?.toString() ?: NULL
    private fun bool(value: Boolean): Int = if (value) 1 else 0
    private fun String.decoded(): String {
        require(startsWith("!"))
        return String(Base64.getUrlDecoder().decode(drop(1)), StandardCharsets.UTF_8)
    }
    private fun String.nullableDecoded(): String? = if (this == NULL) null else decoded()
    private fun String.long(): Long = toLong()
    private fun String.nullableLong(): Long? = if (this == NULL) null else toLong()
    private fun String.int(): Int = toInt()
    private fun String.boolean(): Boolean = when (this) { "1" -> true; "0" -> false; else -> error("잘못된 Boolean 값입니다.") }
    private fun requireSize(values: List<String>, expected: Int) = require(values.size == expected)
    private fun <T> requireUnique(values: List<T>, label: String) = require(values.size == values.toSet().size) { "$label 값이 중복되었습니다." }
    private const val NULL = "~"
}
