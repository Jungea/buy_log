package com.example.buylog.data

import com.example.buylog.domain.MonthRange
import com.example.buylog.domain.PaymentNotificationParser
import com.example.buylog.domain.PaymentParseResult
import com.example.buylog.domain.PaymentSourceEvent
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.ZoneId

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val notificationEventDao: NotificationEventDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private val notificationParser = PaymentNotificationParser(zoneId)

    fun observeCurrentMonthPayments(): Flow<List<PaymentWithMethod>> {
        val range = MonthRange.containing(clock.instant(), zoneId)
        return paymentDao.observePayments(range.fromInclusive, range.toExclusive)
    }

    fun observeAllPayments(): Flow<List<PaymentWithMethod>> = paymentDao.observeAllPayments()

    fun observePaymentMethods(): Flow<List<PaymentMethodEntity>> = paymentMethodDao.observeAll()

    fun observeNotificationEvents(): Flow<List<NotificationEventEntity>> =
        notificationEventDao.observeLatest()

    suspend fun savePayment(draft: PaymentDraft, existing: PaymentEntity? = null) {
        require(draft.amount > 0) { "금액은 0원보다 커야 합니다." }
        require(draft.merchant.isNotBlank()) { "사용처를 입력해 주세요." }

        val now = clock.millis()
        val entity = PaymentEntity(
            id = existing?.id ?: 0,
            amount = draft.amount,
            merchant = draft.merchant.trim(),
            paidAt = draft.paidAt,
            paymentMethodId = draft.paymentMethodId,
            memo = draft.memo.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        if (existing == null) paymentDao.insert(entity) else paymentDao.update(entity)
    }

    suspend fun deletePayment(payment: PaymentEntity) = paymentDao.delete(payment)

    suspend fun addPaymentMethod(name: String): Long {
        require(name.isNotBlank()) { "결제수단 이름을 입력해 주세요." }
        return paymentMethodDao.insert(
            PaymentMethodEntity(name = name.trim(), createdAt = clock.millis()),
        )
    }

    suspend fun recordNotificationEvent(draft: NotificationEventDraft) {
        val result = notificationParser.parse(draft.toPaymentSourceEvent())
        notificationEventDao.insert(
            NotificationEventEntity(
                sourceType = draft.sourceType,
                packageName = draft.packageName,
                appName = draft.appName,
                title = draft.title,
                content = draft.content,
                rawText = draft.rawText,
                receivedAt = draft.receivedAt,
                createdAt = clock.millis(),
            ).withParseResult(result),
        )
    }

    suspend fun parseStoredNotificationEvents() {
        notificationEventDao.getUnprocessed().forEach { event ->
            val result = notificationParser.parse(event.toPaymentSourceEvent())
            val candidate = result.candidate
            notificationEventDao.updateParseResult(
                id = event.id,
                status = result.status.name,
                amount = candidate?.amount,
                merchant = candidate?.merchant,
                paidAt = candidate?.paidAt,
                paymentMethod = candidate?.paymentMethod,
                message = result.message,
            )
        }
    }
}

data class PaymentDraft(
    val amount: Long,
    val merchant: String,
    val paidAt: Long,
    val paymentMethodId: Long,
    val memo: String,
)

data class NotificationEventDraft(
    val sourceType: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val rawText: String,
    val receivedAt: Long,
)

private fun NotificationEventDraft.toPaymentSourceEvent(): PaymentSourceEvent = PaymentSourceEvent(
    sourceType = sourceType,
    packageName = packageName,
    appName = appName,
    title = title,
    content = content,
    rawText = rawText,
    receivedAt = receivedAt,
)

private fun NotificationEventEntity.toPaymentSourceEvent(): PaymentSourceEvent = PaymentSourceEvent(
    sourceType = sourceType,
    packageName = packageName,
    appName = appName,
    title = title,
    content = content,
    rawText = rawText,
    receivedAt = receivedAt,
)

private fun NotificationEventEntity.withParseResult(result: PaymentParseResult): NotificationEventEntity {
    val candidate = result.candidate
    return copy(
        parseStatus = result.status.name,
        parsedAmount = candidate?.amount,
        parsedMerchant = candidate?.merchant,
        parsedPaidAt = candidate?.paidAt,
        parsedPaymentMethod = candidate?.paymentMethod,
        parseMessage = result.message,
    )
}
