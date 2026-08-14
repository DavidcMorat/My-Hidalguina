package com.example.tasks.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tasks.data.TaskModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    modifier: Modifier = Modifier,
    tasksViewModel: TasksViewModel = viewModel(),
    isTeacher: Boolean = false,
    onBack: () -> Unit = {}
) {
    val tasks by tasksViewModel.tasks.collectAsState()
    val filterTab by tasksViewModel.filterTab.collectAsState()
    val showCreateDialog by tasksViewModel.showCreateDialog.collectAsState()
    val currentUid = tasksViewModel.currentUserId
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    var studentGrade by remember { mutableStateOf("") }
    var studentSection by remember { mutableStateOf("") }
    var teacherClassrooms by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(currentUid) {
        if (currentUid.isNotEmpty()) {
            val sharedPrefs = context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)
            studentGrade = sharedPrefs.getString("grade_backup_$currentUid", "") ?: ""
            studentSection = sharedPrefs.getString("section_backup_$currentUid", "") ?: ""
            val classroomsSet = sharedPrefs.getStringSet("teachingClassrooms_backup_$currentUid", emptySet<String>()) ?: emptySet<String>()
            teacherClassrooms = classroomsSet.toList().sorted()

            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(currentUid).get().await()
                studentGrade = doc.getString("grade") ?: studentGrade
                studentSection = doc.getString("section") ?: studentSection
                val fClassrooms = (doc.get("teachingClassrooms") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (fClassrooms.isNotEmpty()) {
                    teacherClassrooms = fClassrooms.sorted()
                }
            } catch (e: Exception) {}
        }
    }

    val studentClassroom = "$studentGrade$studentSection"

    val visibleTasksForStudent = remember(tasks, studentGrade, studentSection, isTeacher, currentUid) {
        if (isTeacher) {
            tasks
        } else {
            tasks.filter { task ->
                val matchSpecific = task.targetType == "SPECIFIC" && task.grade == studentGrade && task.section == studentSection
                val matchGlobal = task.targetType == "GLOBAL" && (task.targetClassrooms.isEmpty() || task.targetClassrooms.contains(studentClassroom))
                matchSpecific || matchGlobal
            }
        }
    }

    LaunchedEffect(isTeacher) {
        tasksViewModel.setTeacherRole(isTeacher)
    }

    LaunchedEffect(Unit) {
        tasksViewModel.snackBarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val pendingTasks = remember(visibleTasksForStudent, currentUid) {
        visibleTasksForStudent.filter { !it.completedBy.contains(currentUid) }
    }
    val completedTasks = remember(visibleTasksForStudent, currentUid) {
        visibleTasksForStudent.filter { it.completedBy.contains(currentUid) }
    }
    val teacherTasks = remember(tasks, currentUid) {
        tasks.filter { it.teacherId == currentUid }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isTeacher) "Gestión de Tareas Docente" else "Tareas y Actividades",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.textPrimary
                        )
                        Text(
                            text = if (isTeacher) "Asigna y supervisa actividades escolares" else "Revisa y entrega tus tareas a tiempo",
                            fontSize = 12.sp,
                            color = ThemeColors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = ThemeColors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeColors.surface)
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                ExtendedFloatingActionButton(
                    onClick = { tasksViewModel.openCreateDialog() },
                    containerColor = ThemeColors.primary,
                    contentColor = ThemeColors.onPrimary,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = ThemeColors.onPrimary) },
                    text = { Text("Asignar Tarea", fontWeight = FontWeight.Bold, color = ThemeColors.onPrimary) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.background)
                .padding(innerPadding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = filterTab,
                containerColor = ThemeColors.surface,
                contentColor = ThemeColors.primary
            ) {
                Tab(
                    selected = filterTab == 0,
                    onClick = { tasksViewModel.setFilterTab(0) },
                    text = {
                        Text(
                            "Pendientes (${pendingTasks.size})",
                            fontWeight = if (filterTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (filterTab == 0) ThemeColors.primary else ThemeColors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = filterTab == 1,
                    onClick = { tasksViewModel.setFilterTab(1) },
                    text = {
                        Text(
                            "Entregadas (${completedTasks.size})",
                            fontWeight = if (filterTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (filterTab == 1) ThemeColors.primary else ThemeColors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                )
                if (isTeacher) {
                    Tab(
                        selected = filterTab == 2,
                        onClick = { tasksViewModel.setFilterTab(2) },
                        text = {
                            Text(
                                "Mis Asignaciones (${teacherTasks.size})",
                                fontWeight = if (filterTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (filterTab == 2) ThemeColors.primary else ThemeColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            val displayedList = when (filterTab) {
                0 -> pendingTasks
                1 -> completedTasks
                2 -> teacherTasks
                else -> pendingTasks
            }

            if (displayedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(RedPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (filterTab == 1) Icons.Filled.CheckCircle else Icons.Filled.Assignment,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (filterTab) {
                                0 -> "¡Estás al día!"
                                1 -> "No has entregado tareas aún"
                                else -> "No has creado tareas"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = BlackTertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (filterTab) {
                                0 -> "No tienes actividades pendientes por entregar asignadas por tus profesores."
                                1 -> "Las tareas que marques como entregadas aparecerán aquí para tu registro académico."
                                else -> "Toca el botón 'Asignar Tarea' para publicar una actividad para tus estudiantes."
                            },
                            fontSize = 13.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedList, key = { it.id }) { task ->
                        val isDone = task.completedBy.contains(currentUid)
                        TaskCardItem(
                            task = task,
                            isCompleted = isDone,
                            isTeacher = isTeacher,
                            isCreator = task.teacherId == currentUid,
                            onToggleComplete = { tasksViewModel.toggleTaskCompletion(task.id, !isDone) },
                            onDelete = { tasksViewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            teacherClassrooms = teacherClassrooms,
            onDismiss = { tasksViewModel.closeCreateDialog() },
            onCreate = { title, desc, subject, dueDate, targetType, grade, section, classrooms ->
                tasksViewModel.createTask(
                    title = title,
                    description = desc,
                    subject = subject,
                    dueDate = dueDate,
                    targetType = targetType,
                    grade = grade,
                    section = section,
                    targetClassrooms = classrooms
                )
            }
        )
    }
}

@Composable
fun TaskCardItem(
    task: TaskModel,
    isCompleted: Boolean,
    isTeacher: Boolean,
    isCreator: Boolean,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (task.subject.lowercase()) {
                        "matemáticas", "matematicas" -> RedPrimary.copy(alpha = 0.12f)
                        "historia" -> YellowSecondary.copy(alpha = 0.25f)
                        "ciencias", "química", "física" -> Color(0xFFE8F5E9)
                        else -> BlackTertiary.copy(alpha = 0.08f)
                    }
                ) {
                    Text(
                        text = task.subject,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (task.subject.lowercase()) {
                            "matemáticas", "matematicas" -> RedPrimary
                            "historia" -> Color(0xFFB8860B)
                            "ciencias", "química", "física" -> Color(0xFF2E7D32)
                            else -> BlackTertiary
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (task.dueDate.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Entrega: ${task.dueDate}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray
                        )
                    }
                }

                if (isCreator) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BlackTertiary
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.description,
                    fontSize = 13.sp,
                    color = BlackTertiary.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DividerGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Prof: ${task.teacherName}",
                    fontSize = 12.sp,
                    color = TextGray
                )

                if (!isTeacher) {
                    Button(
                        onClick = onToggleComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) Color(0xFF4CAF50) else RedPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Filled.Check else Icons.Filled.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCompleted) "Entregada" else "Marcar Entregada",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = "${task.completedBy.size} alumno(s) completaron",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    teacherClassrooms: List<String>,
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String, subject: String, dueDate: String, targetType: String, grade: String, section: String, classrooms: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    
    // Target Selection: SPECIFIC or GLOBAL
    var targetType by remember { mutableStateOf("SPECIFIC") } // "SPECIFIC" or "GLOBAL"
    var selectedGrade by remember { mutableStateOf("1") }
    var selectedSection by remember { mutableStateOf("A") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Asignar Nueva Tarea / Actividad", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Materia (ej. Matemáticas, Historia)") },
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
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la tarea") },
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
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Instrucciones y detalles") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DividerGray,
                        focusedTextColor = BlackTertiary,
                        unfocusedTextColor = BlackTertiary
                    )
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Fecha límite (ej. Hoy 6pm, Mañana, 25 Mayo)") },
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

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = DividerGray.copy(alpha = 0.5f))

                // Target Options
                Text("Destinatarios", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlackTertiary)
                
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
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("Por Aula (Específico)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { targetType = "GLOBAL" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (targetType == "GLOBAL") RedPrimary else BackgroundGray,
                            contentColor = if (targetType == "GLOBAL") Color.White else BlackTertiary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("Global (Mis aulas)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (targetType == "SPECIFIC") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Selecciona el Grado:", fontSize = 12.sp, color = TextGray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("1", "2", "3", "4", "5").forEach { g ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedGrade == g) RedPrimary else BackgroundGray,
                                    modifier = Modifier.clickable { selectedGrade = g }
                                ) {
                                    Text(
                                        text = "${g}°",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedGrade == g) Color.White else BlackTertiary
                                    )
                                }
                            }
                        }

                        Text("Selecciona la Sección:", fontSize = 12.sp, color = TextGray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("A", "B", "C", "D", "E", "F", "G", "H").forEach { s ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedSection == s) RedPrimary else BackgroundGray,
                                    modifier = Modifier.clickable { selectedSection = s }
                                ) {
                                    Text(
                                        text = s,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedSection == s) Color.White else BlackTertiary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Global target: show list of teaching classrooms
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(YellowSecondary.copy(alpha = 0.15f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Asignación Global:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFB8860B)
                        )
                        Text(
                            text = "Esta tarea se publicará automáticamente en todas tus aulas registradas:",
                            fontSize = 11.sp,
                            color = BlackTertiary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (teacherClassrooms.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                teacherClassrooms.forEach { classroom ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DividerGray)
                                    ) {
                                        Text(
                                            text = classroom,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BlackTertiary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Aún no has seleccionado tus aulas enseñadas. Configúralas en tu Perfil.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RedPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onCreate(
                        title, 
                        desc, 
                        subject, 
                        dueDate, 
                        targetType, 
                        selectedGrade, 
                        selectedSection, 
                        teacherClassrooms
                    ) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Publicar Tarea", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextGray)
            }
        }
    )
}
