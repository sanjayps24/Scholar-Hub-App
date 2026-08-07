package com.example.data.local

import androidx.room.*
import com.example.data.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Study Materials ---
    @Query("SELECT * FROM study_materials ORDER BY dateAdded DESC")
    fun getAllMaterials(): Flow<List<StudyMaterial>>

    @Query("SELECT * FROM study_materials WHERE id = :id")
    suspend fun getMaterialById(id: Long): StudyMaterial?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: StudyMaterial): Long

    @Query("DELETE FROM study_materials WHERE id = :id")
    suspend fun deleteMaterial(id: Long)

    // --- Calendar Events ---
    @Query("SELECT * FROM calendar_events ORDER BY dateMillis ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEvent): Long

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteCalendarEvent(id: Long)

    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Update
    suspend fun updateTask(task: Task)

    // --- Health Logs ---
    @Query("SELECT * FROM health_logs WHERE dateString = :dateString LIMIT 1")
    suspend fun getHealthLogByDate(dateString: String): HealthLog?

    @Query("SELECT * FROM health_logs ORDER BY dateString DESC")
    fun getAllHealthLogs(): Flow<List<HealthLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthLog(healthLog: HealthLog)

    // --- Quizzes ---
    @Query("SELECT * FROM quizzes ORDER BY id DESC")
    fun getAllQuizzes(): Flow<List<Quiz>>

    @Query("SELECT * FROM quizzes WHERE materialId = :materialId")
    fun getQuizzesForMaterial(materialId: Long): Flow<List<Quiz>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: Quiz): Long

    @Update
    suspend fun updateQuiz(quiz: Quiz)

    @Query("DELETE FROM quizzes WHERE id = :id")
    suspend fun deleteQuiz(id: Long)

    // --- Study Groups ---
    @Query("SELECT * FROM study_groups ORDER BY id DESC")
    fun getAllStudyGroups(): Flow<List<StudyGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGroup(group: StudyGroup): Long

    @Query("DELETE FROM study_groups WHERE id = :id")
    suspend fun deleteStudyGroup(id: Long)

    // --- Student Profile ---
    @Query("SELECT * FROM student_profile WHERE id = 1 LIMIT 1")
    fun getStudentProfile(): Flow<StudentProfile?>

    @Query("SELECT * FROM student_profile WHERE id = 1 LIMIT 1")
    suspend fun getStudentProfileSync(): StudentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: StudentProfile)

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()
}
