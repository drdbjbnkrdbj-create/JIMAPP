package com.example

import com.example.ui.schedule.ScheduleTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTextParserTest {
    @Test
    fun parsesEachDayAndItsOwnTimeRange() {
        val sessions = ScheduleTextParser.parse(
            """
                الأحد: هندسة البرمجيات من 08:00 إلى 09:00 قاعة H2
                الإثنين: الرياضيات المتقطعة 09:15 - 10:30 H1
                الثلاثاء: قواعد البيانات 11:00 - 12:00 C1
            """.trimIndent()
        )

        assertEquals(3, sessions.size)
        assertEquals(listOf(1, 2, 3), sessions.map { it.dayOfWeek })
        assertEquals(listOf("08:00" to "09:00", "09:15" to "10:30", "11:00" to "12:00"),
            sessions.map { it.startTime to it.endTime })
        assertEquals("هندسة البرمجيات", sessions.first().courseName)
        assertEquals("قاعة H2", sessions.first().classroom)
    }

    @Test
    fun parsesArabicDigitsAndTwelveHourTimes() {
        val sessions = ScheduleTextParser.parse(
            """
                الأربعاء
                فيزياء
                ٠٨:٣٠ ص
                ١٠:٠٠ ص
            """.trimIndent()
        )

        assertEquals(1, sessions.size)
        assertEquals("08:30", sessions.single().startTime)
        assertEquals("10:00", sessions.single().endTime)
    }

    @Test
    fun ignoresRowsWithoutDayOrTwoValidTimesInsteadOfInventingTimes() {
        val sessions = ScheduleTextParser.parse(
            """
                مادة بدون وقت
                يوم غير معروف مادة 08:00 - 09:00
                الخميس: محاضرة بلا وقت 08:00
            """.trimIndent()
        )

        assertTrue(sessions.isEmpty())
    }

    @Test
    fun rejectsInvalidOrReversedSessionTimes() {
        assertTrue(ScheduleTextParser.hasValidTimeRange("08:00", "09:15"))
        assertTrue(!ScheduleTextParser.hasValidTimeRange("09:15", "08:00"))
        assertTrue(!ScheduleTextParser.hasValidTimeRange("25:00", "26:00"))
    }

    @Test
    fun parsesTheSampleScheduleWithoutInvalidTimeRanges() {
        val sessions = ScheduleTextParser.parse(
            """
                الأحد: هندسة البرمجيات من 08:00 إلى 09:00 قاعة H2
                الإثنين: الرياضيات المتقطعة 09:00 - 10:30 H1
                الثلاثاء: معمل قواعد البيانات من 10:00 إلى 12:00 Lab6
                الأربعاء: الذكاء الاصطناعي 09:00 - 11:00 H3
                الخميس: لغة إنجليزية 11:00 - 13:00 C1
            """.trimIndent()
        )

        assertEquals(5, sessions.size)
        assertEquals("قواعد البيانات", sessions[2].courseName)
        assertEquals("Lab6", sessions[2].classroom)
        assertTrue(sessions.all { ScheduleTextParser.hasValidTimeRange(it.startTime, it.endTime) })
    }
}
