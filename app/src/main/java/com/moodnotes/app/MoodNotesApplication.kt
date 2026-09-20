package com.moodnotes.app

import android.app.Application
import com.moodnotes.app.data.AppDatabase
import com.moodnotes.app.data.MoodRepository
import com.moodnotes.app.data.SettingsRepository

class MoodNotesApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: MoodRepository by lazy {
        MoodRepository(database.moodDao(), database.diaryDao(), database.customMoodDao(), database.savedThemeDao())
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // 同步上次选择的应用语言（或采纳系统侧的 per-app 语言设置）
        SettingsRepository.restoreLanguage(this)
    }
}
