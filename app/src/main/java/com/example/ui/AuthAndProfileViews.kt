package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.StudentProfile
import java.text.SimpleDateFormat
import java.util.*

enum class AuthPage {
    LANDING, LOGIN, SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeAuthScreen(viewModel: StudyViewModel) {
    var currentPage by remember { mutableStateOf(AuthPage.LANDING) }

    AnimatedContent(
        targetState = currentPage,
        label = "AuthNavigationAnimation"
    ) { page ->
        when (page) {
            AuthPage.LANDING -> {
                LandingHomeScreen(
                    onNavigateToLogin = { currentPage = AuthPage.LOGIN },
                    onNavigateToSignUp = { currentPage = AuthPage.SIGNUP }
                )
            }
            AuthPage.LOGIN -> {
                LoginScreen(
                    viewModel = viewModel,
                    onBackToHome = { currentPage = AuthPage.LANDING },
                    onNavigateToSignUp = { currentPage = AuthPage.SIGNUP }
                )
            }
            AuthPage.SIGNUP -> {
                SignUpScreen(
                    viewModel = viewModel,
                    onBackToHome = { currentPage = AuthPage.LANDING },
                    onNavigateToLogin = { currentPage = AuthPage.LOGIN }
                )
            }
        }
    }
}

// --- PAGE 1: LANDING HOME ENTRY SCREEN ---

@Composable
fun LandingHomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HERO VIEWPORT: Centered Logo & App Name filling the screen initially
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight - 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Centered App Logo
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = "Scholar Hub Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // App Title
                    Text(
                        text = "Scholar Hub",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI Academic Suite",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Subtle Scroll Cue
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Scroll down to explore features",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // EXPLORE FEATURES SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Explore Intelligent Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                FeatureCard(
                    icon = Icons.Filled.MenuBook,
                    iconBg = Color(0xFFEEF2FF),
                    iconTint = Color(0xFF4F46E5),
                    title = "AI Note Summarizer & PDF Insights",
                    description = "Upload lecture slides or PDFs to automatically extract summaries, key takeaways, timelines, and concept graphs."
                )

                FeatureCard(
                    icon = Icons.Filled.Quiz,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = Color(0xFF059669),
                    title = "Interactive AI Quiz & Flashcard Generator",
                    description = "Master your coursework with custom AI-generated quizzes, instant scoring, step-by-step explanations, and flashcards."
                )

                FeatureCard(
                    icon = Icons.Filled.CalendarMonth,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    title = "Smart Syllabus Planner & Exam Reminders",
                    description = "Organize course tasks, track assignment deadlines with live countdown timers, and manage your weekly study schedule."
                )

