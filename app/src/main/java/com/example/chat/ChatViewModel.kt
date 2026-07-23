package com.example.chat

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

data class ChatUserWithStatus(
    val user: ChatUser,
    val lastTimestamp: Long,
    val hasUnread: Boolean,
    val lastMessageText: String
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val chatDao = ChatDatabase.getDatabase(application).chatDao()

    val myUid = auth.currentUser?.uid ?: ""

    // Active chat screen tracking to prevent showing duplicate notifications
    var activeChatUserId: String? = null

    // Set of processed message IDs to prevent duplicate notification spam and double-processing
    private val processedMessageIds = java.util.Collections.synchronizedSet(HashSet<String>())

    // State for user search
    private val _searchResults = MutableStateFlow<List<ChatUser>>(emptyList())
    val searchResults: StateFlow<List<ChatUser>> = _searchResults
    
    private val _classroomUsers = MutableStateFlow<List<ChatUser>>(emptyList())
    val classroomUsers: StateFlow<List<ChatUser>> = _classroomUsers
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // Unread status triggers
    private val _unreadTrigger = MutableStateFlow(0)

    // Local chats list
    val localUsers = chatDao.getAllUsers()

    val localUsersWithStatus = combine(
        chatDao.getAllUsers(),
        chatDao.getAllMessagesFlow(),
        _unreadTrigger
    ) { users, messages, _ ->
        users.map { user ->
            val userMessages = messages.filter { 
                (it.senderId == myUid && it.receiverId == user.uid) || 
                (it.senderId == user.uid && it.receiverId == myUid)
            }
            val lastMsg = userMessages.maxByOrNull { it.timestamp }
            val lastTimestamp = lastMsg?.timestamp ?: 0L
            val lastText = lastMsg?.text ?: ""
            
            val sharedPrefs = getApplication<Application>().getSharedPreferences("chat_unread_prefs", Context.MODE_PRIVATE)
            val hasUnread = sharedPrefs.getBoolean("unread_${user.uid}", false)
            
            ChatUserWithStatus(user, lastTimestamp, hasUnread, lastText)
        }.sortedWith(
            compareByDescending<ChatUserWithStatus> { it.hasUnread }
                .thenByDescending { it.lastTimestamp }
        )
    }

    private val _favoritesPack = MutableStateFlow<StickerPack>(
        StickerPack("favorites", "Favoritos ❤️", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExMWhnM280cTFrbmdnYnptNXQ3N2l0amtrdGFleGlrczN1enJ1eDlpZCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/o7R0N2F/giphy.gif", emptyList())
    )

    val stickerPacks: StateFlow<List<StickerPack>> = _favoritesPack.map { favorites ->
        listOf(favorites) + StickerManager.defaultPacks
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _giphySearchResults = MutableStateFlow<List<Sticker>>(emptyList())
    val giphySearchResults: StateFlow<List<Sticker>> = _giphySearchResults

    private val _isGiphySearching = MutableStateFlow(false)
    val isGiphySearching: StateFlow<Boolean> = _isGiphySearching

    init {
        createNotificationChannel()
        // Load custom user stickers
        loadFavorites()
        loadTrendingGiphy()

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
                val myDoc = try {
                    db.collection("users").document(myUid).get().await()
                } catch (e: Exception) {
                    null
                }
                
                val sharedPrefs = getApplication<Application>().getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)
                val grade = myDoc?.getString("grade") ?: sharedPrefs.getString("grade_backup_$myUid", null) ?: return@launch
                val section = myDoc?.getString("section") ?: sharedPrefs.getString("section_backup_$myUid", null) ?: return@launch
                
                val result = db.collection("users")
                    .whereEqualTo("grade", grade)
                    .whereEqualTo("section", section)
                    .get().await()
                    
                val users = result.documents.mapNotNull { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    if (uid == myUid) return@mapNotNull null
                    
                    val displayName = doc.getString("displayName") ?: ""
                    val studentName = doc.getString("studentName") ?: ""
                    
                    val display = if (studentName.isNotEmpty() && displayName.isNotEmpty()) {
                        "$studentName ($displayName)"
                    } else if (studentName.isNotEmpty()) {
                        studentName
                    } else if (displayName.isNotEmpty()) {
                        displayName
                    } else {
                        "Estudiante"
                    }
                    ChatUser(uid, display)
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
                    // Optimized: Use arrayUnion to append message in a single operation without creating new documents
                    db.collection("messages").document(receiverId)
                        .update("pending_list", com.google.firebase.firestore.FieldValue.arrayUnion(firestoreMsg)).await()
                } catch (e: Exception) {
                    try {
                        // If the document doesn't exist yet, we create it
                        db.collection("messages").document(receiverId)
                            .set(hashMapOf("pending_list" to listOf(firestoreMsg)), com.google.firebase.firestore.SetOptions.merge()).await()
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            }
        }
    }

    fun loadFavorites() {
        _favoritesPack.value = StickerManager.loadFavorites(getApplication())
    }

    fun loadTrendingGiphy() {
        viewModelScope.launch {
            _isGiphySearching.value = true
            val trending = StickerManager.getTrendingGiphy()
            _giphySearchResults.value = trending
            _isGiphySearching.value = false
        }
    }

    fun searchGiphy(query: String) {
        if (query.isBlank()) {
            loadTrendingGiphy()
            return
        }
        viewModelScope.launch {
            _isGiphySearching.value = true
            val results = StickerManager.searchGiphy(query)
            _giphySearchResults.value = results
            _isGiphySearching.value = false
        }
    }

    fun saveStickerToFavorites(urlOrBase64Data: String, isGif: Boolean): Boolean {
        val saved = if (urlOrBase64Data.startsWith("http")) {
            StickerManager.saveFavoriteSticker(
                getApplication(),
                id = "sticker_${System.currentTimeMillis()}",
                url = urlOrBase64Data,
                isGif = isGif
            )
        } else {
            StickerManager.saveReceivedSticker(getApplication(), urlOrBase64Data, isGif) != null
        }
        if (saved) {
            loadFavorites()
        }
        return saved
    }

    fun sendSticker(receiverId: String, sticker: Sticker) {
        val url = sticker.url ?: ""
        if (url.isNotEmpty()) {
            val prefix = if (sticker.isGif) "[STICKER_GIF]:" else "[STICKER]:"
            sendMessage(receiverId, prefix + url)
        } else if (sticker.localPath != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val file = java.io.File(sticker.localPath)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val prefix = if (sticker.isGif) "[STICKER_GIF]:" else "[STICKER]:"
                        withContext(Dispatchers.Main) {
                            sendMessage(receiverId, prefix + base64Data)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun markChatAsRead(otherUserId: String) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("chat_unread_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("unread_$otherUserId", false).apply()
        _unreadTrigger.value += 1
    }

    private fun listenForIncomingMessages() {
        if (myUid.isEmpty()) return
        
        // Listen to legacy pending collection (batch delete optimization)
        db.collection("messages").document(myUid).collection("pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                
                viewModelScope.launch {
                    var hasNewUnread = false
                    val batch = db.batch()
                    var batchCount = 0
                    
                    for (doc in snapshot.documents) {
                        val senderId = doc.getString("senderId") ?: continue
                        val text = doc.getString("text") ?: continue
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val id = doc.getString("id") ?: continue

                        val isAlreadyProcessed = !processedMessageIds.add(id)

                        if (!isAlreadyProcessed) {
                            // We need to ensure we know the sender. If we don't, fetch their name from Firestore
                            var user = withContext(Dispatchers.IO) { chatDao.getUser(senderId) }
                            if (user == null) {
                                try {
                                    val userDoc = db.collection("users").document(senderId).get().await()
                                    val displayName = userDoc.getString("displayName") ?: ""
                                    val studentName = userDoc.getString("studentName") ?: ""
                                    val display = if (studentName.isNotEmpty() && displayName.isNotEmpty()) {
                                        "$studentName ($displayName)"
                                    } else if (studentName.isNotEmpty()) {
                                        studentName
                                    } else if (displayName.isNotEmpty()) {
                                        displayName
                                    } else {
                                        "Compañero"
                                    }
                                    user = ChatUser(senderId, display)
                                    withContext(Dispatchers.IO) { chatDao.insertUser(user) }
                                } catch (e: Exception) {
                                    user = ChatUser(senderId, "Compañero")
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
                            
                            if (senderId != myUid) {
                                val sharedPrefs = getApplication<Application>().getSharedPreferences("chat_unread_prefs", android.content.Context.MODE_PRIVATE)
                                sharedPrefs.edit().putBoolean("unread_$senderId", true).apply()
                                hasNewUnread = true
                                if (senderId != activeChatUserId) {
                                    showNotification(senderId, user?.displayName ?: "Compañero", text)
                                }
                            }
                        }
                        
                        // Add to batch delete to save unit deletions
                        val docRef = db.collection("messages").document(myUid)
                            .collection("pending").document(id)
                        batch.delete(docRef)
                        batchCount++
                    }
                    if (hasNewUnread) {
                        _unreadTrigger.value += 1
                    }
                    if (batchCount > 0) {
                        batch.commit()
                    }
                }
            }

        // Listen to optimized pending_list array
        db.collection("messages").document(myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                
                val pendingList = snapshot.get("pending_list") as? List<Map<String, Any>> ?: emptyList()
                if (pendingList.isEmpty()) return@addSnapshotListener
                
                viewModelScope.launch {
                    var hasNewUnread = false
                    
                    for (msgMap in pendingList) {
                        val senderId = msgMap["senderId"] as? String ?: continue
                        val text = msgMap["text"] as? String ?: continue
                        val timestamp = (msgMap["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        val id = msgMap["id"] as? String ?: continue

                        val isAlreadyProcessed = !processedMessageIds.add(id)

                        if (!isAlreadyProcessed) {
                            var user = withContext(Dispatchers.IO) { chatDao.getUser(senderId) }
                            if (user == null) {
                                try {
                                    val userDoc = db.collection("users").document(senderId).get().await()
                                    val displayName = userDoc.getString("displayName") ?: ""
                                    val studentName = userDoc.getString("studentName") ?: ""
                                    val display = if (studentName.isNotEmpty() && displayName.isNotEmpty()) {
                                        "$studentName ($displayName)"
                                    } else if (studentName.isNotEmpty()) {
                                        studentName
                                    } else if (displayName.isNotEmpty()) {
                                        displayName
                                    } else {
                                        "Compañero"
                                    }
                                    user = ChatUser(senderId, display)
                                    withContext(Dispatchers.IO) { chatDao.insertUser(user) }
                                } catch (e: Exception) {
                                    user = ChatUser(senderId, "Compañero")
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
                            
                            if (senderId != myUid) {
                                val sharedPrefs = getApplication<Application>().getSharedPreferences("chat_unread_prefs", android.content.Context.MODE_PRIVATE)
                                sharedPrefs.edit().putBoolean("unread_$senderId", true).apply()
                                hasNewUnread = true
                                if (senderId != activeChatUserId) {
                                    showNotification(senderId, user?.displayName ?: "Compañero", text)
                                }
                            }
                        }
                    }
                    if (hasNewUnread) {
                        _unreadTrigger.value += 1
                    }
                    
                    // Optimized clearance: Uses arrayRemove instead of unit deletions!
                    if (pendingList.isNotEmpty()) {
                        db.collection("messages").document(myUid)
                            .update("pending_list", com.google.firebase.firestore.FieldValue.arrayRemove(*pendingList.toTypedArray()))
                    }
                }
            }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Mensajes de Chat"
            val descriptionText = "Notificaciones de nuevos mensajes recibidos"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel("chat_messages_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getApplication<Application>().getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(senderId: String, senderName: String, messageText: String) {
        val context = getApplication<Application>()
        
        // Check notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        // Use a generic intent to open the app or MainActivity
        val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Parse special sticker markers to show a nice text like "Te envió un sticker"
        val displayBody = when {
            messageText.startsWith("[STICKER]:") || messageText.startsWith("[STICKER_GIF]:") -> "Te envió un sticker"
            else -> messageText
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, "chat_messages_channel")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(senderName)
            .setContentText(displayBody)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val notificationId = senderId.hashCode()
        notificationManager.notify(notificationId, builder.build())
    }
}
