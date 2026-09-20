package com.moodnotes.app.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.moodnotes.app.R

/**
 * 心情目录：8 种心情，含 Emoji、名称与主题色。
 */
enum class Mood(
    val id: Int,
    val emoji: String,
    @StringRes val labelRes: Int,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
) {
    JOY(0, "😄", R.string.mood_joy, Color(0xFFFFE9B8), Color(0xFF3E2E00), Color(0xFF5D4A00), Color(0xFFFFE9B8)),
    SURPRISED(1, "🤩", R.string.mood_surprised, Color(0xFFFFD9E4), Color(0xFF3E0020), Color(0xFF7A2A4E), Color(0xFFFFD9E4)),
    CALM(2, "😌", R.string.mood_calm, Color(0xFFC9F2D8), Color(0xFF00391F), Color(0xFF1D5C3B), Color(0xFFC9F2D8)),
    NEUTRAL(3, "😐", R.string.mood_neutral, Color(0xFFE2E6EC), Color(0xFF2B333D), Color(0xFF3D4653), Color(0xFFE2E6EC)),
    TIRED(4, "🥱", R.string.mood_tired, Color(0xFFE9E0FF), Color(0xFF2C1B5C), Color(0xFF503C85), Color(0xFFE9E0FF)),
    ANXIOUS(5, "😰", R.string.mood_anxious, Color(0xFFFFE0CC), Color(0xFF472000), Color(0xFF7C3F1E), Color(0xFFFFE0CC)),
    SAD(6, "😢", R.string.mood_sad, Color(0xFFD8E7FF), Color(0xFF0F2F5E), Color(0xFF264E80), Color(0xFFD8E7FF)),
    ANGRY(7, "😠", R.string.mood_angry, Color(0xFFFFDAD5), Color(0xFF3E0E0C), Color(0xFF7E2620), Color(0xFFFFDAD5)),
    ;

    companion object {
        fun fromId(id: Int): Mood = entries.firstOrNull { it.id == id } ?: NEUTRAL
    }
}
