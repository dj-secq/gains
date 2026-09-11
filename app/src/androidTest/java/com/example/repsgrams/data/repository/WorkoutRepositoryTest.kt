package com.example.repsgrams.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.DatabaseInitializer
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: WorkoutRepository
    private val clock = Clock.fixed(Instant.parse("2026-09-11T08:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).build()
        DatabaseInitializer(database, clock).ensureSeeded()
        repository = WorkoutRepository(database, clock, CompletableDeferred(Unit))
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun progressionHistorySelectsOneMatchingPriorSession() = runTest {
        val templateA = requireNotNull(database.workoutTemplateDao().getByDayLabel("A"))
        val templateB = requireNotNull(database.workoutTemplateDao().getByDayLabel("B"))
        val exercise = repository.loadPlan(templateA.id).blocks.first().exercises.first()
        suspend fun addSession(templateId: Long, day: Int, completed: Boolean, reps: List<Int>): Long {
            val id = database.workoutSessionDao().insert(
                com.example.repsgrams.data.db.WorkoutSessionEntity(
                    templateId = templateId,
                    date = java.time.LocalDate.of(2026, 9, day),
                    completed = completed,
                ),
            )
            reps.forEachIndexed { index, value -> repository.logSet(id, exercise.id, index + 1, value, null, null) }
            return id
        }
        addSession(templateA.id, 1, true, listOf(12, 12))
        addSession(templateA.id, 3, true, listOf(10, 12))
        addSession(templateB.id, 4, true, listOf(99))
        addSession(templateA.id, 5, false, listOf(99))
        addSession(templateA.id, 6, true, emptyList())
        addSession(templateA.id, 11, true, listOf(99))
        val currentId = addSession(templateA.id, 11, false, emptyList())
        assertEquals(listOf(10, 12), repository.previousSessionRounds(exercise.id, currentId).map { it.reps })
    }

    @Test
    fun startsLoadsLogsFinishesAndUsesPreviousSessionDefaults() = runTest {
        val firstId = repository.startSession("A")
        assertEquals(firstId, repository.startSession("B"))
        assertFalse(requireNotNull(database.workoutSessionDao().getActive()).completed)

        val first = requireNotNull(repository.getSession(firstId))
        val plan = repository.loadPlan(requireNotNull(first.templateId))
        assertEquals("A", plan.dayLabel)
        assertEquals(5, plan.blocks.size)
        val exercise = plan.blocks.first().exercises.first()
        assertNull(repository.previousSet(exercise.id, firstId))

        repository.logSet(firstId, exercise.id, 1, 20, null, null)
        repository.logSet(firstId, exercise.id, 2, 18, null, null)
        val finished = repository.finishSession(firstId)
        assertTrue(finished.completed)
        assertNotNull(finished.endTime)
        assertEquals(2, repository.setCount(firstId))

        repository = WorkoutRepository(
            database,
            Clock.fixed(Instant.parse("2026-09-12T08:00:00Z"), ZoneOffset.UTC),
            CompletableDeferred(Unit),
        )
        val secondId = repository.startSession("A")
        assertEquals(listOf(20, 18), repository.previousSessionRounds(exercise.id, secondId).map { it.reps })
    }
}
