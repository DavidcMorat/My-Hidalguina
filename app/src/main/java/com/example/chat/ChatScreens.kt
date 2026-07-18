package com.example.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
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
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTab(
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = viewModel(),
    onNavigateToChat: (ChatUser) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDirectory by remember { mutableStateOf(false) }
    val searchResults by chatViewModel.searchResults.collectAsState()
    val localUsersWithStatus by chatViewModel.localUsersWithStatus.collectAsState(initial = emptyList())
    val classroomUsers by chatViewModel.classroomUsers.collectAsState()
    val isSearching by chatViewModel.isSearching.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(BackgroundGray).padding(16.dp)) {
        Text("Mensajes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlackTertiary)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    chatViewModel.searchUser(it)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar estudiante o usuario") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DividerGray,
                    focusedBorderColor = RedPrimary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedTextColor = BlackTertiary,
                    unfocusedTextColor = BlackTertiary
                ),
                singleLine = true
            )
            Button(
                onClick = { 
                    chatViewModel.searchUser(searchQuery)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("Buscar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (searchQuery.isNotEmpty()) {
            Text("Resultados de búsqueda:", fontWeight = FontWeight.SemiBold, color = TextGray)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(searchResults) { user ->
                    UserListItem(user = user, onClick = { 
                        chatViewModel.startChatWithUser(user)
                        onNavigateToChat(user)
                    })
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { showDirectory = false }) {
                    Text("Tus chats", color = if (!showDirectory) RedPrimary else TextGray, fontWeight = if (!showDirectory) FontWeight.Bold else FontWeight.Normal)
                }
                TextButton(onClick = { showDirectory = true }) {
                    Text("Salón de clases", color = if (showDirectory) RedPrimary else TextGray, fontWeight = if (showDirectory) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                if (showDirectory) {
                    items(classroomUsers) { user ->
                        UserListItem(user = user, onClick = { 
                            chatViewModel.startChatWithUser(user)
                            onNavigateToChat(user)
                        })
                    }
                    if (classroomUsers.isEmpty()) {
                        item {
                            Text(
                                "No hay compañeros en tu salón.",
                                color = TextGray,
                                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(localUsersWithStatus) { status ->
                        UserListItem(
                            user = status.user,
                            lastMessage = status.lastMessageText,
                            hasUnread = status.hasUnread,
                            onClick = { onNavigateToChat(status.user) }
                        )
                    }
                    if (localUsersWithStatus.isEmpty()) {
                        item {
                            Text(
                                "Aún no tienes chats.",
                                color = TextGray,
                                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(
    user: ChatUser, 
    lastMessage: String = "",
    hasUnread: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (hasUnread) RedPrimary else YellowSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName, 
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold, 
                color = BlackTertiary,
                fontSize = 16.sp
            )
            if (lastMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lastMessage,
                    color = if (hasUnread) BlackTertiary else TextGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        if (hasUnread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RedPrimary)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "NUEVO", 
                        color = Color.White, 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    user: ChatUser,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val messages by chatViewModel.getMessages(user.uid).collectAsState(initial = emptyList())
    var text by remember { mutableStateOf("") }

    LaunchedEffect(user.uid) {
        chatViewModel.markChatAsRead(user.uid)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = BlackTertiary,
                    navigationIconContentColor = BlackTertiary
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = BackgroundGray,
                        focusedContainerColor = BackgroundGray,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        focusedTextColor = BlackTertiary,
                        unfocusedTextColor = BlackTertiary
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            chatViewModel.sendMessage(user.uid, text)
                            text = ""
                        }
                    },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(RedPrimary)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(padding)
                .padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            // To display correctly with reverseLayout = true, we reverse the list
            val displayMessages = messages.reversed()
            items(displayMessages) { msg ->
                MessageBubble(msg)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val time = formatter.format(Date(msg.timestamp))
    
    val now = System.currentTimeMillis()
    var isRead by remember { mutableStateOf(now - msg.timestamp > 3000) }
    
    LaunchedEffect(msg.timestamp) {
        if (now - msg.timestamp <= 3000) {
            kotlinx.coroutines.delay(3000 - (now - msg.timestamp))
            isRead = true
        }
    }
    
    val borderColor = if (msg.isSentByMe) {
        if (isRead) RedPrimary else YellowSecondary
    } else {
        BlackTertiary
    }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (msg.isSentByMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (msg.isSentByMe) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isSentByMe) 16.dp else 4.dp,
                            bottomEnd = if (msg.isSentByMe) 4.dp else 16.dp
                        )
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (msg.isSentByMe) 16.dp else 4.dp,
                            bottomEnd = if (msg.isSentByMe) 4.dp else 16.dp
                        )
                    )
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Text(
                    text = msg.text,
                    color = BlackTertiary,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(time, fontSize = 10.sp, color = TextGray)
        }
    }
}
