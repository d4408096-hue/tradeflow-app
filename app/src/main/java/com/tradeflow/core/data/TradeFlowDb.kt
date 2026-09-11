package com.tradeflow.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Customer::class,
        Job::class,
        Conversation::class,
        ChatMessage::class,
        MsgTemplate::class,
        DiagEvent::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TradeFlowDb : RoomDatabase() {
    abstract fun customers(): CustomerDao
    abstract fun jobs(): JobDao
    abstract fun conversations(): ConversationDao
    abstract fun messages(): ChatMessageDao
    abstract fun templates(): MsgTemplateDao
    abstract fun diag(): DiagEventDao

    companion object {
        @Volatile
        private var I: TradeFlowDb? = null

        fun get(ctx: Context): TradeFlowDb = I ?: synchronized(this) {
            I ?: Room.databaseBuilder(ctx.applicationContext, TradeFlowDb::class.java, "tradeflow.db")
                // V1 only: safe while version = 1 (no upgrades yet).
                // Before shipping DB version 2, replace with a real Migration.
                .fallbackToDestructiveMigration()
                .build().also { I = it }
        }
    }
}
