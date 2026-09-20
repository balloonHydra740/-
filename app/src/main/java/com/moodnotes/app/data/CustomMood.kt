package com.moodnotes.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自定义心情：支持 Emoji 或本地图片作为图标。
 * iconType: "emoji" 或 "image"；iconValue 分别为 Emoji 字符或图片文件绝对路径。
 */
@Entity(tableName = "custom_moods")
data class CustomMood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val iconType: String,
    val iconValue: String,
    val containerColor: Int,
    val sortOrder: Int,
    val createdAt: Long,
)