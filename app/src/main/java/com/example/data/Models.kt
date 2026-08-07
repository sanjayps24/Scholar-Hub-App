package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_materials")
data class StudyMaterial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceType: String, // "PDF", "YouTube", "Image", "Voice", "Text"
    val sourceUrl: String = "",
    val contentText: String,
    val summaryText: String = "",
    val timelineJson: String = "[]", // Timeline event list
    val knowledgeGraphJson: String = "{}", // Nodes and relationships
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dateMillis: Long,
    val description: String = "",
    val type: String, // "Exam", "Study Session", "Class", "Deadline"
    val balancedWorkloadNote: String = ""
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val courseName: String,
    val dueDate: Long,
    val isCompleted: Boolean = false,
    val difficulty: String = "Medium" // "Easy", "Medium", "Hard"
)

@Entity(tableName = "health_logs")
data class HealthLog(
    @PrimaryKey val dateString: String, // YYYY-MM-DD
    val sleepHours: Double,
    val dailyRoutine: String = "",
    val healthTips: String = ""
)

@Entity(tableName = "quizzes")
data class Quiz(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long = 0,
    val title: String,
    val questionsJson: String, // List of multiple choice questions
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val isTaken: Boolean = false
)

@Entity(tableName = "study_groups")
data class StudyGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val sharedMaterialsJson: String = "[]",
    val sharedQuizzesJson: String = "[]",
    val mockActiveMembers: Int = 3
)

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: Long = 1,
    val fullName: String,
    val email: String,
    val passwordHash: String = "",
    val major: String = "Computer Science",
    val gradeLevel: String = "Undergraduate",
    val targetGpa: String = "3.8",
    val avatarIndex: Int = 0,
    val isLoggedIn: Boolean = true,
    val joinDateMillis: Long = System.currentTimeMillis(),
    val dateOfBirth: String = ""
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sourcesJson: String = "[]"
)

// Auxiliary Data Structures (for serializing JSON)
data class TimelineEvent(
    val date: String,
    val title: String,
    val description: String,
    val citation: String = ""
)

data class GraphNode(
    val id: String,
    val label: String,
    val type: String = "concept" // "concept", "entity", "process"
)

data class GraphEdge(
    val from: String,
    val to: String,
    val label: String
)

data class KnowledgeGraph(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList()
)

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    var selectedAnswerIndex: Int = -1
)
