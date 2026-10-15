package com.example.buylog.data

import com.example.buylog.domain.MonthRange
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.ZoneId

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun observeCurrentMonthPayments(): Flow<List<PaymentWithMethod>> {
        val range = MonthRange.containing(clock.instant(), zoneId)
        return paymentDao.observePayments(range.fromInclusive, range.toExclusive)
    }

    fun observeAllPayments(): Flow<List<PaymentWithMethod>> = paymentDao.observeAllPayments()

    fun observePaymentMethods(): Flow<List<PaymentMethodEntity>> = paymentMethodDao.observeAll()

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
}

data class PaymentDraft(
    val amount: Long,
    val merchant: String,
    val paidAt: Long,
    val paymentMethodId: Long,
    val memo: String,
)
