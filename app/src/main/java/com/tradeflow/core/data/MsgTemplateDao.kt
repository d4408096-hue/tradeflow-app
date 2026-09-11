package com.tradeflow.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MsgTemplateDao {
    // `key` is backticked: KEY is a reserved word in SQLite.
    @Query("SELECT * FROM templates")
    suspend fun all(): List<MsgTemplate>

    @Query("SELECT * FROM templates WHERE `key` = :key LIMIT 1")
    suspend fun byKey(key: String): MsgTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: MsgTemplate)
}
