package com.example.buylog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_events",
    indices = [Index("receivedAt"), Index("packageName")],
)
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val rawText: String,
    val receivedAt: Long,
    val createdAt: Long,
)
