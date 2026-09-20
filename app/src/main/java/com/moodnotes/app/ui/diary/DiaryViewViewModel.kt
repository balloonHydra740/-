package com.moodnotes.app.ui.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.DiaryEntry
import com.moodnotes.app.data.MoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** 查看模式：只读地观察一篇日记与自定义心情，供 DiaryViewScreen 展示。 */
class DiaryViewViewModel(
    private val repo: MoodRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L

    val entry: StateFlow<DiaryEntry?> = repo.observeDiary(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val customMoods: StateFlow<List<CustomMood>> = repo.observeCustomMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
