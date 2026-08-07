package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.local.AppDatabase
import com.example.data.remote.FirebaseSync
import com.example.data.remote.GeminiClient
import com.example.data.remote.WebSource
import com.example.data.repository.StudyRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "StudyViewModel"
    private val repository: StudyRepository
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // Database flow bindings
    val materials: StateFlow<List<StudyMaterial>>
    val calendarEvents: StateFlow<List<CalendarEvent>>
    val tasks: StateFlow<List<Task>>
    val quizzes: StateFlow<List<Quiz>>
    val studyGroups: StateFlow<List<StudyGroup>>
    val healthLogs: StateFlow<List<HealthLog>>
    val studentProfile: StateFlow<StudentProfile?>
    val chatMessages: StateFlow<List<ChatMessage>>

    // Chatbot configuration states
    val isChatLoading = MutableStateFlow(false)
    val selectedChatModel = MutableStateFlow("gemini-3.5-flash") // "gemini-3.1-flash-lite-preview", "gemini-3.5-flash", "gemini-3.1-pro-preview"
    val selectedSystemRole = MutableStateFlow("🎓 Academic Tutor") // "🎓 Academic Tutor", "📝 Exam Cram Coach", "🔬 STEM & Code Expert", "💡 Creative Mind-Mapper"
    val authError = MutableStateFlow<String?>(null)
    val showProfileDialog = MutableStateFlow(false)

    // UI state states
    val isLoading = MutableStateFlow(false)
    val selectedMaterial = MutableStateFlow<StudyMaterial?>(null)
    val todayHealthLog = MutableStateFlow<HealthLog?>(null)

    // Q&A session states
    val qaQuery = MutableStateFlow("")
    val qaAnswer = MutableStateFlow("")
    val qaSources = MutableStateFlow<List<WebSource>>(emptyList())
    val isQaLoading = MutableStateFlow(false)
    val enableSearchGrounding = MutableStateFlow(true)

    // Quiz session states
    val activeQuiz = MutableStateFlow<Quiz?>(null)
    val quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val currentQuestionIndex = MutableStateFlow(0)
    val quizScore = MutableStateFlow(0)
    val isQuizFinished = MutableStateFlow(false)

    // Theme and connection states
    val darkThemeEnabled = MutableStateFlow(true) // Professional dark theme by default
    val firebaseAvailable = MutableStateFlow(false)
    val userEmail = MutableStateFlow("4mh23cs133@gmail.com") // Current User from context
    val notificationAlerts = MutableStateFlow<List<String>>(emptyList())

    init {
        val appDatabase = AppDatabase.getDatabase(application)
        repository = StudyRepository(appDatabase.appDao(), application)

        materials = repository.allMaterials.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        calendarEvents = repository.allCalendarEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        tasks = repository.allTasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        quizzes = repository.allQuizzes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        studyGroups = repository.allStudyGroups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        healthLogs = repository.allHealthLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        studentProfile = repository.studentProfile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        chatMessages = repository.allChatMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Refresh dynamic configurations
        firebaseAvailable.value = FirebaseSync.isFirebaseAvailable(application)
        loadTodayHealth()
        observeExamDeadlines()
        ensureDefaultProfile()
    }

    private fun ensureDefaultProfile() {
        viewModelScope.launch {
            val profile = repository.getStudentProfileSync()
            if (profile == null) {
                // Initialize default profile
                val defaultProf = StudentProfile(
                    id = 1,
                    fullName = "Alex Vance",
                    email = "4mh23cs133@gmail.com",
                    passwordHash = "password123",
                    major = "Computer Science & AI",
                    gradeLevel = "Junior (3rd Year)",
                    targetGpa = "3.9",
                    avatarIndex = 0,
                    isLoggedIn = false
                )
                repository.saveStudentProfile(defaultProf)
            }
        }
    }

    // --- Student Authentication & Profile Management ---
    fun signUp(fullName: String, email: String, password: String, major: String, gradeLevel: String, targetGpa: String, avatarIndex: Int, dateOfBirth: String = "") {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            authError.value = "Please fill in all required fields."
            return
        }
        viewModelScope.launch {
            authError.value = null
            val profile = StudentProfile(
                id = 1,
                fullName = fullName,
                email = email,
                passwordHash = password,
                major = major.ifBlank { "Computer Science & Engineering" },
                gradeLevel = gradeLevel.ifBlank { "1st Year / Freshman" },
                targetGpa = targetGpa.ifBlank { "3.8" },
                avatarIndex = avatarIndex,
                isLoggedIn = true,
                dateOfBirth = dateOfBirth
            )
            repository.saveStudentProfile(profile)
            userEmail.value = email
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val profile = repository.getStudentProfileSync()
            if (profile != null) {
                if (profile.email.equals(email, ignoreCase = true) && (profile.passwordHash.isEmpty() || profile.passwordHash == password)) {
                    authError.value = null
                    repository.saveStudentProfile(profile.copy(isLoggedIn = true))
                    userEmail.value = email
                } else if (profile.email.equals(email, ignoreCase = true) && profile.passwordHash != password) {
                    authError.value = "Incorrect password. Please try again."
                } else {
                    // Create quick account for the new email
                    authError.value = null
                    val newProf = StudentProfile(
                        id = 1,
                        fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = email,
                        passwordHash = password,
                        isLoggedIn = true
                    )
                    repository.saveStudentProfile(newProf)
                    userEmail.value = email
                }
            } else {
                authError.value = null
                val newProf = StudentProfile(
                    id = 1,
                    fullName = "Student User",
                    email = email,
                    passwordHash = password,
                    isLoggedIn = true
                )
                repository.saveStudentProfile(newProf)
                userEmail.value = email
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.updateLoginState(false)
            showProfileDialog.value = false
        }
    }

    fun updateProfileDetails(fullName: String, major: String, gradeLevel: String, targetGpa: String, avatarIndex: Int) {
        viewModelScope.launch {
            val current = studentProfile.value
            if (current != null) {
                val updated = current.copy(
                    fullName = fullName,
                    major = major,
                    gradeLevel = gradeLevel,
                    targetGpa = targetGpa,
                    avatarIndex = avatarIndex
                )
                repository.saveStudentProfile(updated)
            }
        }
    }

    // --- File Upload Action from URI (Device Folder Picker) ---
    fun uploadFileFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val added = repository.addStudyMaterialFromUri(uri)
                selectedMaterial.value = added
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse and upload file from folder", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    // --- Material Upload Action ---
    fun uploadStudyMaterial(title: String, sourceType: String, contentText: String, sourceUrl: String = "") {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val added = repository.addStudyMaterial(title, sourceType, contentText, sourceUrl)
                selectedMaterial.value = added
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload study material", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun selectMaterial(material: StudyMaterial?) {
        selectedMaterial.value = material
        qaAnswer.value = ""
        qaSources.value = emptyList()
        qaQuery.value = ""
        activeQuiz.value = null
        isQuizFinished.value = false
    }

    fun deleteMaterial(id: Long) {
        viewModelScope.launch {
            repository.deleteMaterial(id)
            if (selectedMaterial.value?.id == id) {
                selectedMaterial.value = null
            }
        }
    }

    // --- Q&A Action ---
    fun submitQuestion(question: String) {
        val material = selectedMaterial.value ?: return
        if (question.isBlank()) return
        
        viewModelScope.launch {
            isQaLoading.value = true
            qaQuery.value = question
            qaAnswer.value = ""
            qaSources.value = emptyList()
            try {
                val (answer, sources) = GeminiClient.askQuestion(
                    question = question,
                    contentText = material.contentText,
                    enableSearch = enableSearchGrounding.value
                )
                qaAnswer.value = answer
                qaSources.value = sources
            } catch (e: Exception) {
                qaAnswer.value = "An error occurred: ${e.localizedMessage}"
            } finally {
                isQaLoading.value = false
            }
        }
    }

    // --- Interactive Quiz Actions ---
    fun startQuizForMaterial(material: StudyMaterial) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // Find existing quiz in our quizzes list or create on the fly
                val existing = quizzes.value.firstOrNull { it.materialId == material.id }
                if (existing != null) {
                    activeQuiz.value = existing
                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, QuizQuestion::class.java)
                    val questions: List<QuizQuestion> = moshi.adapter<List<QuizQuestion>>(type).fromJson(existing.questionsJson) ?: emptyList()
                    // Reset selected options
                    questions.forEach { it.selectedAnswerIndex = -1 }
                    quizQuestions.value = questions
                } else {
                    // Generate new quiz
                    val questions = GeminiClient.generateQuiz(material.title, material.contentText)
                    val questionsJson = moshi.adapter<List<QuizQuestion>>(
                        com.squareup.moshi.Types.newParameterizedType(List::class.java, QuizQuestion::class.java)
                    ).toJson(questions)

                    val newQuiz = Quiz(
                        materialId = material.id,
                        title = "Quiz: ${material.title}",
                        questionsJson = questionsJson,
                        totalQuestions = questions.size
                    )
                    val id = repository.insertQuiz(newQuiz) // Initialize
                    activeQuiz.value = newQuiz.copy(id = id)
                    quizQuestions.value = questions
                }
                currentQuestionIndex.value = 0
                quizScore.value = 0
                isQuizFinished.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Quiz loading error", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun selectQuizAnswer(questionIndex: Int, answerIndex: Int) {
        val currentQuestions = quizQuestions.value.toMutableList()
        if (questionIndex in currentQuestions.indices) {
            currentQuestions[questionIndex] = currentQuestions[questionIndex].copy(selectedAnswerIndex = answerIndex)
            quizQuestions.value = currentQuestions
        }
    }

    fun nextQuizQuestion() {
        val nextIdx = currentQuestionIndex.value + 1
        if (nextIdx < quizQuestions.value.size) {
            currentQuestionIndex.value = nextIdx
        } else {
            finishQuiz()
        }
    }

    private fun finishQuiz() {
        val questions = quizQuestions.value
        var correctCount = 0
        questions.forEach { q ->
            if (q.selectedAnswerIndex == q.correctAnswerIndex) {
                correctCount++
            }
        }
        quizScore.value = correctCount
        isQuizFinished.value = true

        val quiz = activeQuiz.value ?: return
        viewModelScope.launch {
            repository.submitQuizResult(quiz, correctCount)
        }
    }

    // --- Task & Calendar Management ---
    fun addCalendarEvent(title: String, dateMillis: Long, description: String, type: String) {
        viewModelScope.launch {
            repository.addCalendarEvent(title, dateMillis, description, type)
            observeExamDeadlines()
        }
    }

    fun deleteCalendarEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
            observeExamDeadlines()
        }
    }

    fun addManualTask(title: String, courseName: String, dueDate: Long, difficulty: String) {
        viewModelScope.launch {
            repository.addTask(title, courseName, dueDate, difficulty)
        }
    }

    fun importSyllabus(syllabusText: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.importSyllabusTasks(syllabusText, calendarEvents.value)
            } catch (e: Exception) {
                Log.e(TAG, "Syllabus parsing failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    // --- Health Analyzer Management ---
    private fun loadTodayHealth() {
        viewModelScope.launch {
            todayHealthLog.value = repository.getTodayHealthLog()
        }
    }

    fun logHealthMetrics(sleepHours: Double, routineText: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val log = repository.addHealthLog(sleepHours, routineText)
                todayHealthLog.value = log
            } catch (e: Exception) {
                Log.e(TAG, "Health log saving failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    // --- Collaborative Study Groups ---
    fun createStudyGroup(name: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.createStudyGroup(name)
            } catch (e: Exception) {
                Log.e(TAG, "Group creation failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun joinStudyGroup(code: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                repository.joinStudyGroup(code)
            } catch (e: Exception) {
                Log.e(TAG, "Group joining failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun leaveStudyGroup(id: Long) {
        viewModelScope.launch {
            repository.deleteStudyGroup(id)
        }
    }

    // --- Settings & UI Theme toggles ---
    fun toggleTheme() {
        darkThemeEnabled.value = !darkThemeEnabled.value
    }

    // --- Exam Notification Alerts ---
    private fun observeExamDeadlines() {
        viewModelScope.launch {
            calendarEvents.collect { events ->
                val upcomingAlerts = mutableListOf<String>()
                val now = System.currentTimeMillis()
                events.forEach { event ->
                    val diff = event.dateMillis - now
                    if (event.type == "Exam" || event.type == "Deadline") {
                        if (diff in 0..(86400000 * 7)) { // Within 7 days
                            val daysLeft = (diff / 86400000) + 1
                            upcomingAlerts.add("⚠️ Upcoming exam deadline: \"${event.title}\" is in $daysLeft days! Balance your study sessions carefully.")
                        }
                    }
                }
                notificationAlerts.value = upcomingAlerts
            }
        }
    }

    // --- Live AI Chatbot Actions ---
    fun sendLiveChatMessage(prompt: String, imageB64: String? = null) {
        if (prompt.isBlank() && imageB64.isNullOrBlank()) return
        viewModelScope.launch {
            val userDisplayMessage = if (!imageB64.isNullOrBlank()) {
                if (prompt.isBlank()) "📷 [Uploaded Image for Analysis]" else "📷 $prompt"
            } else {
                prompt
            }

            // Save user message
            repository.addChatMessage(sender = "user", text = userDisplayMessage)
            isChatLoading.value = true

            try {
                val profile = studentProfile.value
                val profileInfo = "Name: ${profile?.fullName ?: "Student"}, Major: ${profile?.major ?: "General"}, Grade: ${profile?.gradeLevel ?: "Undergraduate"}, Target GPA: ${profile?.targetGpa ?: "3.8"}"
                
                val notesContext = materials.value.take(5).joinToString("\n\n") { material ->
                    "Title: ${material.title}\nSummary: ${material.summaryText.take(150)}\nSnippet: ${material.contentText.take(300)}"
                }

                val history = chatMessages.value

                val (aiResponse, sources) = GeminiClient.sendMultiTurnChat(
                    history = history,
                    newPrompt = prompt.ifBlank { "Analyze this uploaded image in detail and extract key study insights." },
                    systemRole = selectedSystemRole.value,
                    studentProfileInfo = profileInfo,
                    notesContext = notesContext,
                    modelName = selectedChatModel.value,
                    enableSearch = enableSearchGrounding.value,
                    imageB64 = imageB64
                )

                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, WebSource::class.java)
                val sourcesJson = moshi.adapter<List<WebSource>>(type).toJson(sources)

                repository.addChatMessage(sender = "ai", text = aiResponse, sourcesJson = sourcesJson)
            } catch (e: Exception) {
                repository.addChatMessage(sender = "ai", text = "I synthesized an answer for you: ${e.localizedMessage}")
            } finally {
                isChatLoading.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }
}
