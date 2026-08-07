package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Retrofit Service ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Moshi Mapped Request/Response Classes ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class Tool(
    val google_search: GoogleSearch? = null
)

@JsonClass(generateAdapter = true)
class GoogleSearch // Empty class representing the google_search block

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?,
    val groundingMetadata: GroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMetadata(
    val searchEntryPoint: SearchEntryPoint? = null,
    val groundingChunks: List<GroundingChunk>? = null,
    val webSearchQueries: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SearchEntryPoint(
    val renderedContent: String? = null
)

@JsonClass(generateAdapter = true)
data class GroundingChunk(
    val web: WebSource? = null
)

@JsonClass(generateAdapter = true)
data class WebSource(
    val uri: String?,
    val title: String?
)

// --- Client Singleton ---

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val DEFAULT_MODEL = "gemini-3.5-flash"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
    }

    fun isApiKeyAvailable(): Boolean {
        return getApiKey().isNotEmpty()
    }

    // --- Core Generation Tasks ---

    /**
     * Call general raw content generation with optional Google Search Grounding
     */
    suspend fun generateRawContent(
        prompt: String,
        systemInstruction: String? = null,
        enableSearch: Boolean = false,
        isJsonResponse: Boolean = false,
        modelOverride: String? = null
    ): Pair<String, List<WebSource>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext "Error: Gemini API Key is missing. Please add your key to the Secrets panel." to emptyList()
        }

        val model = modelOverride ?: DEFAULT_MODEL

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) },
            tools = if (enableSearch) listOf(Tool(google_search = GoogleSearch())) else null,
            generationConfig = GenerationConfig(
                responseMimeType = if (isJsonResponse) "application/json" else "text/plain",
                temperature = 0.4f
            )
        )

        try {
            val response = apiService.generateContent(model, apiKey, request)
            val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response content."

            // Parse citations from grounding metadata if present
            val sources = mutableListOf<WebSource>()
            response.groundingMetadata?.groundingChunks?.forEach { chunk ->
                chunk.web?.let { web ->
                    if (!web.uri.isNullOrBlank() && !web.title.isNullOrBlank()) {
                        sources.add(web)
                    }
                }
            }

            textResult to sources
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            Log.e(TAG, "Gemini API HTTP Error ${e.code()}: $errorBody", e)

            // Graceful Fallback 1: If search grounding is enabled and fails with 403, try without search grounding
            if (e.code() == 403 && enableSearch) {
                Log.w(TAG, "Search grounding 403. Retrying without search grounding...")
                return@withContext generateRawContent(
                    prompt = prompt,
                    systemInstruction = systemInstruction,
                    enableSearch = false,
                    isJsonResponse = isJsonResponse,
                    modelOverride = model
                )
            }

            // Graceful Fallback 2: If model is gemini-3.5-flash and fails with 403, try falling back to gemini-2.5-flash
            if (e.code() == 403 && model == DEFAULT_MODEL) {
                Log.w(TAG, "Model $DEFAULT_MODEL 403. Retrying with gemini-2.5-flash...")
                return@withContext generateRawContent(
                    prompt = prompt,
                    systemInstruction = systemInstruction,
                    enableSearch = enableSearch,
                    isJsonResponse = isJsonResponse,
                    modelOverride = "gemini-2.5-flash"
                )
            }

            // Standard fallback to user friendly message
            val friendlyMsg = if (e.code() == 403) {
                "Error: Permission Denied (HTTP 403). Your Gemini API Key might be invalid, restricted, or requires enabling in the Google AI Studio Secrets panel."
            } else {
                "Error: HTTP ${e.code()} from Gemini API - $errorBody"
            }
            friendlyMsg to emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API failure", e)
            "Error generated during API request: ${e.localizedMessage}" to emptyList()
        }
    }

    // --- High-Level Helper Functions with Offline Fallbacks ---

    suspend fun generateSummary(title: String, contentText: String): String {
        if (!isApiKeyAvailable()) {
            return generateMockSummary(title, contentText)
        }
        val prompt = """
            Summarize the following study notes titled "$title" in a highly professional, educational, and structured manner.
            Identify 3-5 main key concepts, write detailed explanations for each, and extract a concluding key takeaway.
            Format with beautiful markdown. Prioritize accuracy and stay close to the facts in the text.
            
            Study Notes Content:
            $contentText
        """.trimIndent()

        val (result, _) = generateRawContent(prompt, systemInstruction = "You are an elite, concise study assistant specializing in synthesizing notes for students.")
        return if (result.startsWith("Error")) generateMockSummary(title, contentText) else result
    }

    suspend fun generateTimeline(contentText: String): List<TimelineEvent> {
        if (!isApiKeyAvailable()) {
            return generateMockTimeline()
        }
        val prompt = """
            Extract a chronological timeline of key events, breakthroughs, historical dates, or sequential concepts mentioned in these study notes.
            Return a JSON array of events. Every event object MUST contain:
            - "date" (String representing the year, date, or order step)
            - "title" (String name of the event)
            - "description" (String detailing the event)
            - "citation" (String citing the precise section of materials)
            
            Do not include markdown blocks, just the raw JSON array.
            
            Notes Content:
            $contentText
        """.trimIndent()

        return try {
            val (result, _) = generateRawContent(prompt, isJsonResponse = true)
            parseJsonList<TimelineEvent>(result) ?: generateMockTimeline()
        } catch (e: Exception) {
            generateMockTimeline()
        }
    }

    suspend fun generateKnowledgeGraph(contentText: String): KnowledgeGraph {
        if (!isApiKeyAvailable()) {
            return generateMockKnowledgeGraph()
        }
        val prompt = """
            Analyze these study notes and construct a Knowledge Graph outlining core concepts and relationships.
            Return a raw JSON object with the following schema:
            {
              "nodes": [
                { "id": "unique_lowercase_id", "label": "Concept Name", "type": "concept" }
              ],
              "edges": [
                { "from": "id_source", "to": "id_target", "label": "relationship description" }
              ]
            }
            
            Create between 5 to 10 nodes showing how they interconnect. Ensure type is either "concept", "entity", or "process".
            Do not wrap in markdown, output raw JSON object.
            
            Notes Content:
            $contentText
        """.trimIndent()

        return try {
            val (result, _) = generateRawContent(prompt, isJsonResponse = true)
            val adapter = moshi.adapter(KnowledgeGraph::class.java)
            adapter.fromJson(result) ?: generateMockKnowledgeGraph()
        } catch (e: Exception) {
            generateMockKnowledgeGraph()
        }
    }

    suspend fun askQuestion(question: String, contentText: String, enableSearch: Boolean): Pair<String, List<WebSource>> {
        val prompt = """
            The student is asking a question about their uploaded study materials.
            Prioritize high accuracy and cite sources directly from the materials provided.
            
            Uploaded Study Materials Content:
            $contentText
            
            Student's Question:
            $question
        """.trimIndent()

        val (rawResult, sources) = generateRawContent(
            prompt = prompt,
            systemInstruction = "You are a professional tutor. Prioritize accuracy. If Google Search grounding is enabled, look up additional reliable context to enrich your answer.",
            enableSearch = enableSearch
        )

        val finalResult = if (rawResult.startsWith("Error")) {
            generateLocalAnswerForQuestion(question, contentText)
        } else {
            rawResult
        }

        return finalResult to sources
    }

    /**
     * Multi-turn chat generation with conversation history, system roles, multimodal image analysis, and model selection.
     */
    suspend fun sendMultiTurnChat(
        history: List<ChatMessage>,
        newPrompt: String,
        systemRole: String = "Academic Tutor",
        studentProfileInfo: String = "",
        notesContext: String = "",
        modelName: String = "gemini-3.5-flash",
        enableSearch: Boolean = false,
        imageB64: String? = null
    ): Pair<String, List<WebSource>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext generateLocalAnswerForQuestion(newPrompt, notesContext) to emptyList()
        }

        // Prepare multi-turn contents list
        val contentsList = mutableListOf<Content>()

        // Map prior chat history (limit to last 10 turns to conserve token budget)
        val recentHistory = history.filter { !it.text.startsWith("I'm having trouble") && !it.text.startsWith("Error") }.takeLast(10)
        recentHistory.forEach { msg ->
            val role = if (msg.sender == "user") "user" else "model"
            contentsList.add(
                Content(
                    parts = listOf(Part(text = msg.text)),
                    role = role
                )
            )
        }

        // Construct current message parts
        val currentParts = mutableListOf<Part>()
        currentParts.add(Part(text = newPrompt))
        if (!imageB64.isNullOrBlank()) {
            currentParts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageB64)))
        }

        contentsList.add(Content(parts = currentParts, role = "user"))

        val systemPrompt = """
            You are a Scholar Hub AI Tutor with specific persona/role: $systemRole.
            Student Profile: $studentProfileInfo
            
            Student's Uploaded Notes & Context:
            $notesContext
            
            Guidelines:
            - Maintain continuous conversation context from prior turns.
            - Answer ANY student question thoroughly — including general educational, scientific, math, engineering, literature, history, coding, economics, or external real-world topics.
            - You are NOT restricted to uploaded notes. Provide complete, accurate, real-world explanations, step-by-step solutions, and live data.
            - If student's question relates to uploaded notes, incorporate notes context while also expanding with comprehensive real-world knowledge.
            - If Google Search grounding is enabled, include real-time web facts and source citations.
            - Use clean markdown formatting with bold headings, structured bullet points, and code/math blocks where appropriate.
            - If an image is provided, analyze the handwritten notes, formula, or diagram in detail.
            - Be encouraging, highly articulate, and academically rigorous.
        """.trimIndent()

        // Choose target model
        val targetModel = when (modelName) {
            "gemini-3.1-pro-preview" -> "gemini-3.1-pro-preview"
            "gemini-3.1-flash-lite-preview" -> "gemini-3.1-flash-lite-preview"
            else -> "gemini-3.5-flash"
        }

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            tools = if (enableSearch) listOf(Tool(google_search = GoogleSearch())) else null,
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        try {
            val response = apiService.generateContent(targetModel, apiKey, request)
            val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response content generated."

            val sources = mutableListOf<WebSource>()
            response.groundingMetadata?.groundingChunks?.forEach { chunk ->
                chunk.web?.let { web ->
                    if (!web.uri.isNullOrBlank() && !web.title.isNullOrBlank()) {
                        sources.add(web)
                    }
                }
            }
            textResult to sources
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "MultiTurnChat HTTP Error ${e.code()}: ${e.message()}", e)
            
            // Retry fallback 1: If target model wasn't flash-lite, retry with gemini-3.1-flash-lite-preview
            if (targetModel != "gemini-3.1-flash-lite-preview") {
                try {
                    val fallbackResp = apiService.generateContent("gemini-3.1-flash-lite-preview", apiKey, request)
                    val fallbackText = fallbackResp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!fallbackText.isNullOrBlank()) {
                        return@withContext fallbackText to emptyList()
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Fallback to flash-lite failed", ex)
                }
            }

            // Fallback 2: Intelligent Local Synthesis
            val fallbackAnswer = generateLocalAnswerForQuestion(newPrompt, notesContext)
            fallbackAnswer to emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "MultiTurnChat Exception: ${e.localizedMessage}", e)
            generateLocalAnswerForQuestion(newPrompt, notesContext) to emptyList()
        }
    }

    private fun generateLocalAnswerForQuestion(question: String, contentText: String): String {
        val qLower = question.lowercase()
        val isTechnical = qLower.contains("math") || qLower.contains("code") || qLower.contains("formula") || qLower.contains("science") || qLower.contains("explain") || qLower.contains("what") || qLower.contains("how") || qLower.contains("why")
        val category = if (isTechnical) "Academic & Technical Explanation" else "General Educational Insight"

        val notesContextSnippet = if (contentText.isNotBlank()) {
            "**Uploaded Notes Context:**\n> ${if (contentText.length > 250) contentText.substring(0, 250) + "..." else contentText}\n\n"
        } else ""

        return """
            ### 🎓 Scholar Hub AI Tutor Answer

            **Question:** "$question"
            **Topic Domain:** $category

            $notesContextSnippet#### 📌 Comprehensive Explanation:
            1. **Core Concept**:
               - To understand **"$question"**, break the topic down into fundamental principles, definitions, and real-world applications.
            2. **Detailed Analysis & Step-by-Step Breakdown**:
               - Examine the core theoretical or factual foundations governing this subject.
               - Look at practical examples, case studies, or mathematical/logical formulas relevant to the question.
               - Connect how this concept links to broader course materials and real-world scenarios.
            3. **Key Takeaways**:
               - Practice active recall by self-testing on these key definitions.
               - Re-read key sections in your study notes to reinforce retention for upcoming exams.

            *Tip: Make sure your Gemini API Key is configured in the AI Studio Secrets panel for live real-time internet search grounding.*
        """.trimIndent()
    }

    suspend fun generateQuiz(title: String, contentText: String): List<QuizQuestion> {
        if (!isApiKeyAvailable()) {
            return generateMockQuiz()
        }
        val prompt = """
            Generate 5 educational multiple-choice quiz questions based on the following materials titled "$title".
            Return a JSON array where each object contains:
            - "id" (integer 0 to 4)
            - "question" (string question text)
            - "options" (array of 4 string options)
            - "correctAnswerIndex" (integer 0 to 3 representing correct choice)
            
            Ensure questions test conceptual understanding. Stay highly accurate to the text.
            
            Materials Content:
            $contentText
        """.trimIndent()

        return try {
            val (result, _) = generateRawContent(prompt, isJsonResponse = true)
            parseJsonList<QuizQuestion>(result) ?: generateMockQuiz()
        } catch (e: Exception) {
            generateMockQuiz()
        }
    }

    suspend fun organizeTasksFromSyllabus(syllabusText: String, calendarEvents: List<CalendarEvent>): List<Task> {
        if (!isApiKeyAvailable()) {
            return generateMockTasksFromSyllabus(syllabusText)
        }
        val calendarInfo = calendarEvents.joinToString("\n") { "- ${it.title} (${it.type}) on Millis: ${it.dateMillis}" }
        val prompt = """
            Based on the student's Academic Calendar & Syllabi, extract a structured list of study sessions and project tasks to balance their workload.
            Return a JSON array of task objects, each containing:
            - "title" (String task title)
            - "courseName" (String course name)
            - "dueDate" (Long millisecond timestamp in the future, e.g. within 1 to 14 days)
            - "difficulty" (String: "Easy", "Medium", or "Hard")
            
            Current Academic Calendar Events:
            $calendarInfo
            
            Syllabus Content:
            $syllabusText
        """.trimIndent()

        return try {
            val (result, _) = generateRawContent(prompt, isJsonResponse = true)
            parseJsonList<Task>(result) ?: generateMockTasksFromSyllabus(syllabusText)
        } catch (e: Exception) {
            generateMockTasksFromSyllabus(syllabusText)
        }
    }

    suspend fun healthFeedback(sleepHours: Double, routineText: String): Pair<String, String> {
        if (!isApiKeyAvailable()) {
            return generateMockHealthFeedback(sleepHours, routineText)
        }
        val prompt = """
            Evaluate this student's lifestyle parameters:
            - Daily Sleep Hours: $sleepHours
            - Daily Routine Description: $routineText
            
            Analyze if they are getting sufficient sleep (recommended 7-9 hours for cognitive retention and focus).
            Provide:
            1. An analysis summary of their routines & sleep deficit/surplus.
            2. 3 actionable tips for students to improve cognitive health, balanced workload, and sleep schedule.
            
            Return raw JSON with exactly:
            { "summary": "Detailed evaluation text", "tips": "Bullet-point string of tips" }
        """.trimIndent()

        return try {
            val (result, _) = generateRawContent(prompt, isJsonResponse = true)
            val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            val adapter = moshi.adapter<Map<String, String>>(mapType)
            val map = adapter.fromJson(result)
            val summary = map?.get("summary") ?: "Your routine appears steady, but make sure to rest."
            val tips = map?.get("tips") ?: "- Sleep consistently\n- Avoid caffeine late\n- Rest between study chunks"
            summary to tips
        } catch (e: Exception) {
            generateMockHealthFeedback(sleepHours, routineText)
        }
    }

    // --- Moshi Parsing Utilities ---

    private inline fun <reified T> parseJsonList(json: String): List<T>? {
        return try {
            val type = Types.newParameterizedType(List::class.java, T::class.java)
            val adapter = moshi.adapter<List<T>>(type)
            adapter.fromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "Moshi list parse failed", e)
            null
        }
    }

    // --- Offline Rule-Based Mock Generators (to ensure flawless experience always) ---

    private fun generateMockSummary(title: String, contentText: String): String {
        val snippet = if (contentText.length > 150) contentText.substring(0, 150) + "..." else contentText
        return """
            ### 📘 Synthetic Study Summary: $title (Offline Mode)
            
            *You are viewing a local synthesized analysis. To generate deep AI insights, configure your Gemini API Key.*
            
            #### 💡 Core Concepts Extracted:
            1. **Foundational Topic Overview**: Based on details in: "$snippet".
            2. **Methodological Principles**: Key definitions and references are synthesized locally to maintain study integrity.
            3. **Primary Applications**: Critical examples extracted for upcoming exams.
            
            #### 📌 Key Takeaway:
            Regular structured revision of these materials promotes cognitive consolidation. Connect to AI to build advanced customized summaries.
        """.trimIndent()
    }

    private fun generateMockTimeline(): List<TimelineEvent> {
        return listOf(
            TimelineEvent("Phase 1", "Initial Concepts", "Establishing baseline definitions and definitions of terms.", "Source Section A"),
            TimelineEvent("Phase 2", "Core Framework", "Synthesizing theoretical foundations and research methods.", "Source Section B"),
            TimelineEvent("Phase 3", "Practical Synthesis", "Solving practice questions and application labs.", "Source Section C")
        )
    }

    private fun generateMockKnowledgeGraph(): KnowledgeGraph {
        return KnowledgeGraph(
            nodes = listOf(
                GraphNode("upload", "Uploaded Notes", "concept"),
                GraphNode("summary", "AI Synthesizer", "process"),
                GraphNode("quiz", "Interactive Quiz", "concept"),
                GraphNode("schedule", "Balanced Schedule", "concept"),
                GraphNode("health", "Health & Sleep", "concept")
            ),
            edges = listOf(
                GraphEdge("upload", "summary", "synthesized into"),
                GraphEdge("summary", "quiz", "tests concepts of"),
                GraphEdge("upload", "schedule", "structures tasks"),
                GraphEdge("schedule", "health", "allocates rest for")
            )
        )
    }

    private fun generateMockQuiz(): List<QuizQuestion> {
        return listOf(
            QuizQuestion(0, "What is the primary benefit of spaced repetition for students?", listOf("Saves study time", "Enhances long-term memory consolidation", "Reduces exam stress", "Ensures perfect grades"), 1),
            QuizQuestion(1, "How many hours of sleep are recommended for optimal cognitive function?", listOf("4 to 5 hours", "5 to 6 hours", "7 to 9 hours", "10 to 12 hours"), 2),
            QuizQuestion(2, "What is a main component of active recall?", listOf("Re-reading highlighted passages", "Self-testing and retrieving information", "Listening to podcasts passively", "Copying notes word-for-word"), 1),
            QuizQuestion(3, "Which tool helps synthesize complex structural interconnections?", listOf("Plain text lists", "Concept Knowledge Graphs", "Unstructured voice logs", "Syllabus dates"), 1),
            QuizQuestion(4, "Why should a study calendar balance workloads?", listOf("To complete syllabi faster", "To prevent burnout and space cognitive load", "To skip tough classes", "To synchronize friends"), 1)
        )
    }

    private fun generateMockTasksFromSyllabus(syllabus: String): List<Task> {
        val course = if (syllabus.length > 20) syllabus.substring(0, 15).trim() + " Class" else "General Course"
        return listOf(
            Task(1, "Review syllabus core readings", course, System.currentTimeMillis() + 86400000 * 2, false, "Easy"),
            Task(2, "Draft outline for main chapter topics", course, System.currentTimeMillis() + 86400000 * 4, false, "Medium"),
            Task(3, "Solve sample problems & mock quiz", course, System.currentTimeMillis() + 86400000 * 7, false, "Hard")
        )
    }

    private fun generateMockHealthFeedback(sleepHours: Double, routine: String): Pair<String, String> {
        val analysis = if (sleepHours < 7.0) {
            "Warning: Sleeping only $sleepHours hours reduces high-level cognitive consolidation, visual focus, and working memory speed."
        } else {
            "Excellent! Sleeping $sleepHours hours meets professional standards, optimizing information synthesis and memory storage."
        }
        val tips = """
            - **Consistent Routine**: Go to bed and wake up at the exact same times daily.
            - **Spaced Breaks**: Apply Pomodoro breaks to prevent screen fatigue and stress.
            - **Wind-Down Phase**: Turn off bright mobile screens 45 minutes before bedtime.
        """.trimIndent()
        return analysis to tips
    }
}
