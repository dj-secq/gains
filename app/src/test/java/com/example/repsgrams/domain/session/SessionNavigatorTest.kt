package com.example.repsgrams.domain.session

import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SessionNavigatorTest {
    private val plan = WorkoutPlan(
        templateId = 1,
        name = "Workout A",
        dayLabel = "A",
        maxDurationMinutes = 40,
        blocks = listOf(
            block(1, BlockKind.WARM_UP, rounds = 1, rest = null, exercises = 2),
            block(2, BlockKind.SUPERSET, rounds = 3, rest = 60, exercises = 2),
            block(3, BlockKind.STANDARD, rounds = 2, rest = 90, exercises = 1, optional = true),
        ),
    )

    @Test
    fun `warm up advances without rest`() {
        assertEquals(
            SessionAdvance.Continue(SessionCursor(0, 1, 1)),
            SessionNavigator.afterExercise(plan, SessionCursor(0, 0, 1)),
        )
        assertEquals(
            SessionAdvance.Continue(SessionCursor(1, 0, 1)),
            SessionNavigator.afterExercise(plan, SessionCursor(0, 1, 1)),
        )
    }

    @Test
    fun `superset moves directly between exercises then rests between rounds`() {
        assertEquals(
            SessionAdvance.Continue(SessionCursor(1, 1, 1)),
            SessionNavigator.afterExercise(plan, SessionCursor(1, 0, 1)),
        )
        assertEquals(
            SessionAdvance.Rest(SessionCursor(1, 0, 2), 60),
            SessionNavigator.afterExercise(plan, SessionCursor(1, 1, 1)),
        )
    }

    @Test
    fun `last guided round advances without an extra rest`() {
        assertEquals(
            SessionAdvance.Continue(SessionCursor(2, 0, 1)),
            SessionNavigator.afterExercise(plan, SessionCursor(1, 1, 3)),
        )
        assertEquals(
            SessionAdvance.Finished,
            SessionNavigator.afterExercise(plan, SessionCursor(2, 0, 2)),
        )
    }

    @Test
    fun `optional block can be skipped`() {
        assertEquals(SessionAdvance.Finished, SessionNavigator.skipBlock(plan, 2))
        assertThrows(IllegalArgumentException::class.java) { SessionNavigator.skipBlock(plan, 1) }
    }

    private fun block(
        id: Long,
        kind: BlockKind,
        rounds: Int,
        rest: Int?,
        exercises: Int,
        optional: Boolean = false,
    ) = WorkoutBlock(
        id = id,
        label = "Block $id",
        kind = kind,
        targetRoundsMin = rounds,
        targetRoundsMax = rounds,
        restSecondsBetweenRounds = rest,
        isOptional = optional,
        exercises = List(exercises) { index ->
            WorkoutExercise(
                id = id * 10 + index,
                name = "Exercise $index",
                notes = null,
                tracksWeight = false,
                targetValueLow = 8,
                targetValueHigh = 12,
                repType = RepType.REPS,
                perSide = false,
            )
        },
    )
}
