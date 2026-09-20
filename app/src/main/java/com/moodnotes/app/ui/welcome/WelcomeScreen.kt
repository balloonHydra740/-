package com.moodnotes.app.ui.welcome

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moodnotes.app.MoodNotesApplication
import com.moodnotes.app.R
import com.moodnotes.app.ui.components.EaseOutQuint
import com.moodnotes.app.ui.components.StaggeredAppear
import com.moodnotes.app.ui.components.springy
import com.moodnotes.app.ui.theme.ThemePresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 首次启动欢迎流程：介绍 → 语言 → 外观 → 许可协议。
 * 语言/外观选择即时写入设置并实时预览。
 */
@Composable
fun WelcomeScreen(
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoodNotesApplication
    val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()
    val savedThemes by app.repository.observeSavedThemes()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 4 })
    var agreed by remember { mutableStateOf(false) }
    var showSkipLicense by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // 顶部步骤指示
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pagerState.currentPage > 0) {
                IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.welcome_btn_back))
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            StepDots(current = pagerState.currentPage, total = 4)
            // 跳过 = 直接弹出许可协议，必须同意才能进入应用
            TextButton(onClick = { showSkipLicense = true }) {
                Text(stringResource(R.string.welcome_btn_skip))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = pagerState.currentPage != 3,
        ) { page ->
            when (page) {
                0 -> IntroPage()
                1 -> LanguagePage(
                    current = settings.languageIndex,
                    onSelect = { index -> app.settingsRepository.update { it.copy(languageIndex = index) } },
                )
                2 -> LookPage(
                    themeColorIndex = settings.themeColorIndex,
                    customSeed = settings.customSeedColor,
                    darkModeIndex = settings.darkModeIndex,
                    savedThemes = savedThemes.map { it.seedColor },
                    onPickPreset = { index ->
                        app.settingsRepository.update { it.copy(themeColorIndex = index, customSeedColor = null) }
                    },
                    onPickSeed = { seed ->
                        app.settingsRepository.applySeedColor(seed)
                    },
                    onDarkMode = { index ->
                        app.settingsRepository.update { it.copy(darkModeIndex = index) }
                    },
                )
                else -> LicensePage(
                    agreed = agreed,
                    onAgreeChange = { agreed = it },
                )
            }
        }

        // 底部操作按钮
        val isLast = pagerState.currentPage == 3
        val canProceed = !isLast || agreed
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp)) {
            Column {
                Button(
                    onClick = {
                        if (isLast) {
                            onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    enabled = canProceed,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(if (isLast) R.string.license_btn_agree else R.string.welcome_btn_next),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (isLast) {
                    TextButton(
                        onClick = { (context as? Activity)?.finishAffinity() },
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
                    ) {
                        Text(stringResource(R.string.license_btn_exit), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // 「跳过」触发的许可协议弹窗：同意才算完成，不同意则退出应用
    if (showSkipLicense) {
        SkipLicenseDialog(
            onAgree = onFinish,
            onExit = { (context as? Activity)?.finishAffinity() },
            onDismiss = { showSkipLicense = false },
        )
    }
}

/** 许可协议快捷弹窗（跳过引导时）。 */
@Composable
private fun SkipLicenseDialog(
    onAgree: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isZh = java.util.Locale.getDefault().language != "en"
    val fileName = if (isZh) "license/ncal_zh.md" else "license/ncal_en.md"
    val text by produceState(initialValue = "", fileName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(fileName).bufferedReader().readText() }.getOrDefault("")
        }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.license_title_in_app)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(stringResource(R.string.license_btn_agree), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text(stringResource(R.string.license_btn_exit), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val active = i == current
            val width by animateDpAsState(
                if (active) 22.dp else 8.dp,
                androidx.compose.animation.core.spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
            )
        }
    }
}

@Composable
private fun IntroPage() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            StaggeredAppear(index = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💜", fontSize = 64.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.welcome_intro_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.welcome_intro_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        val cards = listOf(
            Triple("🌤️", R.string.intro_card1_title, R.string.intro_card1_desc),
            Triple("📅", R.string.intro_card2_title, R.string.intro_card2_desc),
            Triple("📖", R.string.intro_card3_title, R.string.intro_card3_desc),
            Triple("📊", R.string.intro_card4_title, R.string.intro_card4_desc),
        )
        cards.forEachIndexed { i, (emoji, title, desc) ->
            item {
                StaggeredAppear(index = i + 1) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(emoji, fontSize = 26.sp)
                            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                                Text(
                                    text = stringResource(title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePage(
    current: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        StaggeredAppear(index = 0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌐", fontSize = 44.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.step_language_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.step_language_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(30.dp))
        val options = listOf(
            stringResource(R.string.lang_follow_system),
            "简体中文",
            "English",
        )
        options.forEachIndexed { index, label ->
            StaggeredAppear(index = index + 1) {
                val selected = current == index
                val scale by animateFloatAsState(if (selected) 1f else 0.98f, springy())
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(index) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LookPage(
    themeColorIndex: Int,
    customSeed: Long?,
    darkModeIndex: Int,
    savedThemes: List<Long>,
    onPickPreset: (Int) -> Unit,
    onPickSeed: (Long) -> Unit,
    onDarkMode: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎨", fontSize = 44.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.step_look_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.step_look_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(26.dp))
        }
        item {
            StaggeredAppear(index = 0) {
                Column {
                    ThemeColorGrid(
                        themeColorIndex = themeColorIndex,
                        customSeed = customSeed,
                        savedSeeds = savedThemes,
                        onPickPreset = onPickPreset,
                        onPickSeed = onPickSeed,
                    )
                    Spacer(Modifier.height(26.dp))
                    DarkModeChips(current = darkModeIndex, onSelect = onDarkMode)
                }
            }
        }
    }
}

/** 主题色网格：内置预设 + 已保存主题（欢迎页与设置页共用）。 */
@Composable
fun ThemeColorGrid(
    themeColorIndex: Int,
    customSeed: Long?,
    savedSeeds: List<Long>,
    onPickPreset: (Int) -> Unit,
    onPickSeed: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ThemePresets.forEachIndexed { index, preset ->
                val selected = customSeed == null && themeColorIndex == index
                val scale by animateFloatAsState(if (selected) 1.12f else 1f, springy())
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(CircleShape)
                            .background(preset.seed)
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                },
                            )
                            .clickable { onPickPreset(index) },
                    )
                    Text(
                        text = stringResource(preset.nameRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            savedSeeds.forEachIndexed { index, seed ->
                val selected = customSeed == seed
                val scale by animateFloatAsState(if (selected) 1.12f else 1f, springy())
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(CircleShape)
                            .background(Color(seed.toInt()))
                            .then(
                                if (selected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                },
                            )
                            .clickable { onPickSeed(seed) },
                    )
                    Text(
                        text = "#${Integer.toHexString(seed.toInt()).takeLast(6).uppercase()}",
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
}

/** 深浅模式三选一（欢迎页与设置页共用）。 */
@Composable
fun DarkModeChips(
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = listOf(
        stringResource(R.string.dark_follow_system),
        stringResource(R.string.dark_light),
        stringResource(R.string.dark_dark),
    )
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = current == index,
                onClick = { onSelect(index) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun LicensePage(
    agreed: Boolean,
    onAgreeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val isZh = java.util.Locale.getDefault().language != "en"
    val fileName = if (isZh) "license/ncal_zh.md" else "license/ncal_en.md"
    val licenseText by produceState(initialValue = "", fileName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(fileName).bufferedReader().readText()
            }.getOrDefault("")
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.step_license_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = licenseText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onAgreeChange(!agreed) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = agreed, onCheckedChange = onAgreeChange)
            Text(
                text = stringResource(R.string.license_agree_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
