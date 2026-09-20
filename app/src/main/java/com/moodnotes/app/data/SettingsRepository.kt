package com.moodnotes.app.data

import android.content.Context
import android.app.LocaleManager
import android.os.Build
import com.moodnotes.app.util.Security
import com.moodnotes.app.util.SecurityQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        if (next.languageIndex != _languagePersisted) {
            _languagePersisted = next.languageIndex
            applyLanguage(next.languageIndex)
        }
        prefs.edit()
            .putInt("themeColorIndex", next.themeColorIndex)
            .putLong("customSeedColor", next.customSeedColor ?: -1L)
            .putInt("backgroundIndex", next.backgroundIndex)
            .putInt("fontScaleIndex", next.fontScaleIndex)
            .putInt("darkModeIndex", next.darkModeIndex)
            .putBoolean("reduceMotion", next.reduceMotion)
            .putBoolean("weekStartMonday", next.weekStartMonday)
            .putInt("languageIndex", next.languageIndex)
            .putBoolean("onboarded", next.onboarded)
            .putBoolean("lockEnabled", next.lockEnabled)
            .putBoolean("lockBiometric", next.lockBiometric)
            .putBoolean("reminderEnabled", next.reminderEnabled)
            .putInt("reminderHour", next.reminderHour)
            .putInt("reminderMinute", next.reminderMinute)
            .apply()
    }

    /** 首次启动引导是否完成。 */
    fun completeOnboarding() = update { it.copy(onboarded = true) }

    /** 应用主题册颜色：seed 为 Long ARGB，null 表示回到内置预设。 */
    fun applySeedColor(seed: Long?) = update { it.copy(customSeedColor = seed) }

    // ---- 隐私锁 ----

    fun setLock(pin: String, questions: List<SecurityQuestion>, biometric: Boolean) {
        // 防御：只接受完整 4 位 PIN，避免把空/残缺 PIN 落盘成无法解锁的坏锁
        if (pin.length != 4) return
        val salt = Security.randomSalt()
        prefs.edit()
            .putBoolean("lockEnabled", true)
            .putBoolean("lockBiometric", biometric)
            .putString("lockPinSalt", salt)
            .putString("lockPinHash", Security.hash(pin, salt))
            .putString("lockQuestions", Security.questionsToJson(questions))
            .putInt("lockFailCount", 0)
            .putLong("lockCooldownUntil", 0L)
            .apply()
        _settings.value = _settings.value.copy(lockEnabled = true, lockBiometric = biometric)
    }

    fun updateLockPin(pin: String) {
        val salt = Security.randomSalt()
        prefs.edit()
            .putString("lockPinSalt", salt)
            .putString("lockPinHash", Security.hash(pin, salt))
            .putInt("lockFailCount", 0)
            .putLong("lockCooldownUntil", 0L)
            .apply()
    }

    fun updateLockQuestions(questions: List<SecurityQuestion>) {
        prefs.edit().putString("lockQuestions", Security.questionsToJson(questions)).apply()
    }

    fun setLockBiometric(enabled: Boolean) {
        prefs.edit().putBoolean("lockBiometric", enabled).apply()
        _settings.value = _settings.value.copy(lockBiometric = enabled)
    }

    fun disableLock() {
        prefs.edit()
            .putBoolean("lockEnabled", false)
            .putBoolean("lockBiometric", false)
            .remove("lockPinSalt")
            .remove("lockPinHash")
            .remove("lockQuestions")
            .putInt("lockFailCount", 0)
            .putLong("lockCooldownUntil", 0L)
            .apply()
        _settings.value = _settings.value.copy(lockEnabled = false, lockBiometric = false)
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString("lockPinSalt", null) ?: return false
        val hash = prefs.getString("lockPinHash", null) ?: return false
        return Security.verify(pin, salt, hash)
    }

    fun lockQuestions(): List<SecurityQuestion> =
        Security.questionsFromJson(prefs.getString("lockQuestions", null).orEmpty())

    /** 保密问题答案校验：返回 [questionIndex] 是否正确。 */
    fun verifySecurityAnswer(index: Int, answer: String): Boolean {
        val q = lockQuestions().getOrNull(index) ?: return false
        return Security.verify(answer, q.answerSalt, q.answerHash)
    }

    /** 失败计数与递增冷却（PIN 与保密问题共用）。 */
    fun registerFail(): Long {
        val count = prefs.getInt("lockFailCount", 0) + 1
        val cooldown = if (count >= 5) {
            listOf(30L, 60L, 300L, 600L)[minOf(count - 5, 3)] * 1000L
        } else 0L
        val until = if (cooldown > 0) System.currentTimeMillis() + cooldown else 0L
        prefs.edit().putInt("lockFailCount", count).putLong("lockCooldownUntil", until).apply()
        return until
    }

    fun clearFails() = prefs.edit().putInt("lockFailCount", 0).putLong("lockCooldownUntil", 0L).apply()

    fun cooldownRemainingMs(): Long =
        (prefs.getLong("lockCooldownUntil", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)

    // ---- 语言 ----

    /** 把选择的语言应用到系统 per-app locale（API 33+，本工程 minSdk 34）。 */
    private fun applyLanguage(index: Int) {
        val manager = appContext.getSystemService(LocaleManager::class.java) ?: return
        val locales = when (index) {
            1 -> android.os.LocaleList.forLanguageTags("zh-CN")
            2 -> android.os.LocaleList.forLanguageTags("en")
            else -> android.os.LocaleList.getEmptyLocaleList()
        }
        runCatching { manager.applicationLocales = locales }
    }

    private var _languagePersisted = -1

    private fun load(): AppSettings {
        _languagePersisted = prefs.getInt("languageIndex", 0)
        return AppSettings(
            themeColorIndex = prefs.getInt("themeColorIndex", 0),
            customSeedColor = prefs.getLong("customSeedColor", -1L).takeIf { it >= 0L },
            backgroundIndex = prefs.getInt("backgroundIndex", 0),
            fontScaleIndex = prefs.getInt("fontScaleIndex", 1),
            darkModeIndex = prefs.getInt("darkModeIndex", 0),
            reduceMotion = prefs.getBoolean("reduceMotion", false),
            weekStartMonday = prefs.getBoolean("weekStartMonday", true),
            languageIndex = _languagePersisted,
            onboarded = prefs.getBoolean("onboarded", false),
            lockEnabled = prefs.getBoolean("lockEnabled", false),
            lockBiometric = prefs.getBoolean("lockBiometric", false),
            reminderEnabled = prefs.getBoolean("reminderEnabled", false),
            reminderHour = prefs.getInt("reminderHour", 21),
            reminderMinute = prefs.getInt("reminderMinute", 0),
        )
    }

    companion object {
        /**
         * 进程启动时同步语言：若系统侧已有 per-app 语言（用户在系统设置中改过），采纳之；
         * 否则应用 App 内存储的选择。
         */
        fun restoreLanguage(context: Context) {
            val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val manager =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.getSystemService(LocaleManager::class.java)
                } else null
            val current = manager?.applicationLocales
            if (current != null && !current.isEmpty) {
                val lang = current[0]?.language ?: return
                val index = when (lang) {
                    "zh" -> 1
                    "en" -> 2
                    else -> 0
                }
                prefs.edit().putInt("languageIndex", index).apply()
                return
            }
            val index = prefs.getInt("languageIndex", 0)
            val locales = when (index) {
                1 -> android.os.LocaleList.forLanguageTags("zh-CN")
                2 -> android.os.LocaleList.forLanguageTags("en")
                else -> return
            }
            runCatching { manager?.let { it.applicationLocales = locales } }
        }
    }
}
