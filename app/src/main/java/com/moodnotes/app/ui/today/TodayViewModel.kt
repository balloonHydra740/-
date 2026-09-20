package com.moodnotes.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.DiaryEntry
import com.moodnotes.app.data.MoodRecord
import com.moodnotes.app.data.MoodRepository
import com.moodnotes.app.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val repo: MoodRepository) : ViewModel() {

    /** 「今天」随时间自动刷新：每 30 秒重算，跨午夜后今日页自动切换到新的一天。 */
    private val todayFlow = flow {
        while (true) {
            emit(Dates.todayEpochDay())
            delay(30_000)
        }
    }

    val todayMood: StateFlow<MoodRecord?> = todayFlow
        .flatMapLatest { repo.observeMood(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val todayDiaries: StateFlow<List<DiaryEntry>> = todayFlow
        .flatMapLatest { repo.observeDiariesByDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentMoods: StateFlow<List<MoodRecord>> = repo.observeRecentMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customMoods: StateFlow<List<CustomMood>> = repo.observeCustomMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun todayDate(): LocalDate = Dates.today()

    fun saveMood(moodId: Int, customMoodId: Long?, intensity: Int, note: String) {
        viewModelScope.launch {
            val epoch = Dates.todayEpochDay()
            val now = System.currentTimeMillis()
            val existing = repo.getMood(epoch)
            repo.saveMood(
                MoodRecord(
                    date = epoch,
                    moodId = moodId,
                    customMoodId = customMoodId,
                    intensity = intensity,
                    note = note.trim(),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun deleteMood() {
        viewModelScope.launch { repo.deleteMood(Dates.todayEpochDay()) }
    }

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
}
