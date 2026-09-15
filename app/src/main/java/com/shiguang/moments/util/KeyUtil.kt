package com.shiguang.moments.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 日期键：yyyyMMdd（Int）便于排序与等价比较 */
object KeyUtil {
    private val dayFmt = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    private val wkFmt = SimpleDateFormat("yyyy-'W'ww", Locale.CHINA) // ISO 周

    fun dayLong(ts: Long): Long = dayFmt.format(Date(ts)).toLong()
    fun dayString(ts: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(ts))

    /** 当天本地 0 点（用于日期选择器默认值） */
    fun dayBoundaryStart(ts: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** 一年中的周（用周里的中间那天的 "yyyy-Www" 表示，简单够用） */
    fun weekId(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        c.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        return wkFmt.format(c.time)
    }
    fun currentWeekId(): String = weekId(System.currentTimeMillis())
    fun currentDayLong(): Long = dayLong(System.currentTimeMillis())
}