package com.example

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat.MessagesTab
import com.example.chat.ChatUser
import com.example.tasks.data.TaskModel
import com.example.tasks.data.TaskRepository
import com.example.tasks.ui.TasksScreen
import com.example.announcements.data.AnnouncementModel
import com.example.announcements.data.AnnouncementRepository
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

data class TeacherStudentModel(
    val uid: String = "",
    val studentName: String = "",
    val displayName: String = "",
    val grade: String = "",
    val section: String = ""
)

suspend fun fetchStudentsRobust(db: FirebaseFirestore, grade: String? = null, section: String? = null): List<TeacherStudentModel> {
    val exceptions = mutableListOf<String>()
    
    // Attempt 1: Query by role = "estudiante" (often allowed by rules and index-free)
    try {
        val result = db.collection("users").whereEqualTo("role", "estudiante").get().await()
        val list = result.documents.mapNotNull { doc ->
            val r = doc.getString("role")?.trim() ?: ""
            val g = doc.getString("grade")?.trim() ?: ""
            val s = doc.getString("section")?.trim() ?: ""
            
            val roleMatch = r.equals("estudiante", ignoreCase = true)
            val gradeMatch = grade == null || g.equals(grade, ignoreCase = true)
            val sectionMatch = section == null || s.equals(section, ignoreCase = true)
            
            if (roleMatch && gradeMatch && sectionMatch) {
                val uid = doc.getString("uid") ?: doc.id
                val studentName = doc.getString("studentName")?.takeIf { it.isNotBlank() } ?: doc.getString("displayName") ?: "Estudiante"
                val dispName = doc.getString("displayName") ?: ""
                TeacherStudentModel(
                    uid = uid,
                    studentName = studentName,
                    displayName = dispName,
                    grade = g,
                    section = s
                )
            } else null
        }
        if (list.isNotEmpty()) {
            android.util.Log.d("TeacherDashboard", "Success fetching students via Attempt 1 (role=estudiante)")
            return list
        }
    } catch (e: Exception) {
        exceptions.add("Query by role=estudiante failed: ${e.message}")
    }

    // Attempt 2: If grade/section is provided, query by grade and filter section/role in-memory
    if (grade != null) {
        try {
            val result = db.collection("users").whereEqualTo("grade", grade).get().await()
            val list = result.documents.mapNotNull { doc ->
                val r = doc.getString("role")?.trim() ?: ""
                val g = doc.getString("grade")?.trim() ?: ""
                val s = doc.getString("section")?.trim() ?: ""
                
                val roleMatch = r.equals("estudiante", ignoreCase = true)
                val sectionMatch = section == null || s.equals(section, ignoreCase = true)
                
                if (roleMatch && sectionMatch) {
                    val uid = doc.getString("uid") ?: doc.id
                    val studentName = doc.getString("studentName")?.takeIf { it.isNotBlank() } ?: doc.getString("displayName") ?: "Estudiante"
                    val dispName = doc.getString("displayName") ?: ""
                    TeacherStudentModel(
                        uid = uid,
                        studentName = studentName,
                        displayName = dispName,
                        grade = g,
                        section = s
                    )
                } else null
            }
            if (list.isNotEmpty()) {
                android.util.Log.d("TeacherDashboard", "Success fetching students via Attempt 2 (grade filter)")
                return list
            }
        } catch (e: Exception) {
            exceptions.add("Query by grade failed: ${e.message}")
        }
    }

    // Attempt 3: General get() on users collection
    try {
        val result = db.collection("users").get().await()
        val list = result.documents.mapNotNull { doc ->
            val r = doc.getString("role")?.trim() ?: ""
            val g = doc.getString("grade")?.trim() ?: ""
            val s = doc.getString("section")?.trim() ?: ""
            
            val roleMatch = r.equals("estudiante", ignoreCase = true)
            val gradeMatch = grade == null || g.equals(grade, ignoreCase = true)
            val sectionMatch = section == null || s.equals(section, ignoreCase = true)
            
            if (roleMatch && gradeMatch && sectionMatch) {
                val uid = doc.getString("uid") ?: doc.id
                val studentName = doc.getString("studentName")?.takeIf { it.isNotBlank() } ?: doc.getString("displayName") ?: "Estudiante"
                val dispName = doc.getString("displayName") ?: ""
                TeacherStudentModel(
                    uid = uid,
                    studentName = studentName,
                    displayName = dispName,
                    grade = g,
                    section = s
                )
            } else null
        }
        android.util.Log.d("TeacherDashboard", "Success fetching students via Attempt 3 (general get)")
        return list
    } catch (e: Exception) {
        exceptions.add("General get() failed: ${e.message}")
    }

    android.util.Log.e("TeacherDashboard", "All student fetching methods failed. Exceptions: $exceptions")
    return emptyList()
}

