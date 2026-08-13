package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.RedPrimary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser

    var username by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        val sharedPrefs = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)

        val localUsername = sharedPrefs.getString("username_backup_$uid", null)
        val localStudentName = sharedPrefs.getString("student_name_backup_$uid", null)
        val localGrade = sharedPrefs.getString("grade_backup_$uid", null)
        val localSection = sharedPrefs.getString("section_backup_$uid", null)

        if (localUsername != null) username = localUsername else username = currentUser.displayName ?: ""
        if (localStudentName != null) studentName = localStudentName
        if (localGrade != null) grade = localGrade
        if (localSection != null) section = localSection

        try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                doc.getString("displayName")?.let { username = it }
                doc.getString("studentName")?.let { studentName = it }
                doc.getString("grade")?.let { grade = it }
                doc.getString("section")?.let { section = it }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = GoldSecondary, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Mi Perfil Escolar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Email (Read only)
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Correo Electrónico (Registrado)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = RedPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = Color.LightGray,
                        unfocusedTextColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de Usuario (Apodo)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RedPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Student Name
                OutlinedTextField(
                    value = studentName,
                    onValueChange = { studentName = it },
                    label = { Text("Nombre Completo del Estudiante") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = RedPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("Grado") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = RedPrimary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Sección") },
                        leadingIcon = { Icon(Icons.Default.Class, contentDescription = null, tint = RedPrimary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val uid = currentUser?.uid ?: return@Button
                        isSaving = true

                        val sharedPrefs = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
                        sharedPrefs.edit().apply {
                            putString("username_backup_$uid", username)
                            putString("student_name_backup_$uid", studentName)
                            putString("grade_backup_$uid", grade)
                            putString("section_backup_$uid", section)
                            apply()
                        }

                        val userData = hashMapOf(
                            "uid" to uid,
                            "email" to email,
                            "displayName" to username,
                            "studentName" to studentName,
                            "grade" to grade,
                            "section" to section
                        )

                        firestore.collection("users").document(uid).set(userData)
                            .addOnSuccessListener {
                                isSaving = false
                                Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                isSaving = false
                                Toast.makeText(context, "Guardado localmente. (Sin conexión a Firestore)", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        authViewModel.signOut()
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = RedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
