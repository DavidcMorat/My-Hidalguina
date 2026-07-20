package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// Model definition for dynamic course list
data class Course(
    val id: String,
    val name: String,
    val teacher: String,
    val progress: Float,
    val color: Color,
    val icon: ImageVector,
    val topics: List<String>,
    val tasks: List<String>
)

data class StudentAnnouncement(
    val id: String,
    val text: String,
    val senderName: String,
    val area: String,
    val timestamp: Long
)

suspend fun generateAiStudyPlan(courseName: String, topicName: String): List<String> = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext listOf(
            "Paso 1: Leer conceptos fundamentales de $topicName en $courseName",
            "Paso 2: Realizar 5 ejercicios prácticos interactivos sobre este tema",
            "Paso 3: Explicar lo aprendido con un resumen para autoevaluación"
        )
    }

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
    val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val prompt = """
        Genera un plan de estudio interactivo de exactamente 3 pasos tipo Duolingo para el curso escolar '$courseName' sobre el tema '$topicName'.
        Cada paso debe ser una oración corta y motivadora (máximo 12 palabras), indicando una actividad concreta que el estudiante debe hacer.
        Devuelve ÚNICAMENTE un arreglo JSON de strings de longitud exacta de 3 elementos, por ejemplo:
        ["Paso 1: Revisa el concepto clave de...", "Paso 2: Resuelve los ejercicios prácticos de...", "Paso 3: Haz una autoevaluación rápida de..."]
        No incluyas texto extra, ni formato markdown como ```json o ```, solo el arreglo de strings JSON válido.
    """.trimIndent()

    val requestJson = JSONObject().apply {
        put("contents", JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", prompt)
                    })
                })
            })
        })
    }

    val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder().url(url).post(body).build()

    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext listOf(
                    "Paso 1: Leer conceptos fundamentales de $topicName en $courseName",
                    "Paso 2: Realizar 5 ejercicios prácticos interactivos sobre este tema",
                    "Paso 3: Explicar lo aprendido con un resumen para autoevaluación"
                )
            }
            val responseString = response.body?.string() ?: ""
            val responseJson = JSONObject(responseString)
            val candidates = responseJson.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val textResponse = parts.getJSONObject(0).getString("text").trim()

            var cleanText = textResponse
            if (cleanText.startsWith("```")) {
                cleanText = cleanText.removePrefix("```json").removePrefix("```").trim()
                if (cleanText.endsWith("```")) {
                    cleanText = cleanText.removeSuffix("```").trim()
                }
            }

            val stepsArray = JSONArray(cleanText)
            val steps = mutableListOf<String>()
            for (i in 0 until stepsArray.length()) {
                steps.add(stepsArray.getString(i))
            }
            if (steps.size == 3) steps else listOf(
                "Paso 1: Leer conceptos fundamentales de $topicName",
                "Paso 2: Realizar ejercicios de práctica guiada",
                "Paso 3: Completar una autoevaluación final"
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        listOf(
            "Paso 1: Leer conceptos fundamentales de $topicName (Modo Offline)",
            "Paso 2: Realizar 5 ejercicios prácticos (Modo Offline)",
            "Paso 3: Autoexplicar lo aprendido en un resumen (Modo Offline)"
        )
    }
}

