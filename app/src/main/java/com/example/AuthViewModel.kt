package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUser
        get() = try { auth.currentUser } catch (e: Exception) { null }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Correo y contraseña son obligatorios")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error de inicio de sesión")
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onComplete: (String) -> Unit) {
        if (email.isBlank()) {
            onComplete("Ingresa tu correo electrónico para restablecer la contraseña.")
            return
        }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                onComplete("Se ha enviado un correo para restablecer tu contraseña.")
            } catch (e: Exception) {
                onComplete(e.message ?: "Error al enviar el correo de recuperación.")
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, username: String, studentName: String, grade: String, section: String) {
        if (email.isBlank() || pass.isBlank() || username.isBlank() || studentName.isBlank() || grade.isBlank() || section.isBlank()) {
            _authState.value = AuthState.Error("Todos los campos son obligatorios")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = authResult.user
                if (user != null) {
                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                        displayName = username
                    }
                    user.updateProfile(profileUpdates).await()

                    try {
                        user.sendEmailVerification().await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val sharedPrefs = getApplication<Application>().getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().apply {
                        putString("username_backup_${user.uid}", username)
                        putString("student_name_backup_${user.uid}", studentName)
                        putString("grade_backup_${user.uid}", grade)
                        putString("section_backup_${user.uid}", section)
                        apply()
                    }

                    val userData = hashMapOf(
                        "uid" to user.uid,
                        "email" to email,
                        "displayName" to username,
                        "studentName" to studentName,
                        "grade" to grade,
                        "section" to section
                    )

                    try {
                        db.collection("users").document(user.uid).set(userData).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al registrarse")
            }
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
