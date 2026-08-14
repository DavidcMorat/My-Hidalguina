package com.example.tasks.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class TaskModel(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "General",
    val teacherId: String = "",
    val teacherName: String = "Docente",
    val dueDate: String = "",
    val targetType: String = "SPECIFIC", // "GLOBAL" or "SPECIFIC"
    val grade: String = "",
    val section: String = "",
    val targetClassrooms: List<String> = emptyList(),
    val completedBy: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

class TaskRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun getTasksFlow(): Flow<List<TaskModel>> = callbackFlow {
        val listener = db.collection("tasks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    docToTask(doc)
                } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createTask(
        title: String,
        description: String,
        subject: String,
        teacherId: String,
        teacherName: String,
        dueDate: String,
        targetType: String = "SPECIFIC",
        grade: String = "",
        section: String = "",
        targetClassrooms: List<String> = emptyList()
    ): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            val data = hashMapOf(
                "id" to id,
                "title" to title,
                "description" to description,
                "subject" to subject,
                "teacherId" to teacherId,
                "teacherName" to teacherName,
                "dueDate" to dueDate,
                "targetType" to targetType,
                "grade" to grade,
                "section" to section,
                "targetClassrooms" to targetClassrooms,
                "completedBy" to emptyList<String>(),
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("tasks").document(id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(taskId: String, studentUid: String, isCompleted: Boolean): Result<Unit> {
        return try {
            val docRef = db.collection("tasks").document(taskId)
            val snap = docRef.get().await()
            val currentCompleted = (snap.get("completedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val updated = if (isCompleted) {
                if (!currentCompleted.contains(studentUid)) currentCompleted + studentUid else currentCompleted
            } else {
                currentCompleted.filter { it != studentUid }
            }
            docRef.update("completedBy", updated).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            db.collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun docToTask(doc: DocumentSnapshot): TaskModel? {
        val id = doc.getString("id") ?: doc.id
        val title = doc.getString("title") ?: return null
        val description = doc.getString("description") ?: ""
        val subject = doc.getString("subject") ?: "General"
        val teacherId = doc.getString("teacherId") ?: ""
        val teacherName = doc.getString("teacherName") ?: "Docente"
        val dueDate = doc.getString("dueDate") ?: ""
        val targetType = doc.getString("targetType") ?: "SPECIFIC"
        val grade = doc.getString("grade") ?: ""
        val section = doc.getString("section") ?: ""
        val targetClassrooms = (doc.get("targetClassrooms") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val completedBy = (doc.get("completedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

        return TaskModel(
            id = id,
            title = title,
            description = description,
            subject = subject,
            teacherId = teacherId,
            teacherName = teacherName,
            dueDate = dueDate,
            targetType = targetType,
            grade = grade,
            section = section,
            targetClassrooms = targetClassrooms,
            completedBy = completedBy,
            createdAt = createdAt
        )
    }
}
