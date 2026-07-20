package com.example

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.theme.*
import com.example.ui.components.RevealButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    // Teacher profile states
    var teacherName by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var gradeStr by remember { mutableStateOf("") }
    var sectionStr by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    // Navigation and screen states
    var selectedTab by remember { mutableStateOf("Avisos") } // "Avisos" or "Alumnos"
    var selectedSalon by remember { mutableStateOf("") } // e.g. "3ro - A"
    var announcementText by remember { mutableStateOf("") }
    var isSubmittingAnnouncement by remember { mutableStateOf(false) }

    // Dynamic Lists from database
    var announcementsList by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var studentsList by remember { mutableStateOf<List<StudentInfo>>(emptyList()) }
    var isLoadingStudents by remember { mutableStateOf(false) }
    var isLoadingAnnouncements by remember { mutableStateOf(false) }

    // Load profile
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            val sharedPrefs = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
            teacherName = sharedPrefs.getString("studentName_backup_$uid", "") ?: ""
            area = sharedPrefs.getString("area_backup_$uid", "") ?: ""
            gradeStr = sharedPrefs.getString("grade_backup_$uid", "") ?: ""
            sectionStr = sharedPrefs.getString("section_backup_$uid", "") ?: ""
            username = sharedPrefs.getString("displayName_backup_$uid", currentUser.displayName ?: "") ?: ""

            // Fetch from Firestore to keep updated
            try {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc != null && doc.exists()) {
                            teacherName = doc.getString("studentName") ?: teacherName
                            area = doc.getString("area") ?: area
                            gradeStr = doc.getString("grade") ?: gradeStr
                            sectionStr = doc.getString("section") ?: sectionStr
                            username = doc.getString("displayName") ?: username

                            // Update backup
                            sharedPrefs.edit().apply {
                                putString("studentName_backup_$uid", teacherName)
                                putString("area_backup_$uid", area)
                                putString("grade_backup_$uid", gradeStr)
                                putString("section_backup_$uid", sectionStr)
                                putString("displayName_backup_$uid", username)
                                apply()
                            }
                        }
                    }
            } catch (e: Exception) {}
        }
    }

    // Process Salones (Cartesian product of grades and sections)
    val classrooms = remember(gradeStr, sectionStr) {
        val grades = gradeStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val sections = sectionStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val list = mutableListOf<String>()
        for (g in grades) {
            for (s in sections) {
                list.add("$g - $s")
            }
        }
        list
    }

    // Auto-select first classroom
    LaunchedEffect(classrooms) {
        if (!classrooms.contains(selectedSalon) && classrooms.isNotEmpty()) {
            selectedSalon = classrooms.first()
        }
    }

    // Parse active grade and section
    val activeGradeAndSection = remember(selectedSalon) {
        val parts = selectedSalon.split("-")
        val g = parts.getOrNull(0)?.trim() ?: ""
        val s = parts.getOrNull(1)?.trim() ?: ""
        Pair(g, s)
    }

    // Fetch Announcements whenever classroom changes
    LaunchedEffect(selectedSalon, activeGradeAndSection) {
        if (selectedSalon.isNotEmpty()) {
            isLoadingAnnouncements = true
            val (g, s) = activeGradeAndSection
            db.collection("announcements")
                .whereEqualTo("grade", g)
                .whereEqualTo("section", s)
                .addSnapshotListener { snapshot, error ->
                    isLoadingAnnouncements = false
                    if (snapshot != null) {
                        announcementsList = snapshot.documents.mapNotNull { doc ->
                            val text = doc.getString("text") ?: ""
                            val sender = doc.getString("senderName") ?: ""
                            val subject = doc.getString("area") ?: ""
                            val ts = doc.getLong("timestamp") ?: 0L
                            Announcement(
                                id = doc.id,
                                text = text,
                                senderName = sender,
                                area = subject,
                                timestamp = ts,
                                grade = doc.getString("grade") ?: "",
                                section = doc.getString("section") ?: ""
                            )
                        }.sortedByDescending { it.timestamp }
                    }
                }
        }
    }

    // Fetch Students whenever classroom changes
    LaunchedEffect(selectedSalon, activeGradeAndSection) {
        if (selectedSalon.isNotEmpty()) {
            isLoadingStudents = true
            val (g, s) = activeGradeAndSection
            db.collection("users")
                .whereEqualTo("role", "student")
                .addSnapshotListener { snapshot, error ->
                    isLoadingStudents = false
                    if (snapshot != null) {
                        studentsList = snapshot.documents.mapNotNull { doc ->
                            val docGrade = doc.getString("grade") ?: ""
                            val docSection = doc.getString("section") ?: ""
                            
                            // Local filter to avoid missing composite index errors
                            if (docGrade == g && docSection == s) {
                                val uid = doc.getString("uid") ?: doc.id
                                val sName = doc.getString("studentName") ?: doc.getString("displayName") ?: "Estudiante"
                                val uname = doc.getString("displayName") ?: ""
                                val points = doc.getLong("points") ?: 100L
                                StudentInfo(
                                    uid = uid,
                                    studentName = sName,
                                    username = uname,
                                    points = points.toInt(),
                                    grade = g,
                                    section = s
                                )
                            } else {
                                null
                            }
                        }
                    } else {
                        studentsList = emptyList()
                    }
                }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            TeacherBottomNavigation(selectedTab = selectedTab) { selectedTab = it }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(innerPadding)
        ) {
            // Header Card (Teacher Persona and area)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BlackTertiary)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(RedPrimary.copy(alpha = 0.2f))
                            .border(2.dp, RedPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Docente: $teacherName",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Área de $area",
                            color = YellowSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        }
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = "Cerrar Sesión", tint = Color.White)
                    }
                }
            }

            // Classroom Navigation List ("Navegación entre salones")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Selecciona tu Aula / Salón:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackTertiary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(classrooms) { salon ->
                        val isSelected = selectedSalon == salon
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) RedPrimary else Color.White)
                                .border(1.dp, if (isSelected) RedPrimary else DividerGray, RoundedCornerShape(12.dp))
                                .clickable { selectedSalon = salon }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.MeetingRoom,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else RedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = salon,
                                    color = if (isSelected) Color.White else BlackTertiary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = DividerGray, thickness = 1.dp)

            if (selectedTab == "Avisos") {
                // AVISOS PANEL (Primary feature, highlighted)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Compose announcement card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Nuevo Aviso para $selectedSalon",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = announcementText,
                                onValueChange = { announcementText = it },
                                placeholder = { Text("Escribe un aviso o comunicado para tus alumnos...", fontSize = 13.sp, color = TextGray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = DividerGray,
                                    focusedBorderColor = RedPrimary,
                                    unfocusedContainerColor = BackgroundGray,
                                    focusedContainerColor = BackgroundGray,
                                    focusedTextColor = BlackTertiary,
                                    unfocusedTextColor = BlackTertiary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (announcementText.isNotBlank() && !isSubmittingAnnouncement) {
                                RevealButton(
                                    onClick = {
                                        if (announcementText.isNotBlank()) {
                                            isSubmittingAnnouncement = true
                                            val (g, s) = activeGradeAndSection
                                            val newAnnouncement = hashMapOf(
                                                "text" to announcementText,
                                                "senderName" to teacherName,
                                                "area" to area,
                                                "grade" to g,
                                                "section" to s,
                                                "timestamp" to System.currentTimeMillis()
                                            )
                                            db.collection("announcements")
                                                .add(newAnnouncement)
                                                .addOnSuccessListener {
                                                    announcementText = ""
                                                    isSubmittingAnnouncement = false
                                                }
                                                .addOnFailureListener {
                                                    isSubmittingAnnouncement = false
                                                }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    backgroundColor = RedPrimary,
                                    revealColor = Color(0xFFFF5252),
                                    contentColor = Color.White
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Publicar Comunicado", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = Color.LightGray,
                                        disabledContentColor = TextGray
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Publicar Comunicado", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextGray)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Historial de Avisos en este Salón:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackTertiary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (isLoadingAnnouncements) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = RedPrimary)
                        }
                    } else if (announcementsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Campaign, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Aún no hay comunicados publicados", color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                                Text("Sé el primero en enviar un aviso.", color = TextGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(announcementsList) { announcement ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, DividerGray)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Campaign, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(announcement.senderName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BlackTertiary)
                                            }
                                            val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                                            Text(
                                                text = sdf.format(Date(announcement.timestamp)),
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
                    }
                }
            } else if (selectedTab == "Alumnos") {
                // LISTA DE ALUMNOS PANEL (Classroom view)
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
                        Text(
                            text = "Estudiantes de $selectedSalon (${studentsList.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackTertiary
                        )
                        Icon(Icons.Filled.People, contentDescription = null, tint = RedPrimary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingStudents) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = RedPrimary)
                        }
                    } else if (studentsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No se encontraron estudiantes registrados en esta sección", color = TextGray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(studentsList) { student ->
                                var awardSuccess by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, DividerGray)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(YellowSecondary.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = student.studentName.take(1).uppercase(),
                                                    color = BlackTertiary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = student.studentName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = BlackTertiary
                                                )
                                                Text(
                                                    text = "Usuario: @${student.username}",
                                                    fontSize = 11.sp,
                                                    color = TextGray
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Star, contentDescription = null, tint = YellowSecondary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "${student.points} pts",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RedPrimary
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
            } else if (selectedTab == "Cursos") {
                // CURSOS PANEL (Add topics and tasks)
                var newContentTitle by remember { mutableStateOf("") }
                var newContentDesc by remember { mutableStateOf("") }
                var isSubmittingContent by remember { mutableStateOf(false) }
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Agregar Tareas y Temas",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackTertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Nueva actividad para $selectedSalon",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            OutlinedTextField(
                                value = newContentTitle,
                                onValueChange = { newContentTitle = it },
                                placeholder = { Text("Título de la tarea o tema...", fontSize = 13.sp, color = TextGray) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = DividerGray,
                                    focusedBorderColor = RedPrimary,
                                    unfocusedContainerColor = BackgroundGray,
                                    focusedContainerColor = BackgroundGray,
                                    focusedTextColor = BlackTertiary,
                                    unfocusedTextColor = BlackTertiary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = newContentDesc,
                                onValueChange = { newContentDesc = it },
                                placeholder = { Text("Descripción o instrucciones detalladas...", fontSize = 13.sp, color = TextGray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = DividerGray,
                                    focusedBorderColor = RedPrimary,
                                    unfocusedContainerColor = BackgroundGray,
                                    focusedContainerColor = BackgroundGray,
                                    focusedTextColor = BlackTertiary,
                                    unfocusedTextColor = BlackTertiary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (newContentTitle.isNotBlank() && !isSubmittingContent) {
                                RevealButton(
                                    onClick = {
                                        isSubmittingContent = true
                                        val (g, s) = activeGradeAndSection
                                        val newContent = hashMapOf(
                                            "title" to newContentTitle,
                                            "description" to newContentDesc,
                                            "teacherName" to teacherName,
                                            "area" to area,
                                            "grade" to g,
                                            "section" to s,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        db.collection("class_tasks")
                                            .add(newContent)
                                            .addOnSuccessListener {
                                                newContentTitle = ""
                                                newContentDesc = ""
                                                isSubmittingContent = false
                                            }
                                            .addOnFailureListener {
                                                isSubmittingContent = false
                                            }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    backgroundColor = RedPrimary,
                                    revealColor = Color(0xFFFF5252),
                                    contentColor = Color.White
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Publicar Actividad", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = Color.LightGray,
                                        disabledContentColor = TextGray
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Add, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Publicar Actividad", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextGray)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Esta actividad aparecerá en la sección 'Tareas' de los estudiantes de $selectedSalon.",
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TeacherBottomNavigation(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == "Avisos",
            onClick = { onTabSelected("Avisos") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = "Avisos"
                )
            },
            label = { Text("Avisos", fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = RedPrimary,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray,
                indicatorColor = RedPrimary // Highlighted primary red background for active icon!
            )
        )
        NavigationBarItem(
            selected = selectedTab == "Alumnos",
            onClick = { onTabSelected("Alumnos") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = "Salón o Alumnos"
                )
            },
            label = { Text("Mi Salón", fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = RedPrimary,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray,
                indicatorColor = RedPrimary
            )
        )
        NavigationBarItem(
            selected = selectedTab == "Cursos",
            onClick = { onTabSelected("Cursos") },
            icon = {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = "Cursos"
                )
            },
            label = { Text("Cursos", fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = RedPrimary,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray,
                indicatorColor = RedPrimary
            )
        )
    }
}

data class Announcement(
    val id: String,
    val text: String,
    val senderName: String,
    val area: String,
    val timestamp: Long,
    val grade: String,
    val section: String
)

data class StudentInfo(
    val uid: String,
    val studentName: String,
    val username: String,
    val points: Int,
    val grade: String,
    val section: String
)
