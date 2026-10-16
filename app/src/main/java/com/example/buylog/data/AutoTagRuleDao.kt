package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoTagRuleDao {
    @Query("SELECT * FROM auto_tag_rules ORDER BY phrase COLLATE NOCASE")
    fun observeAll(): Flow<List<AutoTagRuleEntity>>

    @Query("SELECT * FROM auto_tag_rules")
    suspend fun getAll(): List<AutoTagRuleEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: AutoTagRuleEntity): Long

    @Query("DELETE FROM auto_tag_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM auto_tag_rules WHERE tagId = :tagId")
    suspend fun deleteByTagId(tagId: Long)
}
