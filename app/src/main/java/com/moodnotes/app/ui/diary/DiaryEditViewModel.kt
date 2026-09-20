package com.moodnotes.app.ui.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.DiaryEntry
import com.moodnotes.app.data.MoodRepository
import com.moodnotes.app.util.Dates
import com.moodnotes.app.util.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryEditViewModel(
    private val repo: MoodRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L
    private val dateEpoch: Long =
        savedStateHandle.get<Long>("date")?.takeIf { it > 0L } ?: Dates.todayEpochDay()

    val isNew: Boolean = entryId == 0L
    val dateLabel: String = Dates.fullDate(Dates.fromEpochDay(dateEpoch))

    var title by mutableStateOf("")
        private set
    var content by mutableStateOf("")
        private set
    var moodId by mutableStateOf<Int?>(null)
        private set
    var customMoodId by mutableStateOf<Long?>(null)
        private set
    var images by mutableStateOf<List<String>>(emptyList())
        private set
    var videos by mutableStateOf<List<String>>(emptyList())
        private set
    var isEditingDraft by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set

    /** 编辑期间被移除、但尚未物理删除的媒体路径；保存/删除时才真正清理。 */
    private val removedImages = mutableSetOf<String>()
    private val removedVideos = mutableSetOf<String>()

    val customMoods: StateFlow<List<CustomMood>> = repo.observeCustomMoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.getDiary(entryId)?.let { entry ->
                    title = entry.title
                    content = entry.content
                    moodId = entry.moodId
                    customMoodId = entry.customMoodId
                    images = entry.imageList()
                    videos = entry.videoList()
                    isEditingDraft = entry.isDraft
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        title = value
    }

    fun onContentChange(value: String) {
        content = value
    }

    fun onMoodSelect(value: Int?) {
        moodId = value
        customMoodId = null
    }

    fun onCustomMoodSelect(value: Long?) {
        customMoodId = value
        moodId = null
    }

    fun addImages(paths: List<String>) {
        images = (images + paths).distinct()
    }

    /** 只从列表移除并记录，等保存/删除时统一删文件（中途退出媒体不丢）。 */
    fun removeImage(path: String) {
        images = images - path
        removedImages.add(path)
    }

    fun addVideos(paths: List<String>) {
        videos = (videos + paths).distinct()
    }

    fun removeVideo(path: String) {
        videos = videos - path
        removedVideos.add(path)
    }

    /** publish=true 发布；false 存入草稿箱。 */
    fun save(publish: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = if (isNew) null else repo.getDiary(entryId)
            repo.saveDiary(
                DiaryEntry(
                    id = entryId,
                    date = dateEpoch,
                    title = title.trim(),
                    content = content.trim(),
                    moodId = moodId,
                    customMoodId = customMoodId,
                    images = images.joinToString("\n"),
                    videos = videos.joinToString("\n"),
                    isDraft = !publish,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            // 已不再被引用的媒体此时才物理删除（IO 线程）
            val staleImages = removedImages.filter { it !in images }
            val staleVideos = removedVideos.filter { it !in videos }
            withIoContext {
                staleImages.forEach(ImageStore::delete)
                staleVideos.forEach(ImageStore::delete)
            }
            removedImages.clear()
            removedVideos.clear()
            saved = true
        }
    }

    fun delete() {
        viewModelScope.launch {
            if (!isNew) {
                val entry = repo.getDiary(entryId)
                val toDelete = entry?.imageList().orEmpty() + entry?.videoList().orEmpty()
                repo.deleteDiary(entryId)
                withIoContext { toDelete.forEach(ImageStore::delete) }
            }
            saved = true
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

    private suspend fun withIoContext(block: suspend () -> Unit) {
        kotlinx.coroutines.withContext(Dispatchers.IO) { block() }
    }
}
