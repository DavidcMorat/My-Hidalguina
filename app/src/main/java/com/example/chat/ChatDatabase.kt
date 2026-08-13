package com.example.chat

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_users")
data class ChatUser(
    @PrimaryKey val uid: String,
    val displayName: String
)

@Entity(tableName = "chat_messages")
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
    suspend fun insertUser(user: ChatUser)

    @Query("SELECT * FROM chat_users WHERE uid = :uid LIMIT 1")
    suspend fun getUser(uid: String): ChatUser?

    @Query("SELECT * FROM chat_messages WHERE (senderId = :myUid AND receiverId = :otherUid) OR (senderId = :otherUid AND receiverId = :myUid) ORDER BY timestamp ASC")
    fun getMessagesWithUser(myUid: String, otherUid: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
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
