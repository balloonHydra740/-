package com.moodnotes.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedThemeDao {

    @Query("SELECT * FROM saved_themes ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<SavedTheme>>

    @Query("SELECT * FROM saved_themes WHERE id = :id")
    suspend fun getById(id: Long): SavedTheme?

    @Upsert
    suspend fun upsert(theme: SavedTheme)

    @Query("DELETE FROM saved_themes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT MAX(sortOrder) FROM saved_themes")
    suspend fun maxSortOrder(): Int?
}
