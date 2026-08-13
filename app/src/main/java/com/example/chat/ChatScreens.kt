package com.example.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.RedPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel = viewModel(),
    onNavigateToChat: (ChatUser) -> Unit
) {
    DisposableEffect(Unit) {
        chatViewModel.activateChatSession()
        onDispose {
            chatViewModel.deactivateChatSession()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showDirectory by remember { mutableStateOf(false) }
    val searchResults by chatViewModel.searchResults.collectAsState()
    val classroomUsers by chatViewModel.classroomUsers.collectAsState()
    val usersWithStatus by chatViewModel.localUsersWithStatus.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Mensajes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                chatViewModel.searchUser(it)
            },
            placeholder = { Text("Buscar compañeros de clase...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RedPrimary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = RedPrimary,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    showDirectory = !showDirectory
                    if (showDirectory) chatViewModel.loadClassroomUsers()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showDirectory) GoldSecondary else DarkSurfaceVariant,
                    contentColor = if (showDirectory) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (showDirectory) "Ocultar Directorio" else "Ver Directorio de Salón")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchQuery.isNotBlank()) {
            Text("Resultados de búsqueda", color = Color.Gray, fontSize = 14.sp)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { user ->
                    UserChatItem(user = user, lastMessage = "", hasUnread = false, lastTimestamp = 0L) {
                        chatViewModel.startChatWithUser(user)
                        onNavigateToChat(user)
                    }
                }
            }
        } else if (showDirectory) {
            Text("Compañeros de tu Salón", color = GoldSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(classroomUsers) { user ->
                    UserChatItem(user = user, lastMessage = "Haz clic para iniciar chat", hasUnread = false, lastTimestamp = 0L) {
                        chatViewModel.startChatWithUser(user)
                        onNavigateToChat(user)
                    }
                }
            }
        } else {
            if (usersWithStatus.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes conversaciones activas.\nBusca a un compañero arriba o abre el Directorio.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(usersWithStatus) { statusItem ->
                        UserChatItem(
                            user = statusItem.user,
                            lastMessage = statusItem.lastMessageText,
                            hasUnread = statusItem.hasUnread,
                            lastTimestamp = statusItem.lastTimestamp
                        ) {
                            chatViewModel.markChatAsRead(statusItem.user.uid)
                            onNavigateToChat(statusItem.user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserChatItem(
    user: ChatUser,
    lastMessage: String,
    hasUnread: Boolean,
    lastTimestamp: Long,
    onClick: () -> Unit
) {
    val timeFormatted = if (lastTimestamp > 0) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(lastTimestamp))
    } else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RedPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                    color = Color.White,
                    fontSize = 16.sp
                )
                if (lastMessage.isNotEmpty()) {
                    Text(
                        text = lastMessage,
                        color = if (hasUnread) GoldSecondary else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
            if (timeFormatted.isNotEmpty() || hasUnread) {
                Column(horizontalAlignment = Alignment.End) {
                    if (timeFormatted.isNotEmpty()) {
                        Text(text = timeFormatted, color = Color.Gray, fontSize = 11.sp)
                    }
                    if (hasUnread) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GoldSecondary)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    user: ChatUser,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    DisposableEffect(Unit) {
        chatViewModel.activateChatSession()
        onDispose {
            chatViewModel.deactivateChatSession()
        }
    }

    val messages by chatViewModel.getMessages(user.uid).collectAsState(initial = emptyList())
    var text by remember { mutableStateOf("") }

    LaunchedEffect(user.uid) {
        chatViewModel.markChatAsRead(user.uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(user.displayName, color = Color.White, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    ChatMessageItem(msg)
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Escribe un mensaje...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            chatViewModel.sendMessage(user.uid, text)
                            text = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RedPrimary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isMe = message.isSentByMe
    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isMe) RedPrimary else DarkSurfaceVariant
    val textColor = Color.White

    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = sdf.format(Date(message.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 0.dp,
                        bottomEnd = if (isMe) 0.dp else 16.dp
                    )
                )
                .background(bgColor)
                .padding(10.dp)
        ) {
            Text(text = message.text, color = textColor, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeStr,
                color = Color.LightGray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
