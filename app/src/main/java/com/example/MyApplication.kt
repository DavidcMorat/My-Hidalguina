package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    // Fallback to explicit options from google-services.json configuration
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:919509499769:android:49e8fc9d1f71325447b9a8")
                        .setApiKey("AIzaSyBlKWXl9_R0pjiME5SDhKvGJFy2m854wSU")
                        .setProjectId("my-hidalguina")
                        .setGcmSenderId("919509499769")
                        .setDatabaseUrl("https://my-hidalguina-default-rtdb.firebaseio.com")
                        .setStorageBucket("my-hidalguina.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.d("MyApplication", "FirebaseApp initialized with explicit fallback options")
                } else {
                    Log.d("MyApplication", "FirebaseApp initialized via default provider")
                }
            }
        } catch (e: Exception) {
            Log.e("MyApplication", "Standard Firebase initialization encountered an exception: ${e.message}", e)
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:919509499769:android:49e8fc9d1f71325447b9a8")
                    .setApiKey("AIzaSyBlKWXl9_R0pjiME5SDhKvGJFy2m854wSU")
                    .setProjectId("my-hidalguina")
                    .setGcmSenderId("919509499769")
                    .setDatabaseUrl("https://my-hidalguina-default-rtdb.firebaseio.com")
                    .setStorageBucket("my-hidalguina.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("MyApplication", "FirebaseApp recovered with fallback options")
            } catch (fallbackError: Exception) {
                Log.e("MyApplication", "Fatal error initializing Firebase fallback: ${fallbackError.message}", fallbackError)
            }
        }
    }
}
