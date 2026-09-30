package com.example.repsgrams.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.DatabaseInitializer
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
        DatabaseInitializer(firstDatabase, clock).ensureSeeded()
        val supplements = firstDatabase.supplementDao().getAll()
        val creatine = checkNotNull(supplements.find { it.name == "Creatine" })
        val whey = checkNotNull(supplements.find { it.name == "Whey Protein" })
        val firstRepository = DefaultSupplementRepository(firstDatabase, clock)
        firstRepository.setSupplementTaken(date, creatine, true)
        firstRepository.setSupplementTaken(date, whey, true)
        firstRepository.setSupplementTaken(date, creatine, false)
        firstDatabase.close()
        database = null

        val reopenedDatabase = openDatabase()
        val creatineLog = reopenedDatabase.supplementIntakeLogDao().getForDateAndSupplement(date, creatine.id)
        val wheyLog = reopenedDatabase.supplementIntakeLogDao().getForDateAndSupplement(date, whey.id)

        assertFalse(checkNotNull(creatineLog).taken)
        assertEquals(0f, creatineLog.actualAmount)
        assertTrue(checkNotNull(wheyLog).taken)
        assertEquals(whey.doseAmount, wheyLog.actualAmount)
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
