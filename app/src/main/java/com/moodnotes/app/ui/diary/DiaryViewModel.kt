package com.moodnotes.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.DiaryEntry
import com.moodnotes.app.data.MoodRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn

class DiaryViewModel(private val repo: MoodRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    @OptIn(FlowPreview::class)
    val filteredEntries: StateFlow<List<DiaryEntry>> = combine(
        repo.observeDiaries(),
        _query.debounce(200),
    ) { entries, q ->
        val term = q.trim()
        if (term.isEmpty()) {
            entries
        } else {
            entries.filter {
                it.title.contains(term, ignoreCase = true) ||
                    it.content.contains(term, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customMoods: StateFlow<List<CustomMood>> = repo.observeCustomMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
