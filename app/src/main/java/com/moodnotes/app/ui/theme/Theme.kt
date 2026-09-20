package com.moodnotes.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.moodnotes.app.ui.components.EaseInOutQuint
import com.moodnotes.app.ui.components.EaseOutQuint

/** 当前是否处于应用自己的暗色模式（与系统解耦，组件用它取颜色）。 */
val LocalAppDarkTheme = compositionLocalOf { false }

/** 是否开启减少动画。 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * Motion：统一的动画规格工厂。所有页面内动画应经由它取 spec，
 * 「减少动画」开启时自动退化为 1ms 直切，一处开关全局生效。
 */
object Motion {
    private const val SNAP = 1

    @Composable
    fun <T> tween(durationMs: Int, delayMs: Int = 0): androidx.compose.animation.core.TweenSpec<T> {
        val ms = if (LocalReduceMotion.current) SNAP else durationMs
        return androidx.compose.animation.core.tween(ms, delayMs, EaseOutQuint)
    }

    @Composable
    fun <T> tweenInOut(durationMs: Int, delayMs: Int = 0): androidx.compose.animation.core.TweenSpec<T> {
        val ms = if (LocalReduceMotion.current) SNAP else durationMs
        return androidx.compose.animation.core.tween(ms, delayMs, EaseInOutQuint)
    }

    @Composable
    fun <T> spring(
        dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        stiffness: Float = Spring.StiffnessMediumLow,
    ): SpringSpec<T> = androidx.compose.animation.core.spring(
        dampingRatio = if (LocalReduceMotion.current) Spring.DampingRatioNoBouncy else dampingRatio,
        stiffness = if (LocalReduceMotion.current) Spring.StiffnessHigh else stiffness,
    )

    /** 当前的动画时长（减少动画时为 1ms），用于不便泛型化的场景。 */
    @Composable
    fun duration(normalMs: Int): Int = if (LocalReduceMotion.current) SNAP else normalMs
}

/**
 * 对两套 ColorScheme 逐色插值，让主题色/深浅切换时整套颜色平滑过渡。
 */
private fun lerpScheme(from: ColorScheme, to: ColorScheme, fraction: Float): ColorScheme =
    from.copy(
        primary = lerpColor(from.primary, to.primary, fraction),
        onPrimary = lerpColor(from.onPrimary, to.onPrimary, fraction),
        primaryContainer = lerpColor(from.primaryContainer, to.primaryContainer, fraction),
        onPrimaryContainer = lerpColor(from.onPrimaryContainer, to.onPrimaryContainer, fraction),
        inversePrimary = lerpColor(from.inversePrimary, to.inversePrimary, fraction),
        secondary = lerpColor(from.secondary, to.secondary, fraction),
        onSecondary = lerpColor(from.onSecondary, to.onSecondary, fraction),
        secondaryContainer = lerpColor(from.secondaryContainer, to.secondaryContainer, fraction),
        onSecondaryContainer = lerpColor(from.onSecondaryContainer, to.onSecondaryContainer, fraction),
        tertiary = lerpColor(from.tertiary, to.tertiary, fraction),
        onTertiary = lerpColor(from.onTertiary, to.onTertiary, fraction),
        tertiaryContainer = lerpColor(from.tertiaryContainer, to.tertiaryContainer, fraction),
        onTertiaryContainer = lerpColor(from.onTertiaryContainer, to.onTertiaryContainer, fraction),
        background = lerpColor(from.background, to.background, fraction),
        onBackground = lerpColor(from.onBackground, to.onBackground, fraction),
        surface = lerpColor(from.surface, to.surface, fraction),
        onSurface = lerpColor(from.onSurface, to.onSurface, fraction),
        surfaceVariant = lerpColor(from.surfaceVariant, to.surfaceVariant, fraction),
        onSurfaceVariant = lerpColor(from.onSurfaceVariant, to.onSurfaceVariant, fraction),
        surfaceTint = lerpColor(from.surfaceTint, to.surfaceTint, fraction),
        inverseSurface = lerpColor(from.inverseSurface, to.inverseSurface, fraction),
        inverseOnSurface = lerpColor(from.inverseOnSurface, to.inverseOnSurface, fraction),
        error = lerpColor(from.error, to.error, fraction),
        onError = lerpColor(from.onError, to.onError, fraction),
        errorContainer = lerpColor(from.errorContainer, to.errorContainer, fraction),
        onErrorContainer = lerpColor(from.onErrorContainer, to.onErrorContainer, fraction),
        outline = lerpColor(from.outline, to.outline, fraction),
        outlineVariant = lerpColor(from.outlineVariant, to.outlineVariant, fraction),
        scrim = lerpColor(from.scrim, to.scrim, fraction),
        surfaceBright = lerpColor(from.surfaceBright, to.surfaceBright, fraction),
        surfaceDim = lerpColor(from.surfaceDim, to.surfaceDim, fraction),
        surfaceContainer = lerpColor(from.surfaceContainer, to.surfaceContainer, fraction),
        surfaceContainerHigh = lerpColor(from.surfaceContainerHigh, to.surfaceContainerHigh, fraction),
        surfaceContainerHighest = lerpColor(from.surfaceContainerHighest, to.surfaceContainerHighest, fraction),
        surfaceContainerLow = lerpColor(from.surfaceContainerLow, to.surfaceContainerLow, fraction),
        surfaceContainerLowest = lerpColor(from.surfaceContainerLowest, to.surfaceContainerLowest, fraction),
    )

private fun lerpColor(from: Color, to: Color, fraction: Float): Color =
    Color(
        red = from.red + (to.red - from.red) * fraction,
        green = from.green + (to.green - from.green) * fraction,
        blue = from.blue + (to.blue - from.blue) * fraction,
        alpha = from.alpha + (to.alpha - from.alpha) * fraction,
    )

@Composable
fun MoodNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color = ThemePresets[0].seed,
    fontScale: Float = 1f,
    background: BackgroundPreset = BackgroundPreset.DEFAULT,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val targetScheme = remember(darkTheme, seedColor) { schemeFromSeed(seedColor, darkTheme) }

    // settled：上一套完全到位的配色；progress：settled → target 的过渡进度
    var settled by remember { mutableStateOf(targetScheme) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(targetScheme, reduceMotion) {
        if (settled != targetScheme) {
            if (reduceMotion) {
                settled = targetScheme
                progress.snapTo(1f)
            } else {
                // 中途被打断时，把当前视觉状态冻结为新起点，避免跳变
                settled = lerpScheme(settled, targetScheme, progress.value)
                progress.snapTo(0f)
                progress.animateTo(1f, animationSpec = tween(650, easing = EaseInOutQuint))
                settled = targetScheme
            }
        }
    }
    val lerped = if (settled == targetScheme) targetScheme else lerpScheme(settled, targetScheme, progress.value)

    val typography = remember(fontScale) { MoodTypography.scaled(fontScale) }
    CompositionLocalProvider(
        LocalAppDarkTheme provides darkTheme,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = lerped,
            typography = typography,
            shapes = MoodShapes,
        ) {
            val brush = remember(darkTheme, background) { background.brush(darkTheme) }
            if (brush == null) {
                Box(Modifier.fillMaxSize().background(lerped.background)) { content() }
            } else {
                Box(Modifier.fillMaxSize().background(brush)) { content() }
            }
        }
    }
}
