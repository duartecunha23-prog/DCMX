package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalModelEntity::class,
        LanServerEntity::class,
        ChatMessageEntity::class,
        ScannedCodeEntity::class,
        VisionHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localModelDao(): LocalModelDao
    abstract fun lanServerDao(): LanServerDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun scannedCodeDao(): ScannedCodeDao
    abstract fun visionHistoryDao(): VisionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nucleo_ia_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
