package com.moodnotes.app.ui.diary

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moodnotes.app.MoodNotesApplication
import com.moodnotes.app.R
import com.moodnotes.app.ui.components.FullscreenImage
import com.moodnotes.app.ui.components.ImageThumb
import com.moodnotes.app.ui.components.MarkdownText
import com.moodnotes.app.ui.components.VideoThumb
import com.moodnotes.app.ui.mood.displayLabel
import com.moodnotes.app.ui.mood.visual
import com.moodnotes.app.ui.theme.LocalAppDarkTheme
import com.moodnotes.app.util.Dates
import com.moodnotes.app.util.ImageStore

/**
 * 日记查看模式：展示标题、正文（Markdown）、心情、图片与视频。
 * 图片可点击放大，视频点击调起系统播放器，右上角进入编辑。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryViewScreen(
    onBack: () -> Unit,
    onEdit: (entryId: Long, date: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as MoodNotesApplication
    val viewModel: DiaryViewViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DiaryViewViewModel(app.repository, createSavedStateHandle()) }
        },
    )
    val entry by viewModel.entry.collectAsStateWithLifecycle()
    val customMoods by viewModel.customMoods.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dark = LocalAppDarkTheme.current

    var fullImagePath by remember { mutableStateOf<String?>(null) }

    // 已展示过一篇日记后如果变为 null（在编辑页被删除），自动返回上级。
    var shownOnce by remember { mutableStateOf(false) }
    LaunchedEffect(entry) {
        if (entry != null) shownOnce = true
        else if (shownOnce) onBack()
    }

    entry?.let { e ->
        val visual = e.visual(customMoods, dark)
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // 顶栏：返回 / 标题（编辑键）
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = stringResource(R.string.diary_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (e.isDraft) {
                    Text(
                        text = stringResource(R.string.draft_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                }
                IconButton(onClick = { onEdit(e.id, e.date) }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.cd_edit_diary),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = Dates.fullDate(Dates.fromEpochDay(e.date)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            // 心情标签（若有）
            if (visual != null) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (visual.isImage) {
                        ImageThumb(
                            path = visual.imagePath,
                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)),
                        )
                    } else {
                        Text(visual.emoji ?: "•", fontSize = 22.sp)
                    }
                    Text(
                        text = visual.displayLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            // 标题
            Spacer(Modifier.height(16.dp))
            Text(
                text = e.title.ifBlank { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // 正文（Markdown 渲染）
            if (e.content.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                MarkdownText(
                    text = e.content,
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }

            // 图片
            val images = remember(e.images) { e.imageList() }
            if (images.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.images_count, images.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    images.forEach { path ->
                        ImageThumb(
                            path = path,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { fullImagePath = path },
                        )
                    }
                }
            }

            // 视频
            val videos = remember(e.videos) { e.videoList() }
            if (videos.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.videos_count, videos.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    videos.forEach { path ->
                        VideoThumb(
                            path = path,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val intent = ImageStore.openVideoIntent(context, path)
                                    if (intent != null) {
                                        runCatching { context.startActivity(intent) }
                                            .onFailure {
                                                Toast.makeText(context, context.getString(R.string.no_video_app), Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.no_video_app), Toast.LENGTH_SHORT).show()
                                    }
                                },
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        fullImagePath?.let { path ->
            FullscreenImage(
                path = path,
                onDismiss = { fullImagePath = null },
            )
        }
    }
}
