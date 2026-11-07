package com.example.notifications

import java.util.Calendar

internal object DailyReminderTime {
    fun nextTriggerAtMillis(time: String, nowMillis: Long): Long? {
        val match = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""").matchEntire(time)
            ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
}
