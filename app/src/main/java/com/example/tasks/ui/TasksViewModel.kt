package com.example.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tasks.data.TaskModel
import com.example.tasks.data.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TasksViewModel(private val repository: TaskRepository = TaskRepository()) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    private val _tasks = MutableStateFlow<List<TaskModel>>(emptyList())
    val tasks: StateFlow<List<TaskModel>> = _tasks.asStateFlow()

    private val _filterTab = MutableStateFlow(0) // 0: Pendientes, 1: Entregadas, 2: Creadas por mí (si es docente)
    val filterTab: StateFlow<Int> = _filterTab.asStateFlow()

    private val _isTeacher = MutableStateFlow(false)
    val isTeacher: StateFlow<Boolean> = _isTeacher.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getTasksFlow().collect { list ->
                _tasks.value = list
            }
        }
    }

    fun setTeacherRole(isTeacherRole: Boolean) {
        _isTeacher.value = isTeacherRole
    }

    fun setFilterTab(tab: Int) {
        _filterTab.value = tab
    }

    fun openCreateDialog() {
        _showCreateDialog.value = true
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        val uid = currentUserId
        if (uid.isBlank()) return
        viewModelScope.launch {
            repository.toggleTaskCompleted(taskId, uid, isCompleted)
            _snackBarMessage.emit(if (isCompleted) "¡Tarea entregada con éxito! ⭐" else "Tarea marcada como pendiente")
        }
    }

    fun createTask(
        title: String,
        description: String,
        subject: String,
        dueDate: String,
        targetType: String,
        grade: String,
        section: String,
        targetClassrooms: List<String> = emptyList()
    ) {
        if (title.isBlank() || subject.isBlank() || dueDate.isBlank()) {
            viewModelScope.launch { _snackBarMessage.emit("Por favor completa los campos obligatorios") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val teacherName = auth.currentUser?.displayName ?: "Docente"
            val res = repository.createTask(
                title = title.trim(),
                description = description.trim(),
                subject = subject.trim(),
                teacherId = currentUserId,
                teacherName = teacherName,
                dueDate = dueDate.trim(),
                targetType = targetType,
                grade = if (targetType == "SPECIFIC") grade.trim() else "",
                section = if (targetType == "SPECIFIC") section.trim() else "",
                targetClassrooms = if (targetType == "GLOBAL") targetClassrooms else emptyList()
            )
            _isLoading.value = false
            if (res.isSuccess) {
                _showCreateDialog.value = false
                _snackBarMessage.emit("Tarea asignada y publicada para los alumnos exitosamente")
            } else {
                _snackBarMessage.emit("Error al publicar la tarea: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            _snackBarMessage.emit("Tarea eliminada")
        }
    }
}
