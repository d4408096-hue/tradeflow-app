package com.tradeflow.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayOffDao {
    @Query("SELECT * FROM daysoff ORDER BY startDate ASC")
    fun all(): Flow<List<DayOff>>

    @Query("SELECT * FROM daysoff WHERE startDate <= :date AND endDate >= :date LIMIT 1")
    suspend fun offOn(date: String): DayOff?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(d: DayOff): Long

    @Query("DELETE FROM daysoff WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM daysoff WHERE startDate <= :date AND endDate >= :date")
    suspend fun deleteOn(date: String)
}
