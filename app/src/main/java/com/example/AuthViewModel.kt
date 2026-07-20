package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
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

class AuthViewModel(application: Application) : AndroidViewModel(application) {
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

    fun signUpWithEmail(
        email: String,
        pass: String,
        username: String,
        realName: String,
        role: String,
        grade: String,
        section: String,
        area: String = ""
    ) {
        if (email.isBlank() || pass.isBlank() || username.isBlank() || realName.isBlank() || grade.isBlank() || section.isBlank() || (role == "teacher" && area.isBlank())) {
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
                    
                    // Save locally in SharedPreferences as backup
                    val sharedPrefs = getApplication<Application>().getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().apply {
                        putString("role_backup_${user.uid}", role)
                        putString("studentName_backup_${user.uid}", realName)
                        putString("grade_backup_${user.uid}", grade)
                        putString("section_backup_${user.uid}", section)
                        putString("displayName_backup_${user.uid}", username)
                        putString("area_backup_${user.uid}", area)
                        apply()
                    }

                    // Save to Firestore
                    val userData = hashMapOf(
                        "uid" to user.uid,
                        "displayName" to username,
                        "role" to role,
                        "studentName" to realName,
                        "grade" to grade,
                        "section" to section,
                        "area" to area,
                        "email" to email
                    )
                    try {
                        db.collection("users").document(user.uid).set(userData).await()
                    } catch (e: Exception) {
                        // Log or ignore Firestore failures during sign up so the user is not stuck,
                        // as they can now set/save these values directly in the Profile screen.
                    }
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al registrarse")
            }
        }
    }

    fun updateProfile(displayName: String, realName: String, grade: String, section: String, role: String = "student", area: String = "") {
        if (displayName.isBlank() || realName.isBlank() || grade.isBlank() || section.isBlank() || (role == "teacher" && area.isBlank())) {
            _authState.value = AuthState.Error("Todos los campos son obligatorios")
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
                    
                    // Save locally in SharedPreferences as backup
                    val sharedPrefs = getApplication<Application>().getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().apply {
                        putString("role_backup_${user.uid}", role)
                        putString("studentName_backup_${user.uid}", realName)
                        putString("grade_backup_${user.uid}", grade)
                        putString("section_backup_${user.uid}", section)
                        putString("displayName_backup_${user.uid}", displayName)
                        putString("area_backup_${user.uid}", area)
                        apply()
                    }

                    // Update in Firestore
                    val userData = hashMapOf(
                        "displayName" to displayName,
                        "role" to role,
                        "studentName" to realName,
                        "grade" to grade,
                        "section" to section,
                        "area" to area
                    )
                    db.collection("users").document(user.uid).set(userData, SetOptions.merge()).await()
                    
                    _authState.value = AuthState.Success
                } else {
                    _authState.value = AuthState.Error("Usuario no autenticado")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al actualizar el perfil")
            }
        }
    }

    fun clearAuthState() {
        _authState.value = AuthState.Idle
    }
}
