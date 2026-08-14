package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var studentName by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("estudiante") }
    var teacherSubject by remember { mutableStateOf("") }
    var hasTutoria by remember { mutableStateOf(false) }
    var tutoriaGrade by remember { mutableStateOf("1") }
    var tutoriaSection by remember { mutableStateOf("A") }
    var selectedTeachingClassrooms by remember { mutableStateOf(setOf<String>()) }
    val authState by authViewModel.authState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        authViewModel.clearAuthState()
    }

    LaunchedEffect(user?.uid) {
        val uid = user?.uid
        if (uid != null) {
            val sharedPrefs = context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)
            studentName = sharedPrefs.getString("studentName_backup_$uid", "") ?: ""
            grade = sharedPrefs.getString("grade_backup_$uid", "") ?: ""
            section = sharedPrefs.getString("section_backup_$uid", "") ?: ""
            displayName = sharedPrefs.getString("displayName_backup_$uid", user.displayName ?: "") ?: ""
            role = sharedPrefs.getString("role_backup_$uid", "estudiante") ?: "estudiante"
            teacherSubject = sharedPrefs.getString("subject_backup_$uid", "") ?: ""
            hasTutoria = sharedPrefs.getBoolean("hasTutoria_backup_$uid", false)
            tutoriaGrade = sharedPrefs.getString("tutoriaGrade_backup_$uid", "1") ?: "1"
            tutoriaSection = sharedPrefs.getString("tutoriaSection_backup_$uid", "A") ?: "A"
            selectedTeachingClassrooms = sharedPrefs.getStringSet("teachingClassrooms_backup_$uid", emptySet()) ?: emptySet()

            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(uid).get().await()
                val fStudentName = doc.getString("studentName") ?: ""
                val fGrade = doc.getString("grade") ?: ""
                val fSection = doc.getString("section") ?: ""
                val fDisplayName = doc.getString("displayName") ?: ""
                val fRole = doc.getString("role") ?: "estudiante"
                val fSubject = doc.getString("subject") ?: ""
                val fHasTutoria = doc.getBoolean("hasTutoria") ?: false
                val fTutoriaGrade = doc.getString("tutoriaGrade") ?: "1"
                val fTutoriaSection = doc.getString("tutoriaSection") ?: "A"
                val fClassrooms = (doc.get("teachingClassrooms") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                
                if (fStudentName.isNotEmpty()) studentName = fStudentName
                if (fGrade.isNotEmpty()) grade = fGrade
                if (fSection.isNotEmpty()) section = fSection
                if (fDisplayName.isNotEmpty()) displayName = fDisplayName
                if (fRole.isNotEmpty()) role = fRole
                if (fSubject.isNotEmpty()) teacherSubject = fSubject
                hasTutoria = fHasTutoria
                if (fTutoriaGrade.isNotEmpty()) tutoriaGrade = fTutoriaGrade
                if (fTutoriaSection.isNotEmpty()) tutoriaSection = fTutoriaSection
                if (fClassrooms.isNotEmpty()) selectedTeachingClassrooms = fClassrooms.toSet()
                
                sharedPrefs.edit().apply {
                    putString("studentName_backup_$uid", studentName)
                    putString("grade_backup_$uid", grade)
                    putString("section_backup_$uid", section)
                    putString("displayName_backup_$uid", displayName)
                    putString("role_backup_$uid", role)
                    putString("subject_backup_$uid", teacherSubject)
                    putBoolean("hasTutoria_backup_$uid", hasTutoria)
                    putString("tutoriaGrade_backup_$uid", tutoriaGrade)
                    putString("tutoriaSection_backup_$uid", tutoriaSection)
                    putStringSet("teachingClassrooms_backup_$uid", selectedTeachingClassrooms)
                    apply()
                }
            } catch (e: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeColors.background)
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Perfil",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeColors.textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(ThemeColors.divider)
                .border(4.dp, if (role == "docente") YellowSecondary else ThemeColors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (role == "docente") Icons.Filled.Star else Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                tint = ThemeColors.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (role == "docente") YellowSecondary else ThemeColors.primary.copy(alpha = 0.15f)
        ) {
            Text(
                text = if (role == "docente") "Cuenta de Docente" else "Cuenta de Estudiante",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (role == "docente") BlackTertiary else ThemeColors.primary
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (role == "docente") "Nombre completo del Docente" else "Nombre de estudiante", color = ThemeColors.textSecondary) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = ThemeColors.divider,
                focusedBorderColor = ThemeColors.primary,
                unfocusedContainerColor = ThemeColors.inputBackground,
                focusedContainerColor = ThemeColors.inputBackground,
                focusedTextColor = ThemeColors.inputTextColor,
                unfocusedTextColor = ThemeColors.inputTextColor
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre de usuario", color = ThemeColors.textSecondary) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = ThemeColors.divider,
                focusedBorderColor = ThemeColors.primary,
                unfocusedContainerColor = ThemeColors.inputBackground,
                focusedContainerColor = ThemeColors.inputBackground,
                focusedTextColor = ThemeColors.inputTextColor,
                unfocusedTextColor = ThemeColors.inputTextColor
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (role == "estudiante") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = grade,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                            grade = newValue
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Grado", color = ThemeColors.textSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = ThemeColors.divider,
                        focusedBorderColor = ThemeColors.primary,
                        unfocusedContainerColor = ThemeColors.inputBackground,
                        focusedContainerColor = ThemeColors.inputBackground,
                        focusedTextColor = ThemeColors.inputTextColor,
                        unfocusedTextColor = ThemeColors.inputTextColor
                    ),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = section,
                    onValueChange = { newValue ->
                        if (newValue.length <= 1) {
                            val upper = newValue.uppercase()
                            if (upper.isEmpty() || upper.all { it.isLetter() }) {
                                section = upper
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Sección", color = ThemeColors.textSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = ThemeColors.divider,
                        focusedBorderColor = ThemeColors.primary,
                        unfocusedContainerColor = ThemeColors.inputBackground,
                        focusedContainerColor = ThemeColors.inputBackground,
                        focusedTextColor = ThemeColors.inputTextColor,
                        unfocusedTextColor = ThemeColors.inputTextColor
                    ),
                    singleLine = true
                )
            }
        } else {
            OutlinedTextField(
                value = teacherSubject,
                onValueChange = { teacherSubject = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Materia o Especialidad", color = ThemeColors.textSecondary) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = ThemeColors.divider,
                    focusedBorderColor = ThemeColors.primary,
                    unfocusedContainerColor = ThemeColors.inputBackground,
                    focusedContainerColor = ThemeColors.inputBackground,
                    focusedTextColor = ThemeColors.inputTextColor,
                    unfocusedTextColor = ThemeColors.inputTextColor
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Salón de Tutoría (Opcional)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeColors.cardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (hasTutoria) YellowSecondary else ThemeColors.divider)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Salón de Tutoría (Opcional)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ThemeColors.textPrimary
                            )
                            Text(
                                if (hasTutoria) "Tutor de: ${tutoriaGrade}° $tutoriaSection (Chat activo)" else "Sin tutoría a cargo (Sin chat)",
                                fontSize = 11.sp,
                                color = ThemeColors.textSecondary
                            )
                        }
                        Switch(
                            checked = hasTutoria,
                            onCheckedChange = { hasTutoria = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ThemeColors.primary, checkedTrackColor = ThemeColors.primary.copy(alpha = 0.3f))
                        )
                    }

                    if (hasTutoria) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val gradesList = listOf("1", "2", "3", "4", "5")

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Grado de Tutoría", fontSize = 11.sp, color = ThemeColors.textSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    gradesList.forEach { g ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (tutoriaGrade == g) ThemeColors.primary else ThemeColors.inputBackground,
                                            modifier = Modifier.clickable { tutoriaGrade = g }
                                        ) {
                                            Text(
                                                text = "${g}°",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tutoriaGrade == g) ThemeColors.onPrimary else ThemeColors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sección Tutoría", fontSize = 11.sp, color = ThemeColors.textSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("A", "B", "C", "D").forEach { s ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (tutoriaSection == s) ThemeColors.primary else ThemeColors.inputBackground,
                                            modifier = Modifier.clickable { tutoriaSection = s }
                                        ) {
                                            Text(
                                                text = s,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tutoriaSection == s) ThemeColors.onPrimary else ThemeColors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Grados y Secciones que enseña (A a H)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeColors.cardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.divider)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Grados y Secciones que enseña",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ThemeColors.textPrimary
                        )
                        Text(
                            "${selectedTeachingClassrooms.size} aulas",
                            fontSize = 11.sp,
                            color = ThemeColors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val grades = listOf("1", "2", "3", "4", "5")
                    val sections = listOf("A", "B", "C", "D", "E", "F", "G", "H")

                    grades.forEach { g ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${g}°",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColors.textPrimary,
                                modifier = Modifier.width(28.dp)
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                sections.forEach { s ->
                                    val code = "$g$s"
                                    val isSelected = selectedTeachingClassrooms.contains(code)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) ThemeColors.primary else ThemeColors.inputBackground,
                                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(0.5.dp, ThemeColors.divider),
                                        modifier = Modifier
                                            .clickable {
                                                selectedTeachingClassrooms = if (isSelected) {
                                                    selectedTeachingClassrooms - code
                                                } else {
                                                    selectedTeachingClassrooms + code
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = s,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ThemeColors.onPrimary else ThemeColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                authViewModel.updateProfile(
                    displayName = displayName,
                    studentName = studentName,
                    grade = grade,
                    section = section,
                    role = role,
                    subject = teacherSubject,
                    tutoriaGrade = if (hasTutoria) tutoriaGrade else "",
                    tutoriaSection = if (hasTutoria) tutoriaSection else "",
                    hasTutoria = hasTutoria,
                    teachingClassrooms = selectedTeachingClassrooms.toList().sorted()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.primary, contentColor = ThemeColors.onPrimary)
        ) {
            Text("Guardar Cambios", color = ThemeColors.onPrimary, fontWeight = FontWeight.Bold)
        }

        if (authState is AuthState.Success) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("¡Perfil actualizado con éxito!", color = Color(0xFF4CAF50), fontSize = 14.sp)
        } else if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text((authState as AuthState.Error).message, color = ThemeColors.primary, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeColors.primary),
            border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.primary)
        ) {
            Text("Cerrar Sesión", color = ThemeColors.primary)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
