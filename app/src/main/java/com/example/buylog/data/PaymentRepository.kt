package com.example.buylog.data

import com.example.buylog.domain.MonthRange
import com.example.buylog.domain.PaymentNotificationParser
import com.example.buylog.domain.PaymentParseResult
import com.example.buylog.domain.PaymentSourceEvent
import com.example.buylog.domain.PaymentCandidate
import com.example.buylog.domain.PaymentDuplicateDetector
import com.example.buylog.domain.ExistingPaymentForDuplicate
import com.example.buylog.domain.CandidateRegistrationStatus
import com.example.buylog.domain.DuplicateConfidence
import com.example.buylog.domain.DuplicateDecision
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.ZoneId

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val notificationEventDao: NotificationEventDao,
    private val tagDao: TagDao,
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

    fun observeTags(): Flow<List<TagEntity>> = tagDao.observeAll()

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
        val paymentId = if (existing == null) paymentDao.insert(entity) else {
            paymentDao.update(entity)
            existing.id
        }
        tagDao.clearPaymentTags(paymentId)
        tagDao.insertPaymentTags(draft.tagIds.map { PaymentTagCrossRef(paymentId, it) })
    }

    suspend fun deletePayment(payment: PaymentEntity) = paymentDao.delete(payment)

    suspend fun addPaymentMethod(name: String): Long {
        require(name.isNotBlank()) { "결제수단 이름을 입력해 주세요." }
        return paymentMethodDao.insert(
            PaymentMethodEntity(name = name.trim(), createdAt = clock.millis()),
        )
    }

    suspend fun addTag(name: String, exclude: Boolean): Long {
        require(name.isNotBlank()) { "태그 이름을 입력해 주세요." }
        return tagDao.insert(TagEntity(name = name.trim(), excludeFromSpending = exclude, createdAt = clock.millis()))
    }

    suspend fun updateTag(tag: TagEntity, name: String, exclude: Boolean) {
        require(name.isNotBlank()) { "태그 이름을 입력해 주세요." }
        tagDao.update(tag.id, name.trim(), exclude)
    }

    suspend fun deleteTag(tag: TagEntity) {
        tagDao.clearTagAssignments(tag.id)
        tagDao.delete(tag.id)
    }

    suspend fun recordNotificationEvent(draft: NotificationEventDraft) {
        val result = notificationParser.parse(draft.toPaymentSourceEvent())
        val eventId = notificationEventDao.insert(
            NotificationEventEntity(
                sourceType = draft.sourceType,
                packageName = draft.packageName,
                appName = draft.appName,
                title = draft.title,
                content = draft.content,
                rawText = draft.rawText,
                receivedAt = draft.receivedAt,
                createdAt = clock.millis(),
            ).withParseResult(result).withRegistrationPending(),
        )
        if (result.candidate != null) processCandidate(eventId, result.candidate)
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

    suspend fun processPendingCandidates() {
        notificationEventDao.getPendingRegistrations().forEach { event ->
            event.toCandidate()?.let { candidate -> processCandidate(event.id, candidate) }
        }
    }

    suspend fun confirmDuplicateAsSame(event: NotificationEventEntity) {
        require(event.registrationStatus == CandidateRegistrationStatus.DUPLICATE_CANDIDATE.name)
        requireNotNull(event.suggestedPaymentId) { "연결할 기존 결제를 찾을 수 없습니다." }
        notificationEventDao.updateRegistration(
            id = event.id,
            status = CandidateRegistrationStatus.REGISTERED.name,
            confidence = DuplicateConfidence.MEDIUM.name,
            paymentId = event.suggestedPaymentId,
            suggestedPaymentId = null,
        )
    }

    suspend fun confirmDuplicateAsSeparate(event: NotificationEventEntity) {
        require(event.registrationStatus == CandidateRegistrationStatus.DUPLICATE_CANDIDATE.name)
        val candidate = requireNotNull(event.toCandidate()) { "결제 후보 정보가 없습니다." }
        registerAsNewPayment(event.id, candidate)
    }

    private suspend fun processCandidate(eventId: Long, candidate: PaymentCandidate) {
        val methodName = candidate.paymentMethod
        if (methodName == null) {
            notificationEventDao.updateRegistration(
                eventId, CandidateRegistrationStatus.NEEDS_PAYMENT_METHOD.name,
                DuplicateConfidence.NONE.name, null, null,
            )
            return
        }
        val method = findOrCreatePaymentMethod(methodName)
        val matches = paymentDao.findPotentialDuplicates(
            candidate.amount,
            method.id,
            candidate.paidAt - duplicateWindowMillis,
            candidate.paidAt + duplicateWindowMillis,
        ).map { payment ->
            ExistingPaymentForDuplicate(
                paymentId = payment.payment.id,
                amount = payment.payment.amount,
                merchant = payment.payment.merchant,
                paidAt = payment.payment.paidAt,
                linkedSourceTypes = notificationEventDao.getLinkedSourceTypes(payment.payment.id).toSet(),
            )
        }
        val decision = PaymentDuplicateDetector.decide(candidate, matches)
        when (decision.first) {
            DuplicateDecision.REGISTER_NEW -> registerAsNewPayment(eventId, candidate)
            DuplicateDecision.LINK_HIGH -> notificationEventDao.updateRegistration(
                eventId, CandidateRegistrationStatus.REGISTERED.name,
                DuplicateConfidence.HIGH.name, decision.second, null,
            )
            DuplicateDecision.REVIEW_MEDIUM -> notificationEventDao.updateRegistration(
                eventId, CandidateRegistrationStatus.DUPLICATE_CANDIDATE.name,
                DuplicateConfidence.MEDIUM.name, null, decision.second,
            )
        }
    }

    private suspend fun registerAsNewPayment(eventId: Long, candidate: PaymentCandidate) {
        val methodName = requireNotNull(candidate.paymentMethod) { "결제수단을 확인할 수 없습니다." }
        val method = findOrCreatePaymentMethod(methodName)
        val now = clock.millis()
        val paymentId = paymentDao.insert(
            PaymentEntity(
                amount = candidate.amount,
                merchant = candidate.merchant,
                paidAt = candidate.paidAt,
                paymentMethodId = method.id,
                memo = "알림에서 자동 등록",
                createdAt = now,
                updatedAt = now,
            ),
        )
        notificationEventDao.updateRegistration(
            eventId, CandidateRegistrationStatus.REGISTERED.name,
            DuplicateConfidence.LOW.name, paymentId, null,
        )
    }

    private suspend fun findOrCreatePaymentMethod(name: String): PaymentMethodEntity {
        return paymentMethodDao.findByName(name)
            ?: run {
                paymentMethodDao.insert(PaymentMethodEntity(name = name, createdAt = clock.millis()))
                requireNotNull(paymentMethodDao.findByName(name))
            }
    }
}

data class PaymentDraft(
    val amount: Long,
    val merchant: String,
    val paidAt: Long,
    val paymentMethodId: Long,
    val memo: String,
    val tagIds: Set<Long> = emptySet(),
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

private fun NotificationEventEntity.withRegistrationPending(): NotificationEventEntity =
    if (parseStatus == "CANDIDATE") copy(registrationStatus = CandidateRegistrationStatus.PENDING.name) else this

private fun NotificationEventEntity.toCandidate(): PaymentCandidate? {
    if (parseStatus != "CANDIDATE") return null
    val amount = parsedAmount ?: return null
    val merchant = parsedMerchant ?: return null
    val paidAt = parsedPaidAt ?: return null
    return PaymentCandidate(amount, merchant, paidAt, parsedPaymentMethod, sourceType, packageName)
}

private const val duplicateWindowMillis = 5 * 60 * 1000L
