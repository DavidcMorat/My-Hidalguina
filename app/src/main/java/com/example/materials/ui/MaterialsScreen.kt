package com.example.materials.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.materials.data.MaterialModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(
    modifier: Modifier = Modifier,
    isTeacher: Boolean = false,
    studentGrade: String = "",
    studentSection: String = "",
    teachingClassrooms: List<String> = emptyList(),
    onBack: () -> Unit,
    materialsViewModel: MaterialsViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val materials by materialsViewModel.materials.collectAsState()
    val selectedSubject by materialsViewModel.selectedSubject.collectAsState()
    val searchQuery by materialsViewModel.searchQuery.collectAsState()
    val showCreateDialog by materialsViewModel.showCreateDialog.collectAsState()
    val isLoading by materialsViewModel.isLoading.collectAsState()
    val currentUserId = materialsViewModel.currentUserId

    var selectedMaterialForDetail by remember { mutableStateOf<MaterialModel?>(null) }
    var materialToDelete by remember { mutableStateOf<MaterialModel?>(null) }

    LaunchedEffect(Unit) {
        materialsViewModel.snackBarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Filter materials
    val studentClassroomCode = "${studentGrade}${studentSection.uppercase()}".trim()
    val filteredMaterials = remember(materials, selectedSubject, searchQuery, isTeacher, studentClassroomCode) {
        materials.filter { mat ->
            // Audience filter
            val isVisibleAudience = if (isTeacher) {
                true
            } else {
                mat.targetType == "GLOBAL" ||
                (mat.grade.isNotBlank() && mat.grade == studentGrade && (mat.section.isBlank() || mat.section.equals(studentSection, ignoreCase = true))) ||
                (studentClassroomCode.isNotBlank() && mat.targetClassrooms.contains(studentClassroomCode))
            }

            // Subject filter
            val matchesSubject = selectedSubject == "Todas" || mat.subject.equals(selectedSubject, ignoreCase = true)

            // Search query
            val matchesSearch = searchQuery.isBlank() ||
                    mat.title.contains(searchQuery, ignoreCase = true) ||
                    mat.description.contains(searchQuery, ignoreCase = true) ||
                    mat.subject.contains(searchQuery, ignoreCase = true) ||
                    mat.teacherName.contains(searchQuery, ignoreCase = true) ||
                    mat.fileName.contains(searchQuery, ignoreCase = true)

            isVisibleAudience && matchesSubject && matchesSearch
        }
    }

    val subjectList = listOf(
        "Todas",
        "Matemática",
        "Comunicación",
        "Ciencias",
        "Ciencias Sociales",
        "Inglés",
        "Desarrollo Personal",
        "Educación para el Trabajo",
        "Tutoría",
        "General"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isTeacher) "Materiales y Recursos" else "Material Educativo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColors.textPrimary
                        )
                        Text(
                            text = if (isTeacher) "Sube y comparte guías, PDFs y recursos" else "Guías, libros y recursos de tus profesores",
                            fontSize = 12.sp,
                            color = ThemeColors.textSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = ThemeColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeColors.surface)
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                ExtendedFloatingActionButton(
                    onClick = { materialsViewModel.openCreateDialog() },
                    containerColor = ThemeColors.primary,
                    contentColor = ThemeColors.onPrimary,
                    icon = { Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = ThemeColors.onPrimary) },
                    text = { Text("Subir Material", fontWeight = FontWeight.Bold, color = ThemeColors.onPrimary) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColors.background)
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { materialsViewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por título, materia o profesor...", fontSize = 13.sp, color = ThemeColors.textSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ThemeColors.textSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { materialsViewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpiar", tint = ThemeColors.textSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ThemeColors.surface,
                    unfocusedContainerColor = ThemeColors.surface,
                    focusedBorderColor = ThemeColors.primary,
                    unfocusedBorderColor = ThemeColors.divider,
                    focusedTextColor = ThemeColors.textPrimary,
                    unfocusedTextColor = ThemeColors.textPrimary
                )
            )

            // Subject Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subjectList.forEach { subj ->
                    val isSelected = selectedSubject == subj
                    FilterChip(
                        selected = isSelected,
                        onClick = { materialsViewModel.setSelectedSubject(subj) },
                        label = { Text(subj, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ThemeColors.primary,
                            selectedLabelColor = ThemeColors.onPrimary,
                            containerColor = ThemeColors.surface,
                            labelColor = ThemeColors.textPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) ThemeColors.primary else ThemeColors.divider
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Materials List
            if (filteredMaterials.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = ThemeColors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedSubject != "Todas")
                                "No se encontraron materiales con esos filtros."
                            else if (isTeacher)
                                "Aún no has compartido materiales. ¡Sube tu primer recurso con el botón '+'!"
                            else
                                "No hay materiales publicados para tu grado en este momento.",
                            fontSize = 14.sp,
                            color = ThemeColors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMaterials, key = { it.id }) { material ->
                        MaterialItemCard(
                            material = material,
                            isOwner = material.teacherId == currentUserId,
                            onClick = { selectedMaterialForDetail = material },
                            onDelete = { materialToDelete = material }
                        )
                    }
                }
            }
        }
    }

    // Create Material Dialog
    if (showCreateDialog) {
        CreateMaterialDialog(
            teachingClassrooms = teachingClassrooms,
            isLoading = isLoading,
            onDismiss = { materialsViewModel.closeCreateDialog() },
            onConfirm = { title, desc, subj, fType, fUrl, fName, fSize, tType, g, s, targetClasses ->
                materialsViewModel.createMaterial(
                    title = title,
                    description = desc,
                    subject = subj,
                    fileType = fType,
                    fileUrl = fUrl,
                    fileName = fName,
                    fileSize = fSize,
                    targetType = tType,
                    grade = g,
                    section = s,
                    targetClassrooms = targetClasses
                )
            }
        )
    }

    // Material Detail Dialog
    selectedMaterialForDetail?.let { mat ->
        MaterialDetailDialog(
            material = mat,
            isOwner = mat.teacherId == currentUserId,
            onDismiss = { selectedMaterialForDetail = null },
            onDelete = {
                materialToDelete = mat
                selectedMaterialForDetail = null
            }
        )
    }

    // Confirm Delete Dialog
    materialToDelete?.let { mat ->
        AlertDialog(
            onDismissRequest = { materialToDelete = null },
            containerColor = ThemeColors.surface,
            icon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = RedPrimary) },
            title = { Text("Eliminar Material", fontWeight = FontWeight.Bold, color = ThemeColors.textPrimary) },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar \"${mat.title}\"? Los estudiantes ya no podrán acceder a este recurso.",
                    fontSize = 13.sp,
                    color = ThemeColors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        materialsViewModel.deleteMaterial(mat.id)
                        materialToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { materialToDelete = null }) {
                    Text("Cancelar", color = ThemeColors.textSecondary)
                }
            }
        )
    }
}

