package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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

            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(uid).get().await()
                val fStudentName = doc.getString("studentName") ?: ""
                val fGrade = doc.getString("grade") ?: ""
                val fSection = doc.getString("section") ?: ""
                val fDisplayName = doc.getString("displayName") ?: ""
                
                if (fStudentName.isNotEmpty()) studentName = fStudentName
                if (fGrade.isNotEmpty()) grade = fGrade
                if (fSection.isNotEmpty()) section = fSection
                if (fDisplayName.isNotEmpty()) displayName = fDisplayName
                
                sharedPrefs.edit().apply {
                    putString("studentName_backup_$uid", studentName)
                    putString("grade_backup_$uid", grade)
                    putString("section_backup_$uid", section)
                    putString("displayName_backup_$uid", displayName)
                    apply()
                }
            } catch (e: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Perfil",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BlackTertiary
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .border(4.dp, YellowSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(70.dp), tint = Color.White)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre de estudiante") },
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre de usuario") },
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = grade,
                onValueChange = { newValue ->
                    if (newValue.length <= 1 && (newValue.isEmpty() || newValue.all { it.isDigit() })) {
                        grade = newValue
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text("Grado") },
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
                label = { Text("Sección") },
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { authViewModel.updateProfile(displayName, studentName, grade, section) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
        ) {
            Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
        }

        if (authState is AuthState.Success) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("¡Perfil actualizado con éxito!", color = Color(0xFF4CAF50), fontSize = 14.sp)
        } else if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text((authState as AuthState.Error).message, color = RedPrimary, fontSize = 14.sp)
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, RedPrimary)
        ) {
            Text("Cerrar Sesión")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
