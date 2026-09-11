package com.tradeflow.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Editable message template. Body supports {name} {amount} {link} {slot} placeholders. */
@Entity(tableName = "templates")
data class MsgTemplate(
    @PrimaryKey val key: String,
    val title: String,
    val body: String,
    val editable: Boolean = true
) {
    companion object {
        const val ON_MY_WAY = "on_my_way"
        const val BILL_REVIEW = "bill_review"
        const val DECLINE = "decline"
        const val NUDGE = "nudge"
        const val SLOTS = "slots"
    }
}
