package com.moodnotes.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {

    @Query("SELECT * FROM mood_records WHERE date = :epochDay")
    fun observeByDate(epochDay: Long): Flow<MoodRecord?>

    @Query("SELECT * FROM mood_records WHERE date = :epochDay")
    suspend fun getByDate(epochDay: Long): MoodRecord?

    @Query("SELECT * FROM mood_records ORDER BY date DESC")
    fun observeAll(): Flow<List<MoodRecord>>

    @Query("SELECT * FROM mood_records WHERE date >= :from AND date <= :to ORDER BY date")
    fun observeRange(from: Long, to: Long): Flow<List<MoodRecord>>

    @Query("SELECT * FROM mood_records ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<MoodRecord>>

    @Upsert
    suspend fun upsert(record: MoodRecord)

    @Query("DELETE FROM mood_records WHERE date = :epochDay")
    suspend fun delete(epochDay: Long)
}
