package com.moodnotes.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 日记条目。images / videos 为换行分隔的本地媒体路径列表；isDraft 为草稿标记。
 */
@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: Long,
    val title: String,
    val content: String,
    val moodId: Int? = null,
    val customMoodId: Long? = null,
    val images: String = "",
    val videos: String = "",
    val isDraft: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun imageList(): List<String> =
        if (images.isBlank()) emptyList() else images.split('\n').filter { it.isNotBlank() }

    fun videoList(): List<String> =
        if (videos.isBlank()) emptyList() else videos.split('\n').filter { it.isNotBlank() }
}