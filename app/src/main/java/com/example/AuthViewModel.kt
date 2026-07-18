package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Ingresa tu correo electrónico y contraseña")
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

    fun signUpWithEmail(email: String, pass: String, username: String, studentName: String, grade: String, section: String) {
        if (email.isBlank() || pass.isBlank() || username.isBlank() || studentName.isBlank() || grade.isBlank() || section.isBlank()) {
            _authState.value = AuthState.Error("Todos los campos son obligatorios")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val user = result.user
                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    
                    // Save to Firestore
                    val userData = hashMapOf(
                        "uid" to user.uid,
                        "displayName" to username,
                        "studentName" to studentName,
                        "grade" to grade,
                        "section" to section,
                        "email" to email
                    )
                    db.collection("users").document(user.uid).set(userData).await()
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al registrarse")
            }
        }
    }

    fun updateDisplayName(displayName: String) {
        if (displayName.isBlank()) {
            _authState.value = AuthState.Error("El nombre no puede estar vacío")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = auth.currentUser
                if (user != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    
                    try {
                        // Update in Firestore
                        db.collection("users").document(user.uid).set(hashMapOf("displayName" to displayName), SetOptions.merge()).await()
                    } catch (e: Exception) {
                        // Ignore firestore permission errors for profile update if rules are strict
                    }
                    
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Usuario no autenticado")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al actualizar el perfil")
            }
        }
    }
}
