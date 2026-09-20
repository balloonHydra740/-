package com.moodnotes.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.moodnotes.app.BuildConfig
import com.moodnotes.app.MoodNotesApplication
import com.moodnotes.app.R
import com.moodnotes.app.data.AppSettings
import com.moodnotes.app.ui.components.ImageThumb
import com.moodnotes.app.ui.components.SectionHeader
import com.moodnotes.app.ui.components.StaggeredAppear
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.lock.LockSetupScreen
import com.moodnotes.app.ui.mood.CustomMoodDialog
import com.moodnotes.app.ui.mood.visual
import com.moodnotes.app.ui.theme.BackgroundPreset
import com.moodnotes.app.ui.theme.LocalAppDarkTheme
import com.moodnotes.app.ui.welcome.DarkModeChips
import com.moodnotes.app.ui.welcome.LicenseViewer
import com.moodnotes.app.ui.welcome.ThemeColorGrid
import com.moodnotes.app.ui.welcome.WelcomeScreen
import com.moodnotes.app.work.ReminderWorker
import com.moodnotes.app.ui.theme.brush

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoodNotesApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(app, app.repository, app.settingsRepository) }
        },
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val customMoods by viewModel.customMoods.collectAsStateWithLifecycle()
    val savedThemes by viewModel.savedThemes.collectAsStateWithLifecycle()
    val dark = LocalAppDarkTheme.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showLockSetup by remember { mutableStateOf(false) }
    var replayWelcome by remember { mutableStateOf(false) }
    var showLicense by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.exportData(uri)
    }
    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.setReminder(true, settings.reminderHour, settings.reminderMinute, granted)
    }

    // 全屏覆盖层：隐私锁配置 / 重新欢迎 / 许可协议
    AnimatedContent(
        targetState = listOf(showLockSetup, replayWelcome, showLicense),
        transitionSpec = { fadeIn(Motion60).togetherWith(fadeOut(Motion60)) },
        label = "settingsOverlay",
    ) { flags ->
        val (lock, welcome, license) = flags
        when {
            lock -> LockSetupScreen(onFinished = { showLockSetup = false })
            welcome -> WelcomeScreen(
                onFinish = {
                    replayWelcome = false
                    app.settingsRepository.completeOnboarding()
                },
            )
            license -> LicenseViewer(onClose = { showLicense = false })
            else -> SettingsList(
                onDone = onDone,
                viewModel = viewModel,
                settings = settings,
                customMoods = customMoods,
                savedThemes = savedThemes,
                dark = dark,
                showAddDialog = { showAddDialog = true },
                openLockSetup = { showLockSetup = true },
                replayWelcome = { replayWelcome = true },
                viewLicense = { showLicense = true },
                onExport = { exportLauncher.launch("moodnotes-export.json") },
                onRequestNotifPermission = {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
            )
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

private val Motion60 = androidx.compose.animation.core.tween<Float>(200)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsList(
    onDone: () -> Unit,
    viewModel: SettingsViewModel,
    settings: AppSettings,
    customMoods: List<com.moodnotes.app.data.CustomMood>,
    savedThemes: List<com.moodnotes.app.data.SavedTheme>,
    dark: Boolean,
    showAddDialog: () -> Unit,
    openLockSetup: () -> Unit,
    replayWelcome: () -> Unit,
    viewLicense: () -> Unit,
    onExport: () -> Unit,
    onRequestNotifPermission: () -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        // 主题色（含主题册条目）
        item { SectionHeader(title = stringResource(R.string.theme_color)) }
        item {
            StaggeredAppear(index = 0) {
                ThemeColorGrid(
                    themeColorIndex = settings.themeColorIndex,
                    customSeed = settings.customSeedColor,
                    savedSeeds = savedThemes.map { it.seedColor },
                    onPickPreset = { index ->
                        viewModel.update { it.copy(themeColorIndex = index, customSeedColor = null) }
                    },
                    onPickSeed = { seed -> viewModel.applySeed(seed) },
                )
            }
        }

        // 主题册
        item { SectionHeader(title = stringResource(R.string.section_theme_booklet)) }
        item {
            Text(
                text = stringResource(R.string.theme_booklet_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            var hue by remember { mutableFloatStateOf(260f) }
            var sat by remember { mutableFloatStateOf(0.65f) }
            var light by remember { mutableFloatStateOf(0.55f) }
            ThemeMixerCard(
                hue = hue, saturation = sat, lightness = light,
                onHue = { hue = it },
                onSaturation = { sat = it },
                onLightness = { light = it },
                onSave = { name, seed ->
                    viewModel.saveTheme(name, seed)
                },
            )
        }
        item {
            SavedThemeList(
                themes = savedThemes,
                activeSeed = settings.customSeedColor,
                onUse = { viewModel.applySeed(it.seedColor) },
                onRename = { t, n -> viewModel.renameTheme(t.id, n) },
                onDelete = { t -> viewModel.deleteTheme(t.id) },
            )
        }

        // 背景
        item { SectionHeader(title = stringResource(R.string.section_background)) }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BackgroundPreset.entries.forEachIndexed { index, preset ->
                    val selected = settings.backgroundIndex == index
                    val scale by androidx.compose.animation.core.animateFloatAsState(if (selected) 1.06f else 1f, springy())
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    preset.brush(dark)
                                        ?: Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.background,
                                                MaterialTheme.colorScheme.background,
                                            ),
                                        ),
                                )
                                .then(
                                    if (selected) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                    } else {
                                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                    },
                                )
                                .clickable { viewModel.update { it.copy(backgroundIndex = index) } },
                        )
                        Text(
                            text = stringResource(preset.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        // 字体大小
        item { SectionHeader(title = stringResource(R.string.font_size)) }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val fontLabels = listOf(
                    stringResource(R.string.font_s),
                    stringResource(R.string.font_m),
                    stringResource(R.string.font_l),
                    stringResource(R.string.font_xl),
                )
                fontLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = settings.fontScaleIndex == index,
                        onClick = { viewModel.update { it.copy(fontScaleIndex = index) } },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                }
            }
        }

        // 外观与动效
        item { SectionHeader(title = stringResource(R.string.section_appearance)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dark_mode),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    DarkModeChips(
                        current = settings.darkModeIndex,
                        onSelect = { index -> viewModel.update { it.copy(darkModeIndex = index) } },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.reduce_motion),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.reduce_motion_desc),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.reduceMotion,
                            onCheckedChange = { viewModel.update { s -> s.copy(reduceMotion = !s.reduceMotion) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    Text(
                        text = stringResource(R.string.week_start_day),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.weekStartMonday,
                            onClick = { viewModel.update { it.copy(weekStartMonday = true) } },
                            label = { Text(stringResource(R.string.monday)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                        FilterChip(
                            selected = !settings.weekStartMonday,
                            onClick = { viewModel.update { it.copy(weekStartMonday = false) } },
                            label = { Text(stringResource(R.string.sunday)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val langLabels = listOf(
                            stringResource(R.string.lang_follow_system),
                            "简体中文",
                            "English",
                        )
                        langLabels.forEachIndexed { index, label ->
                            FilterChip(
                                selected = settings.languageIndex == index,
                                onClick = { viewModel.update { it.copy(languageIndex = index) } },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                            )
                        }
                    }
                }
            }
        }

        // 隐私锁
        item { SectionHeader(title = stringResource(R.string.lock_section)) }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { openLockSetup() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔐", fontSize = 26.sp)
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            text = if (settings.lockEnabled) stringResource(R.string.lock_enabled_status)
                            else stringResource(R.string.lock_setup_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.lock_desc),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 每日提醒
        item { SectionHeader(title = stringResource(R.string.section_reminder)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    R.string.reminder_desc,
                                    "%02d:%02d".format(settings.reminderHour, settings.reminderMinute),
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (viewModel.reminderPermissionDenied) {
                                Text(
                                    text = stringResource(R.string.reminder_permission_denied),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Switch(
                            checked = settings.reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33) {
                                    onRequestNotifPermission()
                                } else {
                                    viewModel.setReminder(enabled, settings.reminderHour, settings.reminderMinute, null)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                    if (settings.reminderEnabled) {
                        Text(
                            text = stringResource(R.string.reminder_time_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(8, 12, 18, 21, 22).forEach { h ->
                                FilterChip(
                                    selected = settings.reminderHour == h && settings.reminderMinute == 0,
                                    onClick = { viewModel.setReminder(true, h, 0, true) },
                                    label = { Text("%02d:00".format(h)) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 自定义心情
        item {
            SectionHeader(
                title = stringResource(R.string.custom_moods),
                actionLabel = stringResource(R.string.action_add),
                onAction = showAddDialog,
            )
        }
        if (customMoods.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.custom_moods_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(customMoods, key = { it.id }) { mood ->
                val visual = mood.visual(dark)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(visual.container),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (visual.isImage) {
                                ImageThumb(
                                    path = visual.imagePath,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)),
                                )
                            } else {
                                Text(visual.emoji ?: "•", fontSize = 20.sp)
                            }
                        }
                        Text(
                            text = mood.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                        )
                        IconButton(onClick = { viewModel.deleteCustomMood(mood.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.cd_delete_mood),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        // 数据
        item { SectionHeader(title = stringResource(R.string.section_data)) }
        item {
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.export_json), style = MaterialTheme.typography.titleMedium)
            }
            AnimatedVisibility(
                visible = viewModel.exportState != com.moodnotes.app.ui.settings.ExportState.IDLE,
                enter = fadeIn() + slideInHorizontally { it / 4 },
                exit = fadeOut(),
            ) {
                val failed = viewModel.exportState == com.moodnotes.app.ui.settings.ExportState.FAILED
                Text(
                    text = stringResource(if (failed) R.string.export_failed else R.string.export_done),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // 关于
        item { SectionHeader(title = stringResource(R.string.section_about)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("💜", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.welcome_app_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.version_format, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.author_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.about_tagline),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = viewLicense) {
                            Text(stringResource(R.string.license_view), color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = replayWelcome) {
                            Text(stringResource(R.string.replay_welcome), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
