package com.example.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatDao = ChatDatabase.getDatabase(application).chatDao()

    val myUid = auth.currentUser?.uid ?: ""

    // State for user search
    private val _searchResults = MutableStateFlow<List<ChatUser>>(emptyList())
    val searchResults: StateFlow<List<ChatUser>> = _searchResults
    
    private val _classroomUsers = MutableStateFlow<List<ChatUser>>(emptyList())
    val classroomUsers: StateFlow<List<ChatUser>> = _classroomUsers
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // Local chats list
    val localUsers = chatDao.getAllUsers()

    init {
        // Start listening to incoming messages when the viewmodel is created
        listenForIncomingMessages()
        
        // Add current user to local chats to allow self-chat
        if (myUid.isNotEmpty()) {
            viewModelScope.launch {
                val existing = withContext(Dispatchers.IO) { chatDao.getUser(myUid) }
                if (existing == null) {
                    val displayName = auth.currentUser?.displayName ?: "Mi Chat (Local)"
                    val selfUser = ChatUser(myUid, displayName)
                    withContext(Dispatchers.IO) { chatDao.insertUser(selfUser) }
                }
            }
            loadClassroomUsers()
        }
    }
    
    private fun loadClassroomUsers() {
        viewModelScope.launch {
            try {
                val myDoc = db.collection("users").document(myUid).get().await()
                val grade = myDoc.getString("grade") ?: return@launch
                val section = myDoc.getString("section") ?: return@launch
                
                val result = db.collection("users")
                    .whereEqualTo("grade", grade)
                    .whereEqualTo("section", section)
                    .get().await()
                    
                val users = result.documents.mapNotNull { doc ->
                    val uid = doc.getString("uid")
                    val displayName = doc.getString("displayName")
                    val studentName = doc.getString("studentName")
                    // We can just append studentName to displayName for search purposes, 
                    // or just use displayName if we only have ChatUser(uid, displayName)
                    if (uid != null && displayName != null) {
                        val fullName = if (studentName != null) "$displayName ($studentName)" else displayName
                        ChatUser(uid, fullName)
                    } else null
                }
                _classroomUsers.value = users
            } catch (e: Exception) {
                _classroomUsers.value = emptyList()
            }
        }
    }

    fun searchUser(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        _isSearching.value = true
        val filtered = _classroomUsers.value.filter { 
            it.displayName.contains(query, ignoreCase = true) 
        }
        _searchResults.value = filtered
        _isSearching.value = false
    }

    fun startChatWithUser(user: ChatUser) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { chatDao.insertUser(user) }
        }
    }

    fun getMessages(otherUserId: String) = chatDao.getMessagesWithUser(myUid, otherUserId)

    fun sendMessage(receiverId: String, text: String) {
        if (text.isBlank()) return
        val messageId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        val chatMessage = ChatMessage(
            id = messageId,
            senderId = myUid,
            receiverId = receiverId,
            text = text,
            timestamp = timestamp,
            isSentByMe = true
        )

        viewModelScope.launch {
            // Save locally
            withContext(Dispatchers.IO) { chatDao.insertMessage(chatMessage) }
            
            if (receiverId != myUid) {
                // Send to Firestore under receiver's pending messages
                val firestoreMsg = hashMapOf(
                    "id" to messageId,
                    "senderId" to myUid,
                    "receiverId" to receiverId,
                    "text" to text,
                    "timestamp" to timestamp
                )
                
                try {
                    db.collection("messages").document(receiverId)
                        .collection("pending").document(messageId)
                        .set(firestoreMsg).await()
                } catch (e: Exception) {
                    // Handle failure
                }
            }
        }
    }

    private fun listenForIncomingMessages() {
        if (myUid.isEmpty()) return
        
        db.collection("messages").document(myUid).collection("pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                viewModelScope.launch {
                    for (doc in snapshot.documents) {
                        val senderId = doc.getString("senderId") ?: continue
                        val text = doc.getString("text") ?: continue
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val id = doc.getString("id") ?: continue

                        // We need to ensure we know the sender. If we don't, fetch their name from Firestore
                        var user = withContext(Dispatchers.IO) { chatDao.getUser(senderId) }
                        if (user == null) {
                            try {
                                val userDoc = db.collection("users").document(senderId).get().await()
                                val displayName = userDoc.getString("displayName") ?: "Desconocido"
                                user = ChatUser(senderId, displayName)
                                withContext(Dispatchers.IO) { chatDao.insertUser(user) }
                            } catch (e: Exception) {
                                user = ChatUser(senderId, "Desconocido")
                                withContext(Dispatchers.IO) { chatDao.insertUser(user) }
                            }
                        }

                        val chatMessage = ChatMessage(
                            id = id,
                            senderId = senderId,
                            receiverId = myUid,
                            text = text,
                            timestamp = timestamp,
                            isSentByMe = false
                        )
                        
                        withContext(Dispatchers.IO) { chatDao.insertMessage(chatMessage) }
                        
                        // Delete from Firestore to maintain privacy
                        db.collection("messages").document(myUid)
                            .collection("pending").document(id)
                            .delete()
                    }
                }
            }
    }
}
