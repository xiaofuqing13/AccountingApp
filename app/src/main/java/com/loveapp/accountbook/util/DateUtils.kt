package com.loveapp.accountbook.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val sdfMonth = SimpleDateFormat("yyyy年M月", Locale.CHINA)
    private val sdfDisplay = SimpleDateFormat("M月d日 E", Locale.CHINA)
    private val sdfFull = SimpleDateFormat("yyyy年M月d日 E", Locale.CHINA)
    private val sdfTime = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val sdfYearMonth = SimpleDateFormat("yyyy-MM", Locale.CHINA)

    fun today(): String = sdfDate.format(Date())
    fun todayDisplay(): String = sdfFull.format(Date())
    fun currentTime(): String = sdfTime.format(Date())
    fun currentYearMonth(): String = sdfYearMonth.format(Date())
    fun formatMonth(yearMonth: String): String {
        return try {
            val date = sdfYearMonth.parse(yearMonth)
            sdfMonth.format(date!!)
        } catch (e: Exception) { yearMonth }
    }
    fun formatDateDisplay(dateStr: String): String {
        return try {
            val date = sdfDate.parse(dateStr)
            sdfDisplay.format(date!!)
        } catch (e: Exception) { dateStr }
    }

    fun prevMonth(yearMonth: String): String {
        val cal = Calendar.getInstance()
        val date = sdfYearMonth.parse(yearMonth) ?: return yearMonth
        cal.time = date
        cal.add(Calendar.MONTH, -1)
        return sdfYearMonth.format(cal.time)
    }

    fun nextMonth(yearMonth: String): String {
        val cal = Calendar.getInstance()
        val date = sdfYearMonth.parse(yearMonth) ?: return yearMonth
        cal.time = date
        cal.add(Calendar.MONTH, 1)
        return sdfYearMonth.format(cal.time)
    }

    // 在一起天数计算（从2025-02-14开始）
    private val togetherDate: Calendar = Calendar.getInstance().apply {
        set(2025, Calendar.FEBRUARY, 14, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    data class TimeDiff(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

    fun getTogetherTime(): TimeDiff {
        val now = System.currentTimeMillis()
        val diff = now - togetherDate.timeInMillis
        val totalSecs = diff / 1000
        return TimeDiff(
            days = totalSecs / 86400,
            hours = (totalSecs % 86400) / 3600,
            minutes = (totalSecs % 3600) / 60,
            seconds = totalSecs % 60
        )
    }

    fun getGreeting(): Pair<String, String> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..10 -> "早安，我的小太阳 ☀️" to "新的一天，从想你开始"
            hour in 11..13 -> "中午好，记得吃饭哦 🍱" to "吃饱饱才有力气想我"
            hour in 14..17 -> "下午好，今天也想你了 💭" to "困了就眯一会，梦里有我"
            hour in 18..21 -> "晚上好，今天辛苦了 🌙" to "回家的路上注意安全"
            else -> "夜深了，还没睡吗 🌟" to "快去睡吧，我在梦里等你"
        }
    }
}
