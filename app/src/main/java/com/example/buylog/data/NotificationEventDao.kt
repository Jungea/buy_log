package com.example.buylog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationEventDao {
    @Query("SELECT * FROM notification_events ORDER BY receivedAt DESC, id DESC LIMIT 200")
    fun observeLatest(): Flow<List<NotificationEventEntity>>

    @Insert
    suspend fun insert(event: NotificationEventEntity): Long
}
