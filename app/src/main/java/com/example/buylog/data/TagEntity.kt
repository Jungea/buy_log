package com.example.buylog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val excludeFromSpending: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "payment_tags", primaryKeys = ["paymentId", "tagId"], indices = [Index("tagId")])
data class PaymentTagCrossRef(val paymentId: Long, val tagId: Long)
