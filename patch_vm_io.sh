sed -i '1i import kotlinx.coroutines.Dispatchers' app/src/main/java/com/example/chat/ChatViewModel.kt
sed -i '1i import kotlinx.coroutines.withContext' app/src/main/java/com/example/chat/ChatViewModel.kt
sed -i 's/chatDao.insertUser(user)/withContext(Dispatchers.IO) { chatDao.insertUser(user) }/' app/src/main/java/com/example/chat/ChatViewModel.kt
sed -i 's/chatDao.insertMessage(chatMessage)/withContext(Dispatchers.IO) { chatDao.insertMessage(chatMessage) }/' app/src/main/java/com/example/chat/ChatViewModel.kt
sed -i 's/chatDao.getUser(senderId)/withContext(Dispatchers.IO) { chatDao.getUser(senderId) }/' app/src/main/java/com/example/chat/ChatViewModel.kt
