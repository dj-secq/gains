package com.example.repsgrams.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun calendarSetQueryReturnsExerciseNamesAndPerformanceInLogOrder() = runTest {
        val date = LocalDate.of(2026, 9, 10)
        val exerciseId = database.exerciseDao().insert(ExerciseEntity(name = "Goblet Squat", tracksWeight = true))
        val sessionId = database.workoutSessionDao().insert(WorkoutSessionEntity(templateId = null, date = date))
        database.setLogDao().insert(
            SetLogEntity(sessionId = sessionId, exerciseId = exerciseId, roundNumber = 1,
                reps = 12, weightKg = 16f, loggedAt = Instant.parse("2026-09-10T10:00:00Z")),
        )
        database.setLogDao().insert(
            SetLogEntity(sessionId = sessionId, exerciseId = exerciseId, roundNumber = 2,
                reps = 10, weightKg = 16f, loggedAt = Instant.parse("2026-09-10T10:02:00Z")),
        )

        val rows = database.setLogDao().getForDate(date)
        assertEquals(listOf(1, 2), rows.map { it.roundNumber })
        assertEquals(listOf("Goblet Squat", "Goblet Squat"), rows.map { it.exerciseName })
        assertEquals(listOf(12, 10), rows.map { it.reps })
    }

    @Test
    fun convertersAndUpsertsRoundTrip() = runTest {
        val date = LocalDate.of(2026, 9, 10)
        val suppId = database.supplementDao().insert(SupplementEntity(name = "Test", doseAmount = 10f, unit = "g", scheduleType = "daily", containerSize = 100, lowSupplyThreshold = 10, colorToken = "red", iconName = "pill"))
        database.supplementIntakeLogDao().upsert(
            SupplementIntakeLogEntity(date = date, supplementId = suppId, taken = true, actualAmount = 10f),
        )

        val result = database.supplementIntakeLogDao().getForDateAndSupplement(date, suppId)
        assertEquals(date, result?.date)
        assertEquals(true, result?.taken)
        assertEquals(1, database.supplementIntakeLogDao().observeInRange(date, date).first().size)
    }

    @Test
    fun templateDeleteCascadesConfigurationButPreservesSessionHistory() = runTest {
        val exerciseId = database.exerciseDao().insert(ExerciseEntity(name = "Test", tracksWeight = true))
        val templateId = database.workoutTemplateDao().insert(
            WorkoutTemplateEntity(name = "Workout A", dayLabel = "A", maxDurationMinutes = 40),
        )
        val blockId = database.templateBlockDao().insert(
            TemplateBlockEntity(templateId = templateId, label = "Block", orderIndex = 0,
                kind = BlockKind.SUPERSET, targetRoundsMin = 1, targetRoundsMax = 1),
        )
        database.templateBlockExerciseDao().insert(
            TemplateBlockExerciseEntity(blockId = blockId, exerciseId = exerciseId, orderIndex = 0,
                targetValueLow = 8, targetValueHigh = 12),
        )
        val sessionId = database.workoutSessionDao().insert(
            WorkoutSessionEntity(templateId = templateId, date = LocalDate.of(2026, 9, 10)),
        )

        database.workoutTemplateDao().delete(
            checkNotNull(database.workoutTemplateDao().getByDayLabel("A")),
        )

        assertEquals(0, database.templateBlockDao().count())
        assertNull(database.workoutSessionDao().getById(sessionId)?.templateId)
    }

    @Test
    fun sessionDeleteCascadesSetLogsAndExerciseReferencesAreRestricted() = runTest {
        val exercise = ExerciseEntity(name = "Test", tracksWeight = true)
        val exerciseId = database.exerciseDao().insert(exercise)
        val sessionId = database.workoutSessionDao().insert(
            WorkoutSessionEntity(templateId = null, date = LocalDate.of(2026, 9, 10)),
        )
        database.setLogDao().insert(
            SetLogEntity(sessionId = sessionId, exerciseId = exerciseId, roundNumber = 1,
                reps = 10, weightKg = 12f, loggedAt = Instant.parse("2026-09-10T10:00:00Z")),
        )

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking {
                database.exerciseDao().delete(exercise.copy(id = exerciseId))
            }
        }

        database.workoutSessionDao().delete(checkNotNull(database.workoutSessionDao().getById(sessionId)))
        assertEquals(0, database.setLogDao().count())
    }
}
