package com.example.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tasks.data.TaskModel
import com.example.tutor.data.StudyPlanWithTopics
import com.example.ui.theme.*

@Composable
fun ProgressDialog(
    plans: List<StudyPlanWithTopics>,
    tasks: List<TaskModel>,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val totalTopics = plans.sumOf { it.topics.size }
    val completedTopics = plans.sumOf { it.topics.count { t -> t.status == "LOGRADO" || t.status == "COMPLETED" } }
    val plansProgress = if (totalTopics > 0) (completedTopics.toFloat() / totalTopics.toFloat()) else 0f

    val completedTasksCount = tasks.count { it.completedBy.contains(currentUserId) }
    val totalTasksCount = tasks.size
    val tasksProgress = if (totalTasksCount > 0) (completedTasksCount.toFloat() / totalTasksCount.toFloat()) else 0f

    // Subjects breakdown
    val subjectCounts = plans.groupBy { it.plan.subject }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThemeColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ThemeColors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, tint = ThemeColors.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Mi Progreso Académico", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ThemeColors.textPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title = "Planes de Estudio",
                        value = "${plans.size}",
                        subtitle = "$completedTopics de $totalTopics temas logrados",
                        icon = Icons.Filled.MenuBook,
                        color = ThemeColors.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Tareas Entregadas",
                        value = "$completedTasksCount",
                        subtitle = "de $totalTasksCount asignadas",
                        icon = Icons.Filled.AssignmentTurnedIn,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Global Progress Bars
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColors.inputBackground)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Avance en Planes con IA", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { plansProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = ThemeColors.primary,
                            trackColor = ThemeColors.divider
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(plansProgress * 100).toInt()}% completado",
                            fontSize = 11.sp,
                            color = ThemeColors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Cumplimiento de Tareas Docentes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { tasksProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = YellowSecondary,
                            trackColor = ThemeColors.divider
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(tasksProgress * 100).toInt()}% entregadas",
                            fontSize = 11.sp,
                            color = ThemeColors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (subjectCounts.isNotEmpty()) {
                    Text("Materias Estudiadas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
                    for ((subject, subjectPlans) in subjectCounts) {
                        val subTopics = subjectPlans.sumOf { it.topics.size }
                        val subDone = subjectPlans.sumOf { it.topics.count { t -> t.status == "LOGRADO" || t.status == "COMPLETED" } }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(subject, fontSize = 13.sp, color = ThemeColors.textPrimary, fontWeight = FontWeight.Medium)
                            Text("$subDone/$subTopics temas", fontSize = 12.sp, color = ThemeColors.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (plans.isEmpty() && tasks.isEmpty()) {
                    Text(
                        text = "Aún no tienes actividad registrada. Crea tu primer plan de estudio con el Tutor IA o entrega tus tareas escolares para comenzar a registrar tu avance.",
                        fontSize = 12.sp,
                        color = ThemeColors.textSecondary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.primary, contentColor = ThemeColors.onPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Entendido", color = ThemeColors.onPrimary)
            }
        }
    )
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColors.inputBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary)
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ThemeColors.textPrimary)
            Text(subtitle, fontSize = 9.sp, color = ThemeColors.textSecondary)
        }
    }
}
