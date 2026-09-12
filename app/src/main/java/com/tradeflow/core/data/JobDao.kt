package com.tradeflow.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY startAt ASC")
    fun all(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE startAt >= :from AND startAt < :to ORDER BY startAt ASC")
    fun inRange(from: Long, to: Long): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE customerId = :cid ORDER BY startAt DESC")
    fun forCustomer(cid: Long): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE status != 'CANCELLED' AND startAt < :to AND endAt > :from ORDER BY startAt ASC")
    suspend fun clashing(from: Long, to: Long): List<Job>

    @Query("SELECT * FROM jobs WHERE status = 'IN_PROGRESS' ORDER BY startAt DESC LIMIT 1")
    suspend fun current(): Job?

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun byId(id: Long): Job?

    @Query("SELECT IFNULL(SUM(amountCents), 0) FROM jobs WHERE paid = 1")
    fun paidTotal(): Flow<Long>

    @Query("SELECT COUNT(*) FROM jobs WHERE status != 'CANCELLED'")
    fun bookedCount(): Flow<Int>

    // Patch #10: @Upsert for hygiene (same reason as Customer/Conversation DAOs).
    @Upsert
    suspend fun upsert(j: Job): Long

    @Update
    suspend fun update(j: Job)

    @Query("UPDATE jobs SET status = :s WHERE id = :id")
    suspend fun setStatus(id: Long, s: String)

    @Query("UPDATE jobs SET paid = 1 WHERE id = :id")
    suspend fun markPaid(id: Long)

    @Delete
    suspend fun delete(j: Job)
}
