package com.example.tutor.data

import com.example.tutor.api.AIApiClient
import com.example.tutor.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class StudyRepository(private val dao: StudyPlanDao) {

    fun getStudyPlans(userId: String): Flow<List<StudyPlanWithTopics>> =
        dao.getStudyPlansWithTopics(userId)

    fun getStudyPlanById(planId: String): Flow<StudyPlanWithTopics?> =
        dao.getStudyPlanWithTopicsById(planId)

    fun getChatMessages(userId: String): Flow<List<TutorChatMessageEntity>> =
        dao.getTutorChatMessages(userId)

    suspend fun saveChatMessage(userId: String, role: String, content: String, suggestedPrompt: String? = null) {
        withContext(Dispatchers.IO) {
            dao.insertTutorChatMessage(
                TutorChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    role = role,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    suggestedTopicPrompt = suggestedPrompt
                )
            )
        }
    }

    suspend fun clearChat(userId: String) {
        withContext(Dispatchers.IO) {
            dao.clearTutorChat(userId)
        }
    }

    suspend fun updateTopicStatus(topicId: String, status: String) {
        withContext(Dispatchers.IO) {
            dao.updateTopicStatus(topicId, status, System.currentTimeMillis())
        }
    }

    suspend fun deletePlan(planId: String) {
        withContext(Dispatchers.IO) {
            dao.deleteStudyPlan(planId)
        }
    }

    // --- Groq Socratic Tutor API ---
    suspend fun askGroqTutor(
        userId: String,
        userQuery: String,
        recentHistory: List<TutorChatMessageEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = AIApiClient.getGroqApiKey()
            if (apiKey.isBlank() || apiKey == "MY_GROQ_API_KEY") {
                // Fallback tutor if API key is not configured yet
                val fallbackResponse = "¡Hola! Soy tu Tutor IA. Recuerda que mi misión es guiarte para que descubras la respuesta por ti mismo y no darte la solución directa. ¿Qué paso de este tema o ejercicio se te hace más difícil?"
                saveChatMessage(userId, "user", userQuery)
                saveChatMessage(userId, "assistant", fallbackResponse)
                return@withContext Result.success(fallbackResponse)
            }

            val systemPrompt = """
                Eres el Tutor IA académico oficial de 'My Hidalguina'.
                Tu objetivo es guiar pedagógicamente a estudiantes de nivel medio superior.
                
                REGLAS ESTRICTAS:
                1. NUNCA des respuestas directas ni resuelvas ejercicios o tareas escolares de golpe.
                2. Usa el método socrático: haz preguntas guía, desglosa el problema en pasos pequeños, da pistas y analogías.
                3. Si detectas que el alumno tiene dificultades con un tema o necesita una ruta estructurada de aprendizaje, sugiere crear un Plan de Estudio interactivo y añade al final de tu mensaje la etiqueta [PLAN_SUGGESTION: <tema detallado>] para que pueda generarlo con un toque.
                4. Sé motivador, claro, conciso y cordial.
            """.trimIndent()

            val messages = mutableListOf<GroqMessage>()
            messages.add(GroqMessage(role = "system", content = systemPrompt))

            // Add up to 6 past messages for context
            recentHistory.takeLast(6).forEach { msg ->
                messages.add(GroqMessage(role = msg.role, content = msg.content))
            }
            messages.add(GroqMessage(role = "user", content = userQuery))

            val request = GroqChatRequest(
                model = "openai/gpt-oss-20b",
                messages = messages,
                temperature = 0.7
            )

            val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
            val response = AIApiClient.groqService.createChatCompletion(authHeader, request)
            val assistantText = response.choices?.firstOrNull()?.message?.content
                ?: "No pude obtener una respuesta en este momento. Por favor intenta de nuevo."

            var suggestedPlanPrompt: String? = null
            var cleanText = assistantText
            val planRegex = "\\[PLAN_SUGGESTION:(.*?)\\]".toRegex()
            val match = planRegex.find(assistantText)
            if (match != null) {
                suggestedPlanPrompt = match.groupValues[1].trim()
                cleanText = assistantText.replace(match.value, "").trim()
            }

            saveChatMessage(userId, "user", userQuery)
            saveChatMessage(userId, "assistant", cleanText, suggestedPlanPrompt)

            Result.success(cleanText)
        } catch (e: Exception) {
            val errorMsg = "Hubo un inconveniente al conectar con el Tutor IA (${e.localizedMessage ?: "error de red"})."
            Result.failure(Exception(errorMsg, e))
        }
    }

    // --- Gemini JSON Structured Study Plan Generation ---
    suspend fun generateStudyPlanWithGemini(
        userId: String,
        subjectOrTopic: String,
        notesOrDifficulties: String? = null
    ): Result<StudyPlanEntity> = withContext(Dispatchers.IO) {
        try {
            val apiKey = AIApiClient.getGeminiApiKey()
            val prompt = """
                Genera un plan de estudio interactivo y directo para un estudiante de preparatoria sobre el tema: "$subjectOrTopic".
                ${if (!notesOrDifficulties.isNullOrBlank()) "Dificultades específicas: $notesOrDifficulties" else ""}
                
                ESTRUCTURA OBLIGATORIA POR CADA SUBTEMA (TOPIC):
                1. "title": Nombre claro del subtema o paso.
                2. "description": DEBE COMENZAR OBLIGATORIAMENTE CON "🌐 Infórmate: Busca en la web..." indicando el tema primordial y conceptos previos necesarios para comprenderlo.
                3. "keyConcept": DEBE COMENZAR OBLIGATORIAMENTE CON "📖 Domina la teoría:..." explicando de forma directa las fórmulas, reglas o conceptos clave esenciales sin rodeos.
                
                REGLA CRÍTICA: NO incluyas saludos, ni texto introductorio, ni despedidas.
                DEVUELVE ÚNICAMENTE UN OBJETO JSON VÁLIDO con la siguiente estructura exacta:
                {
                  "title": "Plan de Estudio: $subjectOrTopic",
                  "subject": "Materia académica (ej. Matemáticas, Física, Química, Biología, etc.)",
                  "description": "Objetivo principal de dominio del tema",
                  "estimatedDuration": "Tiempo estimado (ej. 3 días, 1 semana)",
                  "topics": [
                    {
                      "title": "Paso 1: Fundamentos esenciales",
                      "description": "🌐 Infórmate: Busca en la web sobre (tema primordial previo indispensable) para entender las bases...",
                      "keyConcept": "📖 Domina la teoría: (Fórmulas, reglas y conceptos clave explicados directamente)"
                    },
                    {
                      "title": "Paso 2: Aplicación y método",
                      "description": "🌐 Infórmate: Busca en la web sobre (siguiente concepto de soporte)...",
                      "keyConcept": "📖 Domina la teoría: (Procedimiento práctico y reglas esenciales)"
                    },
                    {
                      "title": "Paso 3: Casos complejos y resolución",
                      "description": "🌐 Infórmate: Busca en la web sobre (errores comunes y trucos del tema)...",
                      "keyConcept": "📖 Domina la teoría: (Estrategia de análisis paso a paso)"
                    }
                  ]
                }
            """.trimIndent()

            val request = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt)),
                        role = "user"
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.3f
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            text = "Eres un generador estricto de JSON para planes de estudio académicos directos y prácticos. Devuelve SOLO JSON válido sin comentarios ni texto adicional."
                        )
                    )
                )
            )

            val rawResponse = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                val apiRes = AIApiClient.geminiService.generateContent(apiKey, request)
                apiRes.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            } else {
                createFallbackPlanJson(subjectOrTopic)
            }

            val cleanJson = cleanJsonString(rawResponse)
            val adapter = AIApiClient.moshi.adapter(GeneratedStudyPlanJson::class.java)
            val parsedPlan = adapter.fromJson(cleanJson)
                ?: throw IllegalStateException("No se pudo interpretar el JSON del plan de estudio.")

            val planId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val planEntity = StudyPlanEntity(
                id = planId,
                userId = userId,
                title = parsedPlan.title,
                subject = parsedPlan.subject,
                description = parsedPlan.description,
                estimatedDuration = parsedPlan.estimatedDuration ?: "Flexible",
                createdAt = now
            )

            val topicEntities = parsedPlan.topics.mapIndexed { index, t ->
                StudyTopicEntity(
                    id = UUID.randomUUID().toString(),
                    planId = planId,
                    title = t.title,
                    description = t.description,
                    keyConcept = t.keyConcept ?: "",
                    orderIndex = index,
                    status = "PENDING",
                    miniLessonJson = null,
                    updatedAt = now
                )
            }

            dao.insertStudyPlan(planEntity)
            dao.insertTopics(topicEntities)

            Result.success(planEntity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Gemini / Groq Mini Real-World Practice & Interactive Evaluation ---
    suspend fun getOrGeneratePracticeProblems(
        topic: StudyTopicEntity,
        subject: String
    ): Result<PracticeSessionJson> = withContext(Dispatchers.IO) {
        try {
            if (!topic.miniLessonJson.isNullOrBlank()) {
                val adapter = AIApiClient.moshi.adapter(PracticeSessionJson::class.java)
                val cached = adapter.fromJson(topic.miniLessonJson)
                if (cached != null) {
                    return@withContext Result.success(cached)
                }
            }

            val apiKey = AIApiClient.getGeminiApiKey()
            val prompt = """
                El estudiante va a resolver una práctica interactiva para el tema: "${topic.title}" de la materia "$subject".
                Bases teóricas: "${topic.keyConcept}".
                Referencia: "${topic.description}".

                Genera una sesión de práctica interactiva con 2 a 3 mini problemas de la vida real o situaciones prácticas aplicadas.
                
                REGLA CRÍTICA: NO incluyas saludos ni texto extra. DEVUELVE ÚNICAMENTE UN OBJETO JSON con este formato exacto:
                {
                  "topicTitle": "${topic.title}",
                  "realWorldContext": "Breve explicación de cómo se aplica este tema en el mundo real o situaciones cotidianas.",
                  "theoryTip": "Tip clave para resolver los problemas con éxito.",
                  "problems": [
                    {
                      "question": "Planteamiento del mini problema real 1 con datos concretos.",
                      "options": ["Opción A", "Opción B", "Opción C", "Opción D"],
                      "correctOptionIndex": 0,
                      "stepByStepExplanation": "Explicación paso a paso de por qué esta opción es la correcta y el procedimiento para resolverlo."
                    },
                    {
                      "question": "Planteamiento del mini problema real 2 con aplicación práctica.",
                      "options": ["Opción A", "Opción B", "Opción C", "Opción D"],
                      "correctOptionIndex": 1,
                      "stepByStepExplanation": "Explicación clara del procedimiento paso a paso."
                    }
                  ]
                }
            """.trimIndent()

            val request = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt)),
                        role = "user"
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.3f
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(text = "Eres un profesor creador de prácticas interactivas con problemas de la vida real. Responde ÚNICAMENTE en JSON válido.")
                    )
                )
            )

            val rawResponse = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                val apiRes = AIApiClient.geminiService.generateContent(apiKey, request)
                apiRes.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            } else {
                createFallbackPracticeJson(topic, subject)
            }

            val cleanJson = cleanJsonString(rawResponse)
            val adapter = AIApiClient.moshi.adapter(PracticeSessionJson::class.java)
            val parsedPractice = adapter.fromJson(cleanJson)
                ?: throw IllegalStateException("No se pudo interpretar el JSON de la práctica.")

            dao.saveTopicLessonAndStatus(topic.id, cleanJson, topic.status, System.currentTimeMillis())

            Result.success(parsedPractice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Backwards compatible alias
    suspend fun getOrGenerateMiniLessonAndQuiz(
        topic: StudyTopicEntity,
        subject: String
    ): Result<PracticeSessionJson> = getOrGeneratePracticeProblems(topic, subject)

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        return clean.trim()
    }

    private fun createFallbackPlanJson(topic: String): String {
        return """
        {
          "title": "Plan de Estudio: $topic",
          "subject": "General",
          "description": "Ruta interactiva paso a paso para dominar $topic con fundamentos, teoría y práctica real.",
          "estimatedDuration": "3 días",
          "topics": [
            {
              "title": "Paso 1: Fundamentos esenciales",
              "description": "🌐 Infórmate: Busca en la web sobre los conceptos previos y definiciones base de $topic para entender el origen del tema.",
              "keyConcept": "📖 Domina la teoría: Comprende las reglas principales, definiciones operativas y fórmulas base antes de resolver ejercicios."
            },
            {
              "title": "Paso 2: Métodos y procedimientos prácticos",
              "description": "🌐 Infórmate: Busca en la web sobre ejemplos resueltos paso a paso y trucos de despeje de $topic.",
              "keyConcept": "📖 Domina la teoría: Procedimiento estándar en 3 fases: 1) Extraer variables, 2) Aplicar fórmula directa, 3) Comprobar unidades."
            },
            {
              "title": "Paso 3: Problemas de aplicación real",
              "description": "🌐 Infórmate: Busca en la web sobre cómo se aplica $topic en proyectos reales y situaciones cotidianas.",
              "keyConcept": "📖 Domina la teoría: Análisis de escenarios cotidianos, identificación de variables ocultas y toma de decisiones."
            }
          ]
        }
        """.trimIndent()
    }

    private fun createFallbackPracticeJson(topic: StudyTopicEntity, subject: String): String {
        return """
        {
          "topicTitle": "${topic.title}",
          "realWorldContext": "En situaciones reales de $subject, este concepto te permite tomar decisiones basadas en datos y resolver problemas técnicos o cotidianos.",
          "theoryTip": "Recuerda identificar primero los datos conocidos y la variable objetivo antes de hacer cálculos.",
          "problems": [
            {
              "question": "Un estudiante necesita calcular el resultado óptimo aplicando las reglas de '${topic.title}'. ¿Cuál es la primera acción recomendada?",
              "options": [
                "Identificar los datos conocidos y la fórmula aplicable",
                "Adivinar el resultado sin verificar",
                "Ignorar las condiciones del problema",
                "Copiar un resultado aleatorio"
              ],
              "correctOptionIndex": 0,
              "stepByStepExplanation": "Excelente: En cualquier problema real, estructurar los datos conocidos y el objetivo es el 80% del éxito."
            },
            {
              "question": "En un caso práctico donde se deben comprobar los resultados obtenidos, ¿qué método es el más confiable?",
              "options": [
                "Sustituir los valores en la ecuación original para verificar igualdad",
                "Asumir que el primer intento siempre es correcto",
                "Borrar todo y no verificar",
                "Cambiar los datos si el resultado no gusta"
              ],
              "correctOptionIndex": 0,
              "stepByStepExplanation": "¡Exacto! La verificación por sustitución confirma la validez matemática y lógica del resultado."
            }
          ]
        }
        """.trimIndent()
    }

    private fun createFallbackLessonJson(topic: StudyTopicEntity, subject: String): String = createFallbackPracticeJson(topic, subject)
}
