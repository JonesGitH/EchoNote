package com.example.keywordrecorder

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.keywordrecorder.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_createsDailySummariesTable() {
        // Create a v1 database with a recording row
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """INSERT INTO recordings
                   (filePath, fileName, createdAtEpochMillis, durationMillis, transcriptionStatus,
                    transcriptText, transcribedAtEpochMillis, retryCount, lastErrorMessage, deleted)
                   VALUES ('/fake/rec.m4a','rec.m4a',1000,5000,'PENDING',NULL,NULL,0,NULL,0)"""
            )
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        // daily_summaries table exists and is queryable
        val cursor = db.query("SELECT COUNT(*) FROM daily_summaries")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()

        // recordings data survives
        val recCursor = db.query("SELECT COUNT(*) FROM recordings")
        recCursor.moveToFirst()
        assertEquals(1, recCursor.getInt(0))
        recCursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migratedDatabaseOpensWithRoomSuccessfully() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2)

        // If Room can open the migrated DB without exceptions the schema is valid
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB,
        ).addMigrations(AppDatabase.MIGRATION_1_2).build().apply {
            openHelper.writableDatabase
            close()
        }
    }
}
