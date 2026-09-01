package com.example.materials.data

data class MaterialModel(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val subject: String = "General",
    val teacherId: String = "",
    val teacherName: String = "Docente",
    val fileType: String = "PDF", // "PDF", "DOCUMENTO", "IMAGEN", "ENLACE", "VIDEO", "OTRO"
    val fileUrl: String = "",
    val fileName: String = "",
    val fileSize: String = "",
    val targetType: String = "GLOBAL", // "GLOBAL" or "SPECIFIC"
    val grade: String = "",
    val section: String = "",
    val targetClassrooms: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
