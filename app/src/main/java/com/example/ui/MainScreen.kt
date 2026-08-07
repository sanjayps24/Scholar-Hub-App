package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.data.remote.WebSource
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.AlertRed
import com.example.ui.theme.WarningGold
import com.example.ui.theme.SuccessEmerald
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: StudyViewModel) {
    val darkTheme by viewModel.darkThemeEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val notificationAlerts by viewModel.notificationAlerts.collectAsState()
    val firebaseAvailable by viewModel.firebaseAvailable.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()
    val showProfileDialog by viewModel.showProfileDialog.collectAsState()

    var activeTab by remember { mutableStateOf("Dashboard") }
    var showAddMaterialDialog by remember { mutableStateOf(false) }

    // System File Picker Launcher for Document / File Uploads from device folders
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.uploadFileFromUri(it)
            activeTab = "StudyHub"
        }
    }

    MyApplicationTheme(darkTheme = darkTheme) {
        if (studentProfile?.isLoggedIn != true) {
            WelcomeAuthScreen(viewModel = viewModel)
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = "Scholar Hub Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Scholar Hub",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        actions = {
                            // Theme Toggle button
                            IconButton(onClick = { viewModel.toggleTheme() }) {
                                Icon(
                                    imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "Toggle Theme"
                                )
                            }

                            // TOP RIGHT CORNER STUDENT PROFILE BUTTON
                            IconButton(
                                onClick = { viewModel.showProfileDialog.value = true },
                                modifier = Modifier.testTag("top_right_profile_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val firstChar = studentProfile?.fullName?.trim()?.firstOrNull()?.uppercaseChar() ?: 'S'
                                        Text(
                                            firstChar.toString(),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        val tabs = listOf(
                            Triple("Dashboard", Icons.Filled.Dashboard, "Dashboard"),
                            Triple("StudyHub", Icons.Filled.MenuBook, "Study"),
                            Triple("AIChat", Icons.Filled.AutoAwesome, "AI Chat"),
                            Triple("Planner", Icons.Filled.CalendarMonth, "Planner"),
                            Triple("Groups", Icons.Filled.Groups, "Groups"),
                            Triple("Health", Icons.Filled.Favorite, "Health")
                        )
                        tabs.forEach { (tabId, icon, label) ->
                            NavigationBarItem(
                                selected = activeTab == tabId,
                                onClick = { activeTab = tabId },
                                icon = { Icon(icon, contentDescription = label) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                alwaysShowLabel = true,
                                modifier = Modifier.testTag("nav_${tabId.lowercase()}")
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (activeTab == "Dashboard" || activeTab == "StudyHub") {
                        ExtendedFloatingActionButton(
                            onClick = { showAddMaterialDialog = true },
                            icon = { Icon(Icons.Filled.CloudUpload, "Upload source") },
                            text = { Text("Upload Notes") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("upload_fab")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Background Gradient Accent
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    // Overlay Loading Indicator
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable(enabled = false) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Synthesizing with AI...",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Creating timeline, summaries & quiz",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Main Navigation Routing
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                        },
                        label = "TabTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            "Dashboard" -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { activeTab = it },
                                notificationAlerts = notificationAlerts,
                                onOpenFilePicker = { filePickerLauncher.launch(arrayOf("*/*")) }
                            )
                            "StudyHub" -> StudyHubScreen(viewModel = viewModel)
                            "AIChat" -> AIChatbotScreen(viewModel = viewModel)
                            "Planner" -> PlannerScreen(viewModel = viewModel)
                            "Groups" -> GroupsScreen(viewModel = viewModel)
                            "Health" -> HealthScreen(viewModel = viewModel)
                        }
                    }

                    // Student Profile Modal Dialog
                    if (showProfileDialog) {
                        StudentProfileDialog(
                            viewModel = viewModel,
                            onDismiss = { viewModel.showProfileDialog.value = false }
                        )
                    }

                    // Add Material Dialog Overlay
                    if (showAddMaterialDialog) {
                        AddMaterialDialog(
                            onDismiss = { showAddMaterialDialog = false },
                            onOpenFilePicker = {
                                showAddMaterialDialog = false
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            onUpload = { title, type, content, url ->
                                viewModel.uploadStudyMaterial(title, type, content, url)
                                showAddMaterialDialog = false
                                activeTab = "StudyHub" // Redirect to see details
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 1: DASHBOARD SCREEN ---

@Composable
fun DashboardScreen(
    viewModel: StudyViewModel,
    onNavigateToTab: (String) -> Unit,
    notificationAlerts: List<String>,
    onOpenFilePicker: () -> Unit
) {
    val materials by viewModel.materials.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()

    val avatars = listOf("🎓", "🔬", "💻", "📚", "🚀", "⚡")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and user identification card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.showProfileDialog.value = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val avatarIcon = avatars.getOrElse(studentProfile?.avatarIndex ?: 0) { "🎓" }
                            Text(avatarIcon, fontSize = 28.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${studentProfile?.fullName ?: "Student"}!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${studentProfile?.major ?: "Major"} • ${studentProfile?.gradeLevel ?: "Undergrad"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Profile Details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Folder File Upload Hero Action Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Upload Study Files",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Add any files from your device folders (PDF, TXT, DOCX, CSV, Code, Images, Audio)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onOpenFilePicker,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("dashboard_upload_folder_button")
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick File", fontSize = 12.sp)
                    }
                }
            }
        }

        // Exam Deadlines / Workload Balancer Alerts Section
        if (notificationAlerts.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Alerts & Balanced Workloads",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                    notificationAlerts.forEach { alert ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFFBEB),
                                contentColor = Color(0xFF92400E)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(alert, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Quick Statistics Grid
        item {
            Column {
                Text(
                    "Your Learning Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Notes Synced",
                        value = "${materials.size}",
                        icon = Icons.Filled.Description,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Practice Quizzes",
                        value = "${quizzes.filter { it.isTaken }.size}",
                        icon = Icons.Filled.Quiz,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Pending Tasks",
                        value = "${tasks.filter { !it.isCompleted }.size}",
                        icon = Icons.Filled.TaskAlt,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Study Materials Quick Redirect
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Study Materials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigateToTab("StudyHub") }) {
                        Text("View All")
                    }
                }

                if (materials.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.FolderZip,
                                contentDescription = "Folder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No materials uploaded yet",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Upload PDF, Youtube links, or Voice logs below to synthesize summaries.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(materials.take(4)) { material ->
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable {
                                        viewModel.selectMaterial(material)
                                        onNavigateToTab("StudyHub")
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Icon(
                                        imageVector = getSourceIcon(material.sourceType),
                                        contentDescription = material.sourceType,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = material.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = material.sourceType,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Cognitive tip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFECFDF5),
                    contentColor = Color(0xFF065F46)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.TipsAndUpdates,
                        contentDescription = "Tip",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Weekly Cognitive Study Tip", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Highlight insights and practice active recall quizzes 30 minutes after reviewing your materials. This triggers consolidation before sleep!",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// --- TAB 2: STUDY HUB (SYNTHESIS & AI Q&A) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyHubScreen(viewModel: StudyViewModel) {
    val materials by viewModel.materials.collectAsState()
    val selectedMaterial by viewModel.selectedMaterial.collectAsState()

    if (selectedMaterial == null) {
        // Material List Screen
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Study Materials",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            if (materials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.LibraryBooks,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No synthesis files active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Click \"Upload Notes\" to sync your PDFs, voice summaries, YouTube lectures, or photos to generate automatic timelines, concept graphs, and AI quizzes.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(materials) { material ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectMaterial(material) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getSourceIcon(material.sourceType),
                                    contentDescription = material.sourceType,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        material.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${material.sourceType} • Added ${formatDate(material.dateAdded)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteMaterial(material.id) }) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Material Detail Screen with 5 Tabs
        val material = selectedMaterial!!
        var detailTab by remember { mutableStateOf("Summary") }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.selectMaterial(null) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Icon(
                    imageVector = getSourceIcon(material.sourceType),
                    contentDescription = material.sourceType,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        material.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        material.sourceType,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = listOf("Summary", "Timeline", "Graph", "Ask AI", "Practice Quiz").indexOf(detailTab),
                edgePadding = 12.dp
            ) {
                val tabs = listOf("Summary", "Timeline", "Graph", "Ask AI", "Practice Quiz")
                tabs.forEach { tabName ->
                    Tab(
                        selected = detailTab == tabName,
                        onClick = { detailTab = tabName },
                        text = { Text(tabName, fontSize = 12.sp) }
                    )
                }
            }

            // Tab Content Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (detailTab) {
                    "Summary" -> SummaryTab(material)
                    "Timeline" -> TimelineTab(material)
                    "Graph" -> GraphTab(material)
                    "Ask AI" -> AskAiTab(viewModel, material)
                    "Practice Quiz" -> PracticeQuizTab(viewModel, material)
                }
            }
        }
    }
}

@Composable
fun SummaryTab(material: StudyMaterial) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "AI Automated Study Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = material.summaryText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Original Text reference container
        var showOriginal by remember { mutableStateOf(false) }
        TextButton(onClick = { showOriginal = !showOriginal }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (showOriginal) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showOriginal) "Hide Original Notes Text" else "Show Original Notes Text")
            }
        }
        
        if (showOriginal) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    text = material.contentText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun TimelineTab(material: StudyMaterial) {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val events = remember(material) {
        try {
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, TimelineEvent::class.java)
            moshi.adapter<List<TimelineEvent>>(type).fromJson(material.timelineJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No chronological timeline detected in this material.", fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(events) { event ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Timeline indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(80.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                        )
                    }

                    // Timeline card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = event.date,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 12.sp
                                )
                                if (!event.citation.isNullOrBlank()) {
                                    Text(
                                        text = "Source: ${event.citation}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.description,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GraphTab(material: StudyMaterial) {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val graph = remember(material) {
        try {
            moshi.adapter(KnowledgeGraph::class.java).fromJson(material.knowledgeGraphJson) ?: KnowledgeGraph()
        } catch (e: Exception) {
            KnowledgeGraph()
        }
    }

    var selectedNodeLabel by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Automatic Knowledge Graph",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                Text(
                    "Click the concept nodes in the interactive map to study connected definitions.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
            contentAlignment = Alignment.Center
        ) {
            if (graph.nodes.isEmpty()) {
                Text("Analyzing concept graph dependencies...")
            } else {
                // Layout nodes with fixed positions for simple mock coordinates
                val positions = remember(graph) {
                    val posMap = mutableMapOf<String, Offset>()
                    val nodesCount = graph.nodes.size
                    val centerX = 200f
                    val centerY = 200f
                    val radius = 120f

                    graph.nodes.forEachIndexed { idx, node ->
                        if (idx == 0) {
                            posMap[node.id] = Offset(centerX, centerY) // Center
                        } else {
                            val angle = (2 * Math.PI * (idx - 1) / (nodesCount - 1))
                            val x = centerX + radius * Math.cos(angle).toFloat()
                            val y = centerY + radius * Math.sin(angle).toFloat()
                            posMap[node.id] = Offset(x, y)
                        }
                    }
                    posMap
                }

                // Interactive Custom Concept Graph Renderer
                Canvas(
                    modifier = Modifier
                        .size(400.dp)
                        .clickable {
                            // Clear highlighted node
                            selectedNodeLabel = null
                        }
                ) {
                    // 1. Draw connecting relationships (Edges)
                    graph.edges.forEach { edge ->
                        val start = positions[edge.from] ?: Offset.Zero
                        val end = positions[edge.to] ?: Offset.Zero
                        if (start != Offset.Zero && end != Offset.Zero) {
                            drawLine(
                                color = Color(0xFF6366F1).copy(alpha = 0.4f),
                                start = start,
                                end = end,
                                strokeWidth = 2f
                            )
                        }
                    }

                    // 2. Draw concept nodes (Nodes)
                    graph.nodes.forEach { node ->
                        val offset = positions[node.id] ?: Offset.Zero
                        if (offset != Offset.Zero) {
                            drawCircle(
                                color = when (node.type) {
                                    "process" -> Color(0xFF06B6D4)
                                    "entity" -> Color(0xFF10B981)
                                    else -> Color(0xFF6366F1)
                                },
                                radius = 22f,
                                center = offset
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 24f,
                                center = offset,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                // Place Interactive clickable card overlay for nodes
                graph.nodes.forEach { node ->
                    val pos = positions[node.id] ?: Offset.Zero
                    if (pos != Offset.Zero) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (pos.x / 1.5f).toInt().dp,
                                    y = (pos.y / 1.5f).toInt().dp
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { selectedNodeLabel = "${node.label} (${node.type.uppercase()})" }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = node.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Highlight Info Banner
        AnimatedVisibility(
            visible = selectedNodeLabel != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = selectedNodeLabel ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "A core structural pillar synthesized directly from your study syllabus details.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AskAiTab(viewModel: StudyViewModel, material: StudyMaterial) {
    val query by viewModel.qaQuery.collectAsState()
    val answer by viewModel.qaAnswer.collectAsState()
    val sources by viewModel.qaSources.collectAsState()
    val isQaLoading by viewModel.isQaLoading.collectAsState()
    val searchGrounding by viewModel.enableSearchGrounding.collectAsState()

    var questionInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Grounding toggle & explanation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Google Search Grounding", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Queries the web in real-time to augment your notes.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = searchGrounding,
                onCheckedChange = { viewModel.enableSearchGrounding.value = it }
            )
        }

        // AI Conversation display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (query.isEmpty() && answer.isEmpty()) {
                    // Chat greeting
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Ask anything about your notes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "E.g., \"Synthesize the mathematical formulas\" or \"Cite where sleep guidelines are mentioned.\"",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    // Question bubble
                    Text(
                        "Student Question:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = query,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Answer Bubble
                    Text(
                        "AI Grounded Response:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )

                    if (isQaLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Retrieving citations & drafting accurate response...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(
                            text = answer,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Cite grounding sources if returned
                        if (sources.isNotEmpty()) {
                            Text(
                                "Google Grounding Sources Cited:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            sources.forEach { source ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Link, contentDescription = "Source URL", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(source.title ?: "Reference link", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(source.uri ?: "", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = questionInput,
                onValueChange = { questionInput = it },
                placeholder = { Text("Ask about these notes...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("qa_input"),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (questionInput.isNotBlank()) {
                                viewModel.submitQuestion(questionInput)
                                questionInput = ""
                            }
                        },
                        enabled = !isQaLoading && questionInput.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Submit")
                    }
                }
            )
        }
    }
}

@Composable
fun PracticeQuizTab(viewModel: StudyViewModel, material: StudyMaterial) {
    val activeQuiz by viewModel.activeQuiz.collectAsState()
    val questions by viewModel.quizQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val score by viewModel.quizScore.collectAsState()
    val isFinished by viewModel.isQuizFinished.collectAsState()

    if (activeQuiz == null) {
        // Start Quiz Screen
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Filled.Quiz, contentDescription = "Quiz Logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Synthesize Material via AI Quiz", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Generates 5 multiple choice questions specifically tailored to test your conceptual accuracy and long-term consolidation.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.startQuizForMaterial(material) }) {
                    Text("Start AI Practice Quiz")
                }
            }
        }
    } else {
        if (isFinished) {
            // Quiz Results Dashboard
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "Trophy", tint = Color(0xFFF59E0B), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Practice Completed!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Score: $score / ${questions.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val feedback = when {
                            score == questions.size -> "Perfect score! You've masterfully synthesized these study materials!"
                            score >= 3 -> "Great job! A solid grasp of core concepts. Read summaries again to get 100%!"
                            else -> "Keep studying! Review your Concept Knowledge Graph to resolve conceptual gaps."
                        }
                        Text(
                            text = feedback,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.startQuizForMaterial(material) }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        } else {
            // Active Quiz Question screen
            val question = questions.getOrNull(currentIndex)
            if (question != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Progress Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Question ${currentIndex + 1} of ${questions.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / questions.size },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Question Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = question.question,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Options List
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        question.options.forEachIndexed { optIdx, option ->
                            val isSelected = question.selectedAnswerIndex == optIdx
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectQuizAnswer(currentIndex, optIdx) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.selectQuizAnswer(currentIndex, optIdx) }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(option, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Next/Finish Action Button
                    Button(
                        onClick = { viewModel.nextQuizQuestion() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = question.selectedAnswerIndex != -1
                    ) {
                        Text(if (currentIndex == questions.size - 1) "Finish Quiz" else "Next Question")
                    }
                }
            }
        }
    }
}

// --- TAB 3: CALENDAR, TASK ORGANIZER & SYLLABUS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(viewModel: StudyViewModel) {
    val events by viewModel.calendarEvents.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current

    var plannerTab by remember { mutableStateOf("Schedules") }
    var showSyllabusDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (plannerTab == "Schedules") 0 else 1) {
            Tab(selected = plannerTab == "Schedules", onClick = { plannerTab = "Schedules" }, text = { Text("Schedules & Syllabus") })
            Tab(selected = plannerTab == "Tasks", onClick = { plannerTab = "Tasks" }, text = { Text("AI Workload Tasks") })
        }

        if (plannerTab == "Schedules") {
            // Calendar deadlines and Syllabus auto-generator
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ImportContacts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Course Syllabi Importer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Upload or paste your course syllabus. The AI organizes balanced work tasks and schedules automatic revision deadlines.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { showSyllabusDialog = true }) {
                                Text("Import", fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Academic Calendars", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showAddEventDialog = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Event")
                            }
                        }
                    }
                }

                if (events.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text("No exams, deadlines or classes added yet. Click Add Event to set schedules and compute dynamic workload advice.", fontSize = 12.sp, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(events) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (event.type) {
                                                        "Exam" -> AlertRed
                                                        "Deadline" -> WarningGold
                                                        else -> MaterialTheme.colorScheme.primary
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(event.type, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.deleteCalendarEvent(event.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(event.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Date: ${formatDate(event.dateMillis)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                
                                // Dynamic Balanced workload Note
                                if (event.balancedWorkloadNote.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                            .padding(10.dp)
                                    ) {
                                        Row {
                                            Icon(Icons.Filled.Analytics, contentDescription = "Advice", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = event.balancedWorkloadNote,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Task list with Difficulty badges
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Organized Study Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (tasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text("No tasks populated yet. Paste a Course Syllabus or generate a summary in the Study Hub to organize study session items.", fontSize = 12.sp, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    items(tasks) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { viewModel.toggleTaskCompleted(task) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        style = if (task.isCompleted) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant) else MaterialTheme.typography.bodyMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(task.courseName, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        // Difficulty badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (task.difficulty) {
                                                        "Easy" -> Color(0xFFD1FAE5)
                                                        "Hard" -> Color(0xFFFEE2E2)
                                                        else -> Color(0xFFFEF3C7)
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = task.difficulty,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (task.difficulty) {
                                                    "Easy" -> Color(0xFF065F46)
                                                    "Hard" -> Color(0xFF991B1B)
                                                    else -> Color(0xFF92400E)
                                                }
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AlertRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Syllabi Import Dialog Overlay
    if (showSyllabusDialog) {
        var textInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showSyllabusDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paste Syllabus Text", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Course code, exam schedule, textbook chapters...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSyllabusDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.importSyllabus(textInput)
                                showSyllabusDialog = false
                            },
                            enabled = textInput.isNotBlank()
                        ) {
                            Text("Organize Tasks")
                        }
                    }
                }
            }
        }
    }

    // Add Event Dialog Overlay
    if (showAddEventDialog) {
        var eventTitle by remember { mutableStateOf("") }
        var eventDesc by remember { mutableStateOf("") }
        var eventType by remember { mutableStateOf("Exam") }
        val calendar = Calendar.getInstance()
        var dateMillis by remember { mutableStateOf(calendar.timeInMillis) }

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                dateMillis = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        Dialog(onDismissRequest = { showAddEventDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Academic Event", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = eventDesc,
                        onValueChange = { eventDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Event type selection dropdown / chips
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Exam", "Deadline", "Class").forEach { type ->
                            FilterChip(
                                selected = eventType == type,
                                onClick = { eventType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Select Date: ${formatDate(dateMillis)}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddEventDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.addCalendarEvent(eventTitle, dateMillis, eventDesc, eventType)
                                showAddEventDialog = false
                            },
                            enabled = eventTitle.isNotBlank()
                        ) {
                            Text("Add Event")
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 4: COLLABORATIVE STUDY GROUPS & REAL-TIME DOCUMENT SHARING ---

@Composable
fun GroupsScreen(viewModel: StudyViewModel) {
    val groups by viewModel.studyGroups.collectAsState()
    val materials by viewModel.materials.collectAsState()
    val studentProfile by viewModel.studentProfile.collectAsState()
    var selectedGroup by remember { mutableStateOf<StudyGroup?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (selectedGroup == null) {
        // Study Groups list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Study Groups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showCreateDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Group")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Filled.PeopleOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No active study groups", fontWeight = FontWeight.Bold)
                        Text("Join collaborative study networks or compile shared quizzes to learn with peers.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(groups) { group ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGroup = group },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Invite Code: ${group.code} • ${group.mockActiveMembers} Active Students", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Open")
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Study Group Workspace screen (collaboration and shared document highlights)
        val group = selectedGroup!!
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedGroup = null }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Collaborative Sync Room • Code: ${group.code}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { 
                    viewModel.leaveStudyGroup(group.id)
                    selectedGroup = null 
                }) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = "Leave Group", tint = AlertRed)
                }
            }

            // Real-Time Shared Database Highlight panel simulation
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Real-Time Document Sharing & Highlights", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Students in this room share a synchronized database of study materials, interactive flashcards, and live highlights below.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Text("Group Active Sync Stream", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (materials.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.GroupWork, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Shared Notes Yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Upload notes in the Study tab to share concept highlights with members of this study group.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(materials) { mat ->
                        HighlightFeedItem(
                            studentName = studentProfile?.fullName ?: "Group Member",
                            materialName = mat.title,
                            textHighlighted = if (mat.summaryText.length > 120) mat.summaryText.take(120) + "..." else mat.summaryText,
                            noteText = "Key summary concept from ${mat.title}"
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var groupName by remember { mutableStateOf("") }
        var inviteCode by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Collaborate with Students", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Create New Group (Group Name)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.createStudyGroup(groupName)
                            showCreateDialog = false
                        },
                        enabled = groupName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Room")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = { Text("Join existing (Invite Code)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.joinStudyGroup(inviteCode)
                            showCreateDialog = false
                        },
                        enabled = inviteCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Join Room")
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightFeedItem(studentName: String, materialName: String, textHighlighted: String, noteText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Text(studentName.take(1), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(studentName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Shared in: $materialName", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFEF08A)) // Highlight yellow
                    .padding(8.dp)
            ) {
                Text(
                    text = "\"$textHighlighted\"",
                    fontSize = 11.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                Icon(Icons.Filled.Comment, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = noteText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- TAB 5: HEALTH ANALYZER & SLEEP CALCULATOR ---

@Composable
fun HealthScreen(viewModel: StudyViewModel) {
    val healthLogs by viewModel.healthLogs.collectAsState()
    val todayLog by viewModel.todayHealthLog.collectAsState()

    var studyHoursInput by remember { mutableStateOf(6f) }
    var actualSleepInput by remember { mutableStateOf(7.5f) }
    var intensityLevel by remember { mutableStateOf("Heavy") } // "Light", "Balanced", "Heavy", "Exam Crunch"
    var coffeeCups by remember { mutableStateOf(2) }
    var routineNotes by remember { mutableStateOf("") }

    // Algorithm: Recommended Sleep = Base (7.0 hrs) + (Study Hours * 0.25) + (Intensity Bonus * 0.25)
    val intensityBonus = when (intensityLevel) {
        "Light" -> 0.0f
        "Balanced" -> 0.25f
        "Heavy" -> 0.5f
        "Exam Crunch" -> 0.8f
        else -> 0.25f
    }
    val recommendedSleepCalculated = remember(studyHoursInput, intensityLevel) {
        val calculated = 7.0f + (studyHoursInput * 0.20f) + intensityBonus
        (kotlin.math.round(calculated * 10) / 10f).coerceAtMost(9.5f)
    }

    val sleepDeficit = recommendedSleepCalculated - actualSleepInput
    val readinessScore = remember(actualSleepInput, recommendedSleepCalculated) {
        val ratio = (actualSleepInput / recommendedSleepCalculated).coerceAtMost(1.0f)
        (ratio * 100).toInt()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Health & Cognitive Sleep Analyzer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Calculate optimal sleep based on study load and fatigue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }

        // 1. Study Load & Sleep Calculator Input Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Input Today's Study Load & Routine", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Study Load Hours Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Study Load:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${studyHoursInput.toInt()} Hours / Day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = studyHoursInput,
                        onValueChange = { studyHoursInput = it },
                        valueRange = 1f..14f,
                        steps = 12,
                        modifier = Modifier.testTag("health_study_hours_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Study Intensity Selector
                    Text("Study Intensity & Mental Strain:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Light", "Balanced", "Heavy", "Exam Crunch").forEach { level ->
                            FilterChip(
                                selected = intensityLevel == level,
                                onClick = { intensityLevel = level },
                                label = { Text(level, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Coffee Cups & Actual Sleep
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Caffeine / Coffee Cups:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (coffeeCups > 0) coffeeCups-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }
                                Text("$coffeeCups Cups ☕", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { coffeeCups++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Actual Sleep Logged:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${actualSleepInput} Hours", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                            Slider(
                                value = actualSleepInput,
                                onValueChange = { actualSleepInput = (kotlin.math.round(it * 2) / 2f) },
                                valueRange = 3f..12f,
                                steps = 17
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = routineNotes,
                        onValueChange = { routineNotes = it },
                        label = { Text("Notes on Fatigue or Caffeine Time (Optional)") },
                        placeholder = { Text("e.g. late night exam prep, drank coffee at 9 PM") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val routineSummary = "Studied ${studyHoursInput.toInt()}h ($intensityLevel intensity), $coffeeCups cups coffee. $routineNotes"
                            viewModel.logHealthMetrics(actualSleepInput.toDouble(), routineSummary)
                            routineNotes = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("health_log_metrics_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculate & Analyze Health Metrics")
                    }
                }
            }
        }

        // 2. Dynamic Calculated Sleep Recommendation Gauge Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (readinessScore >= 80) Color(0xFFECFDF5) else Color(0xFFFFF1F2)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (readinessScore >= 80) SuccessEmerald.copy(alpha = 0.5f) else AlertRed.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (readinessScore >= 80) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (readinessScore >= 80) SuccessEmerald else AlertRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Cognitive Readiness Score: $readinessScore%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (readinessScore >= 80) Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (readinessScore >= 80) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = if (sleepDeficit <= 0) "Optimal Recovery" else "-${String.format("%.1f", sleepDeficit)}h Deficit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (readinessScore >= 80) Color(0xFF047857) else Color(0xFFBE123C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Recommended Sleep", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${recommendedSleepCalculated} hrs", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Divider(modifier = Modifier.height(36.dp).width(1.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Actual Sleep Logged", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${actualSleepInput} hrs", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (actualSleepInput / recommendedSleepCalculated).coerceAtMost(1.0f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = if (readinessScore >= 80) SuccessEmerald else WarningGold
                    )
                }
            }
        }

        // 3. Daily Wellness Tips using Material3 Cards
        item {
            Text("Daily Wellness Tips & Cognitive Hygiene", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            WellnessTipCard(
                title = "🧠 Memory Consolidation & REM Stage",
                category = "REM SLEEP",
                tipText = "Your ${studyHoursInput.toInt()}-hour study load requires at least ${recommendedSleepCalculated} hours of sleep tonight. Stage 3/4 deep sleep consolidates declarative facts and synaptic pathways formed today into long-term memory.",
                icon = Icons.Filled.Psychology,
                containerColor = Color(0xFFEEF2FF),
                accentColor = Color(0xFF4F46E5)
            )
        }

        item {
            WellnessTipCard(
                title = "💧 Hydration & Neuro-Glymphatic Flush",
                category = "BRAIN HYDRATION",
                tipText = "Drink 500ml water immediately upon waking. Maintaining 2.5L daily hydration helps the brain's glymphatic system flush metabolic debris and beta-amyloid proteins during deep sleep cycles.",
                icon = Icons.Filled.WaterDrop,
                containerColor = Color(0xFFE0F2FE),
                accentColor = Color(0xFF0284C7)
            )
        }

        item {
            WellnessTipCard(
                title = "👁️ 20-20-20 Visual Fatigue Protocol",
                category = "SCREEN HYGIENE",
                tipText = "For every 20 minutes of reading screen time or textbook study, pause and look at an object at least 20 feet away for 20 seconds. Enable blue light filter mode after 7:00 PM to protect melatonin levels.",
                icon = Icons.Filled.Visibility,
                containerColor = Color(0xFFFEF3C7),
                accentColor = Color(0xFFD97706)
            )
        }

        item {
            WellnessTipCard(
                title = "☕ Caffeine & Adenosine Window",
                category = "CAFFEINE CUTOFF",
                tipText = if (coffeeCups > 2) "You logged $coffeeCups cups of coffee. Stop all caffeine consumption 8 hours before bed (by 2:00 PM). Caffeine blocks adenosine receptors and reduces restorative deep sleep duration." else "Moderate caffeine intake logged ($coffeeCups cups). Maintain a strict caffeine cutoff by 2:00 PM for uninterrupted slow-wave sleep.",
                icon = Icons.Filled.LocalCafe,
                containerColor = Color(0xFFFFEDD5),
                accentColor = Color(0xFFEA580C)
            )
        }

        item {
            WellnessTipCard(
                title = "🧘 4-7-8 Breathing Wind-Down Exercise",
                category = "STRESS REGULATION",
                tipText = "Before bed, practice the 4-7-8 relaxation technique: Inhale quietly through nose for 4 seconds, hold breath for 7 seconds, exhale completely through mouth for 8 seconds. Repeat 4 cycles to activate the parasympathetic nervous system.",
                icon = Icons.Filled.SelfImprovement,
                containerColor = Color(0xFFF3E8FF),
                accentColor = Color(0xFF9333EA)
            )
        }

        // Display current Gemini AI feedback if available
        if (todayLog != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini AI Personalized Health Diagnosis", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = todayLog!!.healthTips,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Text("Historical Health & Sleep Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (healthLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("No historic fatigue data. Log sleep metrics daily to track cognitive retainability trends.", fontSize = 12.sp, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center)
                }
            }
        } else {
            items(healthLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.dateString, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(log.dailyRoutine.ifBlank { "Generic study session" }, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (log.sleepHours >= 7.0) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                            )
                        ) {
                            Text(
                                text = "${log.sleepHours} hrs sleep",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = if (log.sleepHours >= 7.0) Color(0xFF065F46) else Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WellnessTipCard(
    title: String,
    category: String,
    tipText: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(6.dp))
                Text(tipText, fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFF1F2937))
            }
        }
    }
}

// --- HELPER COMPONENT DIALOGS ---

@Composable
fun AddMaterialDialog(
    onDismiss: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onUpload: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("PDF") }
    var content by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Upload Study Material",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Direct File Folder Selector Action
                OutlinedButton(
                    onClick = onOpenFilePicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📁 Pick File from Folder (PDF, DOCX, TXT...)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Or Paste / Synthesize Manually", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g. Calculus Chapter 1)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Source Format Type", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val formats = listOf("PDF", "YouTube", "Image", "Voice", "Text")
                    formats.forEach { format ->
                        FilterChip(
                            selected = sourceType == format,
                            onClick = { sourceType = format },
                            label = { Text(format, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (sourceType == "YouTube") {
                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        label = { Text("YouTube Lecture URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = {
                        Text(
                            when (sourceType) {
                                "PDF" -> "Paste PDF content text here..."
                                "YouTube" -> "Paste video transcripts or subtitles here..."
                                "Image" -> "Paste image text or visual details here..."
                                "Voice" -> "Paste voice transcript notes here..."
                                else -> "Paste study notes text here..."
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 10
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onUpload(title, sourceType, content, sourceUrl) },
                        enabled = title.isNotBlank() && content.isNotBlank()
                    ) {
                        Text("Synthesize Material")
                    }
                }
            }
        }
    }
}

// --- GENERAL HELPERS ---

fun getSourceIcon(type: String): ImageVector {
    return when (type) {
        "PDF" -> Icons.Filled.PictureAsPdf
        "YouTube" -> Icons.Filled.PlayCircle
        "Image" -> Icons.Filled.Image
        "Voice" -> Icons.Filled.Mic
        else -> Icons.Filled.Article
    }
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
