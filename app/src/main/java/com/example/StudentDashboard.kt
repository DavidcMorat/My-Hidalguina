package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.dashboard.AchievementsDialog
import com.example.dashboard.ProgressDialog
import com.example.tasks.data.TaskModel
import com.example.tasks.data.TaskRepository
import com.example.tasks.ui.TasksScreen
import com.example.materials.ui.MaterialsScreen
import com.example.tutor.data.StudyDatabase
import com.example.tutor.data.StudyPlanWithTopics
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

import com.example.ui.components.AnimatedGlassBackground


@Composable
fun StudentDashboard(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLogout: () -> Unit = {}
) {
        val context = LocalContext.current
        val user = FirebaseAuth.getInstance().currentUser
        val currentUserId = user?.uid ?: ""

    var selectedTab by remember { mutableStateOf("Inicio") }
    var tutorInitialTab by remember { mutableStateOf(0) }
    var selectedChatUser by remember { mutableStateOf<com.example.chat.ChatUser?>(null) }
    var showTasksScreen by remember { mutableStateOf(false) }
    var showMaterialsScreen by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }

    // User profile and role
    var role by remember { mutableStateOf("estudiante") }
    var displayName by remember { mutableStateOf(user?.displayName ?: "Usuario") }
    var studentGrade by remember { mutableStateOf("") }
    var studentSection by remember { mutableStateOf("") }

    // Local announcements from Room
    val localAnnouncementDao = remember { StudyDatabase.getDatabase(context).localAnnouncementDao() }
    val localAnnouncements by localAnnouncementDao.getAllLocalAnnouncements().collectAsState(initial = emptyList())

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val sharedPrefs = context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)
            role = sharedPrefs.getString("role_backup_$currentUserId", "estudiante") ?: "estudiante"
            displayName = sharedPrefs.getString("displayName_backup_$currentUserId", user?.displayName ?: "Usuario") ?: "Usuario"
            studentGrade = sharedPrefs.getString("grade_backup_$currentUserId", "") ?: ""
            studentSection = sharedPrefs.getString("section_backup_$currentUserId", "") ?: ""

            try {
                val db = FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(currentUserId).get().await()
                val fRole = doc.getString("role") ?: "estudiante"
                val fDisplayName = doc.getString("displayName") ?: (user?.displayName ?: "Usuario")
                role = fRole
                displayName = fDisplayName
                studentGrade = doc.getString("grade") ?: studentGrade
                studentSection = doc.getString("section") ?: studentSection
            } catch (e: Exception) {
                // Ignore fallback to local prefs
            }

            // Sync announcements from Firebase ONCE per dashboard launch to save reads
            try {
                val firestoreDb = FirebaseFirestore.getInstance()
                val snapshot = firestoreDb.collection("announcements").get().await()
                val announcementsList = snapshot.documents.mapNotNull { doc ->
                    val rawClassrooms = (doc.get("targetClassrooms") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    com.example.tutor.data.LocalAnnouncement(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        teacherId = doc.getString("teacherId") ?: "",
                        teacherName = doc.getString("teacherName") ?: "Docente",
                        subject = doc.getString("subject") ?: "General",
                        targetType = doc.getString("targetType") ?: "GLOBAL",
                        grade = doc.getString("grade") ?: "",
                        section = doc.getString("section") ?: "",
                        priority = doc.getString("priority") ?: "NORMAL",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                }
                if (announcementsList.isNotEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        localAnnouncementDao.clearAll()
                        localAnnouncementDao.insertAnnouncements(announcementsList)
                    }
                }
            } catch (e: Exception) {
                // Offline fallback - will use the existing announcements flow from database
            }
        }
    }

    // Real Study Plans from Room
    val studyPlanDao = remember { StudyDatabase.getDatabase(context).studyPlanDao() }
    val studyPlans by studyPlanDao.getStudyPlansWithTopics(currentUserId).collectAsState(initial = emptyList())

    // Real Tasks from Firestore
    val taskRepository = remember { TaskRepository() }
    val tasks by taskRepository.getTasksFlow().collectAsState(initial = emptyList())

    // Filter tasks and announcements for the current student's classroom
    val studentClassroom = "$studentGrade$studentSection"
    val visibleTasksForStudent = remember(tasks, studentGrade, studentSection) {
        tasks.filter { task ->
            val matchSpecific = task.targetType == "SPECIFIC" && task.grade == studentGrade && task.section == studentSection
            val matchGlobal = task.targetType == "GLOBAL" && (task.targetClassrooms.isEmpty() || task.targetClassrooms.contains(studentClassroom))
            matchSpecific || matchGlobal
        }
    }

    val visibleAnnouncements = remember(localAnnouncements, studentGrade, studentSection) {
        localAnnouncements.filter { announcement ->
            val matchSpecific = announcement.targetType == "SPECIFIC" && announcement.grade == studentGrade && announcement.section == studentSection
            val matchGlobal = announcement.targetType == "GLOBAL"
            matchSpecific || matchGlobal
        }
    }

    // Computed real statistics (no dummy numbers)
    val completedTopicsCount = studyPlans.sumOf { it.topics.count { t -> t.status == "LOGRADO" || t.status == "COMPLETED" } }
    val completedTasksCount = visibleTasksForStudent.count { it.completedBy.contains(currentUserId) }
    val calculatedScore = (completedTopicsCount * 100) + (completedTasksCount * 50)
    val calculatedLevel = 1 + (calculatedScore / 250)
    val progressToNextLevel = ((calculatedScore % 250).toFloat() / 250f).coerceIn(0f, 1f)

    if (role == "docente") {
        TeacherDashboard(
            modifier = modifier,
            authViewModel = authViewModel,
            onLogout = onLogout
        )
        return
    }

    if (showProgressDialog) {
        ProgressDialog(
            plans = studyPlans,
            tasks = tasks,
            currentUserId = currentUserId,
            onDismiss = { showProgressDialog = false }
        )
    }

    if (showAchievementsDialog) {
        AchievementsDialog(
            plans = studyPlans,
            tasks = tasks,
            currentUserId = currentUserId,
            onDismiss = { showAchievementsDialog = false }
        )
    }
    AnimatedGlassBackground {

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = { 
            if (selectedChatUser == null && !showTasksScreen && !showMaterialsScreen) {
                StudentBottomNavigation(selectedTab) { selectedTab = it }
            }
        }
    ) { innerPadding ->
        if (showTasksScreen) {
            TasksScreen(
                modifier = Modifier.padding(innerPadding),
                isTeacher = (role == "docente"),
                onBack = { showTasksScreen = false }
            )
        } else if (showMaterialsScreen) {
            MaterialsScreen(
                modifier = Modifier.padding(innerPadding),
                isTeacher = false,
                studentGrade = studentGrade,
                studentSection = studentSection,
                onBack = { showMaterialsScreen = false }
            )
        } else if (selectedTab == "TutorIA") {
            com.example.tutor.ui.AITutorScreen(
                modifier = Modifier.padding(innerPadding),
                initialTab = tutorInitialTab,
                onBack = { selectedTab = "Inicio" }
            )
        } else if (selectedTab == "Mensajes") {
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
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
                                color = if (role == "docente") YellowSecondary else Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (role == "docente") "Panel Docente" else "Panel Estudiante",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (role == "docente") BlackTertiary else Color.White
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { ThemeState.toggleTheme() }) {
                                    Icon(
                                        painter = painterResource(id = if (ThemeState.isDarkTheme) android.R.drawable.ic_menu_day else android.R.drawable.ic_menu_compass),
                                        contentDescription = "Cambiar Modo Oscuro",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { /* Notificaciones */ }) {
                                    Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = Color.White)
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
                                    .border(3.dp, if (role == "docente") YellowSecondary else Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (role == "docente") Icons.Filled.School else Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = "¡Hola, $displayName!",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (role == "docente") "Gestiona las tareas y actividades escolares de tus estudiantes." else "Aprende a tu ritmo con la IA y entrega tus actividades a tiempo.",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
                
                // Real Score and Level Cards
                val accentAmber = if (ThemeState.isDarkTheme) YellowSecondary else Color(0xFFD97706)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ThemeState.isDarkTheme) ThemeColors.cardSurface else BlackTertiary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Puntaje Académico", color = if (ThemeState.isDarkTheme) ThemeColors.textSecondary else Color.White, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$calculatedScore", color = accentAmber, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Star, contentDescription = null, tint = accentAmber, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nivel $calculatedLevel", color = if (ThemeState.isDarkTheme) ThemeColors.textPrimary else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${(progressToNextLevel * 100).toInt()}%", color = accentAmber, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progressToNextLevel },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = accentAmber,
                                trackColor = if (ThemeState.isDarkTheme) ThemeColors.divider else Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = ThemeColors.cardSurface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (ThemeState.isDarkTheme) YellowSecondary.copy(alpha = 0.2f) else Color(0xFFFEF3C7))
                                    .border(1.5.dp, accentAmber, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = accentAmber, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (calculatedScore > 0) "¡Gran avance!" else "¡Bienvenido!",
                                    color = ThemeColors.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (calculatedScore > 0) "$completedTopicsCount temas y $completedTasksCount tareas logradas." else "Comienza tu primer tema de estudio hoy.",
                                    color = ThemeColors.textSecondary,
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
                
                // UNIFIED TOOLS SECTION (2x2 Grid)
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
                        color = ThemeColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Row 1: Unified Aprendizaje (IA + Planes) & Tareas Escolares
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ToolCard(
                            title = "Aprendizaje",
                            subtitle = "Tutor IA y Planes\nde estudio guiados",
                            badgeText = "IA Integrada",
                            icon = Icons.Filled.AutoAwesome,
                            backgroundColor = ThemeColors.primary,
                            contentColor = ThemeColors.onPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tutorInitialTab = 0
                                selectedTab = "TutorIA"
                            }
                        )
                        ToolCard(
                            title = if (role == "docente") "Gestionar Tareas" else "Tareas",
                            subtitle = if (role == "docente") "Crea y asigna\nactividades" else "Revisa y entrega\ntus actividades",
                            badgeText = if (visibleTasksForStudent.isNotEmpty()) "${visibleTasksForStudent.count { !it.completedBy.contains(currentUserId) }} activas" else null,
                            icon = Icons.Filled.Assignment,
                            backgroundColor = if (ThemeState.isDarkTheme) ThemeColors.cardSurface else BlackTertiary,
                            contentColor = if (ThemeState.isDarkTheme) ThemeColors.textPrimary else Color.White,
                            modifier = Modifier.weight(1f),
                            onClick = { showTasksScreen = true }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2: Materiales & Mi progreso
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ToolCard(
                            title = "Materiales",
                            subtitle = "Guías, PDFs y\nrecursos de clase",
                            badgeText = "Recursos",
                            icon = Icons.Filled.Folder,
                            backgroundColor = if (ThemeState.isDarkTheme) ThemeColors.cardSurface else YellowSecondary,
                            contentColor = if (ThemeState.isDarkTheme) ThemeColors.textPrimary else BlackTertiary,
                            modifier = Modifier.weight(1f),
                            onClick = { showMaterialsScreen = true }
                        )
                        ToolCard(
                            title = "Mi progreso",
                            subtitle = "Revisa tu avance\ny estadísticas",
                            icon = Icons.Filled.BarChart,
                            backgroundColor = if (ThemeState.isDarkTheme) ThemeColors.cardSurface else BlackTertiary,
                            contentColor = if (ThemeState.isDarkTheme) ThemeColors.textPrimary else Color.White,
                            modifier = Modifier.weight(1f),
                            onClick = { showProgressDialog = true }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 3: Logros y Medallas
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAchievementsDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (ThemeState.isDarkTheme) ThemeColors.cardSurface else ThemeColors.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.divider)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(YellowSecondary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = YellowSecondary, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Medallas y Logros",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = ThemeColors.textPrimary
                                    )
                                    Text(
                                        text = "Desbloquea insignias cumpliendo metas de estudio",
                                        fontSize = 11.sp,
                                        color = ThemeColors.textSecondary
                                    )
                                }
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ThemeColors.textSecondary)
                        }
                    }
                }
                
                // Real Upcoming Activities Section (From Firestore Tasks)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // SECTION 1: Avisos de Profesores (Cargados de Room localmente)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Avisos de Profesores",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.textPrimary
                        )
                        if (visibleAnnouncements.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.OfflinePin, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${visibleAnnouncements.size} locales", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (visibleAnnouncements.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ThemeColors.cardSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Campaign, contentDescription = null, tint = ThemeColors.textSecondary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "No hay comunicados o avisos recientes guardados.",
                                    fontSize = 12.sp,
                                    color = ThemeColors.textSecondary
                                )
                            }
                        }
                    } else {
                        visibleAnnouncements.take(3).forEach { announcement ->
                            ExpandableAnnouncementItem(announcement = announcement)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // SECTION 2: Tareas de Profesores (Filtradas por su aula)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tareas y Actividades",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.textPrimary
                        )
                        if (visibleTasksForStudent.isNotEmpty()) {
                            TextButton(onClick = { showTasksScreen = true }) {
                                Text("Ver todas (${visibleTasksForStudent.size})", color = ThemeColors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (visibleTasksForStudent.isEmpty()) {
                        // Clean slate state without dummy data
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = ThemeColors.cardSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.AssignmentLate,
                                    contentDescription = null,
                                    tint = ThemeColors.textSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (role == "docente") "No has asignado actividades escolares aún" else "No hay actividades escolares asignadas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ThemeColors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (role == "docente") "Crea la primera tarea para que tus alumnos puedan entregarla." else "Las tareas que tus profesores publiquen aparecerán aquí en tiempo real.",
                                    fontSize = 12.sp,
                                    color = ThemeColors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                                if (role == "docente") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showTasksScreen = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.primary, contentColor = ThemeColors.onPrimary)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Crear Tarea Ahora", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ThemeColors.onPrimary)
                                    }
                                }
                            }
                        }
                    } else {
                        // Show top 3 actual tasks
                        visibleTasksForStudent.take(3).forEach { task ->
                            val isCompleted = task.completedBy.contains(currentUserId)
                            RealTaskActivityItem(
                                task = task,
                                isCompleted = isCompleted,
                                onClick = { showTasksScreen = true }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
}


@Composable
fun ToolCard(
    badgeText: String? = null,
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
            .height(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                }

                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = contentColor.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }
            
            Column {
                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = contentColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}


@Composable
fun ExpandableAnnouncementItem(announcement: com.example.tutor.data.LocalAnnouncement) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ThemeColors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = ThemeColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = announcement.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.textPrimary
                        )
                        Text(
                            text = "${announcement.subject} • ${announcement.teacherName}",
                            fontSize = 11.sp,
                            color = ThemeColors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (announcement.priority) {
                        "URGENTE" -> ThemeColors.primary.copy(alpha = 0.15f)
                        "IMPORTANTE" -> YellowSecondary.copy(alpha = 0.3f)
                        else -> ThemeColors.background
                    }
                ) {
                    Text(
                        text = announcement.priority,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (announcement.priority) {
                            "URGENTE" -> ThemeColors.primary
                            "IMPORTANTE" -> Color(0xFFB8860B)
                            else -> ThemeColors.textPrimary
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = announcement.content,
                fontSize = 12.sp,
                color = ThemeColors.textPrimary.copy(alpha = 0.85f),
                maxLines = if (isExpanded) 100 else 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Toca para contraer" else "Toca para leer más",
                    fontSize = 10.sp,
                    color = ThemeColors.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = "Guardado localmente",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guardado local",
                        fontSize = 10.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}


@Composable
fun RealTaskActivityItem(
    task: TaskModel,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColors.cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isCompleted) Color(0xFFE8F5E9) else ThemeColors.primary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Assignment,
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF2E7D32) else ThemeColors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColors.textPrimary
                )
                Text(
                    text = "${task.subject} • Docente: ${task.teacherName.ifBlank { "Profesor" }}",
                    fontSize = 11.sp,
                    color = ThemeColors.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (task.dueDate.isNotBlank()) task.dueDate else "Pendiente",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) Color(0xFF2E7D32) else ThemeColors.primary
                )
                Text(
                    text = if (isCompleted) "Entregada" else "Por entregar",
                    fontSize = 10.sp,
                    color = ThemeColors.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ThemeColors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
    }


