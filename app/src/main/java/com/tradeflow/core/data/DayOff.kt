package com.tradeflow.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Unavailability range, inclusive. Dates as yyyy-MM-dd (string compare works). */
@Entity(tableName = "daysoff")
data class DayOff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,
    val endDate: String,
    val reason: String = ""
)
