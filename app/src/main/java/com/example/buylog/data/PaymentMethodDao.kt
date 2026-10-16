package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): PaymentMethodEntity?

    @Query("SELECT * FROM payment_methods WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PaymentMethodEntity?

    @Query("UPDATE payment_methods SET isCard = :isCard, cardCompany = :company, periodStartDay = :startDay, performanceTarget = :target WHERE id = :id")
    suspend fun updateCardSettings(id: Long, isCard: Boolean, company: String, startDay: Int, target: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(method: PaymentMethodEntity): Long

    @Query("SELECT COUNT(*) FROM payments WHERE paymentMethodId = :methodId")
    suspend fun countPayments(methodId: Long): Int

    @Query("UPDATE payments SET paymentMethodId = :replacementMethodId, updatedAt = :updatedAt WHERE paymentMethodId = :methodId")
    suspend fun reassignPayments(methodId: Long, replacementMethodId: Long, updatedAt: Long)

    @Query("DELETE FROM payment_methods WHERE id = :methodId")
    suspend fun deleteMethod(methodId: Long)

    @Transaction
    suspend fun deleteWithReassignment(methodId: Long, replacementMethodId: Long, updatedAt: Long): Int {
        val paymentCount = countPayments(methodId)
        reassignPayments(methodId, replacementMethodId, updatedAt)
        deleteMethod(methodId)
        return paymentCount
    }
}
