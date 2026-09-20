package com.moodnotes.app.ui.lock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.moodnotes.app.MoodNotesApplication
import com.moodnotes.app.R
import com.moodnotes.app.ui.lock.BiometricHelper
import com.moodnotes.app.ui.theme.Motion
import com.moodnotes.app.util.Security
import com.moodnotes.app.util.SecurityQuestion

/**
 * 隐私锁配置（设置页内全屏覆盖层）：
 * stage 0 = 设置 PIN（两遍）→ stage 1 = 三个保密问题 → 完成；
 * 进入时已开启锁则直接展示管理面板（改 PIN / 重设问题 / 生物识别 / 关闭）。
 */
@Composable
fun LockSetupScreen(
    onFinished: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as MoodNotesApplication
    val repo = app.settingsRepository
    val lockEnabled = repo.settings.value.lockEnabled

    // -1 = 管理面板；0 = 输 PIN；1 = 保密问题
    // 用 remember 而非 rememberSaveable：本覆盖层每次打开都应从全新状态开始，
    // 否则 SettingsScreen 的 AnimatedContent 会在关闭再打开时恢复上次的半成品阶段
    // （例如已进保密问题页但 PIN 已丢失），导致用空 PIN 设置锁——正是「锁设不上」的病根。
    var stage by remember { mutableStateOf(if (lockEnabled) -1 else 0) }
    // true = 只改 PIN（完成后保留原保密问题，不进问题页）
    var changePinOnly by remember { mutableStateOf(false) }
    // true = 只重设保密问题（完成后保留原 PIN，不掉锁）
    var resetQuestionsOnly by remember { mutableStateOf(false) }
    var firstPin by remember { mutableStateOf("") }
    var secondPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var shakeKey by remember { mutableIntStateOf(0) }

    val enteringFirst = firstPin.length < 4

    // transitionSpec 非 Composable 作用域：在这里先读开关，进 lambda 用普通 tween
    val reduceMotion = com.moodnotes.app.ui.theme.LocalReduceMotion.current
    val fadeInMs = if (reduceMotion) 1 else 260
    val slideMs = if (reduceMotion) 1 else 320
    val fadeOutMs = if (reduceMotion) 1 else 180
    val slideOutMs = if (reduceMotion) 1 else 260

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            val forward = targetState > initialState
            (fadeIn(tween(fadeInMs)) + slideInHorizontally(tween(slideMs)) { if (forward) it / 4 else -it / 4 })
                .togetherWith(
                    fadeOut(tween(fadeOutMs)) + slideOutHorizontally(tween(slideOutMs)) {
                        if (forward) -it / 5 else it / 5
                    },
                )
        },
        label = "lockSetup",
    ) { s ->
        when (s) {
            0 -> Column(
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onFinished) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                    Text(
                        text = stringResource(R.string.lock_setup_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = if (enteringFirst) stringResource(R.string.lock_setup_pin_hint)
                    else stringResource(R.string.lock_setup_reenter),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                ShakeHost(shakeKey) {
                    PinDots(length = (if (enteringFirst) firstPin else secondPin).length, error = pinError)
                }
                AnimatedVisibility(visible = pinError) {
                    Text(
                        text = stringResource(R.string.pin_mismatch),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
                PinPad(
                    onDigit = { c ->
                        if (enteringFirst) {
                            firstPin += c
                        } else {
                            secondPin += c
                            if (secondPin.length == 4) {
                                if (secondPin == firstPin) {
                                    if (changePinOnly) {
                                        repo.updateLockPin(secondPin)
                                        onFinished()
                                    } else {
                                        stage = 1
                                    }
                                } else {
                                    pinError = true
                                    shakeKey++
                                    firstPin = ""
                                    secondPin = ""
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (enteringFirst) firstPin = firstPin.dropLast(1)
                        else secondPin = secondPin.dropLast(1)
                        pinError = false
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            }

            1 -> QuestionsSetupScreen(
                title = stringResource(R.string.lock_setup_title),
                showBiometric = !resetQuestionsOnly,
                onBack = onFinished,
                onComplete = { questions, biometric ->
                    if (resetQuestionsOnly) {
                        // 仅重设保密问题：保留原 PIN 与开关状态，只更新问题
                        repo.updateLockQuestions(questions)
                        onFinished()
                    } else {
                        repo.setLock(firstPin, questions, biometric)
                        onFinished()
                    }
                },
            )

            else -> LockManagePanel(
                onChangePin = {
                    changePinOnly = true
                    resetQuestionsOnly = false
                    stage = 0
                },
                onResetQuestions = {
                    changePinOnly = false
                    resetQuestionsOnly = true
                    stage = 1
                },
                onExit = onFinished,
            )
        }
    }
}

/**
 * 保密问题设置：3 题，可从 6 个预设里选或自定义，答案走哈希。
 * onComplete 交给外层真正落盘（需要 PIN）。
 */
@Composable
fun QuestionsSetupScreen(
    title: String,
    onBack: () -> Unit,
    onComplete: (questions: List<SecurityQuestion>, biometric: Boolean) -> Unit,
    showBiometric: Boolean = true,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    val presets = listOf(
        stringResource(R.string.sq_birth_city),
        stringResource(R.string.sq_primary_school),
        stringResource(R.string.sq_mother_name),
        stringResource(R.string.sq_first_pet),
        stringResource(R.string.sq_best_friend),
        stringResource(R.string.sq_first_movie),
    )
    // 每题：问题文本（点预设直接填入，可再编辑成自定义问题）+ 答案
    var questions by remember { mutableStateOf(listOf(presets[0], presets[1], presets[2])) }
    var answers by remember { mutableStateOf(listOf("", "", "")) }
    var useBiometric by remember { mutableStateOf(false) }
    val biometricAvailable = activity?.let { BiometricHelper.canAuthenticate(it) } == true

    val allValid = (0..2).all { i -> questions[i].isNotBlank() && answers[i].isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.lock_setup_questions),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        (0..2).forEach { i ->
            Text(
                text = stringResource(R.string.lock_question_n, i + 1),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            QuestionPickerRow(
                presets = presets,
                question = questions[i],
                onPick = { p -> questions = questions.toMutableList().also { it[i] = p } },
                onQuestionChange = { q -> questions = questions.toMutableList().also { it[i] = q } },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = answers[i],
                onValueChange = { v -> answers = answers.toMutableList().also { it[i] = v } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.lock_answer_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
            Spacer(Modifier.height(16.dp))
        }

        if (showBiometric) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lock_biometric_enable),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!biometricAvailable) {
                        Text(
                            text = stringResource(R.string.lock_biometric_unavailable),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = useBiometric && biometricAvailable,
                    onCheckedChange = { useBiometric = it && biometricAvailable },
                    enabled = biometricAvailable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        Button(
            onClick = {
                val qs = (0..2).map { i ->
                    val salt = Security.randomSalt()
                    SecurityQuestion(
                        question = questions[i].trim(),
                        answerHash = Security.hash(answers[i], salt),
                        answerSalt = salt,
                    )
                }
                onComplete(qs, useBiometric && biometricAvailable)
            },
            enabled = allValid,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(50),
        ) {
            Text(stringResource(R.string.action_confirm), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun QuestionPickerRow(
    presets: List<String>,
    question: String,
    onPick: (String) -> Unit,
    onQuestionChange: (String) -> Unit,
) {
    Column {
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        ) {
            presets.forEach { p ->
                FilterChip(
                    selected = question == p,
                    onClick = { onPick(p) },
                    label = { Text(p, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.lock_q_custom)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

/**
 * 已开启状态的管理面板：状态 + 生物识别开关 + 关闭。
 * （改 PIN 通过重新进入 stage 0 完成，问题保留；重设问题走独立阶段）
 */
@Composable
private fun LockManagePanel(
    onChangePin: () -> Unit,
    onResetQuestions: () -> Unit,
    onExit: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as MoodNotesApplication
    val repo = app.settingsRepository
    val settings = repo.settings.value
    var confirmDisable by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(
                text = stringResource(R.string.lock_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.lock_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onChangePin) {
            Text(stringResource(R.string.lock_change_pin), color = MaterialTheme.colorScheme.primary)
        }
        TextButton(onClick = onResetQuestions) {
            Text(stringResource(R.string.lock_reset_questions), color = MaterialTheme.colorScheme.primary)
        }
        val activity = context as? android.app.Activity
        val biometricAvailable = activity?.let { BiometricHelper.canAuthenticate(it) } == true
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lock_biometric_enable),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!biometricAvailable) {
                    Text(
                        text = stringResource(R.string.lock_biometric_unavailable),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(
                checked = settings.lockBiometric && biometricAvailable,
                onCheckedChange = { repo.setLockBiometric(it && biometricAvailable) },
                enabled = biometricAvailable,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { confirmDisable = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Text(stringResource(R.string.action_disable_lock), style = MaterialTheme.typography.titleMedium)
        }
    }

    if (confirmDisable) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDisable = false },
            title = { Text(stringResource(R.string.action_disable_lock)) },
            text = { Text(stringResource(R.string.lock_disable_confirm)) },
            confirmButton = {
                TextButton(onClick = { repo.disableLock(); confirmDisable = false; onExit() }) {
                    Text(stringResource(R.string.action_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisable = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
