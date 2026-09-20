package com.moodnotes.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日一条的心情记录（以日期为主键，重复保存会覆盖当天记录）。
 * moodId 指向内置心情；customMoodId 非空时使用自定义心情。
 */
@Entity(tableName = "mood_records")
data class MoodRecord(
    @PrimaryKey val date: Long,
    val moodId: Int,
    val customMoodId: Long? = null,
    val intensity: Int,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)