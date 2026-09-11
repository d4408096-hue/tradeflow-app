package com.tradeflow.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A homeowner / client of the tradesperson. Phone stored normalized (digits, US 10-digit). */
@Entity(tableName = "customers", indices = [Index("phone")])
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val email: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
