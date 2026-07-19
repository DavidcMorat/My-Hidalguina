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
import androidx.compose.material.icons.filled.Close
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
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.StickyNote2

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
    var showStickers by remember { mutableStateOf(false) }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showStickers = !showStickers }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.StickyNote2,
                            contentDescription = "Stickers",
                            tint = if (showStickers) RedPrimary else TextGray
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { 
                            text = it
                            if (it.isNotEmpty()) {
                                showStickers = false
                            }
                        },
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
                
                if (showStickers) {
                    StickersPanel(
                        chatViewModel = chatViewModel,
                        onStickerSelected = { sticker ->
                            chatViewModel.sendSticker(user.uid, sticker)
                            showStickers = false
                        }
                    )
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
                MessageBubble(msg, onSaveSticker = { base64Data, isGif ->
                    chatViewModel.saveStickerToFavorites(base64Data, isGif)
                })
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(
    msg: ChatMessage,
    onSaveSticker: ((String, Boolean) -> Unit)? = null
) {
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
    
    val isSticker = msg.text.startsWith("[STICKER]:")
    val isStickerGif = msg.text.startsWith("[STICKER_GIF]:")
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (msg.isSentByMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (msg.isSentByMe) Alignment.End else Alignment.Start
        ) {
            if (isSticker || isStickerGif) {
                // Sticker UI
                val prefix = if (isStickerGif) "[STICKER_GIF]:" else "[STICKER]:"
                val stickerData = msg.text.removePrefix(prefix)
                val context = LocalContext.current
                
                val model: Any? = remember(stickerData) {
                    if (stickerData.startsWith("http")) {
                        stickerData
                    } else {
                        try {
                            android.util.Base64.decode(stickerData, android.util.Base64.DEFAULT)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                
                val imageLoader = remember(context) {
                    ImageLoader.Builder(context)
                        .components {
                            if (android.os.Build.VERSION.SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()
                }
                
                if (model != null) {
                    var showSaveDialog by remember { mutableStateOf(false) }
                    
                    if (showSaveDialog && onSaveSticker != null) {
                        AlertDialog(
                            onDismissRequest = { showSaveDialog = false },
                            title = { Text("Guardar Sticker") },
                            text = { Text("¿Deseas guardar este sticker en tus Favoritos?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        onSaveSticker(stickerData, isStickerGif)
                                        showSaveDialog = false
                                    }
                                ) {
                                    Text("Guardar", color = RedPrimary, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSaveDialog = false }) {
                                    Text("Cancelar", color = TextGray)
                                }
                            }
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { 
                                if (onSaveSticker != null) {
                                    showSaveDialog = true 
                                }
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(model)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Sticker",
                            imageLoader = imageLoader,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Text("[Error cargando Sticker]", color = Color.Red, fontSize = 12.sp)
                }
            } else {
                // Text Message UI
                val borderColor = if (msg.isSentByMe) {
                    if (isRead) RedPrimary else YellowSecondary
                } else {
                    BlackTertiary
                }
                
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
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(time, fontSize = 10.sp, color = TextGray)
        }
    }
}

@Composable
fun StickersPanel(
    chatViewModel: ChatViewModel,
    onStickerSelected: (Sticker) -> Unit
) {
    val stickerPacks by chatViewModel.stickerPacks.collectAsState(initial = emptyList())
    val giphyResults by chatViewModel.giphySearchResults.collectAsState(initial = emptyList())
    val isGiphySearching by chatViewModel.isGiphySearching.collectAsState(initial = false)

    var selectedPackId by remember { mutableStateOf("giphy") } // Giphy tab is default now because it's super interactive!
    var searchQuery by remember { mutableStateOf("") }

    // Auto-trigger GIPHY search with debounce
    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(400)
        chatViewModel.searchGiphy(searchQuery)
    }

    // Combined list of tabs: Favorites, GIPHY Search, and then default packs
    val tabs = remember(stickerPacks) {
        val list = mutableListOf<StickerPack>()
        // 1. Favorites
        val fav = stickerPacks.firstOrNull { it.id == "favorites" } ?: StickerPack("favorites", "Favoritos ❤️", "", emptyList())
        list.add(fav)
        // 2. GIPHY tab (synthetic)
        list.add(
            StickerPack(
                id = "giphy",
                name = "GIPHY 🔍",
                iconUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExMWhnM280cTFrbmdnYnptNXQ3N2l0amtrdGFleGlrczN1enJ1eDlpZCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/o7R0N2F/giphy.gif",
                stickers = emptyList()
            )
        )
        // 3. Built-in Packs
        list.addAll(stickerPacks.filter { it.id != "favorites" })
        list
    }

    val selectedPack = stickerPacks.firstOrNull { it.id == selectedPackId }
    val context = LocalContext.current

    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val displayedStickers = if (selectedPackId == "giphy") {
        giphyResults
    } else {
        selectedPack?.stickers ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp) // Adjusted slightly to accommodate search bar comfortably
            .background(Color.White)
            .border(1.dp, BackgroundGray)
    ) {
        // GIPHY Search Bar
        if (selectedPackId == "giphy") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(Color.White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar stickers en GIPHY...", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = TextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = BackgroundGray,
                        focusedContainerColor = BackgroundGray,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        focusedTextColor = BlackTertiary,
                        unfocusedTextColor = BlackTertiary
                    )
                )
            }
        }

        // Grid of Stickers
        Box(
            modifier = Modifier
                .weight(1f)
                .background(BackgroundGray)
                .fillMaxWidth()
        ) {
            if (selectedPackId == "giphy" && isGiphySearching && displayedStickers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = RedPrimary)
                }
            } else if (displayedStickers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedPackId) {
                            "favorites" -> "No tienes stickers guardados.\n¡Haz clic en los stickers que recibas para guardarlos!"
                            "giphy" -> if (isGiphySearching) "Buscando en GIPHY..." else "No se encontraron stickers."
                            else -> "Este paquete está vacío."
                        },
                        color = TextGray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedStickers) { sticker ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEDF2F7))
                                .clickable { onStickerSelected(sticker) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val model: Any = when {
                                sticker.localPath != null -> java.io.File(sticker.localPath)
                                sticker.url?.startsWith("/") == true -> java.io.File(sticker.url)
                                else -> sticker.url ?: ""
                            }
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(model)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                imageLoader = imageLoader,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }

        // Pack Tabs at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.White)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tabs.forEach { pack ->
                val isSelected = pack.id == selectedPackId
                Column(
                    modifier = Modifier
                        .clickable { selectedPackId = pack.id }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) RedPrimary.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pack.id == "favorites") {
                            Icon(
                                imageVector = Icons.Filled.AddReaction,
                                contentDescription = pack.name,
                                tint = if (isSelected) RedPrimary else TextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (pack.id == "giphy") {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = pack.name,
                                tint = if (isSelected) RedPrimary else TextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            AsyncImage(
                                model = pack.iconUrl,
                                contentDescription = pack.name,
                                imageLoader = imageLoader,
                                modifier = Modifier.size(20.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 2.dp)
                            .background(if (isSelected) RedPrimary else Color.Transparent)
                    )
                }
            }
        }
    }
}
