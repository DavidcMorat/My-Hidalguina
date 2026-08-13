package com.example.chat

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class ChatUserWithStatus(
    val user: ChatUser,
    val lastTimestamp: Long,
    val hasUnread: Boolean,
    val lastMessageText: String
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val chatDao = ChatDatabase.getDatabase(application).chatDao()

    val myUid: String
        get() = try { auth.currentUser?.uid ?: "" } catch (e: Exception) { "" }

    private var realtimeDbRef: DatabaseReference? = null
    private var childEventListener: ChildEventListener? = null
    private var isSessionActive = false

    private val _searchResults = MutableStateFlow<List<ChatUser>>(emptyList())
    val searchResults: StateFlow<List<ChatUser>> = _searchResults

    private val _classroomUsers = MutableStateFlow<List<ChatUser>>(emptyList())
    val classroomUsers: StateFlow<List<ChatUser>> = _classroomUsers

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _unreadTrigger = MutableStateFlow(0)

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

    init {
        if (myUid.isNotEmpty()) {
            viewModelScope.launch {
                val existing = withContext(Dispatchers.IO) { chatDao.getUser(myUid) }
                if (existing == null) {
                    val displayName = try { auth.currentUser?.displayName ?: "Mi Chat (Local)" } catch (e: Exception) { "Mi Chat" }
                    val selfUser = ChatUser(myUid, displayName)
                    withContext(Dispatchers.IO) { chatDao.insertUser(selfUser) }
                }
            }
        }
    }

    fun activateChatSession() {
        if (isSessionActive || myUid.isEmpty()) return
        isSessionActive = true

        try {
            FirebaseDatabase.getInstance().goOnline()
            startListeningToRealtimeDbMessages()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        loadClassroomUsers()
    }

    fun deactivateChatSession() {
        if (!isSessionActive) return
        isSessionActive = false

        stopListeningToRealtimeDbMessages()

        try {
            FirebaseDatabase.getInstance().goOffline()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startListeningToRealtimeDbMessages() {
        if (myUid.isEmpty()) return

        try {
            val ref = FirebaseDatabase.getInstance().getReference("messages").child(myUid)
            realtimeDbRef = ref

            val listener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    processIncomingMessageSnapshot(snapshot)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    processIncomingMessageSnapshot(snapshot)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }

            childEventListener = listener
            ref.addChildEventListener(listener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopListeningToRealtimeDbMessages() {
        try {
            if (realtimeDbRef != null && childEventListener != null) {
                realtimeDbRef?.removeEventListener(childEventListener!!)
                childEventListener = null
                realtimeDbRef = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processIncomingMessageSnapshot(snapshot: DataSnapshot) {
        val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: return
        val senderId = snapshot.child("senderId").getValue(String::class.java) ?: return
        val text = snapshot.child("text").getValue(String::class.java) ?: return
        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

        viewModelScope.launch {
            var user = withContext(Dispatchers.IO) { chatDao.getUser(senderId) }
            if (user == null) {
                try {
                    val userDoc = firestore.collection("users").document(senderId).get().await()
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
                val sharedPrefs = getApplication<Application>().getSharedPreferences("chat_unread_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("unread_$senderId", true).apply()
                _unreadTrigger.value += 1
            }

            try {
                snapshot.ref.removeValue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadClassroomUsers() {
        viewModelScope.launch {
            try {
                val myDoc = try {
                    firestore.collection("users").document(myUid).get().await()
                } catch (e: Exception) {
                    null
                }

                val sharedPrefs = getApplication<Application>().getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
                val grade = myDoc?.getString("grade") ?: sharedPrefs.getString("grade_backup_$myUid", null) ?: return@launch
                val section = myDoc?.getString("section") ?: sharedPrefs.getString("section_backup_$myUid", null) ?: return@launch

                val result = firestore.collection("users")
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
            withContext(Dispatchers.IO) { chatDao.insertMessage(chatMessage) }

            if (receiverId != myUid) {
                val rtdbMessage = hashMapOf(
                    "id" to messageId,
                    "senderId" to myUid,
                    "receiverId" to receiverId,
                    "text" to text,
                    "timestamp" to timestamp
                )

                try {
                    FirebaseDatabase.getInstance().getReference("messages")
                        .child(receiverId)
                        .child(messageId)
                        .setValue(rtdbMessage)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun markChatAsRead(otherUserId: String) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("chat_unread_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("unread_$otherUserId", false).apply()
        _unreadTrigger.value += 1
    }

    override fun onCleared() {
        super.onCleared()
        deactivateChatSession()
    }
}
