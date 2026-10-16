package com.example.buylog.data

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Junction

data class PaymentWithMethod(
    @Embedded val payment: PaymentEntity,
    @Relation(
        parentColumn = "paymentMethodId",
        entityColumn = "id",
    )
    val paymentMethod: PaymentMethodEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PaymentTagCrossRef::class,
            parentColumn = "paymentId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
)
