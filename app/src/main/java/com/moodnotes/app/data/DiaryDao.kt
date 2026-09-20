package com.moodnotes.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :epochDay ORDER BY createdAt DESC")
    fun observeByDate(epochDay: Long): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date >= :from AND date <= :to ORDER BY date, createdAt")
    fun observeRange(from: Long, to: Long): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    fun observeById(id: Long): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: Long): DiaryEntry?

    @Upsert
    suspend fun upsert(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun delete(id: Long)
}
