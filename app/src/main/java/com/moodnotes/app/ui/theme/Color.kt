package com.moodnotes.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.moodnotes.app.R
import kotlin.math.abs

/**
 * 主题色预设：用户可在设置中切换，所有方案由种子色程序化生成。
 */
data class ThemePreset(@StringRes val nameRes: Int, val seed: Color)

val ThemePresets = listOf(
    ThemePreset(R.string.theme_violet, Color(0xFF6C5CE7)),
    ThemePreset(R.string.theme_blue, Color(0xFF2E7CF6)),
    ThemePreset(R.string.theme_mint, Color(0xFF00A882)),
    ThemePreset(R.string.theme_orange, Color(0xFFF07B3F)),
    ThemePreset(R.string.theme_rose, Color(0xFFE85D8A)),
    ThemePreset(R.string.theme_amber, Color(0xFFE0A500)),
)

/**
 * 背景预设：纯净（使用主题背景色）或柔和渐变。
 */
enum class BackgroundPreset(
    @StringRes val labelRes: Int,
    val light: List<Color>,
    val dark: List<Color>,
) {
    DEFAULT(R.string.bg_plain, emptyList(), emptyList()),
    SUNRISE(R.string.bg_sunrise, listOf(Color(0xFFFFF3E6), Color(0xFFFFE6F0), Color(0xFFECE6FF)), listOf(Color(0xFF2A2036), Color(0xFF32232F), Color(0xFF251D33))),
    OCEAN(R.string.bg_ocean, listOf(Color(0xFFE2F2FF), Color(0xFFE7F5FF), Color(0xFFF0F8FF)), listOf(Color(0xFF10283A), Color(0xFF122B3A), Color(0xFF0E2232))),
    MINT(R.string.bg_mint, listOf(Color(0xFFE3F7EC), Color(0xFFE9FAF3), Color(0xFFF1FBF6)), listOf(Color(0xFF0E2B21), Color(0xFF102E24), Color(0xFF0C271E))),
    DUSK(R.string.bg_dusk, listOf(Color(0xFFF2EBFF), Color(0xFFEAE2FF), Color(0xFFF5EFFF)), listOf(Color(0xFF201A35), Color(0xFF251E3A), Color(0xFF1C1730))),
}

fun BackgroundPreset.brush(isDark: Boolean): androidx.compose.ui.graphics.Brush? {
    val colors = if (isDark) dark else light
    if (colors.isEmpty()) return null
    return androidx.compose.ui.graphics.Brush.verticalGradient(colors)
}

/** 由种子色生成一套和谐配色。 */
fun schemeFromSeed(seed: Color, dark: Boolean): androidx.compose.material3.ColorScheme {
    val (h0, s0, _) = seed.toHsl()
    val hue = ((h0 % 360f) + 360f) % 360f
    val sat = s0.coerceIn(0.18f, 0.9f)

    fun tone(l: Float, s: Float = sat): Color = hslToColor(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    fun hueTone(offset: Float, l: Float, s: Float = sat): Color =
        hslToColor((hue + offset + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))

    return if (!dark) {
        val primary = seed
        val onPrimary = if (seed.luminance() > 0.55f) Color(0xFF2B2300) else Color.White
        val secondary = hueTone(36f, 0.40f, sat * 0.75f)
        val tertiary = hueTone(-40f, 0.42f, sat * 0.8f)
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = tone(0.90f, sat * 0.55f),
            onPrimaryContainer = tone(0.14f, sat * 0.9f),
            secondary = secondary,
            onSecondary = if (secondary.luminance() > 0.5f) Color(0xFF232420) else Color.White,
            secondaryContainer = hueTone(36f, 0.92f, sat * 0.45f),
            onSecondaryContainer = hueTone(36f, 0.14f, sat * 0.8f),
            tertiary = tertiary,
            onTertiary = if (tertiary.luminance() > 0.5f) Color(0xFF232018) else Color.White,
            tertiaryContainer = hueTone(-40f, 0.92f, sat * 0.45f),
            onTertiaryContainer = hueTone(-40f, 0.14f, sat * 0.8f),
            background = tone(0.985f, sat * 0.12f),
            onBackground = tone(0.08f, sat * 0.3f),
            surface = tone(0.985f, sat * 0.12f),
            onSurface = tone(0.08f, sat * 0.3f),
            surfaceVariant = tone(0.92f, sat * 0.22f),
            onSurfaceVariant = tone(0.34f, sat * 0.25f),
            outline = tone(0.48f, sat * 0.3f),
            outlineVariant = tone(0.85f, sat * 0.2f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = tone(0.97f, sat * 0.16f),
            surfaceContainer = tone(0.945f, sat * 0.18f),
            surfaceContainerHigh = tone(0.92f, sat * 0.2f),
            surfaceContainerHighest = tone(0.88f, sat * 0.2f),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
        )
    } else {
        val primary = tone(0.80f, sat * 0.9f)
        val secondary = hueTone(36f, 0.78f, sat * 0.7f)
        val tertiary = hueTone(-40f, 0.76f, sat * 0.7f)
        darkColorScheme(
            primary = primary,
            onPrimary = tone(0.22f, sat * 0.9f),
            primaryContainer = tone(0.36f, sat * 0.75f),
            onPrimaryContainer = tone(0.92f, sat * 0.5f),
            secondary = secondary,
            onSecondary = hueTone(36f, 0.20f, sat * 0.8f),
            secondaryContainer = hueTone(36f, 0.32f, sat * 0.6f),
            onSecondaryContainer = hueTone(36f, 0.90f, sat * 0.5f),
            tertiary = tertiary,
            onTertiary = hueTone(-40f, 0.20f, sat * 0.8f),
            tertiaryContainer = hueTone(-40f, 0.30f, sat * 0.6f),
            onTertiaryContainer = hueTone(-40f, 0.90f, sat * 0.5f),
            background = tone(0.065f, sat * 0.25f),
            onBackground = tone(0.90f, sat * 0.2f),
            surface = tone(0.065f, sat * 0.25f),
            onSurface = tone(0.90f, sat * 0.2f),
            surfaceVariant = tone(0.22f, sat * 0.3f),
            onSurfaceVariant = tone(0.78f, sat * 0.2f),
            outline = tone(0.55f, sat * 0.25f),
            outlineVariant = tone(0.24f, sat * 0.28f),
            surfaceContainerLowest = tone(0.05f, sat * 0.3f),
            surfaceContainerLow = tone(0.085f, sat * 0.28f),
            surfaceContainer = tone(0.11f, sat * 0.28f),
            surfaceContainerHigh = tone(0.15f, sat * 0.3f),
            surfaceContainerHighest = tone(0.19f, sat * 0.3f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
        )
    }
}

private fun Color.toHsl(): FloatArray {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6)
        max == g -> 60f * ((b - r) / d + 2)
        else -> 60f * ((r - g) / d + 4)
    }
    val s = if (d == 0f) 0f else d / (1f - abs(2f * l - 1f))
    return floatArrayOf(h, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m)
}
