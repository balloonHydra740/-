package com.moodnotes.app.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * 非线性缓动曲线（Material 3 Expressive 风格）
 */
val EaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
val EaseInOutQuint = CubicBezierEasing(0.83f, 0f, 0.17f, 1f)
val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val EaseInOutExpo = CubicBezierEasing(0.87f, 0f, 0.13f, 1f)

/**
 * 弹性弹簧：默认带一点回弹，用于卡片、Emoji 等元素
 */
fun springy(
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
): SpringSpec<Float> = spring(dampingRatio = dampingRatio, stiffness = stiffness)