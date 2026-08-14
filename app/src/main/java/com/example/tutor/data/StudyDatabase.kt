package com.example.tutor.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val subject: String,
    val description: String,
    val estimatedDuration: String,
    val createdAt: Long
)

@Entity(
    tableName = "study_topics",
    foreignKeys = [
        ForeignKey(
            entity = StudyPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["planId"])]
)
data class StudyTopicEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val title: String,
    val description: String,
    val keyConcept: String,
    val orderIndex: Int,
    val status: String, // "PENDING", "LOGRADO", "DIFICULTAD"
    val miniLessonJson: String?,
    val updatedAt: Long
)

@Entity(tableName = "tutor_chat_history")
data class TutorChatMessageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long,
    val suggestedTopicPrompt: String?
)

data class StudyPlanWithTopics(
    @Embedded val plan: StudyPlanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val topics: List<StudyTopicEntity>
)

@Dao
interface StudyPlanDao {
    @Transaction
    @Query("SELECT * FROM study_plans WHERE userId = :userId ORDER BY createdAt DESC")
    fun getStudyPlansWithTopics(userId: String): Flow<List<StudyPlanWithTopics>>

    @Transaction
    @Query("SELECT * FROM study_plans WHERE id = :planId")
    fun getStudyPlanWithTopicsById(planId: String): Flow<StudyPlanWithTopics?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStudyPlan(plan: StudyPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTopics(topics: List<StudyTopicEntity>): List<Long>

    @Query("UPDATE study_topics SET status = :status, updatedAt = :updatedAt WHERE id = :topicId")
    fun updateTopicStatus(topicId: String, status: String, updatedAt: Long): Int

    @Query("UPDATE study_topics SET miniLessonJson = :lessonJson, status = :status, updatedAt = :updatedAt WHERE id = :topicId")
    fun saveTopicLessonAndStatus(topicId: String, lessonJson: String, status: String, updatedAt: Long): Int

    @Query("DELETE FROM study_plans WHERE id = :planId")
    fun deleteStudyPlan(planId: String): Int

    // Chat History
    @Query("SELECT * FROM tutor_chat_history WHERE userId = :userId ORDER BY timestamp ASC")
    fun getTutorChatMessages(userId: String): Flow<List<TutorChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTutorChatMessage(message: TutorChatMessageEntity): Long

    @Query("DELETE FROM tutor_chat_history WHERE userId = :userId")
    fun clearTutorChat(userId: String): Int
}

@Entity(tableName = "local_announcements")
data class LocalAnnouncement(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val teacherId: String,
    val teacherName: String,
    val subject: String,
    val targetType: String,
    val grade: String,
    val section: String,
    val priority: String,
    val createdAt: Long
)

@Dao
interface LocalAnnouncementDao {
    @Query("SELECT * FROM local_announcements ORDER BY createdAt DESC")
    fun getAllLocalAnnouncements(): Flow<List<LocalAnnouncement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAnnouncements(announcements: List<LocalAnnouncement>)

    @Query("DELETE FROM local_announcements")
    fun clearAll(): Int
}

@Database(
    entities = [
        StudyPlanEntity::class,
        StudyTopicEntity::class,
        TutorChatMessageEntity::class,
        LocalAnnouncement::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun localAnnouncementDao(): LocalAnnouncementDao

    companion object {
        @Volatile
        private var INSTANCE: StudyDatabase? = null

        fun getDatabase(context: Context): StudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_plans_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
