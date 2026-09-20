package com.moodnotes.app.ui.diary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.moodnotes.app.ui.components.ImageThumb
import com.moodnotes.app.ui.components.SectionHeader
import com.moodnotes.app.ui.components.StaggeredAppear
import com.moodnotes.app.ui.mood.CustomMoodDialog
import com.moodnotes.app.ui.mood.MoodPicker
import com.moodnotes.app.util.ImageStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as MoodNotesApplication
    val viewModel: DiaryEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DiaryEditViewModel(app.repository, createSavedStateHandle()) }
        },
    )
    val customMoods by viewModel.customMoods.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var justSaved by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris ->
        if (uris.isNotEmpty()) {
            importing = true
            scope.launch {
                val paths = uris.mapNotNull { ImageStore.import(context, it, "diary") }
                viewModel.addImages(paths)
                importing = false
            }
        }
    }
    val pickVideos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3),
    ) { uris ->
        if (uris.isNotEmpty()) {
            importing = true
            scope.launch {
                val paths = uris.mapNotNull { ImageStore.importVideo(context, it) }
                viewModel.addVideos(paths)
                importing = false
            }
        }
    }

    LaunchedEffect(viewModel.saved) {
        if (viewModel.saved) {
            justSaved = true
            delay(280)
            onDone()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(
                    when {
                        viewModel.isNew -> R.string.edit_new_title
                        viewModel.isEditingDraft -> R.string.edit_draft_title
                        else -> R.string.edit_entry_title
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (!viewModel.isNew) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.cd_delete_diary),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        StaggeredAppear(index = 0) {
            Text(
                text = viewModel.dateLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(16.dp))

        SectionHeader(title = stringResource(R.string.mood_optional))
        Spacer(Modifier.height(10.dp))
        MoodPicker(
            customMoods = customMoods,
            selectedMoodId = viewModel.moodId,
            selectedCustomMoodId = viewModel.customMoodId,
            onSelectMood = { viewModel.onMoodSelect(if (viewModel.moodId == it) null else it) },
            onSelectCustomMood = { viewModel.onCustomMoodSelect(if (viewModel.customMoodId == it) null else it) },
            onAddCustom = { showAddDialog = true },
            buttonSize = 50.dp,
            iconSize = 24.sp,
        )

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = viewModel.title,
            onValueChange = viewModel::onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.title_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            textStyle = MaterialTheme.typography.headlineSmall,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = viewModel.content,
            onValueChange = viewModel::onContentChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp),
            placeholder = { Text(stringResource(R.string.content_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )

        AnimatedContent(
            targetState = viewModel.images,
            transitionSpec = {
                (fadeIn(tween(240)) + scaleIn(initialScale = 0.95f, animationSpec = tween(260)))
                    .togetherWith(fadeOut(tween(160)))
            },
            label = "imageGrid",
        ) { images ->
            if (images.isNotEmpty()) {
                Column {
                    Spacer(Modifier.height(16.dp))
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
                            Box {
                                ImageThumb(
                                    path = path,
                                    modifier = Modifier.size(104.dp).clip(RoundedCornerShape(16.dp)),
                                )
                                IconButton(
                                    onClick = { viewModel.removeImage(path) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_remove_image),
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = viewModel.videos,
            transitionSpec = {
                (fadeIn(tween(240)) + scaleIn(initialScale = 0.95f, animationSpec = tween(260)))
                    .togetherWith(fadeOut(tween(160)))
            },
            label = "videoGrid",
        ) { videos ->
            if (videos.isNotEmpty()) {
                Column {
                    Spacer(Modifier.height(16.dp))
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
                            Box {
                                com.moodnotes.app.ui.components.VideoThumb(
                                    path = path,
                                    modifier = Modifier.size(104.dp).clip(RoundedCornerShape(16.dp)),
                                )
                                IconButton(
                                    onClick = { viewModel.removeVideo(path) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.cd_remove_video),
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !importing,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    if (importing) stringResource(R.string.importing) else stringResource(R.string.action_add_image),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(
                onClick = {
                    pickVideos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                enabled = !importing && viewModel.videos.size < 3,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    if (importing) stringResource(R.string.importing) else stringResource(R.string.action_add_video),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (importing) {
                Spacer(Modifier.size(6.dp))
                // 导入中：呼吸闪烁
                val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "importPulse")
                val pulse by infinite.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                    ),
                    label = "pulse",
                )
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                    modifier = Modifier.size(16.dp).graphicsLayer { alpha = pulse },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        val hasContent = viewModel.title.isNotBlank() ||
            viewModel.content.isNotBlank() ||
            viewModel.images.isNotEmpty()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { viewModel.save(publish = false) },
                enabled = hasContent && !importing && !justSaved,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.action_save_draft), style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = { viewModel.save(publish = true) },
                enabled = hasContent && !importing && !justSaved,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                AnimatedContent(
                    targetState = justSaved,
                    transitionSpec = {
                        (fadeIn(tween(180)) + scaleIn(initialScale = 0.6f, animationSpec = tween(220)))
                            .togetherWith(fadeOut(tween(120)))
                    },
                    label = "saveLabel",
                ) { savedDone ->
                    if (savedDone) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                    } else {
                        Text(stringResource(R.string.action_save), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(viewModel.title.ifBlank { stringResource(R.string.untitled) }) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
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
