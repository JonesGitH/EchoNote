package com.example.keywordrecorder.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RecordingEntity::class, DailySummaryEntity::class],
    version = 2,
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun dailySummaryDao(): DailySummaryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
                    """
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
