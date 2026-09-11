package com.tradeflow.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Diagnostics log row. Tags: CALL, SMS, BOT, JOB, SYS. Pruned after 7 days. */
@Entity(tableName = "diag")
data class DiagEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long = System.currentTimeMillis(),
    val tag: String,
    val text: String
)
