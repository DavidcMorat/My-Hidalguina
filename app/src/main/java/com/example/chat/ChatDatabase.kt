package com.example.chat

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_users")
data class ChatUser(
    @PrimaryKey val uid: String,
    val displayName: String
)

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val isSentByMe: Boolean
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_users")
    fun getAllUsers(): Flow<List<ChatUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: ChatUser): Long
    
    @Query("SELECT * FROM chat_users WHERE uid = :uid")
    fun getUser(uid: String): ChatUser?

    @Query("SELECT * FROM messages WHERE (senderId = :otherUserId AND receiverId = :myUserId) OR (senderId = :myUserId AND receiverId = :otherUserId) ORDER BY timestamp ASC")
    fun getMessagesWithUser(myUserId: String, otherUserId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM messages")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: ChatMessage): Long
}

@Database(entities = [ChatUser::class, ChatMessage::class], version = 1, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
