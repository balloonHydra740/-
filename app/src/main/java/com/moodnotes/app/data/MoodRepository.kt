package com.moodnotes.app.data

import kotlinx.coroutines.flow.Flow

class MoodRepository(
    private val moodDao: MoodDao,
    private val diaryDao: DiaryDao,
    private val customMoodDao: CustomMoodDao,
    private val savedThemeDao: SavedThemeDao,
) {

    fun observeMood(epochDay: Long): Flow<MoodRecord?> = moodDao.observeByDate(epochDay)

    suspend fun getMood(epochDay: Long): MoodRecord? = moodDao.getByDate(epochDay)

    fun observeAllMoods(): Flow<List<MoodRecord>> = moodDao.observeAll()

    fun observeMoodsRange(from: Long, to: Long): Flow<List<MoodRecord>> =
        moodDao.observeRange(from, to)

    fun observeRecentMoods(limit: Int = 14): Flow<List<MoodRecord>> =
        moodDao.observeRecent(limit)

    suspend fun saveMood(record: MoodRecord) = moodDao.upsert(record)

    suspend fun deleteMood(epochDay: Long) = moodDao.delete(epochDay)

    fun observeDiaries(): Flow<List<DiaryEntry>> = diaryDao.observeAll()

    fun observeDiariesByDate(epochDay: Long): Flow<List<DiaryEntry>> =
        diaryDao.observeByDate(epochDay)

    fun observeDiariesRange(from: Long, to: Long): Flow<List<DiaryEntry>> =
        diaryDao.observeRange(from, to)

    fun observeDiary(id: Long): Flow<DiaryEntry?> = diaryDao.observeById(id)

    suspend fun getDiary(id: Long): DiaryEntry? = diaryDao.getById(id)

    suspend fun saveDiary(entry: DiaryEntry) = diaryDao.upsert(entry)

    suspend fun deleteDiary(id: Long) = diaryDao.delete(id)

    fun observeCustomMoods(): Flow<List<CustomMood>> = customMoodDao.observeAll()

    suspend fun getCustomMood(id: Long): CustomMood? = customMoodDao.getById(id)

    suspend fun saveCustomMood(mood: CustomMood) = customMoodDao.upsert(mood)

    /** 删除自定义心情：解除相关记录引用后再删除。 */
    suspend fun deleteCustomMood(id: Long) {
        customMoodDao.clearMoodRefs(id)
        customMoodDao.clearDiaryRefs(id)
        customMoodDao.delete(id)
    }

    /** 下一个自定义心情排序号（MAX+1，避免并发添加时撞号）。 */
    suspend fun nextCustomMoodSortOrder(): Int = (customMoodDao.maxSortOrder() ?: -1) + 1

    // ---- 主题册 ----

    fun observeSavedThemes(): Flow<List<SavedTheme>> = savedThemeDao.observeAll()

    suspend fun saveSavedTheme(theme: SavedTheme) = savedThemeDao.upsert(theme)

    suspend fun deleteSavedTheme(id: Long) = savedThemeDao.delete(id)

    suspend fun renameSavedTheme(id: Long, name: String) {
        savedThemeDao.getById(id)?.let { savedThemeDao.upsert(it.copy(name = name)) }
    }
}