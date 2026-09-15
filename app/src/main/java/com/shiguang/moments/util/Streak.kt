package com.shiguang.moments.util

import com.shiguang.moments.data.models.MoodEntity
import com.shiguang.moments.data.models.MomentEntity
import java.util.Calendar

/** 连续记录天数：从今天往前数，连续有「记录或心情打卡」的天数。 */
object Streak {
    fun of(moments: List<MomentEntity>, moods: List<MoodEntity> = emptyList()): Int {
        if (moments.isEmpty() && moods.isEmpty()) return 0
        val days = HashSet<Long>().apply {
            moments.forEach { add(dayKey(it.capturedAt)) }
            moods.forEach { add(it.day) }
        }
        val cal = Calendar.getInstance()
        var count = 0
        if (!days.contains(dayKey(cal.timeInMillis))) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        while (days.contains(dayKey(cal.timeInMillis))) {
            count++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return count
    }

    private fun dayKey(ts: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}