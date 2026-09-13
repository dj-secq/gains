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
        assertNull(repository.previousSet(exercise.id, 1, firstId))

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
    @Test
    fun testZeroHistoryPrefillReturnsNullAndHandlesGracefully() = runTest {
        val firstId = repository.startSession("A")
        val plan = repository.loadPlan(requireNotNull(repository.getSession(firstId)?.templateId))
        val exercise = plan.blocks.first().exercises.first()

        // Zero history: previousSet returns null
        val prev1 = repository.previousSet(exercise.id, 1, firstId)
        assertNull(prev1)
    }

    @Test
    fun testExerciseSwapPrefillIgnoresTemplateId() = runTest {
        // Start session 1, log a set for exercise 999 (custom)
        val firstId = repository.startSession("A")
        repository.logSet(firstId, 999L, 1, 10, null, 50f)
        repository.finishSession(firstId)

        repository = WorkoutRepository(
            database,
            Clock.fixed(Instant.parse("2026-09-12T08:00:00Z"), ZoneOffset.UTC),
            CompletableDeferred(Unit),
        )

        // Start session 2 (different template logic but same exercise swap)
        val secondId = repository.startSession("B")

        // Exercise 999 was logged in session A, now queried in session B
        // Because we removed templateId restriction, it should return the set!
        val prev = repository.previousSet(999L, 1, secondId)

        assertNotNull(prev)
        assertEquals(10, prev?.reps)
        assertEquals(50f, prev?.weightKg)
    }
    @Test
    fun testCancelWorkoutLeavesNoTrace() = runTest {
        // Start a session
        val firstId = repository.startSession("A")
        val plan = repository.loadPlan(requireNotNull(repository.getSession(firstId)?.templateId))
        val exercise = plan.blocks.first().exercises.first()

        // Log a couple of sets
        repository.logSet(firstId, exercise.id, 1, 15, null, 100f)
        repository.logSet(firstId, exercise.id, 2, 12, null, 100f)

        // Assert they exist
        assertEquals(2, repository.getSessionSets(firstId).size)
        assertNotNull(database.workoutSessionDao().getById(firstId))

        // Cancel workout
        repository.deleteSession(firstId)

        // Assert zero trace
        assertNull(database.workoutSessionDao().getById(firstId))
        assertEquals(0, repository.getSessionSets(firstId).size)
        // Since SetLog uses CASCADE on foreign key for sessionId, they should be deleted.
        // Let's verify by checking all sets for that session.
        assertEquals(0, database.setLogDao().getForSession(firstId).size)

        // Also verify Progress / Calendar trace is zero for that session.
        // If session is deleted, it doesn't appear in streak or progress calculators.
    }
}
