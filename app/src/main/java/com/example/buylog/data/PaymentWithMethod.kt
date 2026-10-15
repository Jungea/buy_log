package com.example.buylog.data

import androidx.room.Embedded
import androidx.room.Relation

data class PaymentWithMethod(
    @Embedded val payment: PaymentEntity,
    @Relation(
        parentColumn = "paymentMethodId",
        entityColumn = "id",
    )
    val paymentMethod: PaymentMethodEntity,
)
