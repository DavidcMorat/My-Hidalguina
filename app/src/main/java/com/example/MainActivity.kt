package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val permission = "android.permission.POST_NOTIFICATIONS"
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val currentUserState = remember { mutableStateOf(com.google.firebase.auth.FirebaseAuth.getInstance().currentUser) }
                val userRoleState = remember { mutableStateOf("student") }
                val currentUser = currentUserState.value
                val userRole = userRoleState.value

                androidx.compose.runtime.DisposableEffect(Unit) {
                    val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
                        currentUserState.value = auth.currentUser
                    }
                    com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(listener)
                    onDispose {
                        com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(listener)
                    }
                }

                androidx.compose.runtime.LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        val uid = currentUser.uid
                        val context = this@MainActivity
                        val sharedPrefs = context.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)
                        val cachedRole = sharedPrefs.getString("role_backup_$uid", "student")
                        userRoleState.value = cachedRole ?: "student"

                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc != null) {
                                    val roleFromDb = doc.getString("role") ?: "student"
                                    userRoleState.value = roleFromDb
                                    sharedPrefs.edit().putString("role_backup_$uid", roleFromDb).apply()
                                }
                            }
                    } else {
                        userRoleState.value = "student"
                    }
                }

                val startDest = if (currentUser != null) "dashboard" else "login"
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("dashboard") {
                            if (userRole == "teacher") {
                                TeacherDashboard(
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                )
                            } else {
                                StudentDashboard(
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
