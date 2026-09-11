package com.example.repsgrams.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DatabaseInitializerTest {
    private lateinit var database: AppDatabase
    private val seedDate = java.time.LocalDate.of(2026, 9, 10)
    private val clock = Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), ZoneOffset.UTC)

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun seedsCompleteDefaultProgramExactlyOnce() = runTest {
        val initializer = DatabaseInitializer(database, clock)
        initializer.ensureSeeded()
        initializer.ensureSeeded()

        assertEquals(19, database.exerciseDao().count())
        assertEquals(2, database.workoutTemplateDao().count())
        assertEquals(10, database.templateBlockDao().count())
        assertEquals(24, database.templateBlockExerciseDao().count())
        assertEquals(2, database.supplyInventoryDao().getAll().size)

        val whey = checkNotNull(database.supplyInventoryDao().get(SupplyType.WHEY))
        val creatine = checkNotNull(database.supplyInventoryDao().get(SupplyType.CREATINE))
        assertEquals(65, whey.totalServings)
        assertEquals(65f, whey.servingsRemaining)
        assertEquals(30, creatine.totalServings)
        assertEquals(30f, creatine.servingsRemaining)
        assertEquals(seedDate, whey.startDate)
    }

    @Test
    fun seedsOrderedBlocksAndAllExerciseTargets() = runTest {
        DatabaseInitializer(database, clock).ensureSeeded()

        val expected = mapOf(
            "A/Warm-Up/Jumping Jacks" to Target(40, 60),
            "A/Warm-Up/Arm Circles" to Target(20, 20),
            "A/Warm-Up/Band Pull-Aparts" to Target(15, 15),
            "A/Warm-Up/Bodyweight Squats" to Target(10, 10),
            "A/Warm-Up/Easy Push-Ups" to Target(5, 8),
            "A/Superset A/Pull-Ups" to Target(4, 6),
            "A/Superset A/DB Floor Press" to Target(8, 12),
            "A/Superset B/Single-Arm DB Row" to Target(8, 15, perSide = true),
            "A/Superset B/DB Overhead Press" to Target(8, 12),
            "A/Superset C/DB Bicep Curl" to Target(8, 15),
            "A/Superset C/Bulgarian Split Squat" to Target(8, 12, perSide = true),
            "A/Optional Core/Hanging Knee Raise" to Target(10, 15),
            "B/Warm-Up/Jumping Jacks" to Target(40, 60),
            "B/Warm-Up/Arm Circles" to Target(20, 20),
            "B/Warm-Up/Band Pull-Aparts" to Target(15, 15),
            "B/Warm-Up/Bodyweight Squats" to Target(10, 10),
            "B/Warm-Up/Easy Push-Ups" to Target(5, 8),
            "B/Superset A/Chin-Ups" to Target(4, 7),
            "B/Superset A/Push-Ups" to Target(8, 15),
            "B/Superset B/DB Romanian Deadlift" to Target(8, 15),
            "B/Superset B/Band Lateral Raise" to Target(12, 20),
            "B/Superset C/DB Overhead Triceps Extension" to Target(10, 15),
            "B/Superset C/Band Face Pull" to Target(15, 20),
            "B/Optional Core/Hollow Body Hold" to Target(20, 40, RepType.SECONDS),
        )

        val exercises = database.exerciseDao().getAll().associateBy { it.id }
        val actual = buildMap {
            listOf("A", "B").forEach { dayLabel ->
                val template = checkNotNull(database.workoutTemplateDao().getByDayLabel(dayLabel))
                val blocks = database.templateBlockDao().getForTemplate(template.id)
                assertEquals(listOf(0, 1, 2, 3, 4), blocks.map { it.orderIndex })
                blocks.forEach { block ->
                    val assignments = database.templateBlockExerciseDao().getForBlock(block.id)
                    assignments.forEach { assignment ->
                        val exercise = checkNotNull(exercises[assignment.exerciseId])
                        put("$dayLabel/${block.label}/${exercise.name}", Target(
                            assignment.targetValueLow,
                            assignment.targetValueHigh,
                            assignment.repType,
                            assignment.perSide,
                        ))
                    }
                }
            }
        }
        assertEquals(expected, actual)

        val a = checkNotNull(database.workoutTemplateDao().getByDayLabel("A"))
        val aBlocks = database.templateBlockDao().getForTemplate(a.id)
        assertEquals(listOf(null, 90, 90, 60, null), aBlocks.map { it.restSecondsBetweenRounds })
        assertEquals(2, aBlocks[3].targetRoundsMin)
        assertEquals(3, aBlocks[3].targetRoundsMax)
        assertTrue(aBlocks.last().isOptional)

        val armCircles = checkNotNull(database.exerciseDao().getAll().find { it.name == "Arm Circles" })
        assertEquals("10 forward, 10 backward", armCircles.notes)
        assertFalse(armCircles.tracksWeight)
    }

    private data class Target(
        val low: Int,
        val high: Int,
        val repType: RepType = RepType.REPS,
        val perSide: Boolean = false,
    )
}
