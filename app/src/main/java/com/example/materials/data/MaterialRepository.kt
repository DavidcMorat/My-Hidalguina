package com.example.materials.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MaterialRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun getMaterialsFlow(): Flow<List<MaterialModel>> = callbackFlow {
        val listener = db.collection("materials")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    docToMaterial(doc)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createMaterial(
        title: String,
        description: String,
        subject: String,
        teacherId: String,
        teacherName: String,
        fileType: String,
        fileUrl: String,
        fileName: String,
        fileSize: String,
        targetType: String = "GLOBAL",
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
                "fileType" to fileType,
                "fileUrl" to fileUrl,
                "fileName" to fileName,
                "fileSize" to fileSize,
                "targetType" to targetType,
                "grade" to grade,
                "section" to section,
                "targetClassrooms" to targetClassrooms,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("materials").document(id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMaterial(materialId: String): Result<Unit> {
        return try {
            db.collection("materials").document(materialId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun docToMaterial(doc: DocumentSnapshot): MaterialModel? {
        val id = doc.getString("id") ?: doc.id
        val title = doc.getString("title") ?: return null
        val description = doc.getString("description") ?: ""
        val subject = doc.getString("subject") ?: "General"
        val teacherId = doc.getString("teacherId") ?: ""
        val teacherName = doc.getString("teacherName") ?: "Docente"
        val fileType = doc.getString("fileType") ?: "PDF"
        val fileUrl = doc.getString("fileUrl") ?: ""
        val fileName = doc.getString("fileName") ?: ""
        val fileSize = doc.getString("fileSize") ?: ""
        val targetType = doc.getString("targetType") ?: "GLOBAL"
        val grade = doc.getString("grade") ?: ""
        val section = doc.getString("section") ?: ""
        val targetClassrooms = (doc.get("targetClassrooms") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

        return MaterialModel(
            id = id,
            title = title,
            description = description,
            subject = subject,
            teacherId = teacherId,
            teacherName = teacherName,
            fileType = fileType,
            fileUrl = fileUrl,
            fileName = fileName,
            fileSize = fileSize,
            targetType = targetType,
            grade = grade,
            section = section,
            targetClassrooms = targetClassrooms,
            createdAt = createdAt
        )
    }
}
