package com.example.buylog.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_events",
    indices = [Index("receivedAt"), Index("packageName")],
)
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val rawText: String,
    val receivedAt: Long,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'UNPROCESSED'")
    val parseStatus: String = "UNPROCESSED",
    val parsedAmount: Long? = null,
    val parsedMerchant: String? = null,
    val parsedPaidAt: Long? = null,
    val parsedPaymentMethod: String? = null,
    @ColumnInfo(defaultValue = "''")
    val parseMessage: String = "",
    @ColumnInfo(defaultValue = "'NOT_ELIGIBLE'")
    val registrationStatus: String = "NOT_ELIGIBLE",
    @ColumnInfo(defaultValue = "'NONE'")
    val duplicateConfidence: String = "NONE",
    val paymentId: Long? = null,
    val suggestedPaymentId: Long? = null,
)
