package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat.ChatListScreen
import com.example.chat.ChatUser
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.RedPrimary

data class Course(val name: String, val teacher: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    authViewModel: AuthViewModel,
    onNavigateToChatUser: (ChatUser) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DarkSurface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RedPrimary,
                        selectedTextColor = RedPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Cursos") },
                    label = { Text("Cursos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RedPrimary,
                        selectedTextColor = RedPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Message, contentDescription = "Chat") },
                    label = { Text("Mensajes") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RedPrimary,
                        selectedTextColor = RedPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RedPrimary,
                        selectedTextColor = RedPrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = DarkSurfaceVariant
                    )
                )
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> HomeTab()
                1 -> CoursesTab()
                2 -> ChatListScreen(onNavigateToChat = onNavigateToChatUser)
                3 -> ProfileScreen(authViewModel = authViewModel, onSignOut = onSignOut)
            }
        }
    }
}

@Composable
fun HomeTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Bienvenido a My Hidalguina",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = GoldSecondary
        )
        Text(
            text = "Avisos y Noticias de la Escuela",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "📢 Comunicado Oficial", fontWeight = FontWeight.Bold, color = RedPrimary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recordatorio a todos los estudiantes de revisar sus horarios y participar en los grupos de chat de su salón.",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "📅 Horario Escolar", fontWeight = FontWeight.Bold, color = GoldSecondary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lunes a Viernes de 8:00 AM a 2:00 PM. Revisa el apartado de cursos para más detalles.",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CoursesTab() {
    val courses = remember {
        listOf(
            Course("Matemáticas", "Prof. Gómez", Icons.Default.Calculate),
            Course("Comunicación", "Prof. Torres", Icons.Default.MenuBook),
            Course("Ciencias Sociales", "Prof. Ramos", Icons.Default.Public),
            Course("Ciencia y Tecnología", "Prof. Mendoza", Icons.Default.Science),
            Course("Inglés", "Prof. Smith", Icons.Default.Language)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tus Cursos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(courses) { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(course.icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = course.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text(text = course.teacher, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
