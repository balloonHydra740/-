package com.moodnotes.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.DiaryEntry
import com.moodnotes.app.data.Mood
import com.moodnotes.app.data.MoodRecord
import com.moodnotes.app.data.MoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class DayDetail(
    val date: LocalDate,
    val mood: MoodRecord?,
    val diaries: List<DiaryEntry>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repo: MoodRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    val currentMonth: StateFlow<YearMonth> = month.asStateFlow()

    val monthMoods: StateFlow<List<MoodRecord>> = month.flatMapLatest { m ->
        repo.observeMoodsRange(m.atDay(1).toEpochDay(), m.atEndOfMonth().toEpochDay())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthDiaries: StateFlow<List<DiaryEntry>> = month.flatMapLatest { m ->
        repo.observeDiariesRange(m.atDay(1).toEpochDay(), m.atEndOfMonth().toEpochDay())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customMoods: StateFlow<List<CustomMood>> = repo.observeCustomMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedDetail: StateFlow<DayDetail?> = selectedDay.flatMapLatest { day ->
        if (day == null) {
            flowOf(null)
        } else {
            combine(
                repo.observeMood(day.toEpochDay()),
                repo.observeDiariesByDate(day.toEpochDay()),
            ) { mood, diaries -> DayDetail(day, mood, diaries) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun prevMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }
    fun backToToday() = month.update { YearMonth.now() }

    fun selectDay(date: LocalDate) = selectedDay.update { date }
    fun dismissDay() = selectedDay.update { null }

    fun saveMood(date: LocalDate, moodId: Int, customMoodId: Long?, intensity: Int, note: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repo.getMood(date.toEpochDay())
            repo.saveMood(
                MoodRecord(
                    date = date.toEpochDay(),
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

    companion object {
        /** 生成当月 6 周 × 7 天的网格日期（支持周一/周日起始）。 */
        fun weeksOf(month: YearMonth, startMonday: Boolean): List<List<LocalDate>> {
            val first = month.atDay(1)
            val start = if (startMonday) {
                first.minusDays(((first.dayOfWeek.value + 6) % 7).toLong())
            } else {
                first.minusDays((first.dayOfWeek.value % 7).toLong())
            }
            return (0 until 6).map { week ->
                (0 until 7).map { day -> start.plusDays(week * 7L + day) }
            }
        }
    }
}
