package com.moodnotes.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.MoodRecord
import com.moodnotes.app.data.MoodRepository
import com.moodnotes.app.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class DistributionEntry(
    val moodId: Int,
    val customMoodId: Long?,
    val count: Int,
)

data class StatsUiState(
    val streak: Int = 0,
    val monthCount: Int = 0,
    val totalCount: Int = 0,
    val distribution: List<DistributionEntry> = emptyList(),
    val avgIntensity: Double = 0.0,
    val last7: List<Pair<LocalDate, MoodRecord?>> = emptyList(),
)

class StatsViewModel(private val repo: MoodRepository) : ViewModel() {

    val customMoods: StateFlow<List<CustomMood>> = repo.observeCustomMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 只依赖心情记录流；计算放 Default 线程，结果不变不发射。 */
    val state: StateFlow<StatsUiState> = repo.observeAllMoods()
        .map { moods -> compute(moods) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    private fun compute(moods: List<MoodRecord>): StatsUiState {
        val byDay = moods.associateBy { it.date }
        val today = Dates.today()

        var streak = 0
        var cursor = today
        if (byDay[cursor.toEpochDay()] == null) cursor = cursor.minusDays(1)
        while (byDay[cursor.toEpochDay()] != null) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val currentMonth = YearMonth.now()
        val monthCount = moods.count {
            YearMonth.from(Dates.fromEpochDay(it.date)) == currentMonth
        }

        val distMap = mutableMapOf<Pair<Int, Long?>, Int>()
        moods.forEach { rec ->
            val key = rec.moodId to rec.customMoodId
            distMap[key] = (distMap[key] ?: 0) + 1
        }
        val distribution = distMap.entries
            .map { (key, value) -> DistributionEntry(key.first, key.second, value) }
            .sortedWith(compareByDescending<DistributionEntry> { it.count }.thenBy { it.moodId })

        val avgIntensity = if (moods.isEmpty()) 0.0
        else moods.sumOf { it.intensity }.toDouble() / moods.size

        val last7 = (0L..6L).map { offset ->
            val date = today.minusDays(offset)
            date to byDay[date.toEpochDay()]
        }

        return StatsUiState(
            streak = streak,
            monthCount = monthCount,
            totalCount = moods.size,
            distribution = distribution,
            avgIntensity = avgIntensity,
            last7 = last7,
        )
    }
}
