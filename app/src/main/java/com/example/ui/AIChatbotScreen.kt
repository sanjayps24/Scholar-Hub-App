package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.remote.WebSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatbotScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val enableSearch by viewModel.enableSearchGrounding.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()
    val selectedModel by viewModel.selectedChatModel.collectAsState()
    val selectedRole by viewModel.selectedSystemRole.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageB64 by remember { mutableStateOf<String?>(null) }
    var showSettingsRow by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val moshi = remember { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }

    // Text To Speech state
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var speakingMessageId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Configured
            }
        }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    selectedImageBitmap = bitmap
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val bytes = outputStream.toByteArray()
                    selectedImageB64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Voice Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputPrompt = spokenText
            }
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "💡 Explain my uploaded study notes",
        "📅 Create a study schedule for my exams",
        "❓ Quiz me with 3 practice questions",
        "📷 Analyze my uploaded textbook image"
    )

    val modelOptions = listOf(
        "gemini-3.1-flash-lite-preview" to "⚡ Fast Lite",
        "gemini-3.5-flash" to "🧠 General Flash",
        "gemini-3.1-pro-preview" to "🔬 Pro Reasoning"
    )

    val roleOptions = listOf(
        "🎓 Academic Tutor",
        "📝 Exam Cram Coach",
        "🔬 STEM & Code Expert",
        "💡 Creative Mind-Mapper"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Chatbot Header & Model Config Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Tutor", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Multi-Turn Gemini Chatbot",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "$selectedRole • ${modelOptions.firstOrNull { it.first == selectedModel }?.second ?: selectedModel}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSettingsRow = !showSettingsRow }) {
                            Icon(if (showSettingsRow) Icons.Filled.Tune else Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Expandable Settings Row
                AnimatedVisibility(visible = showSettingsRow) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Select AI Model:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(modelOptions) { (modelKey, label) ->
                                FilterChip(
                                    selected = (selectedModel == modelKey),
                                    onClick = { viewModel.selectedChatModel.value = modelKey },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Select Tutor Role / System Instruction:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(roleOptions) { role ->
                                FilterChip(
                                    selected = (selectedRole == role),
                                    onClick = { viewModel.selectedSystemRole.value = role },
                                    label = { Text(role, fontSize = 11.sp) }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Google Search Web Grounding", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = enableSearch,
                                onCheckedChange = { viewModel.enableSearchGrounding.value = it }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Conversation Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Welcome to Gemini Intelligent Study Chatbot!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Ask questions about your uploaded materials, upload images of notes/diagrams for multimodal analysis, or dictate prompts via voice.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            items(chatMessages) { message ->
                ChatMessageBubble(
                    message = message,
                    studentName = studentProfile?.fullName ?: "You",
                    moshi = moshi,
                    onSpeakText = { text ->
                        if (ttsEngine != null) {
                            if (speakingMessageId == message.id) {
                                ttsEngine?.stop()
                                speakingMessageId = null
                            } else {
                                ttsEngine?.stop()
                                ttsEngine?.speak(text.replace("#", "").replace("*", ""), TextToSpeech.QUEUE_FLUSH, null, "msg_${message.id}")
                                speakingMessageId = message.id
                            }
                        }
                    },
                    isSpeaking = (speakingMessageId == message.id)
                )
            }

            if (isChatLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini is processing response with multi-turn history...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Prompt Action Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            items(quickPrompts) { prompt ->
                SuggestionChip(
                    onClick = {
                        if (prompt.contains("Analyze my uploaded textbook image")) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            viewModel.sendLiveChatMessage(prompt, selectedImageB64)
                            selectedImageUri = null
                            selectedImageBitmap = null
                            selectedImageB64 = null
                        }
                    },
                    label = { Text(prompt, fontSize = 11.sp) }
                )
            }
        }

        // Image Preview Box if attached
        if (selectedImageBitmap != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            bitmap = selectedImageBitmap!!.asImageBitmap(),
                            contentDescription = "Attached image",
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Image Attached for Gemini Multimodal Analysis", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Ready to send with your prompt", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = {
                        selectedImageUri = null
                        selectedImageBitmap = null
                        selectedImageB64 = null
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove attached image")
                    }
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo Picker Button
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.testTag("chat_image_picker_button")
            ) {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Attach photo",
                    tint = if (selectedImageB64 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mic Speech Button
            IconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Gemini Study Tutor...")
                    }
                    try {
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.testTag("chat_voice_mic_button")
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
            }

            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Ask Gemini Tutor or attach photo...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(6.dp))

            FloatingActionButton(
                onClick = {
                    if (inputPrompt.isNotBlank() || selectedImageB64 != null) {
                        viewModel.sendLiveChatMessage(inputPrompt, selectedImageB64)
                        inputPrompt = ""
                        selectedImageUri = null
                        selectedImageBitmap = null
                        selectedImageB64 = null
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("ai_chat_send_button")
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send Message")
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    studentName: String,
    moshi: Moshi,
    onSpeakText: (String) -> Unit,
    isSpeaking: Boolean
) {
    val isUser = message.sender == "user"

    val sources: List<WebSource> = remember(message.sourcesJson) {
        try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, WebSource::class.java)
            moshi.adapter<List<WebSource>>(type).fromJson(message.sourcesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) studentName else "Gemini AI Study Tutor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                    )

                    if (!isUser) {
                        IconButton(
                            onClick = { onSpeakText(message.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Filled.VolumeMute,
                                contentDescription = "Listen to AI voice",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )

                if (sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Sources:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    sources.take(3).forEach { source ->
                        Text("• ${source.title}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val timeStr = remember(message.timestamp) {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
                }
                Text(
                    text = timeStr,
                    fontSize = 9.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
