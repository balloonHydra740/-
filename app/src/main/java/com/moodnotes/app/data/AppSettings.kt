package com.moodnotes.app.data

data class AppSettings(
    val themeColorIndex: Int = 0,
    /** 自定义种子色（主题册应用的颜色），非空时优先于 themeColorIndex。 */
    val customSeedColor: Long? = null,
    val backgroundIndex: Int = 0,
    val fontScaleIndex: Int = 1,
    val darkModeIndex: Int = 0,
    val reduceMotion: Boolean = false,
    val weekStartMonday: Boolean = true,
    val languageIndex: Int = 0,
    val onboarded: Boolean = false,
    // 隐私锁
    val lockEnabled: Boolean = false,
    val lockBiometric: Boolean = false,
    // 每日提醒
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
) {
    companion object {
        val FontScales = listOf(0.85f, 1f, 1.15f, 1.3f)
        val DarkModeLabels = listOf("dark_follow_system", "dark_light", "dark_dark")
    }
}
