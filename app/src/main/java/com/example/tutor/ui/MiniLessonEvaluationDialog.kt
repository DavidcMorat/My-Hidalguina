package com.example.tutor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.tutor.data.StudyTopicEntity
import com.example.tutor.model.PracticeSessionJson
import com.example.ui.theme.BlackTertiary
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.YellowSecondary

@Composable
fun MiniLessonEvaluationDialog(
    topic: StudyTopicEntity,
    lessonData: PracticeSessionJson?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCompleteSuccess: (topicId: String) -> Unit,
    onRecordOutcome: ((topicId: String, isPassed: Boolean, scoreText: String) -> Unit)? = null,
    onAskTutorAboutProblem: ((topicTitle: String, problemQuestion: String, solution: String) -> Unit)? = null
) {
    var selectedAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var isSubmitted by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RedPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = RedPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Práctica con Problemas Reales",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackTertiary
                            )
                            Text(
                                text = topic.title,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                if (isLoading || lessonData == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RedPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "La IA está generando tus problemas reales interactivos...",
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Casos prácticos contextualizados y calificación automática",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    val problems = lessonData.problems
                    val totalProblems = problems.size
                    val correctCount = if (isSubmitted) {
                        problems.indices.count { selectedAnswers[it] == problems[it].correctOptionIndex }
                    } else 0
                    val isPassed = isSubmitted && (correctCount == totalProblems || (totalProblems > 0 && correctCount.toDouble() / totalProblems >= 0.65))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Contexto Real Banner
                        if (!lessonData.realWorldContext.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF3FB)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Filled.Public,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Aplicación en el mundo real",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1976D2)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = lessonData.realWorldContext,
                                            fontSize = 12.sp,
                                            color = BlackTertiary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Summary Score Banner (when submitted)
                        if (isSubmitted) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPassed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPassed) Icons.Filled.CheckCircle else Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = if (isPassed) Color(0xFF2E7D32) else Color(0xFFE65100),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isPassed) "¡Práctica Aprobada! ($correctCount/$totalProblems)" else "Necesitas Reforzar ($correctCount/$totalProblems)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPassed) Color(0xFF2E7D32) else Color(0xFFE65100)
                                        )
                                        Text(
                                            text = if (isPassed) "Estado actualizado a 'Logrado'. ¡Excelente dominio!" else "Tu calificación indica que necesitas ayuda. ¡Usa las explicaciones o consulta al Tutor!",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Practice Problems
                        problems.forEachIndexed { qIndex, problem ->
                            val isQuestionAnswered = selectedAnswers.containsKey(qIndex)
                            val userChoice = selectedAnswers[qIndex]
                            val isCorrectAnswer = isSubmitted && userChoice == problem.correctOptionIndex

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                shape = RoundedCornerShape(16.dp),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Problema #${qIndex + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RedPrimary
                                        )
                                        if (isSubmitted) {
                                            Text(
                                                text = if (isCorrectAnswer) "Correcto ✓" else "Incorrecto ✗",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCorrectAnswer) Color(0xFF2E7D32) else RedPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = problem.question,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BlackTertiary,
                                        lineHeight = 19.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    problem.options.forEachIndexed { optIndex, optionText ->
                                        val isOptionSelected = userChoice == optIndex
                                        val isOptionCorrectAnswer = isSubmitted && optIndex == problem.correctOptionIndex
                                        val isWrongSelection = isSubmitted && isOptionSelected && !isCorrectAnswer

                                        val bgColor = when {
                                            isOptionCorrectAnswer -> Color(0xFFE8F5E9)
                                            isWrongSelection -> Color(0xFFFFEBEE)
                                            isOptionSelected -> YellowSecondary.copy(alpha = 0.25f)
                                            else -> Color.White
                                        }

                                        val borderColor = when {
                                            isOptionCorrectAnswer -> Color(0xFF2E7D32)
                                            isWrongSelection -> RedPrimary
                                            isOptionSelected -> YellowSecondary
                                            else -> Color(0xFFE0E0E0)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(bgColor)
                                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                                .clickable(enabled = !isSubmitted) {
                                                    selectedAnswers = selectedAnswers + (qIndex to optIndex)
                                                }
                                                .padding(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = isOptionSelected,
                                                    onClick = {
                                                        if (!isSubmitted) {
                                                            selectedAnswers = selectedAnswers + (qIndex to optIndex)
                                                        }
                                                    },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = if (isOptionCorrectAnswer) Color(0xFF2E7D32) else RedPrimary
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = optionText,
                                                    fontSize = 13.sp,
                                                    color = BlackTertiary
                                                )
                                            }
                                        }
                                    }

                                    // Feedback explanation after submission
                                    if (isSubmitted) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Filled.TipsAndUpdates,
                                                        contentDescription = null,
                                                        tint = Color(0xFF455A64),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Solución y Procedimiento Paso a Paso:",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF455A64)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = problem.stepByStepExplanation,
                                                    fontSize = 12.sp,
                                                    color = BlackTertiary,
                                                    lineHeight = 16.sp
                                                )

                                                if (!isCorrectAnswer && onAskTutorAboutProblem != null) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    OutlinedButton(
                                                        onClick = {
                                                            onAskTutorAboutProblem(
                                                                topic.title,
                                                                problem.question,
                                                                problem.stepByStepExplanation
                                                            )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Psychology,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Pedir ayuda al Tutor IA sobre este problema", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Actions
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!isSubmitted) {
                        val allAnswered = problems.indices.all { selectedAnswers.containsKey(it) }
                        Button(
                            onClick = {
                                isSubmitted = true
                                val scoreTxt = "$correctCount/$totalProblems (${(correctCount * 100) / totalProblems.coerceAtLeast(1)}%)"
                                onRecordOutcome?.invoke(topic.id, isPassed, scoreTxt)
                                if (isPassed) {
                                    onCompleteSuccess(topic.id)
                                }
                            },
                            enabled = allAnswered,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calificar Práctica Automáticamente", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Reset to try again
                                    selectedAnswers = emptyMap()
                                    isSubmitted = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reintentar", fontSize = 12.sp)
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPassed) Color(0xFF2E7D32) else RedPrimary
                                )
                            ) {
                                Text(
                                    if (isPassed) "¡Listo, Continuar!" else "Cerrar",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

