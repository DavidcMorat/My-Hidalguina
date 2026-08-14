package com.example.tutor.model

import com.squareup.moshi.Json

// --- Groq Models (OpenAI compatible) ---
data class GroqChatRequest(
    val model: String = "openai/gpt-oss-20b",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int = 1024
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqChatResponse(
    val choices: List<GroqChoice>? = null
)

data class GroqChoice(
    val message: GroqMessage? = null
)

// --- Gemini Models ---
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: GeminiSchema? = null,
    val temperature: Float? = null
)

data class GeminiSchema(
    val type: String,
    val properties: Map<String, GeminiSchemaProperty>? = null,
    val required: List<String>? = null,
    val items: GeminiSchemaItem? = null
)

data class GeminiSchemaItem(
    val type: String,
    val properties: Map<String, GeminiSchemaProperty>? = null,
    val required: List<String>? = null
)

data class GeminiSchemaProperty(
    val type: String,
    val description: String? = null,
    val items: GeminiSchemaItem? = null
)

data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)

// --- Structured Study Plan Output from Gemini JSON ---
data class GeneratedStudyPlanJson(
    val title: String,
    val subject: String,
    val description: String,
    val estimatedDuration: String? = null,
    val topics: List<GeneratedTopicJson>
)

data class GeneratedTopicJson(
    val title: String,
    val description: String,
    val keyConcept: String? = null
)

// --- Structured Mini Lesson & Real World Practice Output ---
data class PracticeSessionJson(
    val topicTitle: String,
    val realWorldContext: String? = null,
    val theoryTip: String? = null,
    val problems: List<PracticeProblemJson>
)

data class PracticeProblemJson(
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val stepByStepExplanation: String
)

// Backwards compatibility alias
typealias MiniLessonAndQuizJson = PracticeSessionJson
typealias QuizQuestionJson = PracticeProblemJson

