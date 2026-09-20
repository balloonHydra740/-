package com.moodnotes.app.ui.stats

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moodnotes.app.R
import com.moodnotes.app.data.CustomMood
import com.moodnotes.app.data.Mood
import com.moodnotes.app.data.MoodRecord
import com.moodnotes.app.ui.components.AnimatedCounter
import com.moodnotes.app.ui.components.ImageThumb
import com.moodnotes.app.ui.components.SectionHeader
import com.moodnotes.app.ui.components.StaggeredAppear
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.mood.MoodVisual
import com.moodnotes.app.ui.mood.displayLabel
import com.moodnotes.app.ui.mood.visual
import com.moodnotes.app.ui.theme.LocalAppDarkTheme
import com.moodnotes.app.ui.moodNotesViewModel
import com.moodnotes.app.util.Dates
import java.time.LocalDate

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = moodNotesViewModel { StatsViewModel(it) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val customMoods by viewModel.customMoods.collectAsStateWithLifecycle()
    val dark = LocalAppDarkTheme.current

    fun resolveVisual(entry: DistributionEntry): MoodVisual =
        entry.customMoodId?.let { id -> customMoods.firstOrNull { it.id == id }?.visual(dark) }
            ?: Mood.fromId(entry.moodId).visual(dark)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        item {
            StaggeredAppear(index = 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                        Text(
                            text = stringResource(R.string.streak_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            AnimatedCounter(
                                value = state.streak,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = stringResource(R.string.unit_days),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                        Text(
                            text = stringResource(
                                if (state.streak == 0) R.string.streak_hint_zero else R.string.streak_hint_encourage,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        item {
            StaggeredAppear(index = 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        label = stringResource(R.string.stat_month_count),
                        value = state.monthCount,
                        unit = stringResource(R.string.unit_days),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(R.string.stat_total_count),
                        value = state.totalCount,
                        unit = stringResource(R.string.unit_days),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.mood_distribution)) }

        if (state.distribution.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.dist_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            val maxCount = state.distribution.maxOf { it.count }
            state.distribution.forEachIndexed { i, entry ->
                item(key = "dist-${entry.moodId}-${entry.customMoodId}") {
                    StaggeredAppear(index = i + 1) {
                        DistributionBar(
                            visual = resolveVisual(entry),
                            count = entry.count,
                            maxCount = maxCount,
                        )
                    }
                }
            }
        }

        item {
            StaggeredAppear(index = 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("❤️", fontSize = 30.sp)
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(
                                text = stringResource(R.string.avg_intensity_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.avg_intensity_value, state.avgIntensity),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader(title = stringResource(R.string.week_review)) }

        item {
            StaggeredAppear(index = 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    state.last7.forEach { (date, record) ->
                        WeekDayChip(date = date, record = record, customMoods = customMoods, dark = dark)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: Int,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                AnimatedCounter(
                    value = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun DistributionBar(
    visual: MoodVisual,
    count: Int,
    maxCount: Int,
) {
    val fraction = count.toFloat() / maxCount
    val animated by animateFloatAsState(
        fraction,
        springy(Spring.DampingRatioNoBouncy, Spring.StiffnessLow),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (visual.isImage) {
                ImageThumb(
                    path = visual.imagePath,
                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)),
                )
            } else {
                Text(visual.emoji ?: "•", fontSize = 18.sp)
            }
            Text(
                text = visual.displayLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp).width(64.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animated.coerceIn(0.02f, 1f))
                        .height(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(visual.container),
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 10.dp).width(24.dp),
            )
        }
    }
}

@Composable
private fun WeekDayChip(
    date: LocalDate,
    record: MoodRecord?,
    customMoods: List<CustomMood>,
    dark: Boolean,
) {
    val visual = record?.visual(customMoods, dark)
    val scale by animateFloatAsState(
        if (visual != null) 1f else 0.85f,
        springy(),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = Dates.weekShort(date.dayOfWeek),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(visual?.container ?: MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (visual?.isImage == true) {
                ImageThumb(
                    path = visual.imagePath,
                    modifier = Modifier.size(38.dp).clip(CircleShape),
                )
            } else {
                Text(text = visual?.emoji ?: "·", fontSize = 19.sp)
            }
        }
    }
}
