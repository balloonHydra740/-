package com.moodnotes.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moodnotes.app.R
import com.moodnotes.app.data.SavedTheme
import com.moodnotes.app.ui.theme.schemeFromSeed
import kotlin.math.roundToInt

/**
 * 主题册调色卡：色相/饱和度/明度三滑杆，实时预览整套配色，可保存。
 */
@Composable
fun ThemeMixerCard(
    hue: Float,
    saturation: Float,
    lightness: Float,
    onHue: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onLightness: (Float) -> Unit,
    onSave: (name: String, seed: Long) -> Unit,
) {
    val color = hslColor(hue, saturation, lightness)
    var showSaveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.custom_color),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // 当前色圆点 + 预览小卡
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
                Spacer(Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp, 40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            schemeFromSeed(color, dark = false).primaryContainer,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(schemeFromSeed(color, dark = false).primary))
                }
            }
            Spacer(Modifier.height(14.dp))
            HueSlider(hue = hue, saturation = saturation, lightness = lightness, onChange = onHue)
            LabeledSlider(
                label = stringResource(R.string.slider_saturation),
                value = saturation,
                startColor = hslColor(hue, 0f, lightness),
                endColor = hslColor(hue, 1f, lightness),
                onChange = onSaturation,
            )
            LabeledSlider(
                label = stringResource(R.string.slider_lightness),
                value = lightness,
                startColor = hslColor(hue, saturation, 0f),
                endColor = hslColor(hue, saturation, 1f),
                onChange = onLightness,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { showSaveDialog = true }) {
                Text(stringResource(R.string.action_save_theme), color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.action_save_theme)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.theme_name_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(name, colorToArgbLong(color))
                            showSaveDialog = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_confirm), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

/** 色相滑杆：整条彩虹渐变。 */
@Composable
private fun HueSlider(hue: Float, saturation: Float, lightness: Float, onChange: (Float) -> Unit) {
    val rainbow = remember(saturation, lightness) {
        Brush.horizontalGradient((0..12).map { hslColor(it * 30f, saturation, lightness) })
    }
    GradientSlider(value = hue / 360f, brush = rainbow, onChange = { onChange(it * 360f) })
}

/** 单色渐变滑杆（饱和度/明度）。 */
@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    startColor: Color,
    endColor: Color,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        GradientSlider(
            value = value,
            brush = Brush.horizontalGradient(listOf(startColor, endColor)),
            onChange = onChange,
        )
    }
}

/** 自绘渐变滑杆：支持点击与横向拖动。 */
@Composable
private fun GradientSlider(
    value: Float,
    brush: Brush,
    onChange: (Float) -> Unit,
) {
    var width by remember { mutableFloatStateOf(1f) }
    fun fractionToValue(x: Float) = (x / width).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(26.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .clip(RoundedCornerShape(13.dp))
            .background(brush)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset -> onChange(fractionToValue(offset.x)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onChange(fractionToValue(change.position.x))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.fillMaxWidth().height(26.dp)) {
            val x = size.width * value.coerceIn(0f, 1f)
            val center = Offset(x, size.height / 2f)
            drawCircle(color = Color.White, radius = 11.dp.toPx() / 2f, center = center)
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = 11.dp.toPx() / 2f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

/** 主题册列表：已保存主题卡片（使用 / 重命名 / 删除）。 */
@Composable
fun SavedThemeList(
    themes: List<SavedTheme>,
    activeSeed: Long?,
    onUse: (SavedTheme) -> Unit,
    onRename: (SavedTheme, String) -> Unit,
    onDelete: (SavedTheme) -> Unit,
) {
    var renaming by remember { mutableStateOf<SavedTheme?>(null) }
    var deleting by remember { mutableStateOf<SavedTheme?>(null) }

    if (themes.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        themes.forEach { theme ->
            val active = activeSeed == theme.seedColor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onUse(theme) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(theme.seedColor.toInt()))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (active) stringResource(R.string.theme_in_use)
                            else "#${Integer.toHexString(theme.seedColor.toInt()).takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onUse(theme) }) {
                        Text(
                            stringResource(if (active) R.string.theme_in_use else R.string.theme_use),
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = { renaming = theme }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_rename), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { deleting = theme }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    renaming?.let { theme ->
        var name by remember(theme.id) { mutableStateOf(theme.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.theme_name_hint)) },
                    shape = RoundedCornerShape(16.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { if (name.isNotBlank()) onRename(theme, name); renaming = null }) {
                    Text(stringResource(R.string.action_confirm), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    deleting?.let { theme ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(theme.name) },
            confirmButton = {
                TextButton(onClick = { onDelete(theme); deleting = null }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

internal fun hslColor(h: Float, s: Float, l: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val x = c * (1f - kotlin.math.abs((hh / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        hh < 60 -> Triple(c, x, 0f)
        hh < 120 -> Triple(x, c, 0f)
        hh < 180 -> Triple(0f, c, x)
        hh < 240 -> Triple(0f, x, c)
        hh < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

internal fun colorToArgbLong(color: Color): Long = color.toArgb().toLong() and 0xFFFFFFFFL
