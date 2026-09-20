package com.moodnotes.app.ui.mood

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.DiaryEntry
import com.moodnotes.app.data.Mood
import com.moodnotes.app.data.MoodRecord

/**
 * 心情的展示模型：统一内置心情与自定义心情（Emoji 或图片图标）。
 * 内置心情名称存资源 ID（支持双语），自定义心情存明文名。
 */
@Immutable
data class MoodVisual(
    @StringRes val labelRes: Int = 0,
    val label: String? = null,
    val emoji: String? = null,
    val imagePath: String? = null,
    val container: Color,
    val onContainer: Color,
) {
    val isImage: Boolean get() = !imagePath.isNullOrBlank()
}

/** 在 Composable 中解析显示名。 */
@Composable
fun MoodVisual.displayLabel(): String =
    if (label != null) label else stringResource(labelRes)

fun Mood.visual(dark: Boolean): MoodVisual = MoodVisual(
    labelRes = labelRes,
    emoji = emoji,
    imagePath = null,
    container = if (dark) darkContainer else lightContainer,
    onContainer = if (dark) darkOnContainer else lightOnContainer,
)

fun CustomMood.visual(dark: Boolean): MoodVisual {
    val container = Color(containerColor)
    val on = if (container.luminance() > 0.45f) Color(0xFF1C1B1F) else Color.White
    return MoodVisual(
        label = name,
        emoji = iconValue.takeIf { iconType == "emoji" },
        imagePath = iconValue.takeIf { iconType == "image" },
        container = container,
        onContainer = on,
    )
}

fun MoodRecord.visual(customMoods: List<CustomMood>, dark: Boolean): MoodVisual =
    customMoodId?.let { id -> customMoods.firstOrNull { it.id == id }?.visual(dark) }
        ?: Mood.fromId(moodId).visual(dark)

fun DiaryEntry.visual(customMoods: List<CustomMood>, dark: Boolean): MoodVisual? =
    customMoodId?.let { id -> customMoods.firstOrNull { it.id == id }?.visual(dark) }
        ?: moodId?.let { Mood.fromId(it).visual(dark) }
