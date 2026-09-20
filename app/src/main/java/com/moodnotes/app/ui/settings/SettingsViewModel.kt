package com.moodnotes.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodnotes.app.R
import com.moodnotes.app.data.AppSettings
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.MoodRepository
import com.moodnotes.app.data.SavedTheme
import com.moodnotes.app.data.SettingsRepository
import com.moodnotes.app.util.ImageStore
import com.moodnotes.app.work.ReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** 导出结果（UI 层转文案）。 */
enum class ExportState { IDLE, DONE, FAILED }

class SettingsViewModel(
    private val appContext: Context,
    private val repo: MoodRepository,
    val settingsRepo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings

    val customMoods: StateFlow<List<CustomMood>> = repo.observeCustomMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedThemes: StateFlow<List<SavedTheme>> = repo.observeSavedThemes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var exportState by mutableStateOf(ExportState.IDLE)
        private set
    var reminderPermissionDenied by mutableStateOf(false)
        private set

    fun update(transform: (AppSettings) -> AppSettings) = settingsRepo.update(transform)

    // ---- 自定义心情 ----

    fun saveCustomMood(name: String, iconType: String, iconValue: String, containerColor: Int) {
        viewModelScope.launch {
            repo.saveCustomMood(
                CustomMood(
                    name = name,
                    iconType = iconType,
                    iconValue = iconValue,
                    containerColor = containerColor,
                    sortOrder = repo.nextCustomMoodSortOrder(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteCustomMood(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val mood = repo.getCustomMood(id)
            if (mood?.iconType == "image") ImageStore.delete(mood.iconValue)
            repo.deleteCustomMood(id)
        }
    }

    // ---- 主题册 ----

    fun saveTheme(name: String, seed: Long) {
        viewModelScope.launch {
            val order = (repo.observeSavedThemes().first().maxOfOrNull { it.sortOrder } ?: -1) + 1
            repo.saveSavedTheme(
                SavedTheme(
                    name = name.trim(),
                    seedColor = seed,
                    sortOrder = order,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun renameTheme(id: Long, name: String) {
        viewModelScope.launch { repo.renameSavedTheme(id, name.trim()) }
    }

    fun deleteTheme(id: Long) {
        viewModelScope.launch { repo.deleteSavedTheme(id) }
    }

    /** 应用一个主题册颜色（null 表示回到内置预设）。 */
    fun applySeed(seed: Long?) = settingsRepo.applySeedColor(seed)

    // ---- 每日提醒 ----

    fun setReminder(enabled: Boolean, hour: Int, minute: Int, permissionGranted: Boolean?) {
        update { it.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute) }
        if (enabled && permissionGranted == false) {
            reminderPermissionDenied = true
            return
        }
        reminderPermissionDenied = false
        ReminderWorker.schedule(appContext, enabled, hour, minute)
    }

    // ---- 导出 ----

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            exportState = ExportState.IDLE
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val moods = repo.observeAllMoods().first()
                    val diaries = repo.observeDiaries().first()
                    val customs = repo.observeCustomMoods().first()
                    val version = runCatching {
                        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
                    }.getOrNull() ?: "1.2.0"

                    val json = JSONObject().apply {
                        put("app", "心迹 MoodNotes")
                        put("version", version)
                        put("exportedAt", System.currentTimeMillis())
                        put("moodRecords", JSONArray().apply {
                            moods.forEach { rec ->
                                put(
                                    JSONObject().apply {
                                        put("date", rec.date)
                                        put("moodId", rec.moodId)
                                        put("customMoodId", rec.customMoodId)
                                        put("intensity", rec.intensity)
                                        put("note", rec.note)
                                        put("createdAt", rec.createdAt)
                                        put("updatedAt", rec.updatedAt)
                                    },
                                )
                            }
                        })
                        put("diaryEntries", JSONArray().apply {
                            diaries.forEach { entry ->
                                put(
                                    JSONObject().apply {
                                        put("id", entry.id)
                                        put("date", entry.date)
                                        put("title", entry.title)
                                        put("content", entry.content)
                                        put("moodId", entry.moodId)
                                        put("customMoodId", entry.customMoodId)
                                        put("images", entry.images)
                                        put("isDraft", entry.isDraft)
                                        put("createdAt", entry.createdAt)
                                        put("updatedAt", entry.updatedAt)
                                    },
                                )
                            }
                        })
                        put("customMoods", JSONArray().apply {
                            customs.forEach { mood ->
                                put(
                                    JSONObject().apply {
                                        put("id", mood.id)
                                        put("name", mood.name)
                                        put("iconType", mood.iconType)
                                        put("iconValue", mood.iconValue)
                                        put("containerColor", mood.containerColor)
                                        put("sortOrder", mood.sortOrder)
                                    },
                                )
                            }
                        })
                    }
                    val out = appContext.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("cannot open output")
                    out.use { it.write(json.toString(2).toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            exportState = if (ok) ExportState.DONE else ExportState.FAILED
        }
    }

    companion object {
        fun exportFailedMessage() = R.string.export_failed
    }
}
