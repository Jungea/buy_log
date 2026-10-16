package com.example.buylog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_tag_rules",
    indices = [Index(value = ["phrase", "tagId"], unique = true), Index("tagId")],
)
data class AutoTagRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phrase: String,
    val tagId: Long,
    val createdAt: Long,
)
