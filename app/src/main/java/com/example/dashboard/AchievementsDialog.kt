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

data class AchievementItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isUnlocked: Boolean,
    val progressText: String
)

@Composable
fun AchievementsDialog(
    plans: List<StudyPlanWithTopics>,
    tasks: List<TaskModel>,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val totalPlans = plans.size
    val completedTopics = plans.sumOf { it.topics.count { t -> t.status == "LOGRADO" || t.status == "COMPLETED" } }
    val completedTasks = tasks.count { it.completedBy.contains(currentUserId) }

    val achievements = listOf(
        AchievementItem(
            title = "Primer Paso Académico",
            description = "Crea tu primer plan de estudio personalizado con la IA",
            icon = Icons.Filled.Lightbulb,
            isUnlocked = (totalPlans >= 1),
            progressText = if (totalPlans >= 1) "Desbloqueado ⭐" else "$totalPlans/1 planes creados"
        ),
        AchievementItem(
            title = "Mente Brillante",
            description = "Completa y aprueba 3 temas interactivos con problemas reales",
            icon = Icons.Filled.EmojiEvents,
            isUnlocked = (completedTopics >= 3),
            progressText = if (completedTopics >= 3) "Desbloqueado 🏆" else "$completedTopics/3 temas logrados"
        ),
        AchievementItem(
            title = "Estudiante Responsable",
            description = "Entrega tu primera tarea asignada por un docente",
            icon = Icons.Filled.AssignmentTurnedIn,
            isUnlocked = (completedTasks >= 1),
            progressText = if (completedTasks >= 1) "Desbloqueado ⭐" else "$completedTasks/1 tareas entregadas"
        ),
        AchievementItem(
            title = "Maestro de la Disciplina",
            description = "Completa 5 temas de estudio y entrega al menos 3 tareas",
            icon = Icons.Filled.Star,
            isUnlocked = (completedTopics >= 5 && completedTasks >= 3),
            progressText = if (completedTopics >= 5 && completedTasks >= 3) "Desbloqueado 👑" else "$completedTopics/5 temas, $completedTasks/3 tareas"
        )
    )

    val unlockedCount = achievements.count { it.isUnlocked }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThemeColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(YellowSecondary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ThemeColors.textPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Logros y Medallas", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ThemeColors.textPrimary)
                    Text("$unlockedCount de ${achievements.size} desbloqueados", fontSize = 12.sp, color = ThemeColors.textSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                achievements.forEach { ach ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ach.isUnlocked) (if (ThemeState.isDarkTheme) DarkCardSurface else Color(0xFFFFFDE7)) else ThemeColors.inputBackground
                        ),
                        border = if (ach.isUnlocked) androidx.compose.foundation.BorderStroke(1.dp, YellowSecondary) else androidx.compose.foundation.BorderStroke(0.5.dp, ThemeColors.divider)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (ach.isUnlocked) YellowSecondary else ThemeColors.divider
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ach.icon,
                                    contentDescription = null,
                                    tint = if (ach.isUnlocked) BlackTertiary else ThemeColors.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (ach.isUnlocked) ThemeColors.textPrimary else ThemeColors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ach.description,
                                    fontSize = 11.sp,
                                    color = if (ach.isUnlocked) ThemeColors.textPrimary.copy(alpha = 0.85f) else ThemeColors.textSecondary,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ach.progressText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ach.isUnlocked) ThemeColors.primary else ThemeColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.primary, contentColor = ThemeColors.onPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cerrar", color = ThemeColors.onPrimary)
            }
        }
    )
}
