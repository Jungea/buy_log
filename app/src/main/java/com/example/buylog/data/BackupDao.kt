package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BackupDao {
    @Query("SELECT * FROM payment_methods ORDER BY id")
    suspend fun getPaymentMethods(): List<PaymentMethodEntity>

    @Query("SELECT * FROM payments ORDER BY id")
    suspend fun getPayments(): List<PaymentEntity>

    @Query("SELECT * FROM notification_events ORDER BY id")
    suspend fun getNotificationEvents(): List<NotificationEventEntity>

    @Query("SELECT * FROM tags ORDER BY id")
    suspend fun getTags(): List<TagEntity>

    @Query("SELECT * FROM payment_tags ORDER BY paymentId, tagId")
    suspend fun getPaymentTags(): List<PaymentTagCrossRef>

    @Query("SELECT * FROM auto_tag_rules ORDER BY id")
    suspend fun getAutoTagRules(): List<AutoTagRuleEntity>

    @Query("SELECT * FROM missed_payment_checks ORDER BY id")
    suspend fun getMissedPaymentChecks(): List<MissedPaymentCheckEntity>

    @Query("DELETE FROM payment_tags")
    suspend fun deletePaymentTags()

    @Query("DELETE FROM auto_tag_rules")
    suspend fun deleteAutoTagRules()

    @Query("DELETE FROM missed_payment_checks")
    suspend fun deleteMissedPaymentChecks()

    @Query("DELETE FROM notification_events")
    suspend fun deleteNotificationEvents()

    @Query("DELETE FROM payments")
    suspend fun deletePayments()

    @Query("DELETE FROM tags")
    suspend fun deleteTags()

    @Query("DELETE FROM payment_methods")
    suspend fun deletePaymentMethods()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethods(values: List<PaymentMethodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(values: List<PaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationEvents(values: List<NotificationEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(values: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentTags(values: List<PaymentTagCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutoTagRules(values: List<AutoTagRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissedPaymentChecks(values: List<MissedPaymentCheckEntity>)
}
