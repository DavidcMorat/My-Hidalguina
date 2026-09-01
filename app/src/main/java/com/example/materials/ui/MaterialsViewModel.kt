package com.example.materials.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.materials.data.MaterialModel
import com.example.materials.data.MaterialRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MaterialsViewModel(private val repository: MaterialRepository = MaterialRepository()) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    private val _materials = MutableStateFlow<List<MaterialModel>>(emptyList())
    val materials: StateFlow<List<MaterialModel>> = _materials.asStateFlow()

    private val _selectedSubject = MutableStateFlow("Todas")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackBarMessage = MutableSharedFlow<String>()
    val snackBarMessage: SharedFlow<String> = _snackBarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.getMaterialsFlow().collect { list ->
                _materials.value = list
            }
        }
    }

    fun setSelectedSubject(subject: String) {
        _selectedSubject.value = subject
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openCreateDialog() {
        _showCreateDialog.value = true
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createMaterial(
        title: String,
        description: String,
        subject: String,
        fileType: String,
        fileUrl: String,
        fileName: String,
        fileSize: String,
        targetType: String,
        grade: String,
        section: String,
        targetClassrooms: List<String> = emptyList()
    ) {
        if (title.isBlank() || subject.isBlank()) {
            viewModelScope.launch { _snackBarMessage.emit("Por favor escribe el título y la materia del material") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val teacherName = auth.currentUser?.displayName ?: "Docente"
            val res = repository.createMaterial(
                title = title.trim(),
                description = description.trim(),
                subject = subject.trim(),
                teacherId = currentUserId,
                teacherName = teacherName,
                fileType = fileType,
                fileUrl = fileUrl.trim(),
                fileName = if (fileName.isNotBlank()) fileName.trim() else title.trim(),
                fileSize = if (fileSize.isNotBlank()) fileSize.trim() else "Recurso Digital",
                targetType = targetType,
                grade = if (targetType == "SPECIFIC") grade.trim() else "",
                section = if (targetType == "SPECIFIC") section.trim() else "",
                targetClassrooms = targetClassrooms
            )
            _isLoading.value = false
            if (res.isSuccess) {
                _showCreateDialog.value = false
                _snackBarMessage.emit("Material compartido con éxito 📚")
            } else {
                _snackBarMessage.emit("Error al compartir material: ${res.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun deleteMaterial(materialId: String) {
        viewModelScope.launch {
            repository.deleteMaterial(materialId)
            _snackBarMessage.emit("Material eliminado")
        }
    }
}
