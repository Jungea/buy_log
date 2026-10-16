package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("UPDATE tags SET name = :name, excludeFromSpending = :exclude WHERE id = :id")
    suspend fun update(id: Long, name: String, exclude: Boolean)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM payment_tags WHERE tagId = :tagId")
    suspend fun clearTagAssignments(tagId: Long)

    @Query("DELETE FROM payment_tags WHERE paymentId = :paymentId")
    suspend fun clearPaymentTags(paymentId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaymentTags(refs: List<PaymentTagCrossRef>)
}
