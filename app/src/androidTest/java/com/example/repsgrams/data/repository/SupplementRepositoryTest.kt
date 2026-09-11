package com.example.repsgrams.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.repsgrams.data.db.AppDatabase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SupplementRepositoryTest {
    private lateinit var context: Context
    private var database: AppDatabase? = null
    private val date = LocalDate.of(2026, 9, 10)
    private val clock = Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun togglesPreserveEachOtherAndPersistAcrossDatabaseReopen() = runTest {
        val firstDatabase = openDatabase()
        val firstRepository = DefaultSupplementRepository(firstDatabase, clock)
        firstRepository.setCreatineTaken(date, true)
        firstRepository.setWheyTaken(date, true)
        firstRepository.setCreatineTaken(date, false)
        firstDatabase.close()
        database = null

        val reopenedDatabase = openDatabase()
        val restored = reopenedDatabase.supplementLogDao().getForDate(date)

        assertFalse(checkNotNull(restored).creatineTaken)
        assertEquals(0f, restored.creatineGrams)
        assertTrue(restored.wheyTaken)
        assertEquals(1f, restored.wheyServings)
    }

    private fun openDatabase(): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        DATABASE_NAME,
    ).build().also { database = it }

    private companion object {
        const val DATABASE_NAME = "supplement-repository-test.db"
    }
}
