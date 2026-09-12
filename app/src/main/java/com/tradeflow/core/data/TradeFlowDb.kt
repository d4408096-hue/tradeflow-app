package com.tradeflow.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Customer::class,
        Job::class,
        Conversation::class,
        ChatMessage::class,
        MsgTemplate::class,
        DiagEvent::class,
        DayOff::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TradeFlowDb : RoomDatabase() {
    abstract fun customers(): CustomerDao
    abstract fun jobs(): JobDao
    abstract fun conversations(): ConversationDao
    abstract fun messages(): ChatMessageDao
    abstract fun templates(): MsgTemplateDao
    abstract fun diag(): DiagEventDao
    abstract fun daysoff(): DayOffDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `daysoff` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT NOT NULL, `reason` TEXT NOT NULL)")
            }
        }

        @Volatile
        private var I: TradeFlowDb? = null

        fun get(ctx: Context): TradeFlowDb = I ?: synchronized(this) {
            I ?: Room.databaseBuilder(ctx.applicationContext, TradeFlowDb::class.java, "tradeflow.db")
                .addMigrations(MIGRATION_1_2)
                // Last-resort safety for unknown version jumps only.
                .fallbackToDestructiveMigration()
                .build().also { I = it }
        }
    }
}
