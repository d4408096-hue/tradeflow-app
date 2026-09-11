package com.tradeflow.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMsgAt DESC")
    fun all(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE phone = :phone LIMIT 1")
    suspend fun byPhone(phone: String): Conversation?

    @Query("SELECT * FROM conversations WHERE needsHuman = 1 ORDER BY lastMsgAt DESC")
    fun escalated(): Flow<List<Conversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: Conversation)

    @Update
    suspend fun update(c: Conversation)

    @Query("UPDATE conversations SET unread = 0 WHERE phone = :phone")
    suspend fun clearUnread(phone: String)

    @Delete
    suspend fun delete(c: Conversation)
}
