package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.RevealButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    var role by remember { mutableStateOf("estudiante") } // "estudiante" or "docente"
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var teacherSubject by remember { mutableStateOf("") }
    var hasTutoria by remember { mutableStateOf(false) }
    var tutoriaGrade by remember { mutableStateOf("1") }
    var tutoriaSection by remember { mutableStateOf("A") }
    var selectedTeachingClassrooms by remember { mutableStateOf(setOf<String>()) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onRegisterSuccess()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ThemeColors.background)) {
        // Top Decoration
        TopDecoration(modifier = Modifier.align(Alignment.TopCenter))
        // Bottom Decoration
        BottomDecoration(modifier = Modifier.align(Alignment.BottomCenter))

        // Dark Theme Toggle Button
        IconButton(
            onClick = { ThemeState.toggleTheme() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(40.dp)
                .background(ThemeColors.cardSurface, RoundedCornerShape(20.dp))
        ) {
            Icon(
                painter = painterResource(id = if (ThemeState.isDarkTheme) android.R.drawable.ic_menu_day else android.R.drawable.ic_menu_compass),
                contentDescription = "Cambiar Modo Oscuro",
                tint = ThemeColors.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "Crear Cuenta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Regístrate para comenzar\ntu experiencia educativa.",
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Role Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ThemeColors.cardSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { role = "estudiante" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (role == "estudiante") ThemeColors.primary else Color.Transparent,
                        contentColor = if (role == "estudiante") ThemeColors.onPrimary else ThemeColors.textSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (role == "estudiante") 2.dp else 0.dp)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Estudiante", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { role = "docente" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (role == "docente") YellowSecondary else Color.Transparent,
                        contentColor = if (role == "docente") BlackTertiary else ThemeColors.textSecondary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (role == "docente") 2.dp else 0.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Docente", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (role == "docente") "Nombre completo del Docente" else "Nombre completo del Estudiante",
                        color = ThemeColors.textSecondary
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ThemeColors.textSecondary) },
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

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nombre de usuario", color = ThemeColors.textSecondary) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ThemeColors.textSecondary) },
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
                        placeholder = { Text("Grado (ej. 3)", color = ThemeColors.textSecondary) },
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
                        placeholder = { Text("Sección (ej. A)", color = ThemeColors.textSecondary) },
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
                    placeholder = { Text("Materia o especialidad (ej. Matemáticas)", color = ThemeColors.textSecondary) },
                    leadingIcon = { Icon(Icons.Filled.MenuBook, contentDescription = null, tint = ThemeColors.textSecondary) },
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
                    border = BorderStroke(1.dp, if (hasTutoria) YellowSecondary else ThemeColors.divider)
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
                                    if (hasTutoria) "Tendrás acceso exclusivo al chat con esta aula" else "Marca si eres tutor de alguna sección",
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
                                val sectionsList = listOf("A", "B", "C", "D", "E", "F", "G", "H")

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Grado de Tutoría", fontSize = 11.sp, color = ThemeColors.textSecondary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        gradesList.forEach { g ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (tutoriaGrade == g) ThemeColors.primary else ThemeColors.background,
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
                                                color = if (tutoriaSection == s) ThemeColors.primary else ThemeColors.background,
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
                    border = BorderStroke(1.dp, ThemeColors.divider)
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
                                "${selectedTeachingClassrooms.size} seleccionadas",
                                fontSize = 11.sp,
                                color = ThemeColors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "Selecciona las aulas donde dictas clases (Secciones A - H)",
                            fontSize = 11.sp,
                            color = ThemeColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

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
                                            color = if (isSelected) ThemeColors.primary else ThemeColors.background,
                                            border = if (isSelected) null else BorderStroke(0.5.dp, ThemeColors.divider),
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
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Correo electrónico", color = ThemeColors.textSecondary) },
                leadingIcon = { Icon(Icons.Filled.MailOutline, contentDescription = null, tint = ThemeColors.textSecondary) },
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

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contraseña", color = ThemeColors.textSecondary) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = ThemeColors.textSecondary) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = ThemeColors.textSecondary
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = ThemeColors.divider,
                    focusedBorderColor = ThemeColors.primary,
                    unfocusedContainerColor = ThemeColors.inputBackground,
                    focusedContainerColor = ThemeColors.inputBackground,
                    focusedTextColor = ThemeColors.inputTextColor,
                    unfocusedTextColor = ThemeColors.inputTextColor
                ),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(28.dp))
            RevealButton(
                onClick = {
                    authViewModel.signUpWithEmail(
                        email = email,
                        pass = password,
                        username = username,
                        studentName = fullName,
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
                backgroundColor = ThemeColors.primary,
                revealColor = ThemeColors.primaryAccent,
                contentColor = ThemeColors.onPrimary
            ) {
                Text("Registrarse", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeColors.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp), tint = ThemeColors.onPrimary)
            }
            
            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeColors.primary, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.padding(bottom = 60.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("¿Ya tienes una cuenta? ", color = ThemeColors.textSecondary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Iniciar sesión",
                    color = ThemeColors.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
