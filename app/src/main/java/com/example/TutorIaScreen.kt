package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.BlackTertiary
import com.example.ui.theme.YellowSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class TutorMessage(val text: String, val isUser: Boolean, val timestamp: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorIaScreen(
    modifier: Modifier = Modifier,
    onGeneratePlan: (String, String) -> Unit
) {
    var messages by remember { mutableStateOf(listOf(
        TutorMessage("¡Hola! Soy tu Tutor IA. No te daré las respuestas directas, pero te guiaré para que aprendas. Si necesitas estudiar un tema en particular, dímelo y prepararé un plan de aprendizaje interactivo.", false, System.currentTimeMillis())
    )) }
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF7F7F7))) {
        TopAppBar(
            title = { Text("Tutor IA", fontWeight = FontWeight.Bold, color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = RedPrimary)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) RedPrimary.copy(alpha = 0.9f) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isUser) Color.White else BlackTertiary,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pregunta algo o pide un plan (ej. Plan de Fracciones)") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RedPrimary, 
                    unfocusedContainerColor = Color.White, 
                    focusedContainerColor = Color.White,
                    focusedTextColor = BlackTertiary,
                    unfocusedTextColor = BlackTertiary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank() && !isSending) {
                        val userText = inputText
                        messages = messages + TutorMessage(userText, true, System.currentTimeMillis())
                        inputText = ""
                        isSending = true
                        
                        coroutineScope.launch {
                            val aiResponse = generateTutorResponse(userText)
                            
                            // Check if user is asking for a plan
                            if (userText.lowercase().contains("plan") || userText.lowercase().contains("estudiar")) {
                                // Extract a simple topic (heuristic: take the last words or just the whole text)
                                val topic = userText.replace("plan", "", ignoreCase = true)
                                    .replace("de", "", ignoreCase = true)
                                    .replace("estudiar", "", ignoreCase = true)
                                    .trim()
                                    .ifEmpty { "General" }
                                
                                onGeneratePlan("General", topic)
                                messages = messages + TutorMessage("¡Excelente! He generado un Plan de Aprendizaje IA para '$topic'. Ve a la sección 'Plan de Aprendizaje IA' para completarlo y ganar una insignia.", false, System.currentTimeMillis())
                            } else {
                                messages = messages + TutorMessage(aiResponse, false, System.currentTimeMillis())
                            }
                            isSending = false
                        }
                    }
                },
                containerColor = RedPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

suspend fun generateTutorResponse(message: String): String = withContext(Dispatchers.IO) {
    val apiKey = ConfigManager.getGeminiKey()
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "Recuerda que estoy aquí para guiarte. Piensa en qué conceptos clave están involucrados y trata de deducir el siguiente paso. ¿Qué opinas?"
    }

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
    val client = OkHttpClient.Builder().build()

    val prompt = """
        Eres un Tutor IA para estudiantes escolares.
        REGLA DE ORO: NUNCA des la respuesta directa ni resuelvas el problema.
        En su lugar, guía al estudiante con preguntas socráticas, pistas y explicaciones conceptuales.
        Responde de forma amigable, corta (máximo 3-4 oraciones) y motivadora.
        Mensaje del estudiante: "$message"
    """.trimIndent()

    val requestJson = JSONObject().apply {
        put("contents", JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            })
        })
    }

    val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder().url(url).post(body).build()

    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext "¡Interesante! ¿Por qué no lo intentas desglosar en partes más pequeñas?"
            val responseString = response.body?.string() ?: ""
            val responseJson = JSONObject(responseString)
            val textResponse = responseJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text").trim()
            return@withContext textResponse
        }
    } catch (e: Exception) {
        return@withContext "Tuve un pequeño problema técnico, pero sigue intentándolo. ¡Tú puedes!"
    }
}
