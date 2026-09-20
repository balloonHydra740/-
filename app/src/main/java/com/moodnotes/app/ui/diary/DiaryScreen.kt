package com.moodnotes.app.ui.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.moodnotes.app.data.DiaryEntry
import com.moodnotes.app.ui.components.EmptyState
import com.moodnotes.app.ui.components.ImageThumb
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.mood.visual
import com.moodnotes.app.ui.theme.LocalAppDarkTheme
import com.moodnotes.app.ui.moodNotesViewModel
import com.moodnotes.app.util.Dates
import java.time.YearMonth

@Composable
fun DiaryScreen(
    onOpenEntry: (Long) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = moodNotesViewModel { DiaryViewModel(it) },
) {
    val entries by viewModel.filteredEntries.collectAsStateWithLifecycle()
    val customMoods by viewModel.customMoods.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val dark = LocalAppDarkTheme.current
    val searching = query.isNotBlank()

    val drafts = entries.filter { it.isDraft }
    val published = entries.filter { !it.isDraft }
    // 分组在 Composable 作用域内缓存（LazyListScope 里不能调 remember）
    val groups = remember(published) {
        published.groupBy { YearMonth.from(Dates.fromEpochDay(it.date)) }
            .toSortedMap(compareByDescending { it })
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column {
                    Text(
                        text = stringResource(R.string.diary_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { -it / 3 },
                        exit = fadeOut(tween(180)),
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::onQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        )
                    }
                }
            }

            when {
                entries.isEmpty() && searching -> {
                    item(key = "search-empty") {
                        EmptyState(emoji = "🔍", title = stringResource(R.string.search_empty))
                    }
                }
                entries.isEmpty() -> {
                    item(key = "empty") {
                        EmptyState(
                            emoji = "📔",
                            title = stringResource(R.string.diary_empty_title),
                            subtitle = stringResource(R.string.diary_empty_hint),
                        )
                    }
                }
                searching -> {
                    item(key = "search-count") {
                        Text(
                            text = stringResource(R.string.search_result_count, entries.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(entries, key = { it.id }) { entry ->
                        DiaryCard(
                            entry = entry,
                            customMoods = customMoods,
                            isDraft = entry.isDraft,
                            dark = dark,
                            onClick = { onOpenEntry(entry.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                else -> {
                    if (drafts.isNotEmpty()) {
                        item(key = "draft-header") {
                            Text(
                                text = stringResource(R.string.drafts_count, drafts.size),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                        items(drafts, key = { it.id }) { entry ->
                            DiaryCard(
                                entry = entry,
                                customMoods = customMoods,
                                isDraft = true,
                                dark = dark,
                                onClick = { onOpenEntry(entry.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (published.isNotEmpty()) {
                        groups.forEach { (month, monthEntries) ->
                            item(key = "header-$month") {
                                Text(
                                    text = Dates.monthTitle(month),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            }
                            items(monthEntries, key = { it.id }) { entry ->
                                DiaryCard(
                                    entry = entry,
                                    customMoods = customMoods,
                                    isDraft = false,
                                    dark = dark,
                                    onClick = { onOpenEntry(entry.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            val fabScale by animateFloatAsState(1f, springy(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.cd_write_diary),
                modifier = Modifier.graphicsLayer {
                    scaleX = fabScale
                    scaleY = fabScale
                },
            )
        }
    }
}

@Composable
private fun DiaryCard(
    entry: DiaryEntry,
    customMoods: List<com.moodnotes.app.data.CustomMood>,
    isDraft: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visual = entry.visual(customMoods, dark)
    val images = remember(entry.images) { entry.imageList() }
    val videos = remember(entry.videos) { entry.videoList() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDraft) MaterialTheme.colorScheme.surfaceContainerHighest
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (visual != null) {
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (visual.isImage) {
                            ImageThumb(
                                path = visual.imagePath,
                                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)),
                            )
                        } else {
                            Text(visual.emoji ?: "•", fontSize = 20.sp)
                        }
                    }
                }
                Text(
                    text = entry.title.ifBlank { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = if (visual != null) 8.dp else 0.dp),
                )
                if (isDraft) {
                    Text(
                        text = stringResource(R.string.draft_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp).padding(start = 4.dp),
                )
            }
            if (entry.content.isNotBlank()) {
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    images.take(3).forEach { path ->
                        ImageThumb(
                            path = path,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            } else if (videos.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    videos.take(3).forEach { path ->
                        com.moodnotes.app.ui.components.VideoThumb(
                            path = path,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            val dateText = Dates.monthDay(Dates.fromEpochDay(entry.date))
            Text(
                text = if (isDraft) stringResource(R.string.draft_date_label, dateText) else dateText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDraft) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}
