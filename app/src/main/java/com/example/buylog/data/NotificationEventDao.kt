package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {
    @Query("SELECT * FROM notification_events ORDER BY receivedAt DESC, id DESC LIMIT 200")
    fun observeLatest(): Flow<List<NotificationEventEntity>>

    @Insert
    suspend fun insert(event: NotificationEventEntity): Long

    @Query("SELECT * FROM notification_events WHERE parseStatus = 'UNPROCESSED'")
    suspend fun getUnprocessed(): List<NotificationEventEntity>

    @Query(
        """
        UPDATE notification_events
        SET parseStatus = :status,
            parsedAmount = :amount,
            parsedMerchant = :merchant,
            parsedPaidAt = :paidAt,
            parsedPaymentMethod = :paymentMethod,
            parseMessage = :message
        WHERE id = :id
        """,
    )
    suspend fun updateParseResult(
        id: Long,
        status: String,
        amount: Long?,
        merchant: String?,
        paidAt: Long?,
        paymentMethod: String?,
        message: String,
    )

    @Query("SELECT * FROM notification_events WHERE registrationStatus = 'PENDING'")
    suspend fun getPendingRegistrations(): List<NotificationEventEntity>

    @Query("SELECT sourceType FROM notification_events WHERE paymentId = :paymentId")
    suspend fun getLinkedSourceTypes(paymentId: Long): List<String>

    @Query(
        """
        UPDATE notification_events SET registrationStatus = :status, duplicateConfidence = :confidence,
        paymentId = :paymentId, suggestedPaymentId = :suggestedPaymentId WHERE id = :id
        """,
    )
    suspend fun updateRegistration(
        id: Long, status: String, confidence: String, paymentId: Long?, suggestedPaymentId: Long?,
    )
}
