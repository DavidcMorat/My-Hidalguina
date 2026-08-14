package com.example.tutor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutor.data.StudyDatabase
import com.example.tutor.data.StudyPlanEntity
import com.example.tutor.data.StudyPlanWithTopics
import com.example.tutor.data.StudyRepository
import com.example.tutor.data.StudyTopicEntity
import com.example.tutor.data.TutorChatMessageEntity
import com.example.tutor.model.PracticeSessionJson
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AITutorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository = StudyRepository(
        StudyDatabase.getDatabase(application).studyPlanDao()
    )
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val userId: String get() = auth.currentUser?.uid ?: "local_student"

    // Tab state (0: Planes de Estudio / Aprendizaje, 1: Consultar Tutor IA)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab

    // UI state
    val studyPlans: StateFlow<List<StudyPlanWithTopics>> = repository.getStudyPlans(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<TutorChatMessageEntity>> = repository.getChatMessages(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAskingTutor = MutableStateFlow(false)
    val isAskingTutor: StateFlow<Boolean> = _isAskingTutor

    private val _isGeneratingPlan = MutableStateFlow(false)
    val isGeneratingPlan: StateFlow<Boolean> = _isGeneratingPlan

    private val _isLoadingLesson = MutableStateFlow(false)
    val isLoadingLesson: StateFlow<Boolean> = _isLoadingLesson

    private val _activeLesson = MutableStateFlow<PracticeSessionJson?>(null)
    val activeLesson: StateFlow<PracticeSessionJson?> = _activeLesson

    private val _activeTopic = MutableStateFlow<StudyTopicEntity?>(null)
    val activeTopic: StateFlow<StudyTopicEntity?> = _activeTopic

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _showPracticeDialog = MutableStateFlow(false)
    val showPracticeDialog: StateFlow<Boolean> = _showPracticeDialog

    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage

    // Start clean with NO predefined dummy data: purely student-created plans and chat

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun openCreatePlanDialog(initialTopic: String = "") {
        _showCreateDialog.value = true
    }

    fun closeCreatePlanDialog() {
        _showCreateDialog.value = false
    }

    fun closePracticeDialog() {
        _showPracticeDialog.value = false
        _activeLesson.value = null
        _activeTopic.value = null
    }

    fun openPractice(topic: StudyTopicEntity, subject: String) {
        viewModelScope.launch {
            _activeTopic.value = topic
            _showPracticeDialog.value = true
            _isLoadingLesson.value = true

            val result = repository.getOrGeneratePracticeProblems(topic, subject)
            _isLoadingLesson.value = false

            if (result.isSuccess) {
                _activeLesson.value = result.getOrNull()
            } else {
                _showPracticeDialog.value = false
                _snackBarMessage.emit("No se pudo generar la práctica: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun recordPracticeOutcome(topicId: String, isPassed: Boolean, scoreText: String) {
        viewModelScope.launch {
            val status = if (isPassed) "LOGRADO" else "DIFICULTAD"
            repository.updateTopicStatus(topicId, status)
            if (isPassed) {
                _snackBarMessage.emit("¡Felicidades! Calificación: $scoreText. ¡Tema marcado como LOGRADO! 🌟")
            } else {
                _snackBarMessage.emit("Calificación: $scoreText. Marcado como 'Necesita Ayuda'. ¡Puedes consultar al Tutor IA!")
            }
        }
    }

    fun askTutorAboutError(topicTitle: String, problemQuestion: String, solutionExplanation: String) {
        val userPrompt = "Tengo duda con este problema de $topicTitle: \"$problemQuestion\". Me equivoqué y necesito una pista socrática para entender cómo resolverlo."
        sendMessageToTutor(userPrompt)
        _currentTab.value = 1
        closePracticeDialog()
    }


    fun sendMessageToTutor(userText: String) {
        if (userText.isBlank() || _isAskingTutor.value) return

        viewModelScope.launch {
            _isAskingTutor.value = true
            val history = chatMessages.value
            val result = repository.askGroqTutor(userId, userText.trim(), history)
            _isAskingTutor.value = false
            if (result.isFailure) {
                _snackBarMessage.emit(result.exceptionOrNull()?.message ?: "Error al consultar con el Tutor IA")
            }
        }
    }

    fun generateStudyPlan(subjectOrTopic: String, notes: String? = null) {
        if (subjectOrTopic.isBlank() || _isGeneratingPlan.value) return

        viewModelScope.launch {
            _isGeneratingPlan.value = true
            val result = repository.generateStudyPlanWithGemini(userId, subjectOrTopic.trim(), notes)
            _isGeneratingPlan.value = false
            _showCreateDialog.value = false

            if (result.isSuccess) {
                _snackBarMessage.emit("¡Plan de estudio interactivo generado con éxito!")
                _currentTab.value = 0 // Switch to plans tab
            } else {
                _snackBarMessage.emit("Error al generar el plan: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun markTopicAchieved(topicId: String) {
        viewModelScope.launch {
            repository.updateTopicStatus(topicId, "LOGRADO")
            _snackBarMessage.emit("¡Felicidades! Tema marcado como Logrado 🎉")
        }
    }

    fun markTopicDifficulty(topic: StudyTopicEntity, subject: String) {
        openPractice(topic, subject)
    }

    fun completeEvaluationSuccess(topicId: String) {
        viewModelScope.launch {
            repository.updateTopicStatus(topicId, "LOGRADO")
            closePracticeDialog()
            _snackBarMessage.emit("¡Excelente! Has superado la práctica y el tema quedó LOGRADO 🏆")
        }
    }

    fun deleteStudyPlan(planId: String) {
        viewModelScope.launch {
            repository.deletePlan(planId)
            _snackBarMessage.emit("Plan de estudio eliminado.")
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat(userId)
            _snackBarMessage.emit("Historial de consultas reiniciado.")
        }
    }
}
