package com.moodnotes.app.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moodnotes.app.R
import com.moodnotes.app.ui.mood.MoodVisual
import com.moodnotes.app.ui.mood.displayLabel
import com.moodnotes.app.ui.theme.Motion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地图片缩略图：按目标像素尺寸采样解码 + LruCache 内存缓存，
 * 列表里的小图不再解码整张原图。
 */
private val thumbCache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int =
        runCatching { value.asAndroidBitmap().byteCount }.getOrDefault(1024 * 1024)
}

private fun decodeSampled(path: String, targetPx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= targetPx && bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?.asImageBitmap()
}.getOrNull()

@Composable
fun ImageThumb(
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var targetPx by remember(path) { mutableIntStateOf(0) }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path, targetPx) {
        if (path.isNullOrBlank() || targetPx <= 0) return@produceState
        val key = "$path@$targetPx"
        val cached = thumbCache.get(key)
        value = if (cached != null) cached else withContext(Dispatchers.IO) {
            decodeSampled(path, targetPx)?.also { thumbCache.put(key, it) }
        }
    }
    Box(
        modifier = modifier.onSizeChanged { size ->
            val px = maxOf(size.width, size.height)
            if (px > 0 && px != targetPx) targetPx = px
        },
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

/**
 * 视频缩略图：取首帧解码（缓存），可叠加播放角标。
 */
@Composable
fun VideoThumb(
    path: String?,
    modifier: Modifier = Modifier,
    showPlayBadge: Boolean = true,
) {
    var targetPx by remember(path) { mutableIntStateOf(0) }
    val frame by produceState<ImageBitmap?>(initialValue = null, path, targetPx) {
        if (path.isNullOrBlank() || targetPx <= 0) return@produceState
        val key = "video:$path@$targetPx"
        val cached = thumbCache.get(key)
        value = if (cached != null) cached else withContext(Dispatchers.IO) {
            decodeVideoFrame(path, targetPx)?.also { thumbCache.put(key, it) }
        }
    }
    Box(
        modifier = modifier.onSizeChanged { size ->
            val px = maxOf(size.width, size.height)
            if (px > 0 && px != targetPx) targetPx = px
        },
    ) {
        val bmp = frame
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
        if (showPlayBadge && bmp != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun decodeVideoFrame(path: String, targetPx: Int): ImageBitmap? = runCatching {
    val retriever = android.media.MediaMetadataRetriever()
    try {
        retriever.setDataSource(path)
        val frame = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return@runCatching null
        val maxDim = maxOf(frame.width, frame.height)
        val scaled = if (maxDim > targetPx * 2) {
            val scale = (targetPx * 2f) / maxDim
            android.graphics.Bitmap.createScaledBitmap(
                frame,
                (frame.width * scale).toInt().coerceAtLeast(1),
                (frame.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also { if (it !== frame) frame.recycle() }
        } else frame
        scaled.asImageBitmap()
    } finally {
        runCatching { retriever.release() }
    }
}.getOrNull()

/**
 * 按压缩放点击：所有可点卡片/选项的统一弹簧反馈。
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        Motion.spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, enabled = enabled, onClick = onClick)
}

/**
 * 心情图标按钮：支持 Emoji 或图片图标，选中时带弹性缩放 + 描边。
 */
@Composable
fun MoodIconButton(
    visual: MoodVisual,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    iconSize: TextUnit = 30.sp,
) {
    val scale by animateFloatAsState(
        if (selected) 1.16f else 1f,
        Motion.spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        Motion.spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale * pressScale
                scaleY = scale * pressScale
            }
            .clip(RoundedCornerShape(24.dp))
            .background(visual.container)
            .then(
                if (selected) {
                    Modifier.border(2.5.dp, visual.onContainer, RoundedCornerShape(24.dp))
                } else {
                    Modifier
                },
            )
            .clickable(interactionSource = interactionSource, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (visual.isImage) {
            ImageThumb(
                path = visual.imagePath,
                modifier = Modifier.size(size).clip(RoundedCornerShape(24.dp)),
            )
        } else {
            Text(visual.emoji ?: "•", fontSize = iconSize)
        }
    }
}

/**
 * 心情强度选择器：5 个圆点，带弹簧动画。
 */
@Composable
fun IntensitySelector(
    intensity: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val labels = listOf(
        stringResourceSafe(R.string.intensity_1),
        stringResourceSafe(R.string.intensity_2),
        stringResourceSafe(R.string.intensity_3),
        stringResourceSafe(R.string.intensity_4),
        stringResourceSafe(R.string.intensity_5),
    )
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(5) { i ->
                val level = i + 1
                val active = level <= intensity
                val scale by animateFloatAsState(
                    if (active) 1f else 0.78f,
                    Motion.spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                )
                val dotColor by androidx.compose.animation.animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                )
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val pressScale by animateFloatAsState(
                    if (pressed) 0.85f else 1f,
                    Motion.spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { scaleX = scale * pressScale; scaleY = scale * pressScale }
                        .clip(CircleShape)
                        .background(dotColor)
                        .clickable(enabled = enabled, interactionSource = interactionSource) { onChange(level) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * 分段标题
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/**
 * 空状态占位
 */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 40.sp)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * 数字滚动动画（减少动画时直切）。
 */
@Composable
fun AnimatedCounter(
    value: Int,
    style: TextStyle,
    color: Color = Color.Unspecified,
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = Motion.spring(
            Spring.DampingRatioMediumBouncy,
            Spring.StiffnessLow,
        ),
    )
    Text(text = animated.toString(), style = style, color = color)
}

/**
 * 错峰入场：首次进入组合时淡入 + 上浮，index 用于同级元素错开延迟。
 */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val alpha by animateFloatAsState(
        if (shown) 1f else 0f,
        Motion.tween(320, delayMs = (index * 45).coerceAtMost(360)),
    )
    val translation by animateFloatAsState(
        if (shown) 0f else 44f,
        Motion.tween(360, delayMs = (index * 45).coerceAtMost(360)),
    )
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translation
        },
    ) {
        content()
    }
}

/**
 * 心情摘要横幅：图标 + 名称 + 强度点 + 备注（备注带入场动画）。
 */
@Composable
fun MoodBanner(
    visual: MoodVisual,
    intensity: Int,
    note: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(visual.container)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (visual.isImage) {
            ImageThumb(
                path = visual.imagePath,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)),
            )
        } else {
            Text(visual.emoji ?: "•", fontSize = 36.sp)
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = visual.displayLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = visual.onContainer,
                )
                repeat(5) { i ->
                    val on = i < intensity
                    val dotScale by animateFloatAsState(
                        if (on) 1f else 0.8f,
                        Motion.spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = if (i == 0) 10.dp else 3.dp)
                            .size((if (on) 9.dp else 7.dp) * dotScale)
                            .clip(CircleShape)
                            .background(if (on) visual.onContainer else visual.onContainer.copy(alpha = 0.25f)),
                    )
                }
            }
            AnimatedVisibility(
                visible = note.isNotBlank(),
                enter = fadeIn(Motion.tween(220)) + slideInVertically(Motion.tween(240)) { it / 2 },
                exit = fadeOut(Motion.tween(120)),
            ) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = visual.onContainer.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** stringResource 的简单别名，保持与旧代码一致的调用点。 */
@Composable
fun stringResourceSafe(id: Int): String = androidx.compose.ui.res.stringResource(id)

/**
 * 全屏图片查看：双指缩放、单指拖动，点击任意处关闭。
 */
@Composable
fun FullscreenImage(
    path: String,
    onDismiss: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
        ) {
            ImageThumb(
                path = path,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .transformable(transformState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}
