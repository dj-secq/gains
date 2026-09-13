package com.example.repsgrams.domain.session

import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionNavigatorTest {
    @Test
    fun `warm-up completion starts configured rest before first work block`() {
        val plan = planWithWarmUpAndSuperset()

        val advance = SessionNavigator.afterExercise(plan, SessionCursor(0, 0, 1))

        assertTrue(advance is SessionAdvance.Rest)
        assertEquals(60, (advance as SessionAdvance.Rest).seconds)
        assertEquals(SessionCursor(1, 0, 1), advance.cursorAfterRest)
    }

    @Test
    fun `superset rests after exercise pair between rounds`() {
        val plan = planWithWarmUpAndSuperset()

        val betweenExercises = SessionNavigator.afterExercise(plan, SessionCursor(1, 0, 1))
        val afterPair = SessionNavigator.afterExercise(plan, SessionCursor(1, 1, 1))
        val afterFinalPair = SessionNavigator.afterExercise(plan, SessionCursor(1, 1, 3))

        assertEquals(SessionAdvance.Continue(SessionCursor(1, 1, 1)), betweenExercises)
        assertTrue(afterPair is SessionAdvance.Rest)
        assertEquals(90, (afterPair as SessionAdvance.Rest).seconds)
        assertEquals(SessionCursor(1, 0, 2), afterPair.cursorAfterRest)
        assertTrue(afterFinalPair is SessionAdvance.Rest)
        assertEquals(120, (afterFinalPair as SessionAdvance.Rest).seconds)
        assertEquals(SessionCursor(2, 0, 1), afterFinalPair.cursorAfterRest)
    }

    @Test
    fun `after last round of block with restSecondsAfterBlock transitions to Rest`() {
        val plan = WorkoutPlan(
            templateId = 1,
            name = "Test",
            dayLabel = "A",
            maxDurationMinutes = 60,
            category = "Custom",
            blocks = listOf(
                WorkoutBlock(
                    id = 1,
                    label = "B1",
                    kind = BlockKind.STANDARD,
                    targetRoundsMin = 2,
                    targetRoundsMax = 2,
                    restSecondsBetweenRounds = 60,
                    restSecondsAfterBlock = 120,
                    isOptional = false,
                    exercises = listOf(
                        WorkoutExercise(1, "E1", null, null, false, 10, 10, RepType.REPS, false)
                    )
                ),
                WorkoutBlock(
                    id = 2,
                    label = "B2",
                    kind = BlockKind.STANDARD,
                    targetRoundsMin = 1,
                    targetRoundsMax = 1,
                    restSecondsBetweenRounds = 0,
                    restSecondsAfterBlock = 0,
                    isOptional = false,
                    exercises = listOf(
                        WorkoutExercise(2, "E2", null, null, false, 10, 10, RepType.REPS, false)
                    )
                )
            )
        )

        val advance = SessionNavigator.afterExercise(plan, SessionCursor(0, 0, 2))
        assertTrue(advance is SessionAdvance.Rest)
        assertEquals(120, (advance as SessionAdvance.Rest).seconds)
        assertEquals(1, advance.cursorAfterRest.blockIndex)
        assertEquals(0, advance.cursorAfterRest.exerciseIndex)
        assertEquals(1, advance.cursorAfterRest.roundNumber)
    }

    private fun planWithWarmUpAndSuperset() = WorkoutPlan(
        templateId = 1,
        name = "Test",
        dayLabel = "A",
        maxDurationMinutes = 60,
        category = "Custom",
        blocks = listOf(
            WorkoutBlock(
                id = 1,
                label = "Warm-Up",
                kind = BlockKind.WARM_UP,
                targetRoundsMin = 1,
                targetRoundsMax = 1,
                restSecondsBetweenRounds = null,
                restSecondsAfterBlock = 60,
                isOptional = false,
                exercises = listOf(
                    WorkoutExercise(1, "Warm-up", null, null, false, 10, 10, RepType.REPS, false),
                ),
            ),
            WorkoutBlock(
                id = 2,
                label = "Superset A",
                kind = BlockKind.SUPERSET,
                targetRoundsMin = 3,
                targetRoundsMax = 3,
                restSecondsBetweenRounds = 90,
                restSecondsAfterBlock = 120,
                isOptional = false,
                exercises = listOf(
                    WorkoutExercise(2, "Pull", null, null, false, 10, 10, RepType.REPS, false),
                    WorkoutExercise(3, "Push", null, null, false, 10, 10, RepType.REPS, false),
                ),
            ),
            WorkoutBlock(
                id = 3,
                label = "Superset B",
                kind = BlockKind.SUPERSET,
                targetRoundsMin = 3,
                targetRoundsMax = 3,
                restSecondsBetweenRounds = 90,
                restSecondsAfterBlock = null,
                isOptional = false,
                exercises = listOf(
                    WorkoutExercise(4, "Next", null, null, false, 10, 10, RepType.REPS, false),
                ),
            ),
        ),
    )
}
