package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Transaction
    @Query("SELECT * FROM payments ORDER BY paidAt DESC, id DESC")
    fun observeAllPayments(): Flow<List<PaymentWithMethod>>

    @Transaction
    @Query("SELECT * FROM payments WHERE paidAt >= :fromInclusive AND paidAt < :toExclusive ORDER BY paidAt DESC, id DESC")
    fun observePayments(fromInclusive: Long, toExclusive: Long): Flow<List<PaymentWithMethod>>

    @Insert
    suspend fun insert(payment: PaymentEntity): Long

    @Update
    suspend fun update(payment: PaymentEntity)

    @Delete
    suspend fun delete(payment: PaymentEntity)

}
