package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
fun LoginScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Bienvenido",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Inicia sesión para continuar\ntu aprendizaje.",
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = BlackTertiary,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .height(3.dp)
                                .background(RedPrimary, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = DividerGray)
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "Estudiante",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 0) RedPrimary else TextGray
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            "Docente",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 1) BlackTertiary else TextGray
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Usuario o correo electrónico", color = TextGray) },
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(checkedColor = RedPrimary, uncheckedColor = TextGray)
                    )
                    Text("Recordarme", color = TextGray, fontSize = 14.sp)
                }
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = RedPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { /* TODO */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            RevealButton(
                onClick = { authViewModel.signInWithEmail(username, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                backgroundColor = RedPrimary,
                revealColor = Color(0xFFFF5252),
                contentColor = Color.White
            ) {
                Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerGray)
                Text("O", modifier = Modifier.padding(horizontal = 16.dp), color = TextGray)
                HorizontalDivider(modifier = Modifier.weight(1f), color = DividerGray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            RevealButton(
                onClick = {
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        authViewModel.signInWithGoogleWeb(activity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(BorderStroke(1.dp, DividerGray), RoundedCornerShape(12.dp)),
                backgroundColor = Color.White,
                revealColor = DividerGray,
                contentColor = BlackTertiary
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continuar con Google", color = BlackTertiary)
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.padding(bottom = 160.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("¿No tienes una cuenta? ", color = BlackTertiary, fontWeight = FontWeight.SemiBold)
                Text(
                    "Regístrate",
                    color = RedPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}

@Composable
fun TopDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val w = size.width
        val h = size.height

        // Red foreground curve (large sweeping background from top-right)
        val redPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.7f)
            quadraticTo(w * 0.5f, h * 0.9f, 0f, h * 0.3f)
            close()
        }
        drawPath(redPath, RedPrimary)
        
        // Yellow accent curve below red
        val yellowPath = Path().apply {
            moveTo(0f, h * 0.3f)
            quadraticTo(w * 0.5f, h * 0.9f, w, h * 0.7f)
            lineTo(w, h * 0.85f)
            quadraticTo(w * 0.5f, h * 1.05f, 0f, h * 0.45f)
            close()
        }
        drawPath(yellowPath, YellowSecondary)

        // Black corner on the top left
        val blackPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.4f, 0f)
            quadraticTo(w * 0.2f, h * 0.3f, 0f, h * 0.6f)
            close()
        }
        drawPath(blackPath, BlackTertiary)
    }
}

@Composable
fun BottomDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(100.dp)) {
        val w = size.width
        val h = size.height

        // Yellow curve base
        val yellowPath = Path().apply {
            moveTo(0f, h)
            lineTo(w, h)
            lineTo(w, h * 0.4f)
            quadraticTo(w * 0.5f, 0f, 0f, h * 0.6f)
            close()
        }
        drawPath(yellowPath, YellowSecondary)
        
        // Red corner on the bottom right
        val redPath = Path().apply {
            moveTo(w * 0.4f, h)
            lineTo(w, h)
            lineTo(w, h * 0.4f)
            quadraticTo(w * 0.7f, h * 0.2f, w * 0.4f, h)
            close()
        }
        drawPath(redPath, RedPrimary)

        // Black curve bottom left
        val blackPath = Path().apply {
            moveTo(0f, h)
            lineTo(w * 0.6f, h)
            quadraticTo(w * 0.3f, h * 0.7f, 0f, h * 0.6f)
            close()
        }
        drawPath(blackPath, BlackTertiary)
    }
}
