package com.example.buylog.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_methods",
    indices = [Index(value = ["name"], unique = true)],
)
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val isCard: Boolean = false,
    @ColumnInfo(defaultValue = "''") val cardCompany: String = "",
    @ColumnInfo(defaultValue = "1") val periodStartDay: Int = 1,
    @ColumnInfo(defaultValue = "0") val performanceTarget: Long = 0,
)
