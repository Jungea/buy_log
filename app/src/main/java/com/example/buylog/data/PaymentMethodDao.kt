package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(method: PaymentMethodEntity): Long
}
