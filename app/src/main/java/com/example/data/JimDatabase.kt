package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        CourseEntity::class,
        AssessmentEntity::class,
        LectureEntity::class,
        AssignmentEntity::class,
        ExamEntity::class,
        TimetableSessionEntity::class,
        CourseNoteEntity::class,
        LibraryDocumentEntity::class,
        DocumentAnnotationEntity::class,
        StudentProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JimDatabase : RoomDatabase() {
    abstract fun jimDao(): JimDao

    companion object {
        @Volatile
        private var INSTANCE: JimDatabase? = null

        fun getDatabase(context: Context): JimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JimDatabase::class.java,
                    "jim_academic_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
