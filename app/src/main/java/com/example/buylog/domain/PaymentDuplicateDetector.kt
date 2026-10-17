package com.example.buylog.domain

data class ExistingPaymentForDuplicate(
    val paymentId: Long,
    val amount: Long,
    val merchant: String,
    val paidAt: Long,
    val linkedSourceTypes: Set<String>,
)

enum class DuplicateDecision { REGISTER_NEW, LINK_HIGH, REVIEW_MEDIUM }

enum class CandidateRegistrationStatus {
    NOT_ELIGIBLE,
    PENDING,
    REGISTERED,
    DUPLICATE_CANDIDATE,
    NEEDS_PAYMENT_METHOD,
    HISTORICAL_CANDIDATE,
}

enum class DuplicateConfidence { NONE, HIGH, MEDIUM, LOW }

object PaymentDuplicateDetector {
    private const val duplicateWindowMillis = 5 * 60 * 1000L

    fun decide(candidate: PaymentCandidate, existing: List<ExistingPaymentForDuplicate>): Pair<DuplicateDecision, Long?> {
        val match = existing.firstOrNull {
            it.amount == candidate.amount &&
                normalize(it.merchant) == normalize(candidate.merchant) &&
                kotlin.math.abs(it.paidAt - candidate.paidAt) <= duplicateWindowMillis
        } ?: return DuplicateDecision.REGISTER_NEW to null

        return if (match.linkedSourceTypes.isNotEmpty() && candidate.sourceType !in match.linkedSourceTypes) {
            DuplicateDecision.LINK_HIGH to match.paymentId
        } else {
            DuplicateDecision.REVIEW_MEDIUM to match.paymentId
        }
    }

    private fun normalize(value: String): String = value.lowercase().filter(Char::isLetterOrDigit)
}
