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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("student") } // "student" or "teacher"
    var area by remember { mutableStateOf("") }
    val selectedGrades = remember { mutableStateListOf<String>() }
    val selectedSections = remember { mutableStateListOf<String>() }

    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onRegisterSuccess()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(BackgroundGray)) {
        // Top Decoration
        TopDecoration(modifier = Modifier.align(Alignment.TopCenter))
        // Bottom Decoration
        BottomDecoration(modifier = Modifier.align(Alignment.BottomCenter))

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
                text = "Regístrate para comenzar\ntu labor escolar.",
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Selector de Rol (Estudiante vs Docente)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .border(1.dp, DividerGray, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "student" to "Soy Estudiante",
                    "teacher" to "Soy Docente"
                ).forEach { (r, label) ->
                    val isSelected = role == r
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RedPrimary else Color.Transparent)
                            .clickable { role = r }
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else BlackTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Campo de Nombre Real
            OutlinedTextField(
                value = studentName,
                onValueChange = { studentName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        if (role == "student") "Nombre de estudiante (No editable)" else "Nombre de docente", 
                        color = TextGray
                    ) 
                },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TextGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DividerGray,
                    focusedBorderColor = RedPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedTextColor = BlackTertiary,
                    unfocusedTextColor = BlackTertiary
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Usuario
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nombre de usuario", color = TextGray) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TextGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DividerGray,
                    focusedBorderColor = RedPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedTextColor = BlackTertiary,
                    unfocusedTextColor = BlackTertiary
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (role == "student") {
                // CAMPOS DE ESTUDIANTE: GRADO Y SECCIÓN SIMPLE
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { newValue ->
                            if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                                grade = newValue
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Grado", color = TextGray) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DividerGray,
                            focusedBorderColor = RedPrimary,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedTextColor = BlackTertiary,
                            unfocusedTextColor = BlackTertiary
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
                        placeholder = { Text("Sección", color = TextGray) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DividerGray,
                            focusedBorderColor = RedPrimary,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedTextColor = BlackTertiary,
                            unfocusedTextColor = BlackTertiary
                        ),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // CAMPOS DE DOCENTE: AREA, GRADOS (MULTIPLE) Y SECCIONES (MULTIPLE)
                // Selector de Cursos (Área) Múltiples/Simple (1 click)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DividerGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Área de enseñanza (seleccione 1):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val commonAreas = listOf("Matemática", "Comunicación", "Ciencias", "Historia", "Inglés", "Arte")
                        
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonAreas.forEach { cArea ->
                                val isSelected = area == cArea
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) RedPrimary.copy(alpha = 0.15f) else Color.White)
                                        .border(1.dp, if (isSelected) RedPrimary else DividerGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            area = cArea
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(cArea, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) RedPrimary else BlackTertiary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Grados Múltiples (1ro a 5to)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DividerGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Grados que enseña (seleccione 1 o más):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("1ro", "2do", "3ro", "4to", "5to").forEach { g ->
                                val isSelected = selectedGrades.contains(g)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) RedPrimary.copy(alpha = 0.15f) else Color.White)
                                        .border(1.dp, if (isSelected) RedPrimary else DividerGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isSelected) selectedGrades.remove(g) else selectedGrades.add(g)
                                        }
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Text(g, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) RedPrimary else BlackTertiary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Selector de Secciones Múltiples (A, B, C, D)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DividerGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Secciones a su cargo (seleccione 1 o más):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                        Spacer(modifier = Modifier.height(8.dp))
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("A", "B", "C", "D", "E", "F", "G", "H").forEach { s ->
                                val isSelected = selectedSections.contains(s)
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) RedPrimary.copy(alpha = 0.15f) else Color.White)
                                        .border(1.dp, if (isSelected) RedPrimary else DividerGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isSelected) selectedSections.remove(s) else selectedSections.add(s)
                                        }
                                        .padding(horizontal = 24.dp)
                                        .wrapContentSize(Alignment.Center)
                                ) {
                                    Text(s, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) RedPrimary else BlackTertiary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Correo electrónico", color = TextGray) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TextGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DividerGray,
                    focusedBorderColor = RedPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedTextColor = BlackTertiary,
                    unfocusedTextColor = BlackTertiary
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contraseña", color = TextGray) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextGray) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = TextGray
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DividerGray,
                    focusedBorderColor = RedPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedTextColor = BlackTertiary,
                    unfocusedTextColor = BlackTertiary
                ),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(32.dp))
            RevealButton(
                onClick = { 
                    if (role == "student") {
                        authViewModel.signUpWithEmail(email, password, username, studentName, "student", grade, section)
                    } else {
                        val gStr = selectedGrades.sorted().joinToString(",")
                        val sStr = selectedSections.sorted().joinToString(",")
                        authViewModel.signUpWithEmail(email, password, username, studentName, "teacher", gStr, sStr, area)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                backgroundColor = RedPrimary,
                revealColor = Color(0xFFFF5252),
                contentColor = Color.White
            ) {
                Text("Registrarse", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
            }
            
            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedPrimary, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.padding(bottom = 60.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("¿Ya tienes una cuenta? ", color = BlackTertiary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Iniciar sesión",
                    color = RedPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }
        }
    }
}
