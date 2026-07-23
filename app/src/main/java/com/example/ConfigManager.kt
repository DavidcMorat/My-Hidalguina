package com.example

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ConfigManager {
    suspend fun getGeminiKey(): String {
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("config").document("api_keys").get().await()
            val firestoreKey = doc.getString("gemini_key")
            if (!firestoreKey.isNullOrEmpty()) {
                firestoreKey
            } else {
                BuildConfig.GEMINI_API_KEY
            }
        } catch (e: Exception) {
            BuildConfig.GEMINI_API_KEY
        }
    }

    suspend fun getGiphyKey(): String {
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("config").document("api_keys").get().await()
            val firestoreKey = doc.getString("giphy_key")
            if (!firestoreKey.isNullOrEmpty()) {
                firestoreKey
            } else {
                BuildConfig.GIPHY_API_KEY
            }
        } catch (e: Exception) {
            BuildConfig.GIPHY_API_KEY
        }
    }
}
