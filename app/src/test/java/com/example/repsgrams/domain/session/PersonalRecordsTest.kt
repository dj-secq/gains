package com.example.repsgrams.domain.session

import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordsTest {
    @Test
    fun maxRepsTypeUsesTwoDecimals() {
        assertEquals("maxReps_20.00", maxRepsType(20f))
        assertEquals("maxReps_20.50", maxRepsType(20.5f))
    }

    @Test
    fun warmUpOnlyExerciseIsNotEligible() {
        val plan = plan(
            block(BlockKind.WARM_UP, exercise(1)),
            block(BlockKind.STANDARD, exercise(2)),
            block(BlockKind.SUPERSET, exercise(2), exercise(3)),
        )
        val eligible = prEligibleExerciseIds(plan)
        assertFalse(1L in eligible)
        assertTrue(2L in eligible)
        assertTrue(3L in eligible)
    }

    private fun plan(vararg blocks: WorkoutBlock) = WorkoutPlan(
        templateId = 1,
        name = "A",
        dayLabel = "A",
        maxDurationMinutes = 60,
        category = "Upper",
        blocks = blocks.toList(),
    )

    private fun block(kind: BlockKind, vararg exercises: WorkoutExercise) = WorkoutBlock(
        id = kind.ordinal.toLong(),
        label = kind.name,
        kind = kind,
        targetRoundsMin = 1,
        targetRoundsMax = 1,
        restSecondsBetweenRounds = null,
        restSecondsAfterBlock = null,
        isOptional = false,
        exercises = exercises.toList(),
    )

    private fun exercise(id: Long) = WorkoutExercise(
        id = id,
        name = "Exercise $id",
        imageAssetName = null,
        notes = null,
        tracksWeight = true,
        targetValueLow = 8,
        targetValueHigh = 12,
        repType = RepType.REPS,
        perSide = false,
    )
}
