package com.moodnotes.app.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moodnotes.app.MoodNotesApplication
import com.moodnotes.app.R
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.MoodRecord
import com.moodnotes.app.ui.components.EaseInOutQuint
import com.moodnotes.app.ui.components.EaseOutQuint
import com.moodnotes.app.ui.components.EmptyState
import com.moodnotes.app.ui.components.ImageThumb
import com.moodnotes.app.ui.components.IntensitySelector
import com.moodnotes.app.ui.components.MoodBanner
import com.moodnotes.app.ui.components.SectionHeader
import com.moodnotes.app.ui.components.StaggeredAppear
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.mood.CustomMoodDialog
import com.moodnotes.app.ui.mood.MoodPicker
import com.moodnotes.app.ui.mood.MoodVisual
import com.moodnotes.app.ui.mood.visual
import com.moodnotes.app.ui.theme.LocalAppDarkTheme
import com.moodnotes.app.ui.moodNotesViewModel
import com.moodnotes.app.util.Dates
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onWriteDiary: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEntry: (Long) -> Unit = {},
    viewModel: CalendarViewModel = moodNotesViewModel { CalendarViewModel(it) },
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoodNotesApplication
    val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()

    val month by viewModel.currentMonth.collectAsStateWithLifecycle()
    val moods by viewModel.monthMoods.collectAsStateWithLifecycle()
    val diaries by viewModel.monthDiaries.collectAsStateWithLifecycle()
    val customMoods by viewModel.customMoods.collectAsStateWithLifecycle()
    val detail by viewModel.selectedDetail.collectAsStateWithLifecycle()

    val moodByDay = remember(moods) { moods.associateBy { it.date } }
    val diaryDays = remember(diaries) {
        diaries.filter { !it.isDraft }.mapTo(mutableSetOf()) { it.date }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.prevMonth() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_prev_month),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedContent(
                    targetState = month,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (fadeIn(tween(320, easing = EaseOutQuint)) + slideInHorizontally(
                            tween(380, easing = EaseOutQuint),
                        ) { if (forward) it / 4 else -it / 4 }).togetherWith(
                            fadeOut(tween(200, easing = EaseInOutQuint)) + slideOutHorizontally(
                                tween(300, easing = EaseInOutQuint),
                            ) { if (forward) -it / 6 else it / 6 },
                        )
                    },
                    label = "monthTitle",
                ) { m ->
                    Text(
                        text = Dates.monthTitle(m),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_month),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        TextButton(
            onClick = { viewModel.backToToday() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.back_to_today), color = MaterialTheme.colorScheme.primary)
        }

        val weekLabels = remember(settings.weekStartMonday) { Dates.weekLabels(settings.weekStartMonday) }
        Row(modifier = Modifier.fillMaxWidth()) {
            weekLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val forward = targetState > initialState
                (fadeIn(tween(300)) + slideInHorizontally(tween(400, easing = EaseOutQuint)) { if (forward) it / 3 else -it / 3 })
                    .togetherWith(
                        fadeOut(tween(240, easing = EaseInOutQuint)) +
                            slideOutHorizontally(tween(320, easing = EaseInOutQuint)) { if (forward) -it / 4 else it / 4 },
                    )
            },
            label = "monthGrid",
        ) { m ->
            Column {
                CalendarViewModel.weeksOf(m, settings.weekStartMonday).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        week.forEach { date ->
                            val inMonth = YearMonth.from(date) == m
                            val moodRecord = moodByDay[date.toEpochDay()]
                            val hasDiary = date.toEpochDay() in diaryDays
                            DayCell(
                                date = date,
                                inMonth = inMonth,
                                moodRecord = moodRecord,
                                customMoods = customMoods,
                                hasDiary = hasDiary,
                                onClick = { viewModel.selectDay(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }

    detail?.let { dayDetail ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissDay() },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        ) {
            DayDetailSheet(
                detail = dayDetail,
                customMoods = customMoods,
                onWriteDiary = {
                    onWriteDiary(dayDetail.date.toEpochDay())
                    viewModel.dismissDay()
                },
                onOpenEntry = { id ->
                    onOpenEntry(id)
                    viewModel.dismissDay()
                },
                onSaveMood = { moodId, customMoodId, intensity, note ->
                    viewModel.saveMood(dayDetail.date, moodId, customMoodId, intensity, note)
                },
                onSaveCustomMood = { name, iconType, iconValue, color ->
                    viewModel.saveCustomMood(name, iconType, iconValue, color)
                },
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    moodRecord: MoodRecord?,
    customMoods: List<CustomMood>,
    hasDiary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isToday = date == Dates.today()
    val dark = LocalAppDarkTheme.current
    val visual = moodRecord?.visual(customMoods, dark)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.88f else 1f, springy())

    // 选中/今日底色平滑过渡
    val todayColor by animateColorAsState(
        if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
        tween(260),
    )
    val dotColor by animateColorAsState(
        when {
            visual != null -> visual.container
            hasDiary -> MaterialTheme.colorScheme.tertiaryContainer
            else -> Color.Transparent
        },
        tween(260),
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(todayColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    inMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier.size(16.dp).clip(CircleShape).background(dotColor),
            contentAlignment = Alignment.Center,
        ) {
            when {
                visual?.isImage == true -> {
                    ImageThumb(
                        path = visual.imagePath,
                        modifier = Modifier.size(16.dp).clip(CircleShape),
                    )
                }
                visual != null -> {
                    Text(visual.emoji ?: "·", fontSize = 11.sp)
                }
                hasDiary -> {
                    Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

@Composable
private fun DayDetailSheet(
    detail: DayDetail,
    customMoods: List<CustomMood>,
    onWriteDiary: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onSaveMood: (Int, Long?, Int, String) -> Unit,
    onSaveCustomMood: (String, String, String, Int) -> Unit,
) {
    var sheetMoodId by remember(detail.date) { mutableStateOf<Int?>(null) }
    var sheetCustomMoodId by remember(detail.date) { mutableStateOf<Long?>(null) }
    var intensity by remember(detail.date) { mutableIntStateOf(3) }
    var note by remember(detail.date) { mutableStateOf("") }
    var showAddDialog by remember(detail.date) { mutableStateOf(false) }

    val dark = LocalAppDarkTheme.current
    val publishedDiaries = detail.diaries.filter { !it.isDraft }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            StaggeredAppear(index = 0) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = Dates.fullDate(detail.date),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = Dates.weekday(detail.date.dayOfWeek),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionHeader(title = stringResource(R.string.section_mood))
        }

        item {
            val mood = detail.mood
            if (mood != null) {
                MoodBanner(
                    visual = mood.visual(customMoods, dark),
                    intensity = mood.intensity,
                    note = mood.note,
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.no_mood_that_day),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        MoodPicker(
                            customMoods = customMoods,
                            selectedMoodId = sheetMoodId,
                            selectedCustomMoodId = sheetCustomMoodId,
                            onSelectMood = { id ->
                                sheetMoodId = id
                                sheetCustomMoodId = null
                            },
                            onSelectCustomMood = { id ->
                                sheetCustomMoodId = id
                                sheetMoodId = null
                            },
                            onAddCustom = { showAddDialog = true },
                            buttonSize = 48.dp,
                            iconSize = 22.sp,
                        )
                        AnimatedVisibility(
                            visible = sheetMoodId != null || sheetCustomMoodId != null,
                            enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 3 },
                            exit = fadeOut(tween(160)),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                IntensitySelector(intensity = intensity, onChange = { intensity = it })
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (sheetMoodId != null || sheetCustomMoodId != null) {
                                            onSaveMood(
                                                sheetMoodId ?: com.moodnotes.app.data.Mood.JOY.id,
                                                sheetCustomMoodId,
                                                intensity,
                                                note,
                                            )
                                        }
                                    },
                                    enabled = sheetMoodId != null || sheetCustomMoodId != null,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Text(stringResource(R.string.record_mood_this_day))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.section_diary),
                actionLabel = stringResource(R.string.action_write_diary),
                onAction = onWriteDiary,
            )
        }

        if (publishedDiaries.isEmpty()) {
            item {
                EmptyState(
                    emoji = "✍️",
                    title = stringResource(R.string.no_diary_that_day),
                    subtitle = stringResource(R.string.no_diary_that_day_hint),
                )
            }
        } else {
            items(publishedDiaries, key = { it.id }) { entry ->
                val visual = entry.visual(customMoods, dark)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenEntry(entry.id) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (visual != null) {
                                if (visual.isImage) {
                                    ImageThumb(
                                        path = visual.imagePath,
                                        modifier = Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)),
                                    )
                                } else {
                                    Text(visual.emoji ?: "•", fontSize = 16.sp)
                                }
                                Spacer(Modifier.size(6.dp))
                            }
                            Text(
                                text = entry.title.ifBlank { stringResource(R.string.untitled) },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (entry.content.isNotBlank()) {
                            Text(
                                text = entry.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CustomMoodDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, iconType, iconValue, color ->
                onSaveCustomMood(name, iconType, iconValue, color)
                showAddDialog = false
            },
        )
    }
}