@Composable
fun MaterialItemCard(
    material: MaterialModel,
    isOwner: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val (icon, iconColor, bgBadge) = getFileTypeIconAndColor(material.fileType)
    val formattedDate = remember(material.createdAt) {
        val sdf = SimpleDateFormat("d 'de' MMMM", Locale("es", "PE"))
        sdf.format(Date(material.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgBadge),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = material.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ThemeColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = material.subject,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemeColors.primary
                            )
                            Text(
                                text = " • $formattedDate",
                                fontSize = 11.sp,
                                color = ThemeColors.textSecondary
                            )
                        }
                    }
                }

                if (isOwner) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = ThemeColors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (material.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = material.description,
                    fontSize = 12.sp,
                    color = ThemeColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attachment pill & Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ThemeColors.inputBackground
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Attachment, contentDescription = null, tint = ThemeColors.textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (material.fileName.isNotBlank()) material.fileName else "${material.fileType} • ${material.fileSize}",
                            fontSize = 11.sp,
                            color = ThemeColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 180.dp)
                        )
                    }
                }

                // Open Button
                Button(
                    onClick = {
                        openResource(context, material.fileUrl)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.primary.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = ThemeColors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (material.fileType == "ENLACE" || material.fileType == "VIDEO") "Ver Enlace" else "Abrir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColors.primary
                    )
                }
            }
        }
    }
}