@Composable
fun TeacherDashboard(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val currentUserId = user?.uid ?: ""

    var selectedTab by remember { mutableStateOf("Inicio") }
    var selectedChatUser by remember { mutableStateOf<ChatUser?>(null) }
    
    // Navigation stack inside Dashboard
    var currentSubScreen by remember { mutableStateOf("dashboard") } // "dashboard", "avisos", "tareas", "salones"
    var selectedClassroomForDetails by remember { mutableStateOf("") } // e.g. "3A"

    // Teacher Profile Info
    var displayName by remember { mutableStateOf(user?.displayName ?: "Docente") }
    var hasTutoria by remember { mutableStateOf(false) }
    var tutoriaGrade by remember { mutableStateOf("") }
    var tutoriaSection by remember { mutableStateOf("") }
    var teachingClassrooms by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val sharedPrefs = context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)
            displayName = sharedPrefs.getString("displayName_backup_$currentUserId", user?.displayName ?: "Docente") ?: "Docente"
            hasTutoria = sharedPrefs.getBoolean("hasTutoria_backup_$currentUserId", false)
            tutoriaGrade = sharedPrefs.getString("tutoriaGrade_backup_$currentUserId", "") ?: ""
            tutoriaSection = sharedPrefs.getString("tutoriaSection_backup_$currentUserId", "") ?: ""
            val classroomsSet = sharedPrefs.getStringSet("teachingClassrooms_backup_$currentUserId", emptySet()) ?: emptySet()
            teachingClassrooms = classroomsSet.toList().sorted()

            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(currentUserId).get().await()
                displayName = doc.getString("displayName") ?: displayName
                hasTutoria = doc.getBoolean("hasTutoria") ?: hasTutoria
                tutoriaGrade = doc.getString("tutoriaGrade") ?: tutoriaGrade
                tutoriaSection = doc.getString("tutoriaSection") ?: tutoriaSection
                val fClassrooms = (doc.get("teachingClassrooms") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (fClassrooms.isNotEmpty()) {
                    teachingClassrooms = fClassrooms.sorted()
                }
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (selectedChatUser == null && currentSubScreen == "dashboard") {
                TeacherBottomNavigation(
                    selectedTab = selectedTab,
                    hasTutoria = hasTutoria,
                    onTabSelected = { 
                        selectedTab = it
                        selectedChatUser = null
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == "Mensajes" && hasTutoria) {
                if (selectedChatUser != null) {
                    com.example.chat.ChatDetailScreen(
                        user = selectedChatUser!!,
                        onBack = { selectedChatUser = null }
                    )
                } else {
                    MessagesTab(
                        onNavigateToChat = { selectedChatUser = it }
                    )
                }
            } else if (selectedTab == "Perfil") {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogout = onLogout
                )
            } else {
                // "Inicio" tab handles sub-screens: dashboard, avisos, tareas, salones
                when (currentSubScreen) {
                    "avisos" -> {
                        TeacherAnnouncementsScreen(
                            teachingClassrooms = teachingClassrooms,
                            teacherId = currentUserId,
                            teacherName = displayName,
                            onBack = { currentSubScreen = "dashboard" }
                        )
                    }
                    "tareas" -> {
                        TasksScreen(
                            isTeacher = true,
                            onBack = { currentSubScreen = "dashboard" }
                        )
                    }
                    "salones" -> {
                        TeacherClassroomsScreen(
                            teachingClassrooms = teachingClassrooms,
                            selectedClassroom = selectedClassroomForDetails,
                            onBack = { currentSubScreen = "dashboard" }
                        )
                    }
                    else -> {
                        TeacherDashboardMain(
                            displayName = displayName,
                            hasTutoria = hasTutoria,
                            tutoriaGrade = tutoriaGrade,
                            tutoriaSection = tutoriaSection,
                            teachingClassrooms = teachingClassrooms,
                            onNavigateSubScreen = { screen, classroom ->
                                currentSubScreen = screen
                                selectedClassroomForDetails = classroom ?: ""
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherDashboardMain(
    displayName: String,
    hasTutoria: Boolean,
    tutoriaGrade: String,
    tutoriaSection: String,
    teachingClassrooms: List<String>,
    onNavigateSubScreen: (String, String?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    
    // Active Tasks
    val taskRepository = remember { TaskRepository() }
    val tasks by taskRepository.getTasksFlow().collectAsState(initial = emptyList())
    val myTasks = remember(tasks) { tasks.filter { it.teacherId == FirebaseAuth.getInstance().currentUser?.uid } }

    // Fetch student list to calculate delivery stats
    var studentsList by remember { mutableStateOf<List<TeacherStudentModel>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        studentsList = fetchStudentsRobust(db)
    }

    var selectedTaskForStats by remember { mutableStateOf<TaskModel?>(null) }
    
    // Auto-select first task if none selected
    LaunchedEffect(myTasks) {
        if (selectedTaskForStats == null && myTasks.isNotEmpty()) {
            selectedTaskForStats = myTasks.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeColors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Decorative Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            TeacherDashboardTopDecoration()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = YellowSecondary
                    ) {
                        Text(
                            text = "Portal de Docente",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackTertiary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasTutoria) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "Tutor: ${tutoriaGrade}°$tutoriaSection",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(onClick = { ThemeState.toggleTheme() }) {
                            Icon(
                                painter = painterResource(id = if (ThemeState.isDarkTheme) android.R.drawable.ic_menu_day else android.R.drawable.ic_menu_compass),
                                contentDescription = "Cambiar Modo Oscuro",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(3.dp, YellowSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "¡Hola, Prof. $displayName!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Supervisa el rendimiento escolar y asigna actividades rápidamente.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // GRID 2x2 OF TOOLS (Avisos, Tareas, Salones)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = (-40).dp)
        ) {
            Text(
                text = "Herramientas del Profesor",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeColors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ToolCard(
                    title = "Avisos",
                    subtitle = "Publica comunicados\nglobales o por aula",
                    icon = Icons.Filled.Campaign,
                    backgroundColor = ThemeColors.primary,
                    contentColor = ThemeColors.onPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateSubScreen("avisos", null) }
                )
                ToolCard(
                    title = "Tareas",
                    subtitle = "Asigna y revisa\ntareas escolares",
                    icon = Icons.Filled.Assignment,
                    backgroundColor = if (ThemeState.isDarkTheme) ThemeColors.cardSurface else BlackTertiary,
                    contentColor = if (ThemeState.isDarkTheme) ThemeColors.textPrimary else Color.White,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateSubScreen("tareas", null) }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ToolCard(
                    title = "Salones",
                    subtitle = "Monitorea tus\naulas enseñadas",
                    icon = Icons.Filled.Class,
                    backgroundColor = if (ThemeState.isDarkTheme) ThemeColors.cardSurface else YellowSecondary,
                    contentColor = if (ThemeState.isDarkTheme) ThemeColors.textPrimary else BlackTertiary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateSubScreen("salones", null) }
                )
                // Informational decorative stats tile
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (ThemeState.isDarkTheme) ThemeColors.cardSurface else BlackTertiary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.divider)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ThemeColors.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Group, contentDescription = null, tint = ThemeColors.primary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "${teachingClassrooms.size} Salones",
                                color = if (ThemeState.isDarkTheme) ThemeColors.textPrimary else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enseñas a ${studentsList.size} estudiantes en total",
                                color = if (ThemeState.isDarkTheme) ThemeColors.textSecondary else Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // INTERACTIVE ANALYTICS CARD (Pie Chart of Task Submissions)
        if (myTasks.isNotEmpty()) {
            val selectedTask = selectedTaskForStats ?: myTasks.first()

            // Calculate target students for selected task
            val targetStudents = remember(selectedTask, studentsList) {
                studentsList.filter { student ->
                    val studentClass = "${student.grade}${student.section}"
                    if (selectedTask.targetType == "GLOBAL") {
                        selectedTask.targetClassrooms.isEmpty() || selectedTask.targetClassrooms.contains(studentClass)
                    } else {
                        student.grade == selectedTask.grade && student.section == selectedTask.section
                    }
                }
            }

            val totalCount = targetStudents.size.coerceAtLeast(1) // Avoid division by zero
            val completedCount = targetStudents.count { selectedTask.completedBy.contains(it.uid) }
            val missingCount = (targetStudents.size - completedCount).coerceAtLeast(0)

            val deliveredPercentage = ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt()
            val missingPercentage = 100 - deliveredPercentage

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-20).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rendimiento de Tareas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackTertiary
                    )
                    Text(
                        text = "Selecciona para ver gráfico",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable row of tasks to select
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    myTasks.forEach { task ->
                        val isSelected = selectedTask.id == task.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) RedPrimary else BackgroundGray,
                            modifier = Modifier.clickable { selectedTaskForStats = task }
                        ) {
                            Text(
                                text = task.title,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else BlackTertiary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Graph Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Tarea: ${selectedTask.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BlackTertiary
                        )
                        Text(
                            text = "Destinatarios: " + if (selectedTask.targetType == "GLOBAL") "Todas tus aulas enseñadas" else "${selectedTask.grade}° '${selectedTask.section}'",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Donut Chart
                            Box(
                                modifier = Modifier.size(130.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 14.dp.toPx()
                                    val diameter = size.minDimension - strokeWidth
                                    val rect = androidx.compose.ui.geometry.Rect(
                                        left = strokeWidth / 2,
                                        top = strokeWidth / 2,
                                        right = strokeWidth / 2 + diameter,
                                        bottom = strokeWidth / 2 + diameter
                                    )
                                    
                                    val sweepDelivered = (deliveredPercentage.toFloat() / 100f) * 360f
                                    val sweepMissing = 360f - sweepDelivered
                                    
                                    // Arc delivered (Green)
                                    drawArc(
                                        color = Color(0xFF4CAF50),
                                        startAngle = -90f,
                                        sweepAngle = sweepDelivered,
                                        useCenter = false,
                                        topLeft = rect.topLeft,
                                        size = rect.size,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = strokeWidth,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                    
                                    // Arc missing (RedPrimary)
                                    drawArc(
                                        color = RedPrimary,
                                        startAngle = -90f + sweepDelivered,
                                        sweepAngle = sweepMissing,
                                        useCenter = false,
                                        topLeft = rect.topLeft,
                                        size = rect.size,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = strokeWidth,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$deliveredPercentage%",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = "Entregado",
                                        fontSize = 9.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            // Legend and numbers
                            Column(
                                modifier = Modifier.padding(start = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Entregadas: $completedCount ($deliveredPercentage%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BlackTertiary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(RedPrimary))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Faltantes: $missingCount ($missingPercentage%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BlackTertiary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Gray))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total Alumnos: ${targetStudents.size}",
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = DividerGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini monitor of students completion status
                        Text(
                            text = "Listado de Alumnos y Entrega:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BlackTertiary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (targetStudents.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                targetStudents.take(10).forEach { s ->
                                    val completed = selectedTask.completedBy.contains(s.uid)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(BackgroundGray, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = s.studentName.ifBlank { "Estudiante" },
                                            fontSize = 11.sp,
                                            color = BlackTertiary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (completed) Color(0xFFE8F5E9) else RedPrimary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = if (completed) "Entregado" else "Pendiente",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (completed) Color(0xFF2E7D32) else RedPrimary
                                            )
                                        }
                                    }
                                }
                                if (targetStudents.size > 10) {
                                    Text(
                                        text = "+ ${targetStudents.size - 10} alumnos más...",
                                        fontSize = 10.sp,
                                        color = TextGray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Aún no hay alumnos registrados en el aula correspondiente a esta tarea.",
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }
        } else {
            // Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .offset(y = (-20).dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.AssignmentLate, contentDescription = null, tint = TextGray, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sin tareas asignadas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BlackTertiary
                    )
                    Text(
                        text = "Asigna una tarea en la herramienta 'Tareas' para visualizar estadísticas de entrega.",
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun TeacherBottomNavigation(
    selectedTab: String,
    hasTutoria: Boolean,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = ThemeColors.surface,
        contentColor = ThemeColors.textSecondary,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Inicio") },
            label = { Text("Inicio", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            selected = selectedTab == "Inicio",
            onClick = { onTabSelected("Inicio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ThemeColors.primary,
                selectedTextColor = ThemeColors.primary,
                indicatorColor = ThemeColors.cardSurface,
                unselectedIconColor = ThemeColors.textSecondary,
                unselectedTextColor = ThemeColors.textSecondary
            )
        )

        if (hasTutoria) {
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Chat, contentDescription = "Tutoría Chat") },
                label = { Text("Tutoría", fontSize = 10.sp) },
                selected = selectedTab == "Mensajes",
                onClick = { onTabSelected("Mensajes") },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ThemeColors.primary,
                    selectedTextColor = ThemeColors.primary,
                    indicatorColor = ThemeColors.cardSurface,
                    unselectedIconColor = ThemeColors.textSecondary,
                    unselectedTextColor = ThemeColors.textSecondary
                )
            )
        }

        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
            label = { Text("Perfil", fontSize = 10.sp) },
            selected = selectedTab == "Perfil",
            onClick = { onTabSelected("Perfil") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ThemeColors.primary,
                selectedTextColor = ThemeColors.primary,
                indicatorColor = ThemeColors.cardSurface,
                unselectedIconColor = ThemeColors.textSecondary,
                unselectedTextColor = ThemeColors.textSecondary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAnnouncementsScreen(
    teachingClassrooms: List<String>,
    teacherId: String,
    teacherName: String,
    onBack: () -> Unit
) {
    val repository = remember { AnnouncementRepository() }
    val announcements by repository.getAnnouncementsFlow().collectAsState(initial = emptyList())
    val myAnnouncements = remember(announcements, teacherId) { announcements.filter { it.teacherId == teacherId } }

    var showCreateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunicados y Avisos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BlackTertiary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = BlackTertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = RedPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo Aviso", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(innerPadding)
        ) {
            if (myAnnouncements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No has publicado avisos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BlackTertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Presiona 'Nuevo Aviso' para comunicar novedades a tus aulas.",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(myAnnouncements, key = { it.id }) { announcement ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = when (announcement.priority) {
                                            "URGENTE" -> RedPrimary.copy(alpha = 0.1f)
                                            "IMPORTANTE" -> YellowSecondary.copy(alpha = 0.3f)
                                            else -> BackgroundGray
                                        }
                                    ) {
                                        Text(
                                            text = announcement.priority,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (announcement.priority) {
                                                "URGENTE" -> RedPrimary
                                                "IMPORTANTE" -> Color(0xFFB8860B)
                                                else -> BlackTertiary
                                            }
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                repository.deleteAnnouncement(announcement.id)
                                                snackbarHostState.showSnackbar("Aviso eliminado")
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = announcement.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BlackTertiary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = announcement.content,
                                    fontSize = 13.sp,
                                    color = BlackTertiary.copy(alpha = 0.8f),
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = DividerGray.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Para: " + if (announcement.targetType == "GLOBAL") "Todas tus aulas" else "${announcement.grade}° '${announcement.section}'",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RedPrimary
                                    )
                                    Text(
                                        text = announcement.subject,
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAnnouncementDialog(
            teachingClassrooms = teachingClassrooms,
            onDismiss = { showCreateDialog = false },
            onCreate = { title, content, subject, priority, targetType, grade, section ->
                coroutineScope.launch {
                    repository.createAnnouncement(
                        title = title,
                        content = content,
                        teacherId = teacherId,
                        teacherName = teacherName,
                        subject = subject,
                        targetType = targetType,
                        grade = grade,
                        section = section,
                        targetClassrooms = if (targetType == "GLOBAL") teachingClassrooms else emptyList(),
                        priority = priority
                    )
                    showCreateDialog = false
                    snackbarHostState.showSnackbar("¡Comunicado publicado con éxito! 📢")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnouncementDialog(
    teachingClassrooms: List<String>,
    onDismiss: () -> Unit,
    onCreate: (title: String, content: String, subject: String, priority: String, targetType: String, grade: String, section: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }
    var priority by remember { mutableStateOf("NORMAL") }
    var targetType by remember { mutableStateOf("SPECIFIC") } // "SPECIFIC" or "GLOBAL"
    var grade by remember { mutableStateOf("1") }
    var section by remember { mutableStateOf("A") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publicar Nuevo Comunicado", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del comunicado") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DividerGray,
                        focusedTextColor = BlackTertiary,
                        unfocusedTextColor = BlackTertiary
                    )
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Mensaje / Contenido") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DividerGray,
                        focusedTextColor = BlackTertiary,
                        unfocusedTextColor = BlackTertiary
                    )
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Materia o Categoría (ej. Matemáticas, Tutoría)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DividerGray,
                        focusedTextColor = BlackTertiary,
                        unfocusedTextColor = BlackTertiary
                    )
                )

                Text("Prioridad del Comunicado", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextGray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("NORMAL", "IMPORTANTE", "URGENTE").forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (priority == p) RedPrimary else BackgroundGray,
                            modifier = Modifier.clickable { priority = p }
                        ) {
                            Text(
                                text = p,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (priority == p) Color.White else BlackTertiary
                            )
                        }
                    }
                }

                HorizontalDivider(color = DividerGray.copy(alpha = 0.5f))

                // Target Options
                Text("Destinatarios", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BlackTertiary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { targetType = "SPECIFIC" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (targetType == "SPECIFIC") RedPrimary else BackgroundGray,
                            contentColor = if (targetType == "SPECIFIC") Color.White else BlackTertiary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Por Aula", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { targetType = "GLOBAL" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (targetType == "GLOBAL") RedPrimary else BackgroundGray,
                            contentColor = if (targetType == "GLOBAL") Color.White else BlackTertiary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Todas mis aulas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (targetType == "SPECIFIC") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Grado del Comunicado", fontSize = 11.sp, color = TextGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("1", "2", "3", "4", "5").forEach { g ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (grade == g) RedPrimary else BackgroundGray,
                                    modifier = Modifier.clickable { grade = g }
                                ) {
                                    Text(
                                        text = "${g}°",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (grade == g) Color.White else BlackTertiary
                                    )
                                }
                            }
                        }

                        Text("Sección del Comunicado", fontSize = 11.sp, color = TextGray)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("A", "B", "C", "D", "E", "F", "G", "H").forEach { s ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (section == s) RedPrimary else BackgroundGray,
                                    modifier = Modifier.clickable { section = s }
                                ) {
                                    Text(
                                        text = s,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (section == s) Color.White else BlackTertiary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(YellowSecondary.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Text("Aulas alcanzadas por este comunicado:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8860B))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (teachingClassrooms.isEmpty()) "Aún no tienes aulas registradas en tu perfil." else teachingClassrooms.joinToString(", "),
                            fontSize = 11.sp,
                            color = BlackTertiary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, content, subject, priority, targetType, grade, section) },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Publicar Comunicado", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextGray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherClassroomsScreen(
    teachingClassrooms: List<String>,
    selectedClassroom: String,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var activeClassroom by remember { mutableStateOf(if (selectedClassroom.isNotEmpty()) selectedClassroom else if (teachingClassrooms.isNotEmpty()) teachingClassrooms.first() else "") }
    var classroomStudents by remember { mutableStateOf<List<TeacherStudentModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(activeClassroom) {
        if (activeClassroom.isNotEmpty()) {
            isLoading = true
            val grade = activeClassroom.take(1)
            val section = activeClassroom.drop(1)
            classroomStudents = fetchStudentsRobust(db, grade, section).sortedBy { it.studentName }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Salones y Alumnos", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BlackTertiary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = BlackTertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(innerPadding)
        ) {
            if (teachingClassrooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Configura tus aulas registradas en tu Perfil para verlas aquí.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                        color = TextGray
                    )
                }
            } else {
                // Horizontal list of classrooms
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    teachingClassrooms.forEach { cl ->
                        val isSel = activeClassroom == cl
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) RedPrimary else BackgroundGray,
                            modifier = Modifier.clickable { activeClassroom = cl }
                        ) {
                            Text(
                                text = "Aula $cl",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else BlackTertiary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RedPrimary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Lista de Estudiantes (Total: ${classroomStudents.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BlackTertiary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (classroomStudents.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Text(
                                    text = "No hay estudiantes registrados en este salón todavía.",
                                    modifier = Modifier.padding(24.dp),
                                    color = TextGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(classroomStudents) { student ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(YellowSecondary.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.Person, contentDescription = null, tint = BlackTertiary)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = student.studentName.ifBlank { "Estudiante" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = BlackTertiary
                                                )
                                                Text(
                                                    text = "Nombre de usuario: @${student.displayName}",
                                                    fontSize = 12.sp,
                                                    color = TextGray
                                                )
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

@Composable
fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
fun TeacherDashboardTopDecoration(modifier: Modifier = Modifier) {
    val primaryColor = ThemeColors.primary
    val secondaryColor = YellowSecondary
    val bgColor = ThemeColors.background

    Canvas(modifier = modifier.fillMaxWidth().height(260.dp)) {
        val w = size.width
        val h = size.height
        
        // Base
        drawRect(bgColor)
        
        // Yellow accent shape
        val yellowPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.6f)
            quadraticTo(w * 0.5f, h * 0.7f, 0f, h * 0.9f)
            close()
        }
        drawPath(yellowPath, secondaryColor)

        // Primary foreground shape
        val primaryPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.55f)
            quadraticTo(w * 0.5f, h * 0.65f, 0f, h * 0.85f)
            close()
        }
        drawPath(primaryPath, primaryColor)
    }
}
