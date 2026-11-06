package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.JimDatabase
import com.example.data.JimRepository
import com.example.data.model.CourseEntity
import com.example.data.model.TimetableImportEntry
import com.example.data.model.TimetableSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimetableImportRepositoryTest {
    @Test
    fun manuallyEnteredCourseNameCreatesCourseAndPersistsSchedule() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "manual-timetable-test.db"
        context.deleteDatabase(databaseName)
        var database = openDatabase(context, databaseName)

        try {
            val repository = JimRepository(database.jimDao(), database)
            repository.addSessionForCourseName(
                courseName = "فيزياء",
                dayOfWeek = 3,
                startTime = "10:00",
                endTime = "11:00",
                classroom = "قاعة 2",
                sessionType = "محاضرة"
            )
            repository.addSessionForCourseName(
                courseName = "فيزياء",
                dayOfWeek = 5,
                startTime = "12:00",
                endTime = "13:00",
                classroom = "قاعة 3",
                sessionType = "معمل"
            )

            val courses = database.jimDao().getAllCourses()
            val savedSessions = repository.sessionsFlow.first()
            assertEquals(1, courses.size)
            assertEquals("فيزياء", courses.single().name)
            assertEquals(2, savedSessions.size)
            assertEquals(courses.single().id, savedSessions[0].courseId)
            assertEquals(listOf(3, 5), savedSessions.map { it.dayOfWeek })
            assertEquals(listOf("قاعة 2", "قاعة 3"), savedSessions.map { it.classroom })

            var blankNameRejected = false
            try {
                repository.addSessionForCourseName(
                    courseName = " ",
                    dayOfWeek = 1,
                    startTime = "08:00",
                    endTime = "09:00",
                    classroom = "",
                    sessionType = "محاضرة"
                )
            } catch (_: IllegalArgumentException) {
                blankNameRejected = true
            }
            assertTrue(blankNameRejected)

            database.close()
            database = openDatabase(context, databaseName)
            assertEquals(2, database.jimDao().getAllSessionsFlow().first().size)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun replacesSessionsAndKeepsImportedScheduleAfterDatabaseReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "timetable-import-test.db"
        context.deleteDatabase(databaseName)
        var database = openDatabase(context, databaseName)

        try {
            val dao = database.jimDao()
            val existingCourseId = dao.insertCourse(
                CourseEntity(name = "هندسة البرمجيات", code = "CS301")
            )
            val oldCourseId = dao.insertCourse(CourseEntity(name = "المحاضرة القديمة", code = "OLD"))
            dao.insertSession(
                TimetableSessionEntity(
                    courseId = oldCourseId,
                    dayOfWeek = 1,
                    startTime = "07:00",
                    endTime = "08:00"
                )
            )

            val repository = JimRepository(dao, database)
            repository.replaceSchedule(
                listOf(
                    TimetableImportEntry("هندسة البرمجيات", 1, "08:00", "09:00", "H2"),
                    TimetableImportEntry("هندسة البرمجيات", 2, "09:00", "10:00", "H1"),
                    TimetableImportEntry("شبكات", 5, "12:00", "13:00", "قاعة 201")
                )
            )

            val importedSessions = repository.sessionsFlow.first()
            assertEquals(listOf(1, 2, 5), importedSessions.map { it.dayOfWeek })
            assertEquals(listOf("08:00", "09:00", "12:00"), importedSessions.map { it.startTime })
            assertEquals(existingCourseId, importedSessions[0].courseId)
            assertEquals(existingCourseId, importedSessions[1].courseId)
            assertEquals("شبكات", dao.getCourseById(importedSessions[2].courseId)?.name)

            var emptyImportRejected = false
            try {
                repository.replaceSchedule(emptyList())
            } catch (_: IllegalArgumentException) {
                emptyImportRejected = true
            }
            assertTrue(emptyImportRejected)
            assertEquals(3, repository.sessionsFlow.first().size)

            database.close()
            database = openDatabase(context, databaseName)
            val restoredSessions = database.jimDao().getAllSessionsFlow().first()
            assertEquals(listOf(1, 2, 5), restoredSessions.map { it.dayOfWeek })
            assertEquals(listOf("H2", "H1", "قاعة 201"), restoredSessions.map { it.classroom })
            assertFalse(restoredSessions.any { it.courseId == oldCourseId })
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    private fun openDatabase(context: Context, databaseName: String): JimDatabase =
        Room.databaseBuilder(context, JimDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
}
