package com.example.chat

import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

object SupabaseManager {
    val client: SupabaseClient by lazy {
        AppLogger.d("SUPABASE_DEBUG", "Initializing SupabaseClient")
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        AppLogger.d("SUPABASE_DEBUG", "SUPABASE_URL loaded: ${url.isNotEmpty()}, starts with https: ${url.startsWith("https")}")
        AppLogger.d("SUPABASE_DEBUG", "SUPABASE_ANON_KEY loaded: ${key.isNotEmpty()}, length: ${key.length}")
        
        try {
            createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = key
            ) {
                install(Postgrest)
                install(Realtime)
            }.also {
                AppLogger.d("SUPABASE_DEBUG", "SupabaseClient created successfully")
            }
        } catch (e: Exception) {
            AppLogger.e("SUPABASE_DEBUG", "CRITICAL ERROR: Failed to create SupabaseClient: ${e.message}", e)
            throw e
        }
    }

    @Serializable
    data class SupabaseMessage(
        val id: String,
        val sender_id: String,
        val receiver_id: String,
        val text: String,
        val timestamp: Long,
        val is_gif: Boolean = false
    )

    suspend fun sendMessage(message: SupabaseMessage): Boolean {
        AppLogger.d("SUPABASE_DEBUG", "SupabaseManager.sendMessage() invoked")
        AppLogger.d("SUPABASE_DEBUG", "Message Payload: $message")
        return try {
            AppLogger.d("SUPABASE_DEBUG", "Executing client.postgrest['messages'].insert(message)")
            val result = client.postgrest["messages"].insert(message)
            AppLogger.d("SUPABASE_DEBUG", "Insert operation completed. Result: $result")
            true
        } catch (e: Exception) {
            AppLogger.e("SUPABASE_DEBUG", "Exception in SupabaseManager.sendMessage(): ${e.message}", e)
            AppLogger.e("SUPABASE_DEBUG", "StackTrace: ", e)
            false
        }
    }

    suspend fun fetchPendingMessages(myUid: String): List<SupabaseMessage> {
        AppLogger.d("SUPABASE_DEBUG", "fetchPendingMessages() invoked for uid: $myUid")
        return try {
            val result = client.postgrest["messages"]
                .select {
                    filter {
                        eq("receiver_id", myUid)
                    }
                }.decodeList<SupabaseMessage>()
            AppLogger.d("SUPABASE_DEBUG", "fetchPendingMessages() returned ${result.size} messages")
            result
        } catch (e: Exception) {
            AppLogger.e("SUPABASE_DEBUG", "Exception in fetchPendingMessages(): ${e.message}", e)
            emptyList()
        }
    }

    suspend fun listenForNewMessages(myUid: String): Flow<SupabaseMessage> {
        AppLogger.d("SUPABASE_DEBUG", "listenForNewMessages() invoked for uid: $myUid")
        val channel = client.realtime.channel("public-messages")
        
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
        }.mapNotNull { action ->
            try {
                AppLogger.d("SUPABASE_DEBUG", "Realtime message received: ${action.record}")
                val record = action.record
                val recId = record["receiver_id"]?.jsonPrimitive?.content
                if (recId == myUid) {
                    val msg = SupabaseMessage(
                        id = record["id"]?.jsonPrimitive?.content ?: "",
                        sender_id = record["sender_id"]?.jsonPrimitive?.content ?: "",
                        receiver_id = recId,
                        text = record["text"]?.jsonPrimitive?.content ?: "",
                        timestamp = record["timestamp"]?.jsonPrimitive?.content?.toLongOrNull() ?: System.currentTimeMillis(),
                        is_gif = record["is_gif"]?.jsonPrimitive?.booleanOrNull ?: false
                    )
                    AppLogger.d("SUPABASE_DEBUG", "Realtime message matched our receiver_id, parsed as: $msg")
                    msg
                } else {
                    AppLogger.d("SUPABASE_DEBUG", "Realtime message ignored (receiver_id $recId != myUid $myUid)")
                    null
                }
            } catch (e: Exception) {
                AppLogger.e("SUPABASE_DEBUG", "Error parsing realtime message: ${e.message}", e)
                null
            }
        }
        
        try {
            AppLogger.d("SUPABASE_DEBUG", "Subscribing to Realtime channel 'public-messages'")
            channel.subscribe()
            AppLogger.d("SUPABASE_DEBUG", "Subscribed to Realtime channel successfully")
        } catch (e: Exception) {
            AppLogger.e("SUPABASE_DEBUG", "Error subscribing to Realtime channel: ${e.message}", e)
        }
        
        return flow
    }
}
