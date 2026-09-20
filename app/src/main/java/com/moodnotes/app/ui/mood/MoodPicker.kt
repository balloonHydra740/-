package com.moodnotes.app.ui.mood

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moodnotes.app.R
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.Mood
import com.moodnotes.app.ui.components.ImageThumb
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.theme.LocalAppDarkTheme
import com.moodnotes.app.util.ImageStore
import kotlinx.coroutines.launch

/** 自定义心情可选的表情列表。 */
val CustomMoodEmojis = listOf(
    "😀", "😁", "😂", "😊", "😍", "😘", "😜", "🤪",
    "😎", "🤩", "🥳", "😇", "🤗", "🤔", "😴", "🤤",
    "😭", "😢", "😱", "😡", "🥶", "😷", "🤧", "💪",
    "❤️", "💔", "✨", "🔥", "🌈", "🌙", "☕", "🎉",
)

/** 自定义心情可选的主题色。 */
val MoodColorPalette = listOf(
    Color(0xFFFFE9B8), Color(0xFFFFD9E4), Color(0xFFC9F2D8), Color(0xFFE2E6EC),
    Color(0xFFE9E0FF), Color(0xFFFFE0CC), Color(0xFFD8E7FF), Color(0xFFFFDAD5),
)

/**
 * 心情选择网格：内置心情 + 自定义心情 + 添加入口。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodPicker(
    customMoods: List<CustomMood>,
    selectedMoodId: Int?,
    selectedCustomMoodId: Long?,
    onSelectMood: (Int) -> Unit,
    onSelectCustomMood: (Long) -> Unit,
    onAddCustom: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = LocalAppDarkTheme.current,
    buttonSize: Dp = 60.dp,
    iconSize: TextUnit = 28.sp,
    showAdd: Boolean = true,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Mood.entries.forEach { mood ->
            MoodIconTile(
                visual = mood.visual(dark),
                selected = selectedMoodId == mood.id,
                onClick = { onSelectMood(mood.id) },
                buttonSize = buttonSize,
                iconSize = iconSize,
            )
        }
        customMoods.forEach { custom ->
            MoodIconTile(
                visual = custom.visual(dark),
                selected = selectedCustomMoodId == custom.id,
                onClick = { onSelectCustomMood(custom.id) },
                buttonSize = buttonSize,
                iconSize = iconSize,
            )
        }
        if (showAdd) {
            AddMoodTile(onClick = onAddCustom, buttonSize = buttonSize)
        }
    }
}

@Composable
private fun MoodIconTile(
    visual: MoodVisual,
    selected: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp,
    iconSize: TextUnit,
) {
    val tileWidth = buttonSize + 14.dp
    val scale by animateFloatAsState(if (selected) 1.14f else 1f, springy())
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        springy(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(tileWidth)) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .graphicsLayer { scaleX = scale * pressScale; scaleY = scale * pressScale }
                .clip(RoundedCornerShape(buttonSize / 3))
                .background(visual.container)
                .then(
                    if (selected) {
                        Modifier.border(2.5.dp, visual.onContainer, RoundedCornerShape(buttonSize / 3))
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
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(RoundedCornerShape(buttonSize / 3)),
                )
            } else {
                Text(visual.emoji ?: "•", fontSize = iconSize)
            }
        }
        Text(
            text = visual.displayLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).width(tileWidth),
        )
    }
}

@Composable
private fun AddMoodTile(onClick: () -> Unit, buttonSize: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(buttonSize + 14.dp)) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(buttonSize / 3))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(buttonSize / 3))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_add_mood), tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = stringResource(R.string.custom_mood_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp).width(buttonSize + 14.dp),
        )
    }
}

/**
 * 添加自定义心情弹窗：名称 + 表情/图片图标 + 主题色。
 */
@Composable
fun CustomMoodDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, iconType: String, iconValue: String, containerColor: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var useImage by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf(CustomMoodEmojis.first()) }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var colorIndex by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            importing = true
            scope.launch {
                imagePath = ImageStore.import(context, uri, "mood", maxDim = 512, square = true)
                importing = false
            }
        }
    }

    val valid = name.isNotBlank() && (if (useImage) imagePath != null else emoji.isNotBlank()) && !importing

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_mood_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.mood_name_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useImage,
                        onClick = { useImage = false },
                        label = { Text(stringResource(R.string.use_emoji)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                    FilterChip(
                        selected = useImage,
                        onClick = { useImage = true },
                        label = { Text(stringResource(R.string.use_image)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (useImage) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (imagePath != null) {
                                ImageThumb(path = imagePath, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)))
                            } else {
                                Icon(Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            onClick = {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) {
                            Text(
                                if (importing) stringResource(R.string.importing) else stringResource(R.string.pick_from_gallery),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CustomMoodEmojis.forEach { e ->
                            val selected = e == emoji
                            Text(
                                text = e,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { emoji = e }
                                    .padding(6.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.palette_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoodColorPalette.forEachIndexed { index, color ->
                        val selected = index == colorIndex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (selected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { colorIndex = index },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name.trim(),
                        if (useImage) "image" else "emoji",
                        if (useImage) imagePath.orEmpty() else emoji,
                        MoodColorPalette[colorIndex].toArgb(),
                    )
                },
                enabled = valid,
            ) {
                Text(stringResource(R.string.action_add), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
