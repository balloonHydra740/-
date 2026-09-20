package com.moodnotes.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 主题册：用户保存的自定义主题色（种子色 ARGB Long）。
 */
@Entity(tableName = "saved_themes")
data class SavedTheme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val seedColor: Long,
    val sortOrder: Int,
    val createdAt: Long,
)
