package com.example.buylog.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("paidAt"), Index("paymentMethodId")],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val merchant: String,
    val paidAt: Long,
    val paymentMethodId: Long,
    val memo: String,
    val createdAt: Long,
    val updatedAt: Long,
)
