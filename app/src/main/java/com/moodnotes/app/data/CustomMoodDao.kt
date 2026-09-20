package com.moodnotes.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomMoodDao {

    @Query("SELECT * FROM custom_moods ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<CustomMood>>

    @Query("SELECT * FROM custom_moods WHERE id = :id")
    suspend fun getById(id: Long): CustomMood?

    @Upsert
    suspend fun upsert(mood: CustomMood)

    @Query("SELECT MAX(sortOrder) FROM custom_moods")
    suspend fun maxSortOrder(): Int?

    @Query("DELETE FROM custom_moods WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE mood_records SET customMoodId = NULL WHERE customMoodId = :id")
    suspend fun clearMoodRefs(id: Long)

    @Query("UPDATE diary_entries SET customMoodId = NULL WHERE customMoodId = :id")
    suspend fun clearDiaryRefs(id: Long)
}