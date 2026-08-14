package com.example.tutor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tutor.data.StudyPlanWithTopics
import com.example.tutor.data.StudyTopicEntity
import com.example.tutor.data.TutorChatMessageEntity
import com.example.tutor.viewmodel.AITutorViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITutorScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null,
    viewModel: AITutorViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val studyPlans by viewModel.studyPlans.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAskingTutor by viewModel.isAskingTutor.collectAsState()
    val isGeneratingPlan by viewModel.isGeneratingPlan.collectAsState()
    val isLoadingLesson by viewModel.isLoadingLesson.collectAsState()
    val activeLesson by viewModel.activeLesson.collectAsState()
    val activeTopic by viewModel.activeTopic.collectAsState()
    val showCreateDialog by viewModel.showCreateDialog.collectAsState()
    val showPracticeDialog by viewModel.showPracticeDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialTab) {
        viewModel.setTab(initialTab)
    }

    LaunchedEffect(Unit) {
        viewModel.snackBarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tutor IA & Aprendizaje",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackTertiary
                        )
                        Text(
                            text = "Planes interactivos, práctica real y tutoría",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = BlackTertiary)
                        }
                    }
                },
                actions = {
                    if (currentTab == 1 && chatMessages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Limpiar chat", tint = TextGray)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(innerPadding)
        ) {
            // Segmented Header Switch for the unified section
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentTab == 0,
                        onClick = { viewModel.setTab(0) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AutoStories,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Planes de Estudio", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RedPrimary.copy(alpha = 0.12f),
                            selectedLabelColor = RedPrimary,
                            selectedLeadingIconColor = RedPrimary
                        )
                    )

                    FilterChip(
                        selected = currentTab == 1,
                        onClick = { viewModel.setTab(1) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Consultar Tutor IA", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RedPrimary.copy(alpha = 0.12f),
                            selectedLabelColor = RedPrimary,
                            selectedLeadingIconColor = RedPrimary
                        )
                    )
                }
            }

            // Unified Content Area
            if (currentTab == 0) {
                UnifiedStudyPlansContent(
                    studyPlans = studyPlans,
                    onOpenCreateDialog = { viewModel.openCreatePlanDialog() },
                    onMarkAchieved = { topicId -> viewModel.markTopicAchieved(topicId) },
                    onStartPractice = { topic, subject -> viewModel.openPractice(topic, subject) },
                    onDeletePlan = { planId -> viewModel.deleteStudyPlan(planId) },
                    onGoToChat = { viewModel.setTab(1) }
                )
            } else {
                TutorConsultUnifiedContent(
                    chatMessages = chatMessages,
                    isAsking = isAskingTutor,
                    onSendMessage = { text -> viewModel.sendMessageToTutor(text) },
                    onGeneratePlanFromPrompt = { prompt -> viewModel.generateStudyPlan(prompt) }
                )
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateStudyPlanDialog(
            isGenerating = isGeneratingPlan,
            onDismiss = { viewModel.closeCreatePlanDialog() },
            onGenerate = { topic, notes ->
                viewModel.generateStudyPlan(topic, notes)
            }
        )
    }

    if (showPracticeDialog && activeTopic != null) {
        MiniLessonEvaluationDialog(
            topic = activeTopic!!,
            lessonData = activeLesson,
            isLoading = isLoadingLesson,
            onDismiss = { viewModel.closePracticeDialog() },
            onCompleteSuccess = { topicId ->
                viewModel.completeEvaluationSuccess(topicId)
            },
            onRecordOutcome = { topicId, isPassed, scoreText ->
                viewModel.recordPracticeOutcome(topicId, isPassed, scoreText)
            },
            onAskTutorAboutProblem = { topicTitle, problemQuestion, solution ->
                viewModel.askTutorAboutError(topicTitle, problemQuestion, solution)
            }
        )
    }
}

