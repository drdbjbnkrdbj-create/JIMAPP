package com.example.data

import androidx.room.withTransaction
import com.example.data.dao.JimDao
import com.example.data.model.AssessmentEntity
import com.example.data.model.AssignmentEntity
import com.example.data.model.CourseEntity
import com.example.data.model.CourseNoteEntity
import com.example.data.model.DocumentAnnotationEntity
import com.example.data.model.ExamEntity
import com.example.data.model.LectureEntity
import com.example.data.model.LibraryDocumentEntity
import com.example.data.model.StudentProfileEntity
import com.example.data.model.TimetableSessionEntity
import com.example.data.model.TimetableImportEntry
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class JimRepository(
    private val dao: JimDao,
    private val database: JimDatabase
) {

    val coursesFlow: Flow<List<CourseEntity>> = dao.getAllCoursesFlow()
    val assessmentsFlow: Flow<List<AssessmentEntity>> = dao.getAllAssessmentsFlow()
    val lecturesFlow: Flow<List<LectureEntity>> = dao.getAllLecturesFlow()
    val assignmentsFlow: Flow<List<AssignmentEntity>> = dao.getAllAssignmentsFlow()
    val examsFlow: Flow<List<ExamEntity>> = dao.getAllExamsFlow()
    val sessionsFlow: Flow<List<TimetableSessionEntity>> = dao.getAllSessionsFlow()
    val notesFlow: Flow<List<CourseNoteEntity>> = dao.getAllNotesFlow()
    val documentsFlow: Flow<List<LibraryDocumentEntity>> = dao.getAllDocumentsFlow()
    val profileFlow: Flow<StudentProfileEntity?> = dao.getProfileFlow()

    fun getAssessmentsForCourse(courseId: Long): Flow<List<AssessmentEntity>> =
        dao.getAssessmentsForCourseFlow(courseId)

    fun getLecturesForCourse(courseId: Long): Flow<List<LectureEntity>> =
        dao.getLecturesForCourseFlow(courseId)

    fun getAssignmentsForCourse(courseId: Long): Flow<List<AssignmentEntity>> =
        dao.getAssignmentsForCourseFlow(courseId)

    fun getExamsForCourse(courseId: Long): Flow<List<ExamEntity>> =
        dao.getExamsForCourseFlow(courseId)

    fun getNotesForCourse(courseId: Long): Flow<List<CourseNoteEntity>> =
        dao.getNotesForCourseFlow(courseId)

    fun getDocumentsForCourse(courseId: Long): Flow<List<LibraryDocumentEntity>> =
        dao.getDocumentsForCourseFlow(courseId)

    fun getAnnotationsForDoc(docId: Long): Flow<List<DocumentAnnotationEntity>> =
        dao.getAnnotationsForDocumentFlow(docId)

    suspend fun getCourseById(id: Long): CourseEntity? = dao.getCourseById(id)
    suspend fun getDocById(id: Long): LibraryDocumentEntity? = dao.getDocumentById(id)

    // Course CRUD
    suspend fun addCourse(course: CourseEntity): Long = dao.insertCourse(course)
    suspend fun updateCourse(course: CourseEntity) = dao.updateCourse(course)
    suspend fun deleteCourse(course: CourseEntity) = dao.deleteCourseAndRelatedData(course.id)

    // Assessment CRUD
    suspend fun addAssessment(assessment: AssessmentEntity): Long = dao.insertAssessment(assessment)
    suspend fun updateAssessment(assessment: AssessmentEntity) = dao.updateAssessment(assessment)
    suspend fun deleteAssessment(assessment: AssessmentEntity) = dao.deleteAssessment(assessment)

    // Lecture CRUD
    suspend fun addLecture(lecture: LectureEntity): Long = dao.insertLecture(lecture)
    suspend fun updateLecture(lecture: LectureEntity) = dao.updateLecture(lecture)
    suspend fun deleteLecture(lecture: LectureEntity) = dao.deleteLecture(lecture)

    // Assignment CRUD
    suspend fun addAssignment(assignment: AssignmentEntity): Long = dao.insertAssignment(assignment)
    suspend fun updateAssignment(assignment: AssignmentEntity) = dao.updateAssignment(assignment)
    suspend fun deleteAssignment(assignment: AssignmentEntity) = dao.deleteAssignment(assignment)

    // Exam CRUD
    suspend fun addExam(exam: ExamEntity): Long = dao.insertExam(exam)
    suspend fun updateExam(exam: ExamEntity) = dao.updateExam(exam)
    suspend fun deleteExam(exam: ExamEntity) = dao.deleteExam(exam)

    // Timetable CRUD
    suspend fun addSession(session: TimetableSessionEntity): Long = dao.insertSession(session)
    suspend fun addSessions(sessions: List<TimetableSessionEntity>) = dao.insertSessions(sessions)
    suspend fun updateSession(session: TimetableSessionEntity) = dao.updateSession(session)
    suspend fun deleteSession(session: TimetableSessionEntity) = dao.deleteSession(session)
    suspend fun clearSessions() = dao.clearAllSessions()

    suspend fun replaceSchedule(entries: List<TimetableImportEntry>) {
        require(entries.isNotEmpty()) { "Imported timetable cannot be empty" }
        require(entries.all { entry ->
            normalizeCourseName(entry.courseName).isNotEmpty() &&
                entry.dayOfWeek in 1..5 &&
                isValidTimeRange(entry.startTime, entry.endTime)
        }) { "Imported timetable contains an invalid session" }

        database.withTransaction {
            val courses = dao.getAllCourses().toMutableList()
            val importedCourses = mutableMapOf<String, CourseEntity>()
            val codeSuffix = System.currentTimeMillis()

            val sessions = entries.mapIndexed { index, entry ->
                val courseKey = normalizeCourseName(entry.courseName)

                val course = importedCourses.getOrPut(courseKey) {
                    findMatchingCourse(courses, courseKey) ?: CourseEntity(
                        name = entry.courseName.trim(),
                        code = "IMP-$codeSuffix-$index"
                    ).let { newCourse ->
                        val savedCourse = newCourse.copy(id = dao.insertCourse(newCourse))
                        courses += savedCourse
                        savedCourse
                    }
                }

                TimetableSessionEntity(
                    courseId = course.id,
                    dayOfWeek = entry.dayOfWeek,
                    startTime = entry.startTime,
                    endTime = entry.endTime,
                    classroom = entry.classroom,
                    sessionType = entry.sessionType
                )
            }

            dao.clearAllSessions()
            dao.insertSessions(sessions)
        }
    }

    private fun isValidTimeRange(startTime: String, endTime: String): Boolean {
        fun minutes(value: String): Int? {
            val match = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")
                .matchEntire(value) ?: return null
            return match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
        }

        val start = minutes(startTime) ?: return false
        val end = minutes(endTime) ?: return false
        return end > start
    }

    private fun findMatchingCourse(courses: List<CourseEntity>, courseKey: String): CourseEntity? {
        val normalizedNames = courses.map { it to normalizeCourseName(it.name) }
        normalizedNames.firstOrNull { it.second == courseKey }?.let { return it.first }

        val partialMatches = normalizedNames.filter { (_, name) ->
            name.length >= 5 && courseKey.length >= 5 &&
                (name.contains(courseKey) || courseKey.contains(name))
        }
        val longestMatchLength = partialMatches.maxOfOrNull { it.second.length } ?: return null
        return partialMatches.singleOrNull { it.second.length == longestMatchLength }?.first
    }

    private fun normalizeCourseName(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[أإآ]"), "ا")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]"), "")

    // Notes CRUD
    suspend fun addNote(note: CourseNoteEntity): Long = dao.insertNote(note)
    suspend fun updateNote(note: CourseNoteEntity) = dao.updateNote(note)
    suspend fun deleteNote(note: CourseNoteEntity) = dao.deleteNote(note)

    // Library CRUD
    suspend fun addDocument(doc: LibraryDocumentEntity): Long = dao.insertDocument(doc)
    suspend fun updateDocument(doc: LibraryDocumentEntity) = dao.updateDocument(doc)
    suspend fun deleteDocument(doc: LibraryDocumentEntity) = dao.deleteDocumentAndAnnotations(doc.id)

    // Annotation CRUD
    suspend fun addAnnotation(ann: DocumentAnnotationEntity): Long = dao.insertAnnotation(ann)
    suspend fun deleteAnnotation(ann: DocumentAnnotationEntity) = dao.deleteAnnotation(ann)

    // Profile
    suspend fun saveProfile(profile: StudentProfileEntity) = dao.saveProfile(profile)

    // Backup & Restore
    fun exportBackupJson(
        courses: List<CourseEntity>,
        assessments: List<AssessmentEntity>,
        lectures: List<LectureEntity>,
        assignments: List<AssignmentEntity>,
        exams: List<ExamEntity>,
        sessions: List<TimetableSessionEntity>,
        notes: List<CourseNoteEntity>,
        documents: List<LibraryDocumentEntity>,
        profile: StudentProfileEntity?
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appName", "Jim")

        val coursesArray = JSONArray()
        courses.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("code", c.code)
            obj.put("instructor", c.instructor)
            obj.put("classroom", c.classroom)
            obj.put("creditHours", c.creditHours)
            obj.put("colorHex", c.colorHex)
            obj.put("iconKey", c.iconKey)
            obj.put("description", c.description)
            coursesArray.put(obj)
        }
        root.put("courses", coursesArray)

        val notesArray = JSONArray()
        notes.forEach { n ->
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("courseId", n.courseId ?: -1L)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("category", n.category)
            obj.put("isPinned", n.isPinned)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        val assignmentsArray = JSONArray()
        assignments.forEach { a ->
            val obj = JSONObject()
            obj.put("courseId", a.courseId)
            obj.put("title", a.title)
            obj.put("description", a.description)
            obj.put("dueDate", a.dueDate)
            obj.put("priority", a.priority)
            obj.put("status", a.status)
            assignmentsArray.put(obj)
        }
        root.put("assignments", assignmentsArray)

        return root.toString(2)
    }

    suspend fun restoreFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("courses")) return false

            val coursesArray = root.getJSONArray("courses")
            for (i in 0 until coursesArray.length()) {
                val obj = coursesArray.getJSONObject(i)
                val course = CourseEntity(
                    name = obj.getString("name"),
                    code = obj.getString("code"),
                    instructor = obj.optString("instructor", ""),
                    classroom = obj.optString("classroom", ""),
                    creditHours = obj.optInt("creditHours", 3),
                    colorHex = obj.optString("colorHex", "#2563EB"),
                    iconKey = obj.optString("iconKey", "code"),
                    description = obj.optString("description", "")
                )
                dao.insertCourse(course)
            }

            if (root.has("notes")) {
                val notesArray = root.getJSONArray("notes")
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    val cId = obj.optLong("courseId", -1L)
                    val note = CourseNoteEntity(
                        courseId = if (cId > 0) cId else null,
                        title = obj.getString("title"),
                        content = obj.optString("content", ""),
                        category = obj.optString("category", "عام"),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                    dao.insertNote(note)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
