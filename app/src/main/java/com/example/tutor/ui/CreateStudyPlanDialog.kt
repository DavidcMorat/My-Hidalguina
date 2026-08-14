package com.example.tutor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackTertiary
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.YellowSecondary

@Composable
fun CreateStudyPlanDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (topic: String, notes: String?) -> Unit
) {
    var topicText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val suggestions = listOf(
        "Álgebra y Ecuaciones",
        "Leyes de Newton",
        "Química: Enlaces Químicos",
        "Historia: Revolución Mexicana",
        "Biología: Fotosíntesis y Célula",
        "Geometría y Trigonometría"
    )

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nuevo Plan de Estudio",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackTertiary
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isGenerating) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Gemini AI estructurará una ruta de aprendizaje interactiva paso a paso.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sugerencias rápidas:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BlackTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { sug ->
                        SuggestionChip(
                            onClick = { topicText = sug },
                            label = { Text(sug, fontSize = 11.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = YellowSecondary.copy(alpha = 0.2f),
                                labelColor = BlackTertiary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = topicText,
                    onValueChange = { topicText = it },
                    label = { Text("Tema o materia a estudiar *") },
                    placeholder = { Text("Ej. Ecuaciones de 2do grado") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isGenerating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        focusedLabelColor = RedPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("¿Qué parte se te dificulta? (Opcional)") },
                    placeholder = { Text("Ej. Me cuesta trabajo la factorización...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    enabled = !isGenerating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RedPrimary,
                        focusedLabelColor = RedPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topicText.isNotBlank()) {
                        onGenerate(topicText, notesText.ifBlank { null })
                    }
                },
                enabled = topicText.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generando plan...")
                } else {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generar con IA", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
