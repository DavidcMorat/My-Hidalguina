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
                    db.collection("messages").document(receiverId)
                        .collection("pending").document(messageId)
                        .set(firestoreMsg).await()
                } catch (e: Exception) {
                    // Handle failure
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
        
        db.collection("messages").document(myUid).collection("pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                
                viewModelScope.launch {
                    var hasNewUnread = false
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
                        }
                        
                        // Delete from Firestore to maintain privacy
                        db.collection("messages").document(myUid)
                            .collection("pending").document(id)
                            .delete()
                    }
                    if (hasNewUnread) {
                        _unreadTrigger.value += 1
                    }
                }
            }
    }
}
