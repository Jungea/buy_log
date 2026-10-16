package com.example.buylog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "missed_payment_checks",
    indices = [Index(value = ["dateEpochDay"], unique = true)],
)
data class MissedPaymentCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val startHour: Int,
    val endHour: Int,
    val probabilityPercent: Int,
    val status: String,
    val createdAt: Long,
    val respondedAt: Long? = null,
)