@Composable
fun MaterialDetailDialog(
    material: MaterialModel,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val (icon, iconColor, bgBadge) = getFileTypeIconAndColor(material.fileType)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThemeColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgBadge),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = material.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ThemeColors.textPrimary
                    )
                    Text(
                        text = "${material.subject} • Publicado por ${material.teacherName}",
                        fontSize = 12.sp,
                        color = ThemeColors.textSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (material.description.isNotBlank()) {
                    Text(
                        text = "Descripción e instrucciones:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ThemeColors.textPrimary
                    )
                    Text(
                        text = material.description,
                        fontSize = 13.sp,
                        color = ThemeColors.textSecondary,
                        lineHeight = 18.sp
                    )
                }

                HorizontalDivider(color = ThemeColors.divider)

                // File info box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = ThemeColors.inputBackground
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = material.fileName.ifBlank { "Recurso: ${material.fileType}" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = ThemeColors.textPrimary
                            )
                        }
                        if (material.fileSize.isNotBlank()) {
                            Text(
                                text = "Tamaño: ${material.fileSize}",
                                fontSize = 11.sp,
                                color = ThemeColors.textSecondary,
                                modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                            )
                        }
                    }
                }

                // Audience info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = ThemeColors.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    val audience = if (material.targetType == "GLOBAL") {
                        "Disponible para todo el colegio"
                    } else if (material.targetClassrooms.isNotEmpty()) {
                        "Dirigido a: ${material.targetClassrooms.joinToString(", ")}"
                    } else {
                        "Dirigido a: ${material.grade}° ${material.section}"
                    }
                    Text(text = audience, fontSize = 12.sp, color = ThemeColors.textSecondary)
                }

                if (material.fileUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(material.fileUrl))
                            Toast.makeText(context, "Enlace copiado al portapapeles 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThemeColors.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copiar Enlace", color = ThemeColors.primary, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openResource(context, material.fileUrl)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = ThemeColors.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir Recurso", color = ThemeColors.onPrimary)
            }
        },
        dismissButton = {
            Row {
                if (isOwner) {
                    TextButton(onClick = onDelete) {
                        Text("Eliminar", color = RedPrimary)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = ThemeColors.textSecondary)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMaterialDialog(
    teachingClassrooms: List<String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        subject: String,
        fileType: String,
        fileUrl: String,
        fileName: String,
        fileSize: String,
        targetType: String,
        grade: String,
        section: String,
        targetClassrooms: List<String>
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Matemática") }
    var fileType by remember { mutableStateOf("PDF") }
    var fileUrl by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf("") }

    var targetType by remember { mutableStateOf("GLOBAL") } // "GLOBAL" or "SPECIFIC"
    val selectedClassrooms = remember { mutableStateListOf<String>() }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fileUrl = uri.toString()
            // Extract display name and size from Uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex) ?: "archivo_adjunto"
                    }
                    if (sizeIndex != -1) {
                        val bytes = it.getLong(sizeIndex)
                        fileSize = formatFileSize(bytes)
                    }
                }
            }
            if (fileName.endsWith(".pdf", ignoreCase = true)) fileType = "PDF"
            else if (fileName.endsWith(".docx", ignoreCase = true) || fileName.endsWith(".doc", ignoreCase = true) || fileName.endsWith(".xlsx", ignoreCase = true) || fileName.endsWith(".pptx", ignoreCase = true)) fileType = "DOCUMENTO"
            else if (fileName.endsWith(".png", ignoreCase = true) || fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true)) fileType = "IMAGEN"
        }
    }

    val subjectOptions = listOf("Matemática", "Comunicación", "Ciencias", "Ciencias Sociales", "Inglés", "Desarrollo Personal", "EPT", "Tutoría", "General")
    val fileTypeOptions = listOf("PDF", "DOCUMENTO", "IMAGEN", "ENLACE", "VIDEO")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThemeColors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ThemeColors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = ThemeColors.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Compartir Material", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ThemeColors.textPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título del material *") },
                    placeholder = { Text("Ej. Guía de Vectores y Matrices") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ThemeColors.textPrimary,
                        unfocusedTextColor = ThemeColors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o instrucciones") },
                    placeholder = { Text("Ej. Leer las páginas 10-15 y resolver...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ThemeColors.textPrimary,
                        unfocusedTextColor = ThemeColors.textPrimary
                    )
                )

                Text("Materia:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ThemeColors.textPrimary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subjectOptions.forEach { s ->
                        FilterChip(
                            selected = subject == s,
                            onClick = { subject = s },
                            label = { Text(s, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThemeColors.primary,
                                selectedLabelColor = ThemeColors.onPrimary
                            )
                        )
                    }
                }

                Text("Tipo de Recurso:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ThemeColors.textPrimary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    fileTypeOptions.forEach { t ->
                        FilterChip(
                            selected = fileType == t,
                            onClick = { fileType = t },
                            label = { Text(t, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThemeColors.primary,
                                selectedLabelColor = ThemeColors.onPrimary
                            )
                        )
                    }
                }

                // Attachment / Link Picker Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = ThemeColors.inputBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColors.divider)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Adjuntar Archivo o Enlace:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ThemeColors.textPrimary)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val mime = when (fileType) {
                                        "PDF" -> "application/pdf"
                                        "IMAGEN" -> "image/*"
                                        "DOCUMENTO" -> "*/*"
                                        else -> "*/*"
                                    }
                                    filePickerLauncher.launch(mime)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.primary.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = ThemeColors.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Elegir de mi teléfono", fontSize = 11.sp, color = ThemeColors.primary)
                            }
                        }

                        if (fileName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$fileName ($fileSize)", fontSize = 11.sp, color = ThemeColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        OutlinedTextField(
                            value = fileUrl,
                            onValueChange = { fileUrl = it },
                            label = { Text("O ingresa URL / Enlace web (Drive, Dropbox, Web)") },
                            placeholder = { Text("https://drive.google.com/...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeColors.textPrimary,
                                unfocusedTextColor = ThemeColors.textPrimary
                            )
                        )
                    }
                }

                // Target Audiences
                Text("Audiencia Destino:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ThemeColors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetType == "GLOBAL",
                        onClick = { targetType = "GLOBAL" },
                        label = { Text("Todo el Colegio", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ThemeColors.primary,
                            selectedLabelColor = ThemeColors.onPrimary
                        )
                    )
                    FilterChip(
                        selected = targetType == "SPECIFIC",
                        onClick = { targetType = "SPECIFIC" },
                        label = { Text("Salones Específicos", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ThemeColors.primary,
                            selectedLabelColor = ThemeColors.onPrimary
                        )
                    )
                }

                if (targetType == "SPECIFIC") {
                    val availableClassrooms = remember(teachingClassrooms) {
                        if (teachingClassrooms.isNotEmpty()) teachingClassrooms
                        else listOf("1A", "1B", "2A", "2B", "3A", "3B", "4A", "4B", "5A", "5B")
                    }

                    Text("Selecciona los salones:", fontSize = 12.sp, color = ThemeColors.textSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableClassrooms.forEach { c ->
                            val isSel = selectedClassrooms.contains(c)
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    if (isSel) selectedClassrooms.remove(c) else selectedClassrooms.add(c)
                                },
                                label = { Text(c, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeColors.primary,
                                    selectedLabelColor = ThemeColors.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        description,
                        subject,
                        fileType,
                        fileUrl,
                        fileName,
                        fileSize,
                        targetType,
                        "",
                        "",
                        selectedClassrooms.toList()
                    )
                },
                enabled = !isLoading && title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ThemeColors.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Publicar Material", color = ThemeColors.onPrimary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = ThemeColors.textSecondary)
            }
        }
    )
}

private fun getFileTypeIconAndColor(fileType: String): Triple<ImageVector, Color, Color> {
    return when (fileType.uppercase()) {
        "PDF" -> Triple(Icons.Filled.PictureAsPdf, Color(0xFFD32F2F), Color(0xFFFFEBEE))
        "DOCUMENTO", "DOCUMENT" -> Triple(Icons.Filled.Description, Color(0xFF1976D2), Color(0xFFE3F2FD))
        "IMAGEN", "IMAGE" -> Triple(Icons.Filled.Image, Color(0xFF388E3C), Color(0xFFE8F5E9))
        "VIDEO" -> Triple(Icons.Filled.SmartDisplay, Color(0xFF7B1FA2), Color(0xFFF3E5F5))
        "ENLACE", "LINK" -> Triple(Icons.Filled.Link, Color(0xFFF57C00), Color(0xFFFFF3E0))
        else -> Triple(Icons.Filled.Folder, Color(0xFF616161), Color(0xFFEEEEEE))
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}

private fun openResource(context: Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(context, "Este recurso no contiene un enlace o archivo accesible", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir el enlace: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
