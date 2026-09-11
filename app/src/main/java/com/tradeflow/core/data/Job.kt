package com.tradeflow.core.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A scheduled job linked to a customer. Times are epoch millis. Money in cents. */
@Entity(
    tableName = "jobs",
    indices = [Index("customerId"), Index("startAt")],
    foreignKeys = [ForeignKey(
        entity = Customer::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val title: String,
    val service: String = "",
    val startAt: Long,
    val endAt: Long,
    val status: String = SCHEDULED,
    val amountCents: Int = 0,
    val paid: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SCHEDULED = "SCHEDULED"
        const val IN_PROGRESS = "IN_PROGRESS"
        const val COMPLETED = "COMPLETED"
        const val CANCELLED = "CANCELLED"
    }
}
