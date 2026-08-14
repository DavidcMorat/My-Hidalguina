package com.example.announcements.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class AnnouncementModel(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val teacherId: String = "",
    val teacherName: String = "Docente",
    val subject: String = "General",
    val targetType: String = "GLOBAL", // "GLOBAL" or "SPECIFIC"
    val grade: String = "", // e.g. "3" if specific
    val section: String = "", // e.g. "B" if specific
    val targetClassrooms: List<String> = emptyList(), // e.g. ["1A", "1B", "2A"]
    val priority: String = "NORMAL", // "NORMAL", "IMPORTANTE", "URGENTE"
    val createdAt: Long = System.currentTimeMillis()
)

class AnnouncementRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun getAnnouncementsFlow(): Flow<List<AnnouncementModel>> = callbackFlow {
        val listener = db.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    docToAnnouncement(doc)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createAnnouncement(
        title: String,
        content: String,
        teacherId: String,
        teacherName: String,
        subject: String,
        targetType: String,
        grade: String = "",
        section: String = "",
        targetClassrooms: List<String> = emptyList(),
        priority: String = "NORMAL"
    ): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to id,
                "title" to title,
                "content" to content,
                "teacherId" to teacherId,
                "teacherName" to teacherName,
                "subject" to subject,
                "targetType" to targetType,
                "grade" to grade,
                "section" to section,
                "targetClassrooms" to targetClassrooms,
                "priority" to priority,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("announcements").document(id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(id: String): Result<Unit> {
        return try {
            db.collection("announcements").document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun docToAnnouncement(doc: DocumentSnapshot): AnnouncementModel? {
        return try {
            val rawClassrooms = (doc.get("targetClassrooms") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            AnnouncementModel(
                id = doc.getString("id") ?: doc.id,
                title = doc.getString("title") ?: "",
                content = doc.getString("content") ?: "",
                teacherId = doc.getString("teacherId") ?: "",
                teacherName = doc.getString("teacherName") ?: "Docente",
                subject = doc.getString("subject") ?: "General",
                targetType = doc.getString("targetType") ?: "GLOBAL",
                grade = doc.getString("grade") ?: "",
                section = doc.getString("section") ?: "",
                targetClassrooms = rawClassrooms,
                priority = doc.getString("priority") ?: "NORMAL",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
