package com.tradeflow.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM messages WHERE phone = :phone ORDER BY at ASC")
    fun forPhone(phone: String): Flow<List<ChatMessage>>

    @Insert
    suspend fun insert(m: ChatMessage): Long

    @Query("SELECT COUNT(*) FROM messages WHERE auto = 1")
    fun autoCount(): Flow<Int>

    @Query("DELETE FROM messages WHERE phone = :phone")
    suspend fun clear(phone: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
