package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val instructor: String = "",
    val classroom: String = "",
    val creditHours: Int = 3,
    val colorHex: String = "#2563EB",
    val iconKey: String = "code",
    val description: String = "",
    val semester: String = "الفصل الدراسي الحالي",
    val isArchived: Boolean = false
)

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val name: String,
    val earnedScore: Double,
    val maxScore: Double,
    val weightPercent: Double = 0.0,
    val date: String = "",
    val notes: String = ""
)

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val title: String,
    val lectureNumber: Int = 1,
    val lectureDate: String = "",
    val summary: String = "",
    val instructorNotes: String = "",
    val personalNotes: String = "",
    val isReviewed: Boolean = false
)

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val title: String,
    val description: String = "",
    val dueDate: String = "",
    val priority: String = "متوسط", // عاجل, متوسط, منخفض
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED
    val gradeEarned: Double? = null
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val title: String,
    val examDate: String = "",
    val examTime: String = "",
    val examType: String = "نظري", // نظري, عملي
    val classroom: String = "",
    val topics: String = "",
    val prepNotes: String = "",
    val gradeEarned: Double? = null,
    val isCompleted: Boolean = false
)

@Entity(tableName = "timetable_sessions")
data class TimetableSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: Int, // 1=الأحد, 2=الإثنين, 3=الثلاثاء, 4=الأربعاء, 5=الخميس
    val startTime: String, // e.g. "08:00"
    val endTime: String, // e.g. "09:00"
    val classroom: String = "",
    val sessionType: String = "محاضرة" // محاضرة, معمل
)

data class TimetableImportEntry(
    val courseName: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val classroom: String = "",
    val sessionType: String = "محاضرة"
)

@Entity(tableName = "course_notes")
data class CourseNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long? = null, // null means global quick note
    val title: String,
    val content: String = "",
    val category: String = "عام", // عام, مهم, أسئلة, مراجعة
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "library_documents")
data class LibraryDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long? = null,
    val title: String,
    val fileType: String = "PDF", // PDF, WORD, PPTX, IMAGE
    val fileSize: String = "2.4 MB",
    val uri: String? = null,
    val category: String = "محاضرات",
    val lastReadPage: Int = 1,
    val totalPages: Int = 12,
    val isBookmarked: Boolean = false,
    val contentSnippet: String = ""
)

@Entity(tableName = "document_annotations")
data class DocumentAnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val page: Int,
    val type: String = "HIGHLIGHT", // HIGHLIGHT, NOTE, DRAWING
    val content: String,
    val colorHex: String = "#FFEB3B"
)

@Entity(tableName = "student_profile")
data class StudentProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "أحمد محمد",
    val titleRole: String = "طالب جامعي",
    val universityId: String = "20240123",
    val university: String = "جامعة سيئون",
    val college: String = "كلية الحاسوب",
    val major: String = "تقنية المعلومات",
    val academicLevel: String = "الثاني",
    val academicAdvisor: String = "د. محمد علي",
    val currentGpa: Double = 3.42,
    val targetGpa: Double = 3.75,
    val maxGpaScale: Double = 4.0,
    val totalCredits: Int = 132,
    val completedCredits: Int = 45,
    val semesterCredits: Int = 15,
    val isDarkMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notifyAssignments: Boolean = true,
    val notifyExams: Boolean = true,
    val dailySummaryTime: String = "08:00 ص"
)
