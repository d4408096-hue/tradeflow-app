package com.tradeflow.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One SMS in a conversation. dir = IN (homeowner) or OUT (Mike/bot). */
@Entity(
    tableName = "messages",
    indices = [Index("phone"), Index("at")],
    foreignKeys = [ForeignKey(
        entity = Conversation::class,
        parentColumns = ["phone"],
        childColumns = ["phone"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String,
    val dir: String,
    val body: String,
    val at: Long = System.currentTimeMillis(),
    val auto: Boolean = false
) {
    companion object {
        const val IN = "IN"
        const val OUT = "OUT"
    }
}
