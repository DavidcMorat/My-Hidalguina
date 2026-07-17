package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    val authState by authViewModel.authState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Perfil",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BlackTertiary
        )
        Spacer(modifier = Modifier.height(32.dp))
        
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
        
        Spacer(modifier = Modifier.height(32.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { authViewModel.updateDisplayName(displayName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
        ) {
            Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
        }

        if (authState is AuthState.Success) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("¡Nombre actualizado!", color = Color(0xFF4CAF50), fontSize = 14.sp)
        } else if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text((authState as AuthState.Error).message, color = RedPrimary, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

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
