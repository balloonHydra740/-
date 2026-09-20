package com.moodnotes.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * 日期工具：所有显示格式按当前应用语言输出（中文 xx月xx日，英文 Aug 29）。
 */
object Dates {

    fun today(): LocalDate = LocalDate.now()

    fun todayEpochDay(): Long = today().toEpochDay()

    fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    private fun isEnglish(): Boolean =
        Locale.getDefault().language == "en"

    private val enMonthDay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

    private val enFullDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    private val enMonthTitle: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

    /** 形如「8月29日」/「Aug 29」。 */
    fun monthDay(date: LocalDate): String =
        if (isEnglish()) date.format(enMonthDay) else "${date.monthValue}月${date.dayOfMonth}日"

    /** 形如「2026年8月29日」/「Aug 29, 2026」。 */
    fun fullDate(date: LocalDate): String =
        if (isEnglish()) date.format(enFullDate) else "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

    /** 周几：中文「周一」，英文「Mon」。 */
    fun weekday(dayOfWeek: DayOfWeek): String =
        if (isEnglish()) {
            dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH)
        } else {
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
            }
        }

    /** 旧名保留兼容，等价于 weekday()。 */
    fun weekdayCn(dayOfWeek: DayOfWeek): String = weekday(dayOfWeek)

    /** 周几短标签：中文「一…日」，英文窄字母「M/T/W…」（小尺寸 chip 用）。 */
    fun weekShort(dayOfWeek: DayOfWeek): String =
        if (isEnglish()) {
            dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH)
        } else {
            when (dayOfWeek) {
                DayOfWeek.MONDAY -> "一"
                DayOfWeek.TUESDAY -> "二"
                DayOfWeek.WEDNESDAY -> "三"
                DayOfWeek.THURSDAY -> "四"
                DayOfWeek.FRIDAY -> "五"
                DayOfWeek.SATURDAY -> "六"
                DayOfWeek.SUNDAY -> "日"
            }
        }

    /** 形如「2026年8月」/「Aug 2026」。 */
    fun monthTitle(yearMonth: YearMonth): String =
        if (isEnglish()) yearMonth.format(enMonthTitle) else "${yearMonth.year}年${yearMonth.monthValue}月"

    /** 是否周末（问候语分叉用）。 */
    fun isWeekend(date: LocalDate = today()): Boolean =
        date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    /** 形如「8月29日 周五」/「Aug 29 · Fri」。 */
    fun todayTitle(): String {
        val d = today()
        return if (isEnglish()) {
            "${d.format(enMonthDay)} · ${d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)}"
        } else {
            "${d.monthValue}月${d.dayOfMonth}日 ${weekday(d.dayOfWeek)}"
        }
    }

    fun iso(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** 日历页周标签：中文「一二三…」，英文「M T W…」。 */
    fun weekLabels(startMonday: Boolean): List<String> =
        if (isEnglish()) {
            val narrow = DayOfWeek.entries.map { it.getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH) }
            if (startMonday) narrow else narrow.slice(6..6) + narrow.slice(0..5)
        } else {
            if (startMonday) listOf("一", "二", "三", "四", "五", "六", "日")
            else listOf("日", "一", "二", "三", "四", "五", "六")
        }
}
