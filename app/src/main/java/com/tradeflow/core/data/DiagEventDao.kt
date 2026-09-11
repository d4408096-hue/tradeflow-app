package com.tradeflow.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagEventDao {
    @Query("SELECT * FROM diag ORDER BY at DESC LIMIT :n")
    fun latest(n: Int): Flow<List<DiagEvent>>

    @Query("SELECT * FROM diag ORDER BY at DESC LIMIT :n")
    suspend fun latestNow(n: Int): List<DiagEvent>

    @Insert
    suspend fun insert(e: DiagEvent)

    @Query("DELETE FROM diag WHERE at < :before")
    suspend fun prune(before: Long)
}