@Composable
fun StudentBottomNavigation(selectedTab: String, onTabSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = ThemeColors.surface, // this is our glass surface
            border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.divider),
            shadowElevation = 8.dp,
            modifier = Modifier.height(72.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Filled.Home,
                    label = "Inicio",
                    selected = selectedTab == "Inicio",
                    onClick = { onTabSelected("Inicio") },
                    modifier = Modifier.weight(1f)
                )
                
                // Central FAB-like item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = (-8).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ThemeColors.primary)
                            .clickable { onTabSelected("TutorIA") }
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Tutor IA",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                NavBarItem(
                    icon = Icons.Filled.Chat,
                    label = "Mensajes",
                    selected = selectedTab == "Mensajes",
                    onClick = { onTabSelected("Mensajes") },
                    modifier = Modifier.weight(1f)
                )
                
                NavBarItem(
                    icon = Icons.Filled.Person,
                    label = "Perfil",
                    selected = selectedTab == "Perfil",
                    onClick = { onTabSelected("Perfil") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) ThemeColors.primary else ThemeColors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) ThemeColors.primary else ThemeColors.textSecondary
        )
    }
}


@Composable
fun DashboardTopDecoration(modifier: Modifier = Modifier) {
    val primaryColor = ThemeColors.primary
    val secondaryColor = YellowSecondary
    val bgColor = ThemeColors.background

    Canvas(modifier = modifier.fillMaxWidth().height(280.dp)) {
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
