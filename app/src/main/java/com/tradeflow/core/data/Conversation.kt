package com.tradeflow.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Booking-bot state per phone number. The engine reads [stage] to decide
 * what an incoming reply means. [needsHuman] drives the 🚨 priority alert.
 */
@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val phone: String,
    val stage: String = IDLE,
    val service: String = "",
    val slotLabel: String = "",
    val slotAt: Long = 0,
    val name: String = "",
    val address: String = "",
    val problem: String = "",
    val customerId: Long? = null,
    val lastAutoReplyAt: Long = 0,
    val lastMsgAt: Long = System.currentTimeMillis(),
    val unread: Int = 0,
    val needsHuman: Boolean = false
) {
    companion object {
        const val IDLE = "IDLE"                 // no active bot flow
        const val AWAIT_SERVICE = "AWAIT_SERVICE"   // menu sent, waiting 1-5
        const val AWAIT_SLOT = "AWAIT_SLOT"         // slots sent, waiting A/B
        const val AWAIT_DETAILS = "AWAIT_DETAILS"   // waiting name + address
        const val AWAIT_PROBLEM = "AWAIT_PROBLEM"   // Other-flow: waiting description
        const val ESCALATED = "ESCALATED"           // with Mike (🚨 until he acts)
        const val PENDING_CONFIRM = "PENDING_CONFIRM" // Mike must tap ✅
        const val DONE = "DONE"                 // booked, flow complete
    }
}
