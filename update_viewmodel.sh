#!/bin/bash
cat app/src/main/java/com/example/chat/ChatViewModel.kt | sed '/private fun listenForIncomingMessages() {/,/^    }/d' > temp.kt
cat << 'INNER' >> temp.kt
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
}
INNER
mv temp.kt app/src/main/java/com/example/chat/ChatViewModel.kt
