package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface JimDao {

    // Courses
    @Query("SELECT * FROM courses ORDER BY id ASC")
    fun getAllCoursesFlow(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    // Assessments
    @Query("SELECT * FROM assessments WHERE courseId = :courseId ORDER BY id ASC")
    fun getAssessmentsForCourseFlow(courseId: Long): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments ORDER BY id ASC")
    fun getAllAssessmentsFlow(): Flow<List<AssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity): Long

    @Update
    suspend fun updateAssessment(assessment: AssessmentEntity)

    @Delete
    suspend fun deleteAssessment(assessment: AssessmentEntity)

    // Lectures
    @Query("SELECT * FROM lectures WHERE courseId = :courseId ORDER BY lectureNumber ASC")
    fun getLecturesForCourseFlow(courseId: Long): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures ORDER BY id DESC")
    fun getAllLecturesFlow(): Flow<List<LectureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLecture(lecture: LectureEntity): Long

    @Update
    suspend fun updateLecture(lecture: LectureEntity)

    @Delete
    suspend fun deleteLecture(lecture: LectureEntity)

    // Assignments
    @Query("SELECT * FROM assignments ORDER BY CASE priority WHEN 'عاجل' THEN 1 WHEN 'متوسط' THEN 2 ELSE 3 END, dueDate ASC")
    fun getAllAssignmentsFlow(): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE courseId = :courseId ORDER BY dueDate ASC")
    fun getAssignmentsForCourseFlow(courseId: Long): Flow<List<AssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: AssignmentEntity): Long

    @Update
    suspend fun updateAssignment(assignment: AssignmentEntity)

    @Delete
    suspend fun deleteAssignment(assignment: AssignmentEntity)

    // Exams
    @Query("SELECT * FROM exams ORDER BY examDate ASC, examTime ASC")
    fun getAllExamsFlow(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE courseId = :courseId ORDER BY examDate ASC")
    fun getExamsForCourseFlow(courseId: Long): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    // Timetable Sessions
    @Query("SELECT * FROM timetable_sessions ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSessionsFlow(): Flow<List<TimetableSessionEntity>>

    @Query("SELECT * FROM timetable_sessions WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getSessionsForDayFlow(dayOfWeek: Int): Flow<List<TimetableSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimetableSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<TimetableSessionEntity>)

    @Update
    suspend fun updateSession(session: TimetableSessionEntity)

    @Delete
    suspend fun deleteSession(session: TimetableSessionEntity)

    @Query("DELETE FROM timetable_sessions")
    suspend fun clearAllSessions()

    // Notes
    @Query("SELECT * FROM course_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotesFlow(): Flow<List<CourseNoteEntity>>

    @Query("SELECT * FROM course_notes WHERE courseId = :courseId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesForCourseFlow(courseId: Long): Flow<List<CourseNoteEntity>>

    @Query("SELECT * FROM course_notes WHERE courseId IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getQuickNotesFlow(): Flow<List<CourseNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CourseNoteEntity): Long

    @Update
    suspend fun updateNote(note: CourseNoteEntity)

    @Delete
    suspend fun deleteNote(note: CourseNoteEntity)

    // Library Documents
    @Query("SELECT * FROM library_documents ORDER BY id ASC")
    fun getAllDocumentsFlow(): Flow<List<LibraryDocumentEntity>>

    @Query("SELECT * FROM library_documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: Long): LibraryDocumentEntity?

    @Query("SELECT * FROM library_documents WHERE courseId = :courseId ORDER BY id ASC")
    fun getDocumentsForCourseFlow(courseId: Long): Flow<List<LibraryDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: LibraryDocumentEntity): Long

    @Update
    suspend fun updateDocument(doc: LibraryDocumentEntity)

    @Delete
    suspend fun deleteDocument(doc: LibraryDocumentEntity)

    // Annotations
    @Query("SELECT * FROM document_annotations WHERE documentId = :docId ORDER BY page ASC, id ASC")
    fun getAnnotationsForDocumentFlow(docId: Long): Flow<List<DocumentAnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: DocumentAnnotationEntity): Long

    @Delete
    suspend fun deleteAnnotation(annotation: DocumentAnnotationEntity)

    // Student Profile
    @Query("SELECT * FROM student_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<StudentProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: StudentProfileEntity)

    // Count checks for seed initialization
    @Query("SELECT COUNT(*) FROM courses")
    suspend fun getCourseCount(): Int
}
