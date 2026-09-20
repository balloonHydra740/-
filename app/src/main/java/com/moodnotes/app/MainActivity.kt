package com.moodnotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moodnotes.app.data.AppSettings
import com.moodnotes.app.ui.MoodNotesApp
import com.moodnotes.app.ui.lock.LockScreen
import com.moodnotes.app.ui.theme.BackgroundPreset
import com.moodnotes.app.ui.theme.MoodNotesTheme
import com.moodnotes.app.ui.theme.ThemePresets
import com.moodnotes.app.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as MoodNotesApplication
            val settings by app.settingsRepository.settings.collectAsStateWithLifecycle()

            // 隐私锁：冷启动若已启用则先锁；退到后台（ON_STOP）后再次回来需解锁
            var locked by remember { mutableStateOf(settings.lockEnabled) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, settings.lockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP && settings.lockEnabled) locked = true
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val darkTheme = when (settings.darkModeIndex) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            val seed = settings.customSeedColor?.let { Color(it.toInt()) }
                ?: ThemePresets[settings.themeColorIndex.coerceIn(ThemePresets.indices)].seed
            MoodNotesTheme(
                darkTheme = darkTheme,
                seedColor = seed,
                fontScale = AppSettings.FontScales[settings.fontScaleIndex.coerceIn(AppSettings.FontScales.indices)],
                background = BackgroundPreset.entries[settings.backgroundIndex.coerceIn(BackgroundPreset.entries.indices)],
                reduceMotion = settings.reduceMotion,
            ) {
                when {
                    !settings.onboarded -> WelcomeScreen(
                        onFinish = { app.settingsRepository.completeOnboarding() },
                    )
                    locked -> LockScreen(
                        onUnlocked = { locked = false },
                    )
                    else -> MoodNotesApp()
                }
            }
        }
    }
}
