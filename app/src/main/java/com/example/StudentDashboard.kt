package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StudentDashboard(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = { StudentBottomNavigation() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                DashboardTopDecoration()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { /* TODO */ }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .border(3.dp, YellowSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "¡Hola, David!",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackTertiary
                            )
                            Text(
                                text = "Cada paso que das hoy\nte acerca a tu mejor versión.",
                                fontSize = 14.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }
            
            // Score and Level
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = BlackTertiary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Puntaje", color = Color.White, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("850", color = YellowSecondary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.Star, contentDescription = null, tint = YellowSecondary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nivel 4", color = Color.White, fontSize = 12.sp)
                        LinearProgressIndicator(
                            progress = { 0.7f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = YellowSecondary,
                            trackColor = Color.DarkGray
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(YellowSecondary.copy(alpha = 0.2f))
                                .border(2.dp, YellowSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = YellowSecondary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("¡Vas muy bien!", color = RedPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Sigue así para\nalcanzar el siguiente nivel.", color = TextGray, fontSize = 10.sp, lineHeight = 12.sp)
                        }
                    }
                }
            }
            
            // Tools Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-20).dp)
            ) {
                Text(
                    text = "Herramientas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackTertiary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Using rows for the grid to keep it scrollable in Column
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToolCard(
                        title = "Aprendizaje",
                        subtitle = "Estudia los temas\nde tus cursos",
                        icon = Icons.Filled.MenuBook,
                        backgroundColor = RedPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    ToolCard(
                        title = "Consultar",
                        subtitle = "Pregunta a la IA\ntus dudas",
                        icon = Icons.Filled.Psychology,
                        backgroundColor = YellowSecondary,
                        contentColor = BlackTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    ToolCard(
                        title = "Tareas",
                        subtitle = "Revisa y entrega\ntus actividades",
                        icon = Icons.Filled.Assignment,
                        backgroundColor = BlackTertiary,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToolCard(
                        title = "Plan de estudio",
                        subtitle = "Tu ruta personalizada\nde aprendizaje",
                        icon = Icons.Filled.AdsClick,
                        backgroundColor = YellowSecondary,
                        contentColor = BlackTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    ToolCard(
                        title = "Mi progreso",
                        subtitle = "Revisa tu avance\npor materia",
                        icon = Icons.Filled.BarChart,
                        backgroundColor = BlackTertiary,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    ToolCard(
                        title = "Logros",
                        subtitle = "Desbloquea medallas\ny recompensas",
                        icon = Icons.Filled.EmojiEvents,
                        backgroundColor = RedPrimary,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Upcoming Activities Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Próximas actividades",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlackTertiary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                ActivityItem(
                    title = "Matemáticas",
                    subtitle = "Resolver ejercicios de ecuaciones",
                    date = "Hoy",
                    icon = Icons.Filled.Calculate,
                    iconTint = RedPrimary,
                    iconBg = RedPrimary.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActivityItem(
                    title = "Historia",
                    subtitle = "Leer capítulo 4 y responder preguntas",
                    date = "Mañana",
                    icon = Icons.Filled.DateRange,
                    iconTint = YellowSecondary,
                    iconBg = YellowSecondary.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActivityItem(
                    title = "Ciencia",
                    subtitle = "Entrega de informe: El ecosistema",
                    date = "24 may",
                    icon = Icons.Filled.Science,
                    iconTint = Color.White,
                    iconBg = BlackTertiary
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(140.dp).clickable { },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = contentColor.copy(alpha = 0.8f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ActivityItem(
    title: String,
    subtitle: String,
    date: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
                Text(text = subtitle, fontSize = 12.sp, color = TextGray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = date, fontSize = 12.sp, color = TextGray)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun StudentBottomNavigation() {
    NavigationBar(
        containerColor = Color.White,
        contentColor = TextGray,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Inicio") },
            label = { Text("Inicio", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            selected = true,
            onClick = { /* TODO */ },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RedPrimary,
                selectedTextColor = RedPrimary,
                indicatorColor = Color.White,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.MenuBook, contentDescription = "Cursos") },
            label = { Text("Cursos", fontSize = 10.sp) },
            selected = false,
            onClick = { /* TODO */ },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            )
        )
        // Central Item (Tutor IA)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RedPrimary)
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Tutor IA", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tutor IA", fontSize = 10.sp, color = TextGray)
            }
        }
        
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Chat, contentDescription = "Mensajes") },
            label = { Text("Mensajes", fontSize = 10.sp) },
            selected = false,
            onClick = { /* TODO */ },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Perfil") },
            label = { Text("Perfil", fontSize = 10.sp) },
            selected = false,
            onClick = { /* TODO */ },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            )
        )
    }
}

@Composable
fun DashboardTopDecoration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(280.dp)) {
        val w = size.width
        val h = size.height
        
        // White base
        drawRect(Color.White)
        
        // Yellow accent shape
        val yellowPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.6f)
            quadraticTo(w * 0.5f, h * 0.7f, 0f, h * 0.9f)
            close()
        }
        drawPath(yellowPath, YellowSecondary)

        // Red foreground shape
        val redPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.55f)
            quadraticTo(w * 0.5f, h * 0.65f, 0f, h * 0.85f)
            close()
        }
        drawPath(redPath, RedPrimary)
    }
}