                FeatureCard(
                    icon = Icons.Filled.Bedtime,
                    iconBg = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF9333EA),
                    title = "Cognitive Sleep & Fatigue Calculator",
                    description = "Calculate optimal nightly sleep based on daily study load, mental strain, and caffeine intake for peak retention."
                )

                FeatureCard(
                    icon = Icons.Filled.AutoAwesome,
                    iconBg = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0284C7),
                    title = "24/7 Gemini AI Academic Tutor",
                    description = "Ask questions, practice problem solving, and get tailored explanations aligned with your specific major and course goals."
                )

                FeatureCard(
                    icon = Icons.Filled.Groups,
                    iconBg = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFEA580C),
                    title = "Peer Study Groups & Shared Notes",
                    description = "Collaborate with university classmates, share study notes, and stay synchronized with live group update streams."
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // LOG IN & SIGN UP BUTTONS AT BOTTOM (Scrolls along with page, not fixed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    text = "Get started with Scholar Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Log In Button
                    Button(
                        onClick = onNavigateToLogin,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("landing_login_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    // Sign Up Button
                    OutlinedButton(
                        onClick = onNavigateToSignUp,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("landing_signup_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Up", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- PAGE 2: LOG IN SCREEN ---

@Composable
fun LoginScreen(
    viewModel: StudyViewModel,
    onBackToHome: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val authError by viewModel.authError.collectAsState()

    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToHome) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Home")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Home", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Branding Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Log In to Scholar Hub", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Access your AI notes, quizzes, schedules & tutor", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                if (authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(authError ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text("Student Email *") },
                            placeholder = { Text("e.g. student@university.edu") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("login_email_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password *") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("login_password_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.login(email = loginEmail, password = loginPassword)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_login_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Log In to Scholar Hub", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Filled.Login, contentDescription = null)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.login(email = "4mh23cs133@gmail.com", password = "password123")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Demo Login", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onNavigateToSignUp) {
                        Text("Sign Up Here", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// --- PAGE 3: SIGN UP SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: StudyViewModel,
    onBackToHome: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authError by viewModel.authError.collectAsState()
    val context = LocalContext.current

    // Form states
    var signUpName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Predefined Majors Dropdown state
    val predefinedMajors = listOf(
        "Computer Science & Engineering",
        "Information Technology & Software",
        "Electrical & Electronics Engineering",
        "Mechanical & Mechatronics Engineering",
        "Civil & Structural Engineering",
        "Biotechnology & Biomedical Science",
        "Business Administration & Finance",
        "Medicine, Nursing & Pre-Med",
        "Law & Legal Studies",
        "Data Science & Artificial Intelligence",
        "Arts, Design & Humanities",
        "Other Academic Stream"
    )
    var signUpMajor by remember { mutableStateOf(predefinedMajors[0]) }
    var expandedMajor by remember { mutableStateOf(false) }

    // Predefined Grade / Year Dropdown state
    val predefinedGrades = listOf(
        "1st Year / Freshman",
        "2nd Year / Sophomore",
        "3rd Year / Junior",
        "4th Year / Senior",
        "Postgraduate / Master's",
        "Ph.D. / Doctorate"
    )
    var signUpGrade by remember { mutableStateOf(predefinedGrades[0]) }
    var expandedGrade by remember { mutableStateOf(false) }

    // DOB state with DatePickerDialog
    var signUpDob by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                signUpDob = sdf.format(cal.time)
            },
            calendar.get(Calendar.YEAR) - 20, // default ~20 years old
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    var signUpGpa by remember { mutableStateOf("3.8") }
    var selectedAvatarIndex by remember { mutableStateOf(0) }
    val avatars = listOf("🎓", "🔬", "💻", "📚", "🚀", "⚡")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 540.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToHome) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back to Home")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Home", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logo Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Create Your Student Account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Select your academic details below to set up your profile", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                if (authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(authError ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("1. Personal Information", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Full Name
                        OutlinedTextField(
                            value = signUpName,
                            onValueChange = { signUpName = it },
                            label = { Text("Full Student Name *") },
                            placeholder = { Text("e.g. Alex Johnson") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("signup_name_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Email
                        OutlinedTextField(
                            value = signUpEmail,
                            onValueChange = { signUpEmail = it },
                            label = { Text("Student Email *") },
                            placeholder = { Text("e.g. alex@university.edu") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("signup_email_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Password
                        OutlinedTextField(
                            value = signUpPassword,
                            onValueChange = { signUpPassword = it },
                            label = { Text("Password *") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("signup_password_field"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("2. Academic Background & DOB", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Predefined Major Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = signUpMajor,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Major / Course *") },
                                leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { expandedMajor = !expandedMajor }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedMajor = true },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = expandedMajor,
                                onDismissRequest = { expandedMajor = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                predefinedMajors.forEach { majorItem ->
                                    DropdownMenuItem(
                                        text = { Text(majorItem, fontSize = 13.sp) },
                                        onClick = {
                                            signUpMajor = majorItem
                                            expandedMajor = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Predefined Year / Grade Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = signUpGrade,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Year / Grade Level *") },
                                leadingIcon = { Icon(Icons.Outlined.Class, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { expandedGrade = !expandedGrade }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedGrade = true },
                                shape = RoundedCornerShape(12.dp)
                            )
                            DropdownMenu(
                                expanded = expandedGrade,
                                onDismissRequest = { expandedGrade = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                predefinedGrades.forEach { gradeItem ->
                                    DropdownMenuItem(
                                        text = { Text(gradeItem, fontSize = 13.sp) },
                                        onClick = {
                                            signUpGrade = gradeItem
                                            expandedGrade = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date of Birth (DOB) Picker (Not manually editable, opens DatePickerDialog)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (signUpDob.isBlank()) "Tap to select Date of Birth" else signUpDob,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Date of Birth (DOB) *") },
                                leadingIcon = { Icon(Icons.Outlined.Cake, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick Date of Birth", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { datePickerDialog.show() },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target GPA
                        OutlinedTextField(
                            value = signUpGpa,
                            onValueChange = { signUpGpa = it },
                            label = { Text("Target GPA") },
                            placeholder = { Text("e.g. 3.8") },
                            leadingIcon = { Icon(Icons.Outlined.MilitaryTech, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Avatar Picker
                        Text("3. Choose Profile Avatar Emoji", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            avatars.forEachIndexed { index, emoji ->
                                FilterChip(
                                    selected = selectedAvatarIndex == index,
                                    onClick = { selectedAvatarIndex = index },
                                    label = { Text(emoji, fontSize = 20.sp) },
                                    shape = CircleShape
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                viewModel.signUp(
                                    fullName = signUpName,
                                    email = signUpEmail,
                                    password = signUpPassword,
                                    major = signUpMajor,
                                    gradeLevel = signUpGrade,
                                    targetGpa = signUpGpa,
                                    avatarIndex = selectedAvatarIndex,
                                    dateOfBirth = signUpDob
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_signup_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Create Account & Launch Scholar Hub", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Log In Here", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --- STUDENT PROFILE MODAL DIALOG ---

@Composable
fun StudentProfileDialog(
    viewModel: StudyViewModel,
    onDismiss: () -> Unit
) {
    val profile by viewModel.studentProfile.collectAsState()
    val materials by viewModel.materials.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()

    var isEditing by remember { mutableStateOf(false) }

    var editName by remember(profile) { mutableStateOf(profile?.fullName ?: "") }
    var editMajor by remember(profile) { mutableStateOf(profile?.major ?: "") }
    var editGrade by remember(profile) { mutableStateOf(profile?.gradeLevel ?: "") }
    var editTargetGpa by remember(profile) { mutableStateOf(profile?.targetGpa ?: "") }
    var editAvatarIndex by remember(profile) { mutableStateOf(profile?.avatarIndex ?: 0) }

    val avatars = listOf("🎓", "🔬", "💻", "📚", "🚀", "⚡")

    val student = profile ?: return

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Student Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Avatar Display
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val avatarIcon = avatars.getOrElse(student.avatarIndex) { "🎓" }
                        Text(avatarIcon, fontSize = 36.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (!isEditing) {
                    Text(
                        student.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        student.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Details Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            ProfileDetailRow(icon = Icons.Outlined.School, label = "Major / Field", value = student.major)
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            ProfileDetailRow(icon = Icons.Outlined.Class, label = "Grade Level", value = student.gradeLevel)
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            ProfileDetailRow(icon = Icons.Outlined.Grade, label = "Target GPA", value = "${student.targetGpa} / 4.0")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Academic Progress Stats Grid
                    Text("Academic Activity", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(title = "Notes & Files", count = materials.size.toString(), icon = Icons.Filled.Description, modifier = Modifier.weight(1f))
                        StatCard(title = "Quizzes Taken", count = quizzes.count { it.isTaken }.toString(), icon = Icons.Filled.Quiz, modifier = Modifier.weight(1f))
                        StatCard(title = "Tasks Done", count = tasks.count { it.isCompleted }.toString(), icon = Icons.Filled.CheckCircle, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile")
                        }

                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("logout_button")
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Out")
                        }
                    }
                } else {
                    // EDIT PROFILE FORM
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editMajor,
                        onValueChange = { editMajor = it },
                        label = { Text("Major") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editGrade,
                        onValueChange = { editGrade = it },
                        label = { Text("Grade Level") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editTargetGpa,
                        onValueChange = { editTargetGpa = it },
                        label = { Text("Target GPA") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateProfileDetails(
                                    fullName = editName,
                                    major = editMajor,
                                    gradeLevel = editGrade,
                                    targetGpa = editTargetGpa,
                                    avatarIndex = editAvatarIndex
                                )
                                isEditing = false
                            }
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
