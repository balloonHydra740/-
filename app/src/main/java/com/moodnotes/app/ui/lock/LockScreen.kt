package com.moodnotes.app.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moodnotes.app.MoodNotesApplication
import com.moodnotes.app.R
import kotlinx.coroutines.delay

/** 锁屏阶段：输 PIN → 保密问题 → 重置 PIN。 */
private enum class LockStage { PIN, SECURITY, RESET_PIN }

/**
 * 全屏锁：生物识别优先（若启用），PIN 键盘兜底，
 * 忘记 PIN 可走保密问题（答对 2 题重置）。
 */
@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoodNotesApplication
    val settingsRepo = app.settingsRepository

    var stage by remember { mutableStateOf(LockStage.PIN) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    // 冷却倒计时（秒）；>0 时键盘禁用
    var cooldownSec by remember { mutableLongStateOf(settingsRepo.cooldownRemainingMs() / 1000) }
    LaunchedEffect(cooldownSec) {
        if (cooldownSec > 0) {
            delay(1000)
            cooldownSec = settingsRepo.cooldownRemainingMs() / 1000
        }
    }

    fun failAttempt(msg: String?) {
        error = true
        shakeKey++
        message = msg
        pin = ""
        val untilMs = settingsRepo.registerFail()
        if (untilMs > 0) cooldownSec = (untilMs - System.currentTimeMillis()) / 1000 + 1
    }

    fun success() {
        settingsRepo.clearFails()
        onUnlocked()
    }

    // 生物识别：进入锁屏自动弹一次（启用且设备支持时）
    val biometricEnabled = settingsRepo.settings.value.lockBiometric
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            val activity = context as? android.app.Activity
            if (activity != null) {
                BiometricHelper.authenticate(activity) { ok ->
                    if (ok) success()
                    // 失败/取消则留在 PIN 键盘
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        when (stage) {
            LockStage.PIN -> PinStage(
                pin = pin,
                error = error,
                shakeKey = shakeKey,
                message = message,
                cooldownSec = cooldownSec,
                biometricEnabled = biometricEnabled,
                onPinChange = { pin = it; error = false; message = null },
                onBiometric = {
                    (context as? android.app.Activity)?.let { activity ->
                        BiometricHelper.authenticate(activity) { ok -> if (ok) success() }
                    }
                },
                onSubmit = {
                    if (cooldownSec <= 0) {
                        if (settingsRepo.verifyPin(pin)) success() else failAttempt(null)
                    }
                },
                onForgot = { stage = LockStage.SECURITY; pin = ""; error = false; message = null },
            )

            LockStage.SECURITY -> SecurityStage(
                onBack = { stage = LockStage.PIN; error = false; message = null },
                onFail = { failAttempt(null) },
                onPassed = {
                    settingsRepo.clearFails()
                    stage = LockStage.RESET_PIN
                    message = null
                },
                cooldownSec = cooldownSec,
            )

            LockStage.RESET_PIN -> ResetPinStage(
                cooldownSec = cooldownSec,
                onDone = { newPin ->
                    settingsRepo.updateLockPin(newPin)
                    success()
                },
            )
        }
    }
}

@Composable
private fun PinStage(
    pin: String,
    error: Boolean,
    shakeKey: Int,
    message: String?,
    cooldownSec: Long,
    biometricEnabled: Boolean,
    onPinChange: (String) -> Unit,
    onBiometric: () -> Unit,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
) {
    val locked = cooldownSec > 0
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))
        Text("🔒", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.lock_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (locked) stringResource(R.string.cooldown_msg, cooldownSec)
            else stringResource(R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = if (locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        ShakeHost(shakeKey) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PinDots(length = pin.length, error = error)
                AnimatedVisibility(visible = message != null || error) {
                    Text(
                        text = message ?: stringResource(R.string.pin_wrong),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        PinPad(
            enabled = !locked,
            biometricButton = if (biometricEnabled) {
                { BiometricPadButton(onClick = onBiometric) }
            } else null,
            onDigit = { c ->
                if (pin.length < 4) {
                    val next = pin + c
                    onPinChange(next)
                    if (next.length == 4) onSubmit()
                }
            },
            onBackspace = { onPinChange(pin.dropLast(1)) },
            modifier = Modifier.widthIn(max = 300.dp),
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onForgot, enabled = !locked) {
            Text(stringResource(R.string.forgot_pin), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun SecurityStage(
    onBack: () -> Unit,
    onFail: () -> Unit,
    onPassed: () -> Unit,
    cooldownSec: Long,
) {
    val app = LocalContext.current.applicationContext as MoodNotesApplication
    val questions = remember { app.settingsRepository.lockQuestions() }
    var index by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var shakeKey by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Text(
                text = stringResource(R.string.security_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.security_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        if (questions.isEmpty()) {
            Text(
                text = stringResource(R.string.pin_saved_wrong_state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            return@Column
        }
        val q = questions[index]
        Text(
            text = stringResource(R.string.lock_question_n, index + 1),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        ShakeHost(shakeKey) {
            Text(
                text = q.question,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.OutlinedTextField(
            value = answer,
            onValueChange = { answer = it; wrong = false },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.lock_answer_hint)) },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            isError = wrong,
        )
        AnimatedVisibility(visible = wrong) {
            Text(
                text = stringResource(R.string.security_wrong),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(22.dp))
        androidx.compose.material3.Button(
            onClick = {
                if (cooldownSec > 0) return@Button
                val ok = app.settingsRepository.verifySecurityAnswer(index, answer)
                if (ok) {
                    correctCount++
                    if (correctCount >= 2) {
                        onPassed()
                        return@Button
                    }
                }
                if (index == questions.size - 1) {
                    // 全部答完仍未达 2 题
                    if (correctCount < 2) {
                        wrong = true
                        shakeKey++
                        onFail()
                        index = 0
                        correctCount = 0
                        answer = ""
                    }
                } else {
                    index++
                    answer = ""
                    wrong = !ok
                }
            },
            enabled = answer.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        ) {
            Text(stringResource(R.string.action_confirm), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ResetPinStage(
    cooldownSec: Long,
    onDone: (String) -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var shakeKey by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf(false) }

    val enteringFirst = first.length < 4
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            text = stringResource(R.string.security_passed),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        ShakeHost(shakeKey) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (enteringFirst) stringResource(R.string.new_pin_hint)
                    else stringResource(R.string.confirm_pin_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                PinDots(length = (if (enteringFirst) first else second).length, error = error)
                AnimatedVisibility(visible = error) {
                    Text(
                        text = stringResource(R.string.pin_mismatch),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        PinPad(
            enabled = cooldownSec <= 0,
            onDigit = { c ->
                if (enteringFirst) {
                    first += c
                } else {
                    second += c
                    if (second.length == 4) {
                        if (second == first) onDone(first) else {
                            error = true
                            shakeKey++
                            first = ""
                            second = ""
                        }
                    }
                }
            },
            onBackspace = {
                if (enteringFirst) first = first.dropLast(1) else second = second.dropLast(1)
                error = false
            },
            modifier = Modifier.widthIn(max = 300.dp),
        )
    }
}

/** 保密问题数据（锁屏内使用 util.Security 的存储格式）。 */
typealias LockSecurityQuestion = com.moodnotes.app.util.SecurityQuestion
