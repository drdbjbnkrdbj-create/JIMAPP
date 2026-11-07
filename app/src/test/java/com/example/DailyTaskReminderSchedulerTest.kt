package com.example

import com.example.notifications.DailyReminderTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class DailyTaskReminderSchedulerTest {
    @Test
    fun schedulesReminderLaterTodayWhenTimeHasNotPassed() {
        val now = calendarAt(2026, Calendar.MAY, 12, 7, 0)
        val expected = calendarAt(2026, Calendar.MAY, 12, 8, 30)

        assertEquals(
            expected.timeInMillis,
            DailyReminderTime.nextTriggerAtMillis("08:30", now.timeInMillis)
        )
    }

    @Test
    fun schedulesReminderTomorrowWhenTimeHasAlreadyPassed() {
        val now = calendarAt(2026, Calendar.MAY, 12, 8, 30)
        val expected = calendarAt(2026, Calendar.MAY, 13, 8, 30)

        assertEquals(
            expected.timeInMillis,
            DailyReminderTime.nextTriggerAtMillis("08:30", now.timeInMillis)
        )
    }

    @Test
    fun rejectsInvalidReminderTimes() {
        assertNull(DailyReminderTime.nextTriggerAtMillis("25:10", 0))
    }

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
}
