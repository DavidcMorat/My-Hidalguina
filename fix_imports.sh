sed -i '/import kotlinx.coroutines.withContext/d' app/src/main/java/com/example/chat/ChatViewModel.kt
sed -i '/import kotlinx.coroutines.Dispatchers/d' app/src/main/java/com/example/chat/ChatViewModel.kt
sed -i 's/import kotlinx.coroutines.launch/import kotlinx.coroutines.launch\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext/' app/src/main/java/com/example/chat/ChatViewModel.kt
