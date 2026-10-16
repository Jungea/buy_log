package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MissedPaymentCheckDao {
    @Query("SELECT COUNT(*) FROM missed_payment_checks WHERE dateEpochDay = :dateEpochDay")
    suspend fun countForDate(dateEpochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(check: MissedPaymentCheckEntity): Long

    @Query("UPDATE missed_payment_checks SET status = :status, respondedAt = :respondedAt WHERE dateEpochDay = :dateEpochDay")
    suspend fun recordResponse(dateEpochDay: Long, status: String, respondedAt: Long)
}
