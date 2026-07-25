package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initFirebase(this)
    }

    companion object {
        fun initFirebase(context: android.content.Context) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    try {
                        FirebaseApp.initializeApp(context)
                        Log.d("MainApplication", "FirebaseApp initialized automatically from resources")
                    } catch (e: Exception) {
                        Log.w("MainApplication", "Default initializeApp failed, trying explicit options: ${e.message}")
                        val options = FirebaseOptions.Builder()
                            .setProjectId("appmfhf")
                            .setApplicationId("1:573810459906:android:385cda9d797f97c5206e1b")
                            .setApiKey("AIzaSyB4Bo1RBTXC8t-6ef0Ng-G8Zcod-xmHa28")
                            .setGcmSenderId("573810459906")
                            .setStorageBucket("appmfhf.firebasestorage.app")
                            .build()
                        FirebaseApp.initializeApp(context, options)
                        Log.d("MainApplication", "FirebaseApp initialized manually with explicit options")
                    }
                } else {
                    Log.d("MainApplication", "FirebaseApp already initialized")
                }
            } catch (e: Exception) {
                Log.e("MainApplication", "CRITICAL ERROR initializing FirebaseApp", e)
            }
        }
    }
}
