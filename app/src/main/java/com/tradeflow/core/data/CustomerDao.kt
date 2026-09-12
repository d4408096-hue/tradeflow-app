package com.tradeflow.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    fun all(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun byPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun byId(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%' OR address LIKE '%' || :q || '%' ORDER BY name COLLATE NOCASE ASC")
    fun search(q: String): Flow<List<Customer>>

    // Patch #10: @Upsert, NEVER Insert(REPLACE) — REPLACE deletes the row first and
    // the jobs FK CASCADE would wipe the customer's jobs on every edit.
    @Upsert
    suspend fun upsert(c: Customer): Long

    @Update
    suspend fun update(c: Customer)

    @Delete
    suspend fun delete(c: Customer)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int
}
