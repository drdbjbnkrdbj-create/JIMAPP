package com.example.ui

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.JimDatabase
import com.example.data.JimRepository
import com.example.data.model.AssessmentEntity
import com.example.data.model.AssignmentEntity
import com.example.data.model.CourseEntity
import com.example.data.model.CourseNoteEntity
import com.example.data.model.DocumentAnnotationEntity
import com.example.data.model.ExamEntity
import com.example.data.model.LectureEntity
import com.example.data.model.LibraryDocumentEntity
import com.example.data.model.StudentProfileEntity
import com.example.data.model.TimetableImportEntry
import com.example.data.model.TimetableSessionEntity
import com.example.notifications.JimNotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val index: Int, val titleAr: String) {
    HOME(0, "الرئيسية"),
    COURSES(1, "المواد"),
    SCHEDULE(2, "الجدول"),
    LIBRARY(3, "المكتبة"),
    MORE(4, "المزيد")
}

sealed class SubScreen {
    object None : SubScreen()
    data class CourseWorkspace(val courseId: Long) : SubScreen()
    data class DocumentReader(val docId: Long) : SubScreen()
    object ScheduleImport : SubScreen()
    object GpaCalculator : SubScreen()
    object QuickNotes : SubScreen()
    object StudentProfile : SubScreen()
    object Settings : SubScreen()
    object Onboarding : SubScreen()
}

class JimViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JimRepository

    // Navigation State
    private val _selectedTab = MutableStateFlow(MainTab.HOME)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _subScreen = MutableStateFlow<SubScreen>(SubScreen.None)
    val subScreen: StateFlow<SubScreen> = _subScreen.asStateFlow()

    // Active Schedule Day (1 = Sunday, 2 = Monday, etc.)
    private val _selectedDayOfWeek = MutableStateFlow(1)
    val selectedDayOfWeek: StateFlow<Int> = _selectedDayOfWeek.asStateFlow()

    // Search query for courses & library
    private val _courseSearchQuery = MutableStateFlow("")
    val courseSearchQuery: StateFlow<String> = _courseSearchQuery.asStateFlow()

    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery: StateFlow<String> = _librarySearchQuery.asStateFlow()

    private val _libraryFilter = MutableStateFlow("الكل") // الكل, PDF, Word, PowerPoint
    val libraryFilter: StateFlow<String> = _libraryFilter.asStateFlow()

    private val _notesFilter = MutableStateFlow("الكل") // الكل, عام, مهم, أسئلة, مراجعة
    val notesFilter: StateFlow<String> = _notesFilter.asStateFlow()

    init {
        val database = JimDatabase.getDatabase(application)
        val dao = database.jimDao()
        repository = JimRepository(dao, database)
        if (!application.getSharedPreferences("jim_preferences", Context.MODE_PRIVATE)
                .getBoolean("has_seen_welcome", false)
        ) {
            _subScreen.value = SubScreen.Onboarding
        }

        JimNotificationHelper.createNotificationChannel(application)

        val preferences = application.getSharedPreferences("jim_preferences", Context.MODE_PRIVATE)
        if (!preferences.getBoolean("bundled_examples_removed", false)) {
            viewModelScope.launch {
                repository.removeBundledExamples()
                preferences.edit().putBoolean("bundled_examples_removed", true).apply()
            }
        }
    }

    val courses: StateFlow<List<CourseEntity>> = repository.coursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assessments: StateFlow<List<AssessmentEntity>> = repository.assessmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lectures: StateFlow<List<LectureEntity>> = repository.lecturesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<AssignmentEntity>> = repository.assignmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = repository.examsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<TimetableSessionEntity>> = repository.sessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<CourseNoteEntity>> = repository.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<LibraryDocumentEntity>> = repository.documentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<StudentProfileEntity> = repository.profileFlow
        .combine(MutableStateFlow(StudentProfileEntity())) { p, defaultVal ->
            p ?: defaultVal
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudentProfileEntity())

    // Navigation Methods
    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
        _subScreen.value = SubScreen.None
    }

    fun openCourseWorkspace(courseId: Long) {
        _subScreen.value = SubScreen.CourseWorkspace(courseId)
    }

    fun openDocumentReader(docId: Long) {
        _subScreen.value = SubScreen.DocumentReader(docId)
    }

    fun openScheduleImport() {
        _subScreen.value = SubScreen.ScheduleImport
    }

    fun openGpaCalculator() {
        _subScreen.value = SubScreen.GpaCalculator
    }

    fun openQuickNotes() {
        _subScreen.value = SubScreen.QuickNotes
    }

    fun openProfile() {
        markWelcomeAsSeenIfNeeded()
        _subScreen.value = SubScreen.StudentProfile
    }

    fun openSettings() {
        _subScreen.value = SubScreen.Settings
    }

    fun openOnboarding() {
        _subScreen.value = SubScreen.Onboarding
    }

    fun navigateBack(): Boolean {
        if (_subScreen.value !is SubScreen.None) {
            markWelcomeAsSeenIfNeeded()
            _subScreen.value = SubScreen.None
            return true
        }
        return false
    }

    fun finishOnboarding() {
        getApplication<Application>()
            .getSharedPreferences("jim_preferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("has_seen_welcome", true)
            .apply()
        _subScreen.value = SubScreen.None
    }

    private fun markWelcomeAsSeenIfNeeded() {
        if (_subScreen.value is SubScreen.Onboarding) {
            getApplication<Application>()
                .getSharedPreferences("jim_preferences", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("has_seen_welcome", true)
                .apply()
        }
    }

    fun selectScheduleDay(day: Int) {
        _selectedDayOfWeek.value = day
    }

    fun setCourseSearch(query: String) {
        _courseSearchQuery.value = query
    }

    fun setLibrarySearch(query: String) {
        _librarySearchQuery.value = query
    }

    fun setLibraryFilter(filter: String) {
        _libraryFilter.value = filter
    }

    fun setNotesFilter(filter: String) {
        _notesFilter.value = filter
    }

    // Course Operations
    fun addCourse(name: String, code: String, instructor: String, classroom: String, creditHours: Int, colorHex: String, iconKey: String, description: String) {
        viewModelScope.launch {
            repository.addCourse(
                CourseEntity(
                    name = name,
                    code = code,
                    instructor = instructor,
                    classroom = classroom,
                    creditHours = creditHours,
                    colorHex = colorHex,
                    iconKey = iconKey,
                    description = description
                )
            )
        }
    }

    fun updateCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.updateCourse(course)
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            assignments.value.filter { it.courseId == course.id }
                .forEach { JimNotificationHelper.cancelNotification(getApplication(), it.id.toInt()) }
            exams.value.filter { it.courseId == course.id }
                .forEach { JimNotificationHelper.cancelNotification(getApplication(), it.id.toInt() + 5000) }
            repository.deleteCourse(course)
        }
    }

    // Assessment Operations
    fun addAssessment(courseId: Long, name: String, earnedScore: Double, maxScore: Double, weightPercent: Double, date: String, notes: String) {
        viewModelScope.launch {
            repository.addAssessment(
                AssessmentEntity(
                    courseId = courseId,
                    name = name,
                    earnedScore = earnedScore,
                    maxScore = maxScore,
                    weightPercent = weightPercent,
                    date = date,
                    notes = notes
                )
            )
        }
    }

    fun deleteAssessment(assessment: AssessmentEntity) {
        viewModelScope.launch {
            repository.deleteAssessment(assessment)
        }
    }

    // Lecture Operations
    fun addLecture(courseId: Long, title: String, number: Int, date: String, summary: String, instructorNotes: String, personalNotes: String) {
        viewModelScope.launch {
            repository.addLecture(
                LectureEntity(
                    courseId = courseId,
                    title = title,
                    lectureNumber = number,
                    lectureDate = date,
                    summary = summary,
                    instructorNotes = instructorNotes,
                    personalNotes = personalNotes,
                    isReviewed = false
                )
            )
        }
    }

    fun toggleLectureReviewed(lecture: LectureEntity) {
        viewModelScope.launch {
            repository.updateLecture(lecture.copy(isReviewed = !lecture.isReviewed))
        }
    }

    fun deleteLecture(lecture: LectureEntity) {
        viewModelScope.launch {
            repository.deleteLecture(lecture)
        }
    }

    // Assignment Operations
    fun addAssignment(courseId: Long, title: String, description: String, dueDate: String, priority: String) {
        viewModelScope.launch {
            val id = repository.addAssignment(
                AssignmentEntity(
                    courseId = courseId,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority,
                    status = "NOT_STARTED"
                )
            )
            // Schedule notification if urgent
            if (priority == "عاجل" && profile.value.notifyAssignments) {
                JimNotificationHelper.showTaskReminder(
                    getApplication(),
                    id.toInt(),
                    "مهمة عاجلة قادمة!",
                    "$title - موعد التسليم: $dueDate"
                )
            }
        }
    }

    fun toggleAssignmentStatus(assignment: AssignmentEntity) {
        viewModelScope.launch {
            val newStatus = when (assignment.status) {
                "NOT_STARTED" -> "IN_PROGRESS"
                "IN_PROGRESS" -> "COMPLETED"
                else -> "NOT_STARTED"
            }
            repository.updateAssignment(assignment.copy(status = newStatus))
            if (newStatus == "COMPLETED") {
                JimNotificationHelper.cancelNotification(getApplication(), assignment.id.toInt())
            }
        }
    }

    fun deleteAssignment(assignment: AssignmentEntity) {
        viewModelScope.launch {
            repository.deleteAssignment(assignment)
            JimNotificationHelper.cancelNotification(getApplication(), assignment.id.toInt())
        }
    }

    // Exam Operations
    fun addExam(courseId: Long, title: String, date: String, time: String, type: String, classroom: String, topics: String, notes: String) {
        viewModelScope.launch {
            val id = repository.addExam(
                ExamEntity(
                    courseId = courseId,
                    title = title,
                    examDate = date,
                    examTime = time,
                    examType = type,
                    classroom = classroom,
                    topics = topics,
                    prepNotes = notes
                )
            )
            if (profile.value.notifyExams) {
                JimNotificationHelper.showTaskReminder(
                    getApplication(),
                    id.toInt() + 5000,
                    "موعد اختبار أكاديمي!",
                    "$title في $date ($time)"
                )
            }
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
            JimNotificationHelper.cancelNotification(getApplication(), exam.id.toInt() + 5000)
        }
    }

    // Timetable Session Operations
    fun addSession(
        courseId: Long,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        classroom: String,
        sessionType: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addSession(
                    TimetableSessionEntity(
                        courseId = courseId,
                        dayOfWeek = dayOfWeek,
                        startTime = startTime,
                        endTime = endTime,
                        classroom = classroom,
                        sessionType = sessionType
                    )
                )
                onResult(Result.success(Unit))
            } catch (error: SQLiteException) {
                Log.e("JimViewModel", "Failed to add timetable session", error)
                onResult(Result.failure(error))
            }
        }
    }

    fun addSession(
        courseName: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        classroom: String,
        sessionType: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.addSessionForCourseName(
                    courseName = courseName,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    classroom = classroom,
                    sessionType = sessionType
                )
                onResult(Result.success(Unit))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("JimViewModel", "Failed to add timetable session", error)
                onResult(Result.failure(error))
            }
        }
    }

    fun deleteSession(session: TimetableSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun replaceSchedule(
        entries: List<TimetableImportEntry>,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.replaceSchedule(entries)
                entries.minOfOrNull { it.dayOfWeek }?.let { _selectedDayOfWeek.value = it }
                onResult(Result.success(Unit))
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("JimViewModel", "Failed to replace timetable", error)
                onResult(Result.failure(error))
            }
        }
    }

    // Notes Operations
    fun addNote(courseId: Long?, title: String, content: String, category: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.addNote(
                CourseNoteEntity(
                    courseId = courseId,
                    title = title,
                    content = content,
                    category = category,
                    isPinned = isPinned,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateNote(note: CourseNoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: CourseNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun togglePinNote(note: CourseNoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    // Library Operations
    fun addDocument(
        courseId: Long?,
        title: String,
        fileType: String,
        fileSize: String,
        category: String,
        contentSnippet: String,
        uri: String? = null
    ) {
        viewModelScope.launch {
            repository.addDocument(
                LibraryDocumentEntity(
                    courseId = courseId,
                    title = title,
                    fileType = fileType,
                    fileSize = fileSize,
                    uri = uri,
                    category = category,
                    lastReadPage = 1,
                    totalPages = 10,
                    contentSnippet = contentSnippet
                )
            )
        }
    }

    fun updateLastReadPage(doc: LibraryDocumentEntity, page: Int) {
        viewModelScope.launch {
            repository.updateDocument(doc.copy(lastReadPage = page))
        }
    }

    fun toggleDocBookmark(doc: LibraryDocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(doc.copy(isBookmarked = !doc.isBookmarked))
        }
    }

    fun deleteDocument(doc: LibraryDocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
        }
    }

    fun getAnnotationsForDoc(docId: Long): StateFlow<List<DocumentAnnotationEntity>> {
        return repository.getAnnotationsForDoc(docId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addAnnotation(docId: Long, page: Int, type: String, content: String, colorHex: String) {
        viewModelScope.launch {
            repository.addAnnotation(
                DocumentAnnotationEntity(
                    documentId = docId,
                    page = page,
                    type = type,
                    content = content,
                    colorHex = colorHex
                )
            )
        }
    }

    // Profile & Settings
    fun updateProfile(updated: StudentProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(updated)
        }
    }

    fun toggleDarkMode() {
        val current = profile.value
        updateProfile(current.copy(isDarkMode = !current.isDarkMode))
    }

    fun triggerTestNotification(context: Context) {
        JimNotificationHelper.showTaskReminder(
            context,
            JimNotificationHelper.NOTIF_ID_DAILY,
            "تذكير أكاديمي من جيم (Jim)",
            "لديك مهمتان قادمتان هذا الأسبوع ومراجعة اختبار قواعد البيانات. استمر في التفوق!"
        )
    }

    // Backup & Restore
    fun exportBackup(): String {
        return repository.exportBackupJson(
            courses.value,
            assessments.value,
            lectures.value,
            assignments.value,
            exams.value,
            sessions.value,
            notes.value,
            documents.value,
            profile.value
        )
    }

    fun restoreBackup(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreFromJson(json)
            onComplete(success)
        }
    }
}
