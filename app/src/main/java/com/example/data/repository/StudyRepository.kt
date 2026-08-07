package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.*
import com.example.data.local.AppDao
import com.example.data.remote.FirebaseSync
import com.example.data.remote.GeminiClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class StudyRepository(
    private val appDao: AppDao,
    private val context: Context
) {
    private val TAG = "StudyRepository"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Expose local database reactive streams
    val allMaterials: Flow<List<StudyMaterial>> = appDao.getAllMaterials()
    val allCalendarEvents: Flow<List<CalendarEvent>> = appDao.getAllCalendarEvents()
    val allTasks: Flow<List<Task>> = appDao.getAllTasks()
    val allQuizzes: Flow<List<Quiz>> = appDao.getAllQuizzes()
    val allStudyGroups: Flow<List<StudyGroup>> = appDao.getAllStudyGroups()
    val allHealthLogs: Flow<List<HealthLog>> = appDao.getAllHealthLogs()

    // --- Add Study Material and Automatically Synthesize ---
    suspend fun addStudyMaterial(title: String, sourceType: String, contentText: String, sourceUrl: String = ""): StudyMaterial {
        // 1. Generate Summary, Timeline, and Knowledge Graph in parallel or sequence
        val summary = try {
            GeminiClient.generateSummary(title, contentText)
        } catch (e: Exception) {
            "Offline Summary generated locally."
        }

        val timelineEvents = try {
            GeminiClient.generateTimeline(contentText)
        } catch (e: Exception) {
            emptyList()
        }
        val timelineJson = try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, TimelineEvent::class.java)
            moshi.adapter<List<TimelineEvent>>(type).toJson(timelineEvents)
        } catch (e: Exception) {
            "[]"
        }

        val graph = try {
            GeminiClient.generateKnowledgeGraph(contentText)
        } catch (e: Exception) {
            KnowledgeGraph()
        }
        val graphJson = try {
            moshi.adapter(KnowledgeGraph::class.java).toJson(graph)
        } catch (e: Exception) {
            "{}"
        }

        // 2. Build StudyMaterial Entity
        val material = StudyMaterial(
            title = title,
            sourceType = sourceType,
            sourceUrl = sourceUrl,
            contentText = contentText,
            summaryText = summary,
            timelineJson = timelineJson,
            knowledgeGraphJson = graphJson
        )

        // 3. Save to Room Local DB (Offline Mode First)
        val id = appDao.insertMaterial(material)
        val savedMaterial = material.copy(id = id)

        // 4. Optionally Generate and Save an initial Quiz
        try {
            val questions = GeminiClient.generateQuiz(title, contentText)
            val questionsJson = moshi.adapter<List<QuizQuestion>>(
                com.squareup.moshi.Types.newParameterizedType(List::class.java, QuizQuestion::class.java)
            ).toJson(questions)

            val quiz = Quiz(
                materialId = id,
                title = "Quiz: $title",
                questionsJson = questionsJson,
                totalQuestions = questions.size
            )
            appDao.insertQuiz(quiz)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-generate quiz", e)
        }

        // 5. Sync to cloud if authenticated
        FirebaseSync.uploadNotesToCloud(context, id, title, contentText, summary)

        return savedMaterial
    }

    suspend fun deleteMaterial(id: Long) {
        appDao.deleteMaterial(id)
    }

    // --- Calendar Events ---
    suspend fun addCalendarEvent(title: String, dateMillis: Long, description: String, type: String): CalendarEvent {
        // Auto-compute balanced workload advice or notification details
        val workloadNote = when (type) {
            "Exam" -> "High workload peak detected! Schedule daily 45-minute revision blocks 7 days leading up to this exam to optimize retention."
            "Deadline" -> "Firm deadline. Break this project down into small micro-tasks to prevent night-before cramming."
            else -> "Regular session. Spend 15 minutes reviewing active recall questions afterward."
        }

        val event = CalendarEvent(
            title = title,
            dateMillis = dateMillis,
            description = description,
            type = type,
            balancedWorkloadNote = workloadNote
        )
        val id = appDao.insertCalendarEvent(event)
        return event.copy(id = id)
    }

    suspend fun deleteCalendarEvent(id: Long) {
        appDao.deleteCalendarEvent(id)
    }

    // --- Syllabi Parsing to Tasks ---
    suspend fun importSyllabusTasks(syllabusText: String, calendarEvents: List<CalendarEvent>) {
        val tasks = GeminiClient.organizeTasksFromSyllabus(syllabusText, calendarEvents)
        tasks.forEach { task ->
            appDao.insertTask(task)
        }
    }

    // --- Tasks ---
    suspend fun addTask(title: String, courseName: String, dueDate: Long, difficulty: String = "Medium") {
        appDao.insertTask(Task(title = title, courseName = courseName, dueDate = dueDate, difficulty = difficulty))
    }

    suspend fun toggleTaskCompleted(task: Task) {
        appDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(id: Long) {
        appDao.deleteTask(id)
    }

    // --- Health Analyzer Routine & Sleep Feedback ---
    suspend fun addHealthLog(sleepHours: Double, dailyRoutine: String): HealthLog {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val (analysis, tips) = GeminiClient.healthFeedback(sleepHours, dailyRoutine)

        val log = HealthLog(
            dateString = dateStr,
            sleepHours = sleepHours,
            dailyRoutine = dailyRoutine,
            healthTips = "$analysis\n\n$tips"
        )
        appDao.insertHealthLog(log)
        return log
    }

    suspend fun getTodayHealthLog(): HealthLog? {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return appDao.getHealthLogByDate(dateStr)
    }

    // --- Quiz Responses ---
    suspend fun insertQuiz(quiz: Quiz): Long {
        return appDao.insertQuiz(quiz)
    }

    suspend fun submitQuizResult(quiz: Quiz, score: Int) {
        appDao.updateQuiz(quiz.copy(score = score, isTaken = true))
    }

    // --- Collaborative Groups ---
    suspend fun createStudyGroup(name: String): StudyGroup {
        val randomCode = (100000..999999).random().toString()
        val group = StudyGroup(
            name = name,
            code = randomCode,
            mockActiveMembers = 1 // Creator
        )
        val id = appDao.insertStudyGroup(group)
        return group.copy(id = id)
    }

    suspend fun joinStudyGroup(code: String): StudyGroup? {
        val joinedGroup = StudyGroup(
            name = "Study Group #$code",
            code = code,
            mockActiveMembers = 1
        )
        val id = appDao.insertStudyGroup(joinedGroup)
        return joinedGroup.copy(id = id)
    }

    suspend fun deleteStudyGroup(id: Long) {
        appDao.deleteStudyGroup(id)
    }

    // --- File URI Upload Parsing ---
    suspend fun addStudyMaterialFromUri(uri: android.net.Uri): StudyMaterial {
        val parsed = com.example.util.FileParser.parseUri(context, uri)
        return addStudyMaterial(
            title = parsed.fileName,
            sourceType = parsed.sourceType,
            contentText = parsed.extractedText,
            sourceUrl = uri.toString()
        )
    }

    // --- Student Profile Persistence ---
    val studentProfile: Flow<StudentProfile?> = appDao.getStudentProfile()

    suspend fun getStudentProfileSync(): StudentProfile? {
        return appDao.getStudentProfileSync()
    }

    suspend fun saveStudentProfile(profile: StudentProfile) {
        appDao.insertOrUpdateProfile(profile)
    }

    suspend fun updateLoginState(isLoggedIn: Boolean) {
        val current = appDao.getStudentProfileSync()
        if (current != null) {
            appDao.insertOrUpdateProfile(current.copy(isLoggedIn = isLoggedIn))
        }
    }

    // --- AI Chatbot Messages Persistence ---
    val allChatMessages: Flow<List<ChatMessage>> = appDao.getAllChatMessages()

    suspend fun addChatMessage(sender: String, text: String, sourcesJson: String = "[]"): Long {
        return appDao.insertChatMessage(ChatMessage(sender = sender, text = text, sourcesJson = sourcesJson))
    }

    suspend fun clearChatHistory() {
        appDao.clearChatHistory()
    }
}