@Composable
fun StudentDashboard(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("Inicio") }
    var selectedChatUser by remember { mutableStateOf<com.example.chat.ChatUser?>(null) }
    
    // States for functional features in v0.1.0 (offline states mapped dynamically)
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var activeToolSubScreen by remember { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("student_local_storage", android.content.Context.MODE_PRIVATE) }
    
    // Duolingo-style Point and Level local state (stored in SharedPreferences)
    var localPoints by remember { mutableStateOf(sharedPrefs.getInt("local_points", 0)) }
    var completedAiTasks by remember { 
        val saved = sharedPrefs.getStringSet("completed_ai_tasks", emptySet()) ?: emptySet()
        mutableStateOf(saved)
    }
    
    // AI Study Plan local state
    var aiPlanTopic by remember { mutableStateOf(sharedPrefs.getString("ai_plan_topic", "") ?: "") }
    var aiPlanCourse by remember { mutableStateOf(sharedPrefs.getString("ai_plan_course", "") ?: "") }
    var aiPlanTasks by remember { 
        val savedTasksStr = sharedPrefs.getString("ai_plan_tasks_csv", "") ?: ""
        mutableStateOf(if (savedTasksStr.isEmpty()) emptyList<String>() else savedTasksStr.split("|||"))
    }
    var isGeneratingPlan by remember { mutableStateOf(false) }
    
    // Celebration overlay state (Duolingo Style!)
    var showDuolingoCelebration by remember { mutableStateOf(false) }
    var celebrationPoints by remember { mutableStateOf(0) }
    
    val coroutineScope = rememberCoroutineScope()
    
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val uid = currentUser?.uid

    // Real-time synchronization of points and announcements from teachers
    var teacherAnnouncements by remember { mutableStateOf<List<StudentAnnouncement>>(emptyList()) }
    var isLoadingAnnouncements by remember { mutableStateOf(false) }

    val profilePrefs = remember { context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE) }
    val studentGrade = profilePrefs.getString("grade_backup_${uid}", "5to") ?: "5to"
    val studentSection = profilePrefs.getString("section_backup_${uid}", "A") ?: "A"

    LaunchedEffect(uid) {
        if (uid != null) {
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .addSnapshotListener { snapshot, error ->
                        if (snapshot != null && snapshot.exists()) {
                            val remotePoints = snapshot.getLong("points")
                            if (remotePoints != null) {
                                if (remotePoints.toInt() != localPoints) {
                                    localPoints = remotePoints.toInt()
                                    sharedPrefs.edit().putInt("local_points", localPoints).apply()
                                }
                            }
                        }
                    }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(uid, studentGrade, studentSection) {
        if (uid != null) {
            isLoadingAnnouncements = true
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("announcements")
                    .whereEqualTo("grade", studentGrade)
                    .whereEqualTo("section", studentSection)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        isLoadingAnnouncements = false
                        if (snapshot != null) {
                            teacherAnnouncements = snapshot.documents.mapNotNull { doc ->
                                val text = doc.getString("text") ?: ""
                                val sender = doc.getString("senderName") ?: ""
                                val subject = doc.getString("area") ?: ""
                                val ts = doc.getLong("timestamp") ?: 0L
                                StudentAnnouncement(doc.id, text, sender, subject, ts)
                            }
                        }
                    }
            } catch (e: Exception) {
                isLoadingAnnouncements = false
            }
        }
    }
    
    // Persistent-like offline states using Compose memories
    var completedTasks by remember { mutableStateOf(setOf<String>()) }
    var completedTopics by remember { mutableStateOf(setOf<String>()) }
    var studyReminders by remember { mutableStateOf(listOf("Repasar Matemática", "Practicar vocabulario de Inglés")) }

    // Peruvian secondary school curriculum (cursos en blanco por defecto)
    val standardCoursesList = listOf(
        Course(
            id = "religiosa",
            name = "Educación Religiosa",
            teacher = "Prof. Juan Carlos Pérez",
            progress = 0f,
            color = YellowSecondary,
            icon = Icons.Filled.Church,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "trabajo",
            name = "Educación para el Trabajo",
            teacher = "Prof. Martín Vizcarra",
            progress = 0f,
            color = BlackTertiary,
            icon = Icons.Filled.Work,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "fisica",
            name = "Educación Física",
            teacher = "Prof. Percy Rojas",
            progress = 0f,
            color = RedPrimary,
            icon = Icons.Filled.FitnessCenter,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "arte",
            name = "Arte y Cultura",
            teacher = "Profra. Gabriela Mistral",
            progress = 0f,
            color = YellowSecondary,
            icon = Icons.Filled.Palette,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "ingles",
            name = "Inglés",
            teacher = "Profra. Nancy Cruz",
            progress = 0f,
            color = BlackTertiary,
            icon = Icons.Filled.Translate,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "ciencia",
            name = "Ciencia y Tecnología",
            teacher = "Prof. Luis Solis",
            progress = 0f,
            color = RedPrimary,
            icon = Icons.Filled.Science,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "sociales",
            name = "Ciencias Sociales",
            teacher = "Profra. María Choque",
            progress = 0f,
            color = YellowSecondary,
            icon = Icons.Filled.Public,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "desarrollo",
            name = "Desarrollo Personal, Ciudadanía y Cívica",
            teacher = "Profra. Inés Melchor",
            progress = 0f,
            color = BlackTertiary,
            icon = Icons.Filled.Gavel,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "comunicacion",
            name = "Comunicación",
            teacher = "Profra. Clorinda Matto",
            progress = 0f,
            color = RedPrimary,
            icon = Icons.Filled.MenuBook,
            topics = emptyList(),
            tasks = emptyList()
        ),
        Course(
            id = "matematica",
            name = "Matemática",
            teacher = "Prof. César Vallejo",
            progress = 0f,
            color = YellowSecondary,
            icon = Icons.Filled.Calculate,
            topics = emptyList(),
            tasks = emptyList()
        )
    )

    // Helper functions for score incrementation
    val addPoints: (Int) -> Unit = { amount ->
        val newPoints = localPoints + amount
        localPoints = newPoints
        sharedPrefs.edit().putInt("local_points", newPoints).apply()
    }

    val saveAiPlan: (String, String, List<String>) -> Unit = { course, topic, tasks ->
        aiPlanCourse = course
        aiPlanTopic = topic
        aiPlanTasks = tasks
        sharedPrefs.edit()
            .putString("ai_plan_course", course)
            .putString("ai_plan_topic", topic)
            .putString("ai_plan_tasks_csv", tasks.joinToString("|||"))
            .apply()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { 
            // Bottom navigation bar is visible unless we are viewing a Chat details or Course detail screen
            if (selectedChatUser == null && selectedCourse == null && activeToolSubScreen == null) {
                StudentBottomNavigation(selectedTab) { 
                    selectedTab = it 
                }
            }
        }
    ) { innerPadding ->
        // Duolingo Point Celebration Dialog (No emojis, highly native Material Design 3)
        if (showDuolingoCelebration) {
            AlertDialog(
                onDismissRequest = { showDuolingoCelebration = false },
                confirmButton = {
                    Button(
                        onClick = { showDuolingoCelebration = false },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continuar", color = Color.White)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stars,
                            contentDescription = null,
                            tint = YellowSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buen Trabajo", fontWeight = FontWeight.Bold, color = BlackTertiary)
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "+$celebrationPoints Puntos",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Has completado una actividad de tu plan de estudio de la IA. ¡Sigue avanzando para dominar esta asignatura escolar!",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = TextGray,
                            lineHeight = 18.sp
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
        }

        if (selectedTab == "Mensajes") {
            if (selectedChatUser != null) {
                com.example.chat.ChatDetailScreen(
                    user = selectedChatUser!!,
                    onBack = { selectedChatUser = null }
                )
            } else {
                com.example.chat.MessagesTab(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToChat = { selectedChatUser = it }
                )
            }
        } else if (selectedTab == "Perfil") {
            ProfileScreen(
                modifier = Modifier.padding(innerPadding),
                authViewModel = authViewModel,
                onLogout = onLogout
            )
        } else if (selectedTab == "Cursos") {
            if (selectedCourse != null) {
                CourseDetailScreen(
                    course = selectedCourse!!,
                    completedTopics = completedTopics,
                    onToggleTopic = { topicKey ->
                        completedTopics = if (completedTopics.contains(topicKey)) {
                            completedTopics - topicKey
                        } else {
                            celebrationPoints = 30
                            showDuolingoCelebration = true
                            addPoints(30)
                            completedTopics + topicKey
                        }
                    },
                    completedTasks = completedTasks,
                    onToggleTask = { taskKey ->
                        completedTasks = if (completedTasks.contains(taskKey)) {
                            completedTasks - taskKey
                        } else {
                            celebrationPoints = 50
                            showDuolingoCelebration = true
                            addPoints(50)
                            completedTasks + taskKey
                        }
                    },
                    onBack = { selectedCourse = null }
                )
            } else {
                CursosTab(
                    modifier = Modifier.padding(innerPadding),
                    courses = standardCoursesList,
                    completedTopics = completedTopics,
                    completedTasks = completedTasks,
                    onCourseClick = { selectedCourse = it }
                )
            }
        } else {
            // "Inicio" / MAIN DASHBOARD TAB
            if (activeToolSubScreen == "Aprendizaje") {
                AprendizajeScreen(
                    courses = standardCoursesList,
                    onBack = { activeToolSubScreen = null }
                )
            } else if (activeToolSubScreen == "Tareas") {
                TareasScreen(
                    courses = standardCoursesList,
                    completedTasks = completedTasks,
                    onToggleTask = { taskKey ->
                        completedTasks = if (completedTasks.contains(taskKey)) {
                            completedTasks - taskKey
                        } else {
                            celebrationPoints = 50
                            showDuolingoCelebration = true
                            addPoints(50)
                            completedTasks + taskKey
                        }
                    },
                    onBack = { activeToolSubScreen = null }
                )
            } else if (activeToolSubScreen == "Plan de estudio") {
                PlanEstudioScreen(
                    courses = standardCoursesList,
                    studyReminders = studyReminders,
                    onAddReminder = { rem -> studyReminders = studyReminders + rem },
                    onRemoveReminder = { rem -> studyReminders = studyReminders - rem },
                    aiPlanCourse = aiPlanCourse,
                    aiPlanTopic = aiPlanTopic,
                    aiPlanTasks = aiPlanTasks,
                    completedAiTasks = completedAiTasks,
                    onToggleAiTask = { taskKey ->
                        val newCompleted = if (completedAiTasks.contains(taskKey)) {
                            completedAiTasks - taskKey
                        } else {
                            celebrationPoints = 50
                            showDuolingoCelebration = true
                            addPoints(50)
                            completedAiTasks + taskKey
                        }
                        completedAiTasks = newCompleted
                        sharedPrefs.edit().putStringSet("completed_ai_tasks", newCompleted).apply()
                    },
                    onGeneratePlan = { course, topic ->
                        coroutineScope.launch {
                            isGeneratingPlan = true
                            val steps = generateAiStudyPlan(course, topic)
                            saveAiPlan(course, topic, steps)
                            // Reset tasks completion for the new plan
                            completedAiTasks = emptySet()
                            sharedPrefs.edit().putStringSet("completed_ai_tasks", emptySet()).apply()
                            isGeneratingPlan = false
                        }
                    },
                    isGenerating = isGeneratingPlan,
                    localPoints = localPoints,
                    onAddPoints = addPoints,
                    onBack = { activeToolSubScreen = null }
                )
            } else if (activeToolSubScreen == "Mi progreso") {
                MiProgresoScreen(
                    courses = standardCoursesList,
                    completedTopics = completedTopics,
                    completedTasks = completedTasks,
                    onBack = { activeToolSubScreen = null }
                )
            } else if (activeToolSubScreen == "Logros") {
                LogrosScreen(
                    localPoints = localPoints,
                    aiPlanGenerated = aiPlanTasks.isNotEmpty(),
                    completedAiTasksCount = completedAiTasks.size,
                    onBack = { activeToolSubScreen = null }
                )
            } else {
                // Dashboard Home UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundGray)
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        DashboardTopDecoration()
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { /* Notificaciones stub */ }) {
                                    Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray)
                                        .border(3.dp, YellowSecondary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.White)
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column {
                                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                    val displayName = user?.displayName ?: "Estudiante"
                                    Text(
                                        text = "¡Hola, $displayName!",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Cada paso que das hoy\nte acerca a tu mejor versión.",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Score and Level
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .offset(y = (-40).dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = BlackTertiary),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("Puntaje", color = Color.White, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("$localPoints", color = YellowSecondary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = YellowSecondary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val currentLevel = (localPoints / 200) + 1
                                val pointsInLevel = localPoints % 200
                                val levelProgress = pointsInLevel.toFloat() / 200f
                                Text("Nivel $currentLevel", color = Color.White, fontSize = 12.sp)
                                LinearProgressIndicator(
                                    progress = { levelProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = YellowSecondary,
                                    trackColor = Color.DarkGray
                                )
                            }
                        }
                        
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(YellowSecondary.copy(alpha = 0.2f))
                                        .border(2.dp, YellowSecondary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = YellowSecondary, modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("¡Vas muy bien!", color = RedPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Sigue así para\nalcanzar el siguiente nivel.", color = TextGray, fontSize = 10.sp, lineHeight = 12.sp)
                                }
                            }
                        }
                    }
                    
                    // Tools Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .offset(y = (-20).dp)
                    ) {
                        Text(
                            text = "Herramientas",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackTertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ToolCard(
                                title = "Aprendizaje",
                                subtitle = "Estudia los temas\nde tus cursos",
                                icon = Icons.Filled.MenuBook,
                                backgroundColor = RedPrimary,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = { activeToolSubScreen = "Aprendizaje" }
                            )
                            ToolCard(
                                title = "Tareas",
                                subtitle = "Revisa y entrega\ntus actividades",
                                icon = Icons.Filled.Assignment,
                                backgroundColor = BlackTertiary,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = { activeToolSubScreen = "Tareas" }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ToolCard(
                                title = "Plan de estudio",
                                subtitle = "Tu ruta personalizada\nde aprendizaje",
                                icon = Icons.Filled.AdsClick,
                                backgroundColor = YellowSecondary,
                                contentColor = BlackTertiary,
                                modifier = Modifier.weight(1f),
                                onClick = { activeToolSubScreen = "Plan de estudio" }
                            )
                            ToolCard(
                                title = "Mi progreso",
                                subtitle = "Revisa tu avance\npor materia",
                                icon = Icons.Filled.BarChart,
                                backgroundColor = BlackTertiary,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = { activeToolSubScreen = "Mi progreso" }
                            )
                            ToolCard(
                                title = "Logros",
                                subtitle = "Desbloquea medallas\ny de recompensa",
                                icon = Icons.Filled.EmojiEvents,
                                backgroundColor = RedPrimary,
                                contentColor = Color.White,
                                modifier = Modifier.weight(1f),
                                onClick = { activeToolSubScreen = "Logros" }
                            )
                        }
                    }
                    
                    // Upcoming Activities Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Próximas actividades",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackTertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ActivityItem(
                            title = "Matemática",
                            subtitle = "Resolver ejercicios de Álgebra",
                            date = "Hoy",
                            icon = Icons.Filled.Calculate,
                            iconTint = RedPrimary,
                            iconBg = RedPrimary.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ActivityItem(
                            title = "Comunicación",
                            subtitle = "Leer la obra 'Yawar Fiesta' de José María Arguedas",
                            date = "Mañana",
                            icon = Icons.Filled.MenuBook,
                            iconTint = YellowSecondary,
                            iconBg = YellowSecondary.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ActivityItem(
                            title = "Ciencia y Tecnología",
                            subtitle = "Explicar los tipos de energía en la comunidad",
                            date = "24 may",
                            icon = Icons.Filled.Science,
                            iconTint = Color.White,
                            iconBg = BlackTertiary
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Teacher Announcements Section
                        Text(
                            text = "Avisos de tus Docentes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackTertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (isLoadingAnnouncements) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = RedPrimary)
                            }
                        } else if (teacherAnnouncements.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DividerGray)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Campaign,
                                        contentDescription = null,
                                        tint = TextGray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Tu docente aún no ha publicado comunicados para tu sección ($studentGrade - $studentSection).",
                                        color = TextGray,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        } else {
                            teacherAnnouncements.forEach { announcement ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerGray)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Campaign,
                                                    contentDescription = null,
                                                    tint = RedPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = announcement.senderName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = BlackTertiary
                                                )
                                            }
                                            val sdf = remember { java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()) }
                                            Text(
                                                text = sdf.format(java.util.Date(announcement.timestamp)),
                                                color = TextGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Área: ${announcement.area}",
                                            color = RedPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = announcement.text,
                                            color = BlackTertiary,
                                            fontSize = 13.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = contentColor.copy(alpha = 0.8f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ActivityItem(
    title: String,
    subtitle: String,
    date: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                Text(text = subtitle, fontSize = 12.sp, color = TextGray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = date, fontSize = 12.sp, color = TextGray)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun StudentBottomNavigation(selectedTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = TextGray,
        tonalElevation = 8.dp
    ) {
        val tabColors = NavigationBarItemDefaults.colors(
            selectedIconColor = RedPrimary,
            selectedTextColor = RedPrimary,
            indicatorColor = Color(0xFFFBE9E7),
            unselectedIconColor = TextGray,
            unselectedTextColor = TextGray
        )

        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Inicio") },
            label = { Text("Inicio", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            selected = selectedTab == "Inicio",
            onClick = { onTabSelected("Inicio") },
            colors = tabColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.MenuBook, contentDescription = "Cursos") },
            label = { Text("Cursos", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            selected = selectedTab == "Cursos",
            onClick = { onTabSelected("Cursos") },
            colors = tabColors
        )
        
        // Central Item (Tutor IA) - disabled as requested ("menos el chat de ia")
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RedPrimary.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Tutor IA", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tutor IA", fontSize = 10.sp, color = TextGray)
            }
        }
        
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Chat, contentDescription = "Mensajes") },
            label = { Text("Mensajes", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            selected = selectedTab == "Mensajes",
            onClick = { onTabSelected("Mensajes") },
            colors = tabColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
            label = { Text("Perfil", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            selected = selectedTab == "Perfil",
            onClick = { onTabSelected("Perfil") },
            colors = tabColors
        )
    }
}

@Composable
fun DashboardTopDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(280.dp)) {
        val w = size.width
        val h = size.height
        
        // White base
        drawRect(Color.White)
        
        // Yellow accent shape
        val yellowPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.6f)
            quadraticTo(w * 0.5f, h * 0.7f, 0f, h * 0.9f)
            close()
        }
        drawPath(yellowPath, YellowSecondary)

        // Red foreground shape
        val redPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.55f)
            quadraticTo(w * 0.5f, h * 0.65f, 0f, h * 0.85f)
            close()
        }
        drawPath(redPath, RedPrimary)
    }
}