// ----------------------------------------------------
// SECTION: Planes de Estudio Interactivos y Limpios
// ----------------------------------------------------
@Composable
fun UnifiedStudyPlansContent(
    studyPlans: List<StudyPlanWithTopics>,
    onOpenCreateDialog: () -> Unit,
    onMarkAchieved: (String) -> Unit,
    onStartPractice: (StudyTopicEntity, String) -> Unit,
    onDeletePlan: (String) -> Unit,
    onGoToChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Action Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rutas de Aprendizaje",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackTertiary
                    )
                    Text(
                        text = "Infórmate, domina la teoría y califica tu práctica real.",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenCreateDialog,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crear Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (studyPlans.isEmpty()) {
            // Clean slate empty state with NO dummy data
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(RedPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay planes de estudio creados",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackTertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Crea tu primer plan personalizado con Gemini o haz consultas directas a tu Tutor IA.",
                        fontSize = 13.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onOpenCreateDialog,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear Plan de Estudio con IA", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onGoToChat,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Consultar Tutor IA", color = BlackTertiary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(studyPlans, key = { it.plan.id }) { item ->
                    StudyPlanCard(
                        planWithTopics = item,
                        onMarkAchieved = onMarkAchieved,
                        onStartPractice = { topic -> onStartPractice(topic, item.plan.subject) },
                        onDelete = { onDeletePlan(item.plan.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StudyPlanCard(
    planWithTopics: StudyPlanWithTopics,
    onMarkAchieved: (String) -> Unit,
    onStartPractice: (StudyTopicEntity) -> Unit,
    onDelete: () -> Unit
) {
    val plan = planWithTopics.plan
    val topics = planWithTopics.topics.sortedBy { it.orderIndex }

    val achievedCount = topics.count { it.status == "LOGRADO" }
    val totalCount = topics.size
    val progress = if (totalCount > 0) achievedCount.toFloat() / totalCount.toFloat() else 0f

    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(RedPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoStories, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = plan.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackTertiary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plan.subject,
                                fontSize = 11.sp,
                                color = RedPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (plan.estimatedDuration.isNotBlank()) {
                                Text(
                                    text = " • ${plan.estimatedDuration}",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "Expandir",
                            tint = BlackTertiary
                        )
                    }
                }
            }

            if (plan.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = plan.description,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }

            // Progress Bar
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progreso: $achievedCount de $totalCount temas logrados",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BlackTertiary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (progress == 1f) Color(0xFF2E7D32) else RedPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (progress == 1f) Color(0xFF2E7D32) else YellowSecondary,
                trackColor = Color(0xFFEEEEEE)
            )

            // Topics List with the 3 clear pedagogical steps
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    topics.forEachIndexed { index, topic ->
                        StructuredTopicCard(
                            index = index + 1,
                            topic = topic,
                            onMarkAchieved = { onMarkAchieved(topic.id) },
                            onStartPractice = { onStartPractice(topic) }
                        )
                        if (index < topics.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CARD: Structured Pedagogical Steps for Each Topic
// ----------------------------------------------------
@Composable
fun StructuredTopicCard(
    index: Int,
    topic: StudyTopicEntity,
    onMarkAchieved: () -> Unit,
    onStartPractice: () -> Unit
) {
    val isAchieved = topic.status == "LOGRADO"
    val hasDifficulty = topic.status == "DIFICULTAD"

    val cardBorderColor = when {
        isAchieved -> Color(0xFF81C784)
        hasDifficulty -> Color(0xFFFFB74D)
        else -> Color(0xFFE0E0E0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAchieved -> Color(0xFFF9FCF9)
                hasDifficulty -> Color(0xFFFFFBF5)
                else -> Color(0xFFFAFAFA)
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(cardBorderColor))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Topic Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isAchieved) Color(0xFF2E7D32) else RedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAchieved) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Text(text = "$index", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = topic.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlackTertiary
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isAchieved -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                        hasDifficulty -> Color(0xFFE65100).copy(alpha = 0.15f)
                        else -> Color.Gray.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = when {
                            isAchieved -> "⭐ Logrado"
                            hasDifficulty -> "💡 Necesita Ayuda"
                            else -> "Pendiente"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isAchieved -> Color(0xFF2E7D32)
                            hasDifficulty -> Color(0xFFE65100)
                            else -> Color.Gray
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🌐 PASO 1: "Infórmate: Busca en la web (tema primordial...)"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF5FB)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "1. Infórmate: Busca en la web",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = topic.description.ifBlank { "Investiga las bases y conceptos fundamentales de ${topic.title}." },
                            fontSize = 12.sp,
                            color = BlackTertiary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 📖 PASO 2: "Domina la teoría"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "2. Domina la teoría",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFF57F17)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = topic.keyConcept.ifBlank { "Revisa fórmulas, reglas clave y axiomas esenciales." },
                            fontSize = 12.sp,
                            color = BlackTertiary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🎯 PASO 3: Botón de Práctica con Problemas Reales y Evaluación Automática
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartPractice,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAchieved) Color(0xFF388E3C) else RedPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.AssignmentTurnedIn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAchieved) "Repetir Práctica Real" else "Practica la Teoría (Problemas Reales)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Checkbox manual "Logrado" option
                OutlinedIconToggleButton(
                    checked = isAchieved,
                    onCheckedChange = { onMarkAchieved() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isAchieved) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Marcar Logrado",
                        tint = if (isAchieved) Color(0xFF2E7D32) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// SECTION: Consultar Tutor IA (Socrático - Groq & Gemini)
// ----------------------------------------------------
@Composable
fun TutorConsultUnifiedContent(
    chatMessages: List<TutorChatMessageEntity>,
    isAsking: Boolean,
    onSendMessage: (String) -> Unit,
    onGeneratePlanFromPrompt: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size, isAsking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    val quickQuestions = listOf(
        "Tengo dudas con factorización de trinomios",
        "¿Cómo se aplican las Leyes de Newton?",
        "No entiendo el balanceo de ecuaciones en Química",
        "Ayúdame a comprender la Fotosíntesis",
        "¿Cómo despejar una variable paso a paso?"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Socratic Badge Info Banner
        Surface(
            color = YellowSecondary.copy(alpha = 0.18f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tutor Socrático: Te orienta con preguntas y pistas para que resuelvas por ti mismo.",
                    fontSize = 11.sp,
                    color = BlackTertiary,
                    lineHeight = 14.sp
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(RedPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Psychology, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "¿Qué duda escolar tienes hoy?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlackTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Escribe tu pregunta o selecciona una sugerencia para comenzar:",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        quickQuestions.forEach { q ->
                            SuggestionChip(
                                onClick = { onSendMessage(q) },
                                label = { Text(q, fontSize = 11.sp, color = BlackTertiary) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(vertical = 3.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            items(chatMessages, key = { it.id }) { msg ->
                TutorChatBubble(
                    message = msg,
                    onGeneratePlanFromPrompt = onGeneratePlanFromPrompt
                )
            }

            if (isAsking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(RedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = RedPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("El Tutor IA está pensando...", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Quick Suggestions
        if (chatMessages.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickQuestions) { q ->
                    SuggestionChip(
                        onClick = { onSendMessage(q) },
                        label = { Text(q, fontSize = 10.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Chat Input Bar
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Escribe tu pregunta o duda...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = DividerGray,
                        focusedBorderColor = RedPrimary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isAsking) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAsking,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isAsking) RedPrimary else Color.LightGray)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TutorChatBubble(
    message: TutorChatMessageEntity,
    onGeneratePlanFromPrompt: (String) -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(RedPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Psychology, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) RedPrimary else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        fontSize = 13.sp,
                        color = if (isUser) Color.White else BlackTertiary,
                        lineHeight = 18.sp
                    )

                    // Suggest study plan button if prompt is detected
                    if (!isUser && !message.suggestedTopicPrompt.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onGeneratePlanFromPrompt(message.suggestedTopicPrompt) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = YellowSecondary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BlackTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Crear Plan: ${message.suggestedTopicPrompt}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlackTertiary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
