package com.moodnotes.app.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moodnotes.app.R
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.theme.Motion

/**
 * PIN 输入圆点：4 位，输入几位亮几位，错误时整排变红。
 */
@Composable
fun PinDots(
    length: Int,
    error: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(4) { i ->
            val filled = i < length
            val scale by animateFloatAsState(
                if (filled) 1f else 0.72f,
                Motion.spring(),
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(
                        when {
                            error && filled -> MaterialTheme.colorScheme.error
                            filled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
            )
        }
    }
}

/**
 * 抖动容器：shakeKey 变化时横向抖一下（输错 PIN / 校验失败）。
 */
@Composable
fun ShakeHost(
    shakeKey: Int,
    content: @Composable () -> Unit,
) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            listOf(0f, -20f, 17f, -13f, 9f, -5f, 0f).forEach { target ->
                shake.animateTo(target, tween(38, easing = LinearEasing))
            }
        }
    }
    Box(Modifier.graphicsLayer { translationX = shake.value }) {
        content()
    }
}

/**
 * 数字键盘：3×4，底排左侧放生物识别入口（可为空）。
 */
@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    biometricButton: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9'),
        ).forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                rowKeys.forEach { key ->
                    PadKey(text = key.toString(), enabled = enabled, onClick = { onDigit(key) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (biometricButton != null) biometricButton() else Spacer(Modifier.size(0.dp))
            }
            PadKey(text = "0", enabled = enabled, onClick = { onDigit('0') })
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    if (pressed) 0.85f else 1f,
                    springy(),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = stringResource(R.string.cd_backspace),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(30.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .clickable(enabled = enabled, interactionSource = interaction, onClick = onBackspace),
                )
            }
        }
    }
}

@Composable
private fun PadKey(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        springy(),
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            )
            .clickable(enabled = enabled, interactionSource = interaction, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 26.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
    }
}

/** 键盘里的生物识别圆钮。 */
@Composable
fun BiometricPadButton(
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, springy())
    Icon(
        imageVector = Icons.Rounded.Fingerprint,
        contentDescription = stringResource(R.string.cd_biometric),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(interactionSource = interaction, onClick = onClick),
    )
}