// Sub-screen 1: Cursos List Tab
@Composable
fun CursosTab(
    modifier: Modifier = Modifier,
    courses: List<Course>,
    completedTopics: Set<String>,
    completedTasks: Set<String>,
    onCourseClick: (Course) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Mis Cursos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BlackTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Explora y estudia los temas de tus materias activas.",
            fontSize = 14.sp,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(20.dp))

        courses.forEach { course ->
            val totalItems = course.topics.size + course.tasks.size
            val completedItems = course.topics.count { completedTopics.contains("${course.id}_$it") } +
                    course.tasks.count { completedTasks.contains("${course.id}_$it") }
            val currentProgress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onCourseClick(course) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(course.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(course.icon, contentDescription = null, tint = course.color, modifier = Modifier.size(28.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(course.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                        Text(course.teacher, fontSize = 13.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { currentProgress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = course.color,
                                trackColor = DividerGray
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${(currentProgress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = course.color)
                        }
                    }

                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Sub-screen 2: Detailed Course Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: Course,
    completedTopics: Set<String>,
    onToggleTopic: (String) -> Unit,
    completedTasks: Set<String>,
    onToggleTask: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Temas, 1: Tareas
    var taskToSubmit by remember { mutableStateOf<String?>(null) }
    var submissionText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(course.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = course.color,
                    titleContentColor = if (course.color == YellowSecondary) BlackTertiary else Color.White,
                    navigationIconContentColor = if (course.color == YellowSecondary) BlackTertiary else Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Profesor: ${course.teacher}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = BlackTertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tu progreso de materia", fontSize = 14.sp, color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val totalItems = course.topics.size + course.tasks.size
                    val completedItems = course.topics.count { completedTopics.contains("${course.id}_$it") } +
                            course.tasks.count { completedTasks.contains("${course.id}_$it") }
                    val currentProgress = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = course.color,
                            trackColor = DividerGray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("${(currentProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = course.color)
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.White,
                contentColor = course.color,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = course.color
                    )
                }
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = { Text("Temas de Estudio") }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = { Text("Tareas de Clase") }
                )
            }

            if (selectedSubTab == 0) {
                // STUDY TOPICS TAB
                if (course.topics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = course.color.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Asignatura sin temas asignados",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BlackTertiary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Esta materia escolar de secundaria está vacía por defecto. Ingresa a la herramienta 'Plan de estudio' para crear una ruta personalizada con Inteligencia Artificial.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(course.topics.size) { index ->
                            val topic = course.topics[index]
                            val topicKey = "${course.id}_$topic"
                            val isDone = completedTopics.contains(topicKey)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isDone,
                                        onCheckedChange = { onToggleTopic(topicKey) },
                                        colors = CheckboxDefaults.colors(checkedColor = course.color)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = topic,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDone) TextGray else BlackTertiary,
                                        style = LocalTextStyle.current.copy(
                                            textDecoration = if (isDone) TextDecoration.LineThrough else null
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // TASKS TAB
                if (course.tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Assignment,
                                    contentDescription = null,
                                    tint = course.color.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Sin tareas de clase",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = BlackTertiary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No se registran tareas subidas por tu docente. Recuerda que puedes ganar puntos y desbloquear insignias completando planes de estudio con la IA.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(course.tasks.size) { index ->
                        val task = course.tasks[index]
                        val taskKey = "${course.id}_$task"
                        val isSubmitted = completedTasks.contains(taskKey)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                                        Text("Fecha límite: Mañana", fontSize = 12.sp, color = TextGray)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSubmitted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isSubmitted) "Entregado" else "Pendiente",
                                            color = if (isSubmitted) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (!isSubmitted) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (taskToSubmit == taskKey) {
                                        OutlinedTextField(
                                            value = submissionText,
                                            onValueChange = { submissionText = it },
                                            placeholder = { Text("Escribe tu respuesta o pega el enlace del documento...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = course.color
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { taskToSubmit = null; submissionText = "" }) {
                                                Text("Cancelar", color = TextGray)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    if (submissionText.isNotBlank()) {
                                                        onToggleTask(taskKey)
                                                        taskToSubmit = null
                                                        submissionText = ""
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar("¡Tarea entregada con éxito!")
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = course.color)
                                            ) {
                                                Text("Enviar", color = Color.White)
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { taskToSubmit = taskKey },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = course.color)
                                        ) {
                                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Entregar Tarea")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

// Sub-screen 3: Centro de Aprendizaje (Learning Screen)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AprendizajeScreen(courses: List<Course>, onBack: () -> Unit) {
    var expandedTopic by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centro de Aprendizaje", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Tarjetas de Estudio Rápidas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Toca cualquier tema para expandir la información clave y repasar fórmulas y conceptos importantes.", fontSize = 14.sp, color = TextGray)
            }

            courses.forEach { course ->
                item {
                    Text(course.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = course.color)
                }

                items(course.topics.size) { idx ->
                    val topic = course.topics[idx]
                    val isExpanded = expandedTopic == "${course.id}_$topic"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedTopic = if (isExpanded) null else "${course.id}_$topic" },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(course.icon, contentDescription = null, tint = course.color, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(topic, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BlackTertiary)
                                }
                                Icon(
                                    if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    tint = TextGray
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = DividerGray)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val explanation = when (course.id) {
                                    "math" -> when (idx) {
                                        0 -> "Ecuaciones Lineales:\nUna ecuación lineal es una igualdad algebraica con una o más variables a la primera potencia.\nFórmula general: ax + b = 0\nEjemplo: 2x - 4 = 0 => 2x = 4 => x = 2."
                                        1 -> "Funciones Cuadráticas:\nRepresentan parábolas en el plano.\nFórmula general: f(x) = ax² + bx + c\nEl vértice nos indica el punto máximo o mínimo de la parábola."
                                        else -> "Trigonometría Básica:\nEstudio de las relaciones entre los lados y ángulos de un triángulo rectángulo.\nFunciones principales: Seno (Co/Hi), Coseno (Ca/Hi), Tangente (Co/Ca)."
                                    }
                                    "hist" -> "Historia de México:\nEstudio cronológico de los sucesos patrios. Los periodos abarcan desde la época prehispánica y Mesoamérica, pasando por la Conquista, la Independencia de 1810 hasta la Revolución de 1910."
                                    "science" -> "Ciencias Naturales:\nAnálisis de los seres vivos y su entorno. La célula vegetal cuenta con pared celular de celulosa y cloroplastos para realizar la fotosíntesis, proceso que transforma la energía lumínica en glucosa."
                                    else -> "Lectura y Análisis:\nRepaso de las principales corrientes literarias de Hispanoamérica, uso correcto de reglas ortográficas de acentuación, y redacción de ensayos utilizando argumentos lógicos."
                                }

                                Text(
                                    text = explanation,
                                    fontSize = 14.sp,
                                    color = BlackTertiary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-screen 4: Unified Tasks Center
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasScreen(
    courses: List<Course>,
    completedTasks: Set<String>,
    onToggleTask: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedTabState by remember { mutableStateOf(0) } // 0: Pendientes, 1: Entregadas

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Centro de Actividades", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabState,
                containerColor = Color.White,
                contentColor = RedPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                        color = RedPrimary
                    )
                }
            ) {
                Tab(selected = selectedTabState == 0, onClick = { selectedTabState = 0 }, text = { Text("Pendientes") })
                Tab(selected = selectedTabState == 1, onClick = { selectedTabState = 1 }, text = { Text("Entregadas") })
            }

            val allTasks = courses.flatMap { course ->
                course.tasks.map { task -> Pair(course, task) }
            }

            val filteredTasks = allTasks.filter { (course, task) ->
                val taskKey = "${course.id}_$task"
                val isDone = completedTasks.contains(taskKey)
                if (selectedTabState == 0) !isDone else isDone
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (selectedTabState == 0) Icons.Filled.CheckCircleOutline else Icons.Filled.AssignmentLate,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTabState == 0) "¡Felicidades! No tienes tareas pendientes" else "Aún no has completado ninguna tarea",
                            fontSize = 16.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks.size) { idx ->
                        val (course, task) = filteredTasks[idx]
                        val taskKey = "${course.id}_$task"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(course.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(course.icon, contentDescription = null, tint = course.color, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                                    Text(course.name, fontSize = 12.sp, color = course.color, fontWeight = FontWeight.SemiBold)
                                }
                                Checkbox(
                                    checked = completedTasks.contains(taskKey),
                                    onCheckedChange = { onToggleTask(taskKey) },
                                    colors = CheckboxDefaults.colors(checkedColor = course.color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-screen 5: Timetable Study Planner
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEstudioScreen(
    courses: List<Course>,
    studyReminders: List<String>,
    onAddReminder: (String) -> Unit,
    onRemoveReminder: (String) -> Unit,
    aiPlanCourse: String,
    aiPlanTopic: String,
    aiPlanTasks: List<String>,
    completedAiTasks: Set<String>,
    onToggleAiTask: (String) -> Unit,
    onGeneratePlan: (String, String) -> Unit,
    isGenerating: Boolean,
    localPoints: Int,
    onAddPoints: (Int) -> Unit,
    onBack: () -> Unit
) {
    var newReminderText by remember { mutableStateOf("") }
    var selectedCourseIndex by remember { mutableStateOf(0) }
    var userTopicInput by remember { mutableStateOf("") }

    val selectedCourseName = courses.getOrNull(selectedCourseIndex)?.name ?: "Matemática"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan de Estudio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // AI STUDY PLAN GENERATOR SECTION
            Text("Planificador de Estudio con IA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Genera una ruta de aprendizaje personalizada con Inteligencia Artificial para el currículo peruano.", fontSize = 13.sp, color = TextGray)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. Selecciona la asignatura escolar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal course chips selector
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(courses.size) { idx ->
                            val course = courses[idx]
                            val isSelected = selectedCourseIndex == idx
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCourseIndex = idx },
                                label = { Text(course.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = RedPrimary,
                                    selectedLeadingIconColor = RedPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("2. Escribe el tema que deseas dominar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userTopicInput,
                        onValueChange = { userTopicInput = it },
                        placeholder = { Text("Ej. Fracciones, La célula, Climas de la región...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (userTopicInput.isNotBlank()) {
                                onGeneratePlan(selectedCourseName, userTopicInput.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isGenerating && userTopicInput.isNotBlank()
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generando Plan...", color = Color.White)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generar Plan con IA", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ACTIVE AI STUDY PLAN DISPLAY
            if (aiPlanTasks.isNotEmpty()) {
                Text("Tu Ruta de Aprendizaje Activa", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Completa cada actividad escolar recomendada por la IA para sumar +50 puntos de experiencia.", fontSize = 13.sp, color = TextGray)
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE9E7)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, RedPrimary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Asignatura: $aiPlanCourse", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                                Text("Tema: $aiPlanTopic", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(YellowSecondary.copy(alpha = 0.3f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Duolingo Study", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render the 3 steps of the active study plan
                        aiPlanTasks.forEachIndexed { idx, stepText ->
                            val stepKey = "ai_plan_step_${idx}_${aiPlanTopic}"
                            val isCompleted = completedAiTasks.contains(stepKey)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCompleted) Color(0xFFE8F5E9) else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isCompleted) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isCompleted,
                                        onCheckedChange = { onToggleAiTask(stepKey) },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2E7D32))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stepText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isCompleted) Color(0xFF1B5E20) else BlackTertiary,
                                            style = LocalTextStyle.current.copy(
                                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                                            )
                                        )
                                        Text(
                                            text = if (isCompleted) "¡Completado! +50 XP" else "Pendiente de estudio",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) Color(0xFF2E7D32) else TextGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // WEEKLY SCHOOL SCHEDULE SECTION
            Text("Horario Escolar Semanal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
            Spacer(modifier = Modifier.height(12.dp))

            val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
            val schedule = listOf(
                "08:00 AM - Matemática",
                "09:30 AM - Comunicación",
                "11:00 AM - RECESO ESCOLAR",
                "11:30 AM - Ciencia y Tecnología",
                "01:00 PM - Ciencias Sociales"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    days.forEachIndexed { idx, day ->
                        Text(day, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (idx % 2 == 0) schedule.joinToString("\n") else schedule.reversed().joinToString("\n"),
                            fontSize = 12.sp,
                            color = TextGray,
                            lineHeight = 18.sp
                        )
                        if (idx < days.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DividerGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PERSONAL STUDY REMINDERS SECTION
            Text("Tus Recordatorios de Estudio", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newReminderText,
                    onValueChange = { newReminderText = it },
                    placeholder = { Text("Ej. Estudiar Matemática a las 5:00 PM...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newReminderText.isNotBlank()) {
                            onAddReminder(newReminderText)
                            newReminderText = ""
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Añadir", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            studyReminders.forEach { reminder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Alarm, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(reminder, fontSize = 14.sp, color = BlackTertiary)
                        }
                        IconButton(onClick = { onRemoveReminder(reminder) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = RedPrimary)
                        }
                    }
                }
            }
        }
    }
}

// Sub-screen 6: My Progress Charts
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiProgresoScreen(
    courses: List<Course>,
    completedTopics: Set<String>,
    completedTasks: Set<String>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Progreso Académico", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Estadísticas Generales", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
            Spacer(modifier = Modifier.height(12.dp))

            val totalAllTopics = courses.sumOf { it.topics.size }
            val completedAllTopics = courses.sumOf { course -> course.topics.count { completedTopics.contains("${course.id}_$it") } }
            val totalAllTasks = courses.sumOf { it.tasks.size }
            val completedAllTasks = courses.sumOf { course -> course.tasks.count { completedTasks.contains("${course.id}_$it") } }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Temas de Estudio", fontSize = 12.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$completedAllTopics / $totalAllTopics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Tareas Completadas", fontSize = 12.sp, color = TextGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$completedAllTasks / $totalAllTasks", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = YellowSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart for modern high fidelity visualization of empty states
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Progreso por Asignatura", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                    Spacer(modifier = Modifier.height(20.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        val barCount = courses.size
                        val spacing = 16.dp.toPx()
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        val availableWidth = canvasWidth - (spacing * (barCount + 1))
                        val barWidth = availableWidth / barCount

                        // Draw backing grid lines
                        val linesCount = 4
                        for (i in 0..linesCount) {
                            val y = canvasHeight * (i.toFloat() / linesCount)
                            drawLine(
                                color = DividerGray,
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw colorful dynamic bars based on progress
                        courses.forEachIndexed { index, course ->
                            val cTotal = course.topics.size + course.tasks.size
                            val cDone = course.topics.count { completedTopics.contains("${course.id}_$it") } +
                                    course.tasks.count { completedTasks.contains("${course.id}_$it") }
                            val cProgress = if (cTotal > 0) cDone.toFloat() / cTotal else 0f

                            val x = spacing + index * (barWidth + spacing)
                            val barHeight = canvasHeight * cProgress
                            
                            drawRect(
                                color = course.color,
                                topLeft = androidx.compose.ui.geometry.Offset(x, canvasHeight - barHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        courses.forEach { course ->
                            val cTotal = course.topics.size + course.tasks.size
                            val cDone = course.topics.count { completedTopics.contains("${course.id}_$it") } +
                                    course.tasks.count { completedTasks.contains("${course.id}_$it") }
                            val cProgress = if (cTotal > 0) cDone.toFloat() / cTotal else 0f

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(course.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${course.name}: ${(cProgress * 100).toInt()}% completado",
                                    fontSize = 13.sp,
                                    color = BlackTertiary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Sub-screen 7: Insignias y Logros (Totalmente Local y gamificado como Duolingo)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogrosScreen(
    localPoints: Int,
    aiPlanGenerated: Boolean,
    completedAiTasksCount: Int,
    onBack: () -> Unit
) {
    val achievements = listOf(
        Triple("Primeros Pasos", "Obtén tus primeros 50 puntos de estudio.", localPoints >= 50),
        Triple("Creador de Rutas", "Genera tu primer plan de estudio personalizado con la IA.", aiPlanGenerated),
        Triple("Estudiante de Bronce", "Alcanza los 200 puntos en tu progreso escolar.", localPoints >= 200),
        Triple("Mente Brillante", "Completa al menos 1 paso de estudio de la IA.", completedAiTasksCount >= 1),
        Triple("Estudiante de Plata", "Suma un total de 500 puntos en la plataforma escolar.", localPoints >= 500),
        Triple("Sabio de Oro", "Domina el currículo peruano acumulando 1000 puntos.", localPoints >= 1000)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Logros y Medallas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Tus Medallas Escolares", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Desbloquea insignias especiales estudiando tus planes personalizados de la IA.", fontSize = 14.sp, color = TextGray)
            }

            items(achievements.size) { idx ->
                val (title, desc, isUnlocked) = achievements[idx]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) Color.White else Color(0xFFF0F0F0)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUnlocked) YellowSecondary.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isUnlocked) YellowSecondary else Color.LightGray,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) YellowSecondary else TextGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) BlackTertiary else TextGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                fontSize = 13.sp,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isUnlocked) "Estado: Desbloqueado" else "Estado: Bloqueado",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) Color(0xFF2E7D32) else TextGray
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
