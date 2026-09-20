package com.moodnotes.app.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moodnotes.app.R
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.Mood
import com.moodnotes.app.data.MoodRecord
import com.moodnotes.app.ui.components.EmptyState
import com.moodnotes.app.ui.components.IntensitySelector
import com.moodnotes.app.ui.components.MoodBanner
import com.moodnotes.app.ui.components.SectionHeader
import com.moodnotes.app.ui.components.StaggeredAppear
import com.moodnotes.app.ui.components.bounceClick
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.mood.CustomMoodDialog
import com.moodnotes.app.ui.mood.MoodPicker
import com.moodnotes.app.ui.mood.visual
import com.moodnotes.app.ui.theme.LocalAppDarkTheme
import com.moodnotes.app.ui.moodNotesViewModel
import com.moodnotes.app.util.Dates
import java.time.LocalDate

@Composable
fun TodayScreen(
    onWriteDiary: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiaryTab: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = moodNotesViewModel { TodayViewModel(it) },
) {
    val mood by viewModel.todayMood.collectAsStateWithLifecycle()
    val diaries by viewModel.todayDiaries.collectAsStateWithLifecycle()
    val recent by viewModel.recentMoods.collectAsStateWithLifecycle()
    val customMoods by viewModel.customMoods.collectAsStateWithLifecycle()

    var selectedMoodId by remember { mutableStateOf<Int?>(null) }
    var selectedCustomMoodId by remember { mutableStateOf<Long?>(null) }
    var intensity by remember { mutableIntStateOf(3) }
    var note by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val showCheckIn = mood == null || editing
    val publishedDiaries = diaries.filter { !it.isDraft }
    val today = viewModel.todayDate()
    val isWeekend = Dates.isWeekend(today)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            StaggeredAppear(index = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (isWeekend) R.string.today_greeting_weekend
                                else R.string.today_greeting_weekday,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = Dates.todayTitle(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            StaggeredAppear(index = 1) {
                AnimatedContent(
                    targetState = showCheckIn,
                    transitionSpec = {
                        val msIn = 300
                        (fadeIn(MotionTween300) + scaleIn(initialScale = 0.94f, animationSpec = springy()))
                            .togetherWith(fadeOut(MotionTween200) + scaleOut(targetScale = 0.96f, animationSpec = MotionTween200))
                    },
                    label = "moodCard",
                ) { checkIn ->
                    if (checkIn) {
                        MoodCheckInCard(
                            customMoods = customMoods,
                            selectedMoodId = selectedMoodId,
                            selectedCustomMoodId = selectedCustomMoodId,
                            intensity = intensity,
                            note = note,
                            onSelectMood = { id ->
                                selectedMoodId = id
                                selectedCustomMoodId = null
                            },
                            onSelectCustomMood = { id ->
                                selectedCustomMoodId = id
                                selectedMoodId = null
                            },
                            onAddCustom = { showAddDialog = true },
                            onIntensityChange = { intensity = it },
                            onNoteChange = { note = it },
                            onSave = {
                                if (selectedMoodId != null || selectedCustomMoodId != null) {
                                    viewModel.saveMood(
                                        selectedMoodId ?: Mood.NEUTRAL.id,
                                        selectedCustomMoodId,
                                        intensity,
                                        note,
                                    )
                                }
                                editing = false
                                selectedMoodId = null
                                selectedCustomMoodId = null
                                note = ""
                                intensity = 3
                            },
                        )
                    } else {
                        mood?.let { record ->
                            MoodSummaryCard(
                                record = record,
                                customMoods = customMoods,
                                onEdit = {
                                    editing = true
                                    selectedMoodId = if (record.customMoodId != null) null else record.moodId
                                    selectedCustomMoodId = record.customMoodId
                                    intensity = record.intensity
                                    note = record.note
                                },
                                onDelete = { viewModel.deleteMood() },
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.today_diary_section),
                actionLabel = if (publishedDiaries.isNotEmpty()) stringResource(R.string.action_write_one) else null,
                onAction = { if (publishedDiaries.isNotEmpty()) onWriteDiary() },
            )
        }

        item {
            if (publishedDiaries.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        EmptyState(
                            emoji = "📖",
                            title = stringResource(R.string.empty_diary_title),
                            subtitle = stringResource(R.string.empty_diary_subtitle),
                        )
                        TextButton(
                            onClick = onWriteDiary,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(stringResource(R.string.action_start_writing), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    publishedDiaries.take(3).forEachIndexed { i, entry ->
                        StaggeredAppear(index = i) {
                            TodayDiaryCard(
                                title = entry.title.ifBlank { stringResource(R.string.untitled) },
                                snippet = entry.content,
                                onClick = { onOpenEntry(entry.id) },
                            )
                        }
                    }
                    TextButton(
                        onClick = onOpenDiaryTab,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.action_view_all), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.recent_moods),
                actionLabel = stringResource(R.string.action_calendar),
                onAction = onOpenCalendar,
            )
        }

        item {
            RecentMoodStrip(recentMoods = recent, customMoods = customMoods, today = today)
        }
    }

    if (showAddDialog) {
        CustomMoodDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, iconType, iconValue, color ->
                viewModel.saveCustomMood(name, iconType, iconValue, color)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun MoodCheckInCard(
    customMoods: List<CustomMood>,
    selectedMoodId: Int?,
    selectedCustomMoodId: Long?,
    intensity: Int,
    note: String,
    onSelectMood: (Int) -> Unit,
    onSelectCustomMood: (Long) -> Unit,
    onAddCustom: () -> Unit,
    onIntensityChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.ask_mood_today),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            MoodPicker(
                customMoods = customMoods,
                selectedMoodId = selectedMoodId,
                selectedCustomMoodId = selectedCustomMoodId,
                onSelectMood = onSelectMood,
                onSelectCustomMood = onSelectCustomMood,
                onAddCustom = onAddCustom,
                buttonSize = 56.dp,
                iconSize = 26.sp,
            )
            Spacer(Modifier.height(18.dp))
            AnimatedVisibility(
                visible = selectedMoodId != null || selectedCustomMoodId != null,
                enter = fadeIn(MotionTween260) + scaleIn(initialScale = 0.96f, animationSpec = springy()),
                exit = fadeOut(MotionTween160) + scaleOut(targetScale = 0.97f, animationSpec = MotionTween160),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.feeling_intensity),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    IntensitySelector(intensity = intensity, onChange = onIntensityChange)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                stringResource(R.string.note_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onSave,
                        enabled = selectedMoodId != null || selectedCustomMoodId != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(stringResource(R.string.save_mood_today), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodSummaryCard(
    record: MoodRecord,
    customMoods: List<CustomMood>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dark = LocalAppDarkTheme.current
    val visual = record.visual(customMoods, dark)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = stringResource(R.string.today_mood_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            MoodBanner(visual = visual, intensity = record.intensity, note = record.note)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.action_edit), color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TodayDiaryCard(
    title: String,
    snippet: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (snippet.isNotBlank()) {
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentMoodStrip(
    recentMoods: List<MoodRecord>,
    customMoods: List<CustomMood>,
    today: LocalDate,
) {
    val dark = LocalAppDarkTheme.current
    val moodByDay = remember(recentMoods) { recentMoods.associateBy { it.date } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items((0L..6L).toList()) { offset ->
            val date = today.minusDays(offset)
            val record = moodByDay[date.toEpochDay()]
            val visual = record?.let { it.visual(customMoods, dark) }
            RecentDayChip(date = date, visual = visual, isToday = offset == 0L)
        }
    }
}

@Composable
private fun RecentDayChip(
    date: LocalDate,
    visual: com.moodnotes.app.ui.mood.MoodVisual?,
    isToday: Boolean,
) {
    val scale by animateFloatAsState(if (visual != null) 1f else 0.85f, springy())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp),
    ) {
        Text(
            text = Dates.weekday(date.dayOfWeek),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(visual?.container ?: MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (visual?.isImage == true) {
                com.moodnotes.app.ui.components.ImageThumb(
                    path = visual.imagePath,
                    modifier = Modifier.size(42.dp).clip(CircleShape),
                )
            } else {
                Text(text = visual?.emoji ?: "·", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${date.dayOfMonth}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 本页动画时长（普通 val，可在 transitionSpec 等非 Composable 作用域使用）。 */
private val MotionTween300 = androidx.compose.animation.core.tween<Float>(
    300,
    easing = com.moodnotes.app.ui.components.EaseOutQuint,
)
private val MotionTween260 = androidx.compose.animation.core.tween<Float>(
    260,
    easing = com.moodnotes.app.ui.components.EaseOutQuint,
)
private val MotionTween200 = androidx.compose.animation.core.tween<Float>(
    200,
    easing = com.moodnotes.app.ui.components.EaseInOutQuint,
)
private val MotionTween160 = androidx.compose.animation.core.tween<Float>(
    160,
    easing = com.moodnotes.app.ui.components.EaseInOutQuint,
)
