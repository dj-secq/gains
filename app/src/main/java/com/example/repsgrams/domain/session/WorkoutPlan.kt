package com.example.repsgrams.domain.session

import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType

data class WorkoutPlan(
    val templateId: Long,
    val name: String,
    val dayLabel: String,
    val maxDurationMinutes: Int,
    val blocks: List<WorkoutBlock>,
)

data class WorkoutBlock(
    val id: Long,
    val label: String,
    val kind: BlockKind,
    val targetRoundsMin: Int,
    val targetRoundsMax: Int,
    val restSecondsBetweenRounds: Int?,
    val isOptional: Boolean,
    val exercises: List<WorkoutExercise>,
)

data class WorkoutExercise(
    val id: Long,
    val name: String,
    val notes: String?,
    val tracksWeight: Boolean,
    val targetValueLow: Int,
    val targetValueHigh: Int,
    val repType: RepType,
    val perSide: Boolean,
)

data class SessionCursor(val blockIndex: Int, val exerciseIndex: Int, val roundNumber: Int)

sealed interface SessionAdvance {
    data class Continue(val cursor: SessionCursor) : SessionAdvance
    data class Rest(val cursorAfterRest: SessionCursor, val seconds: Int) : SessionAdvance
    data object Finished : SessionAdvance
}

object SessionNavigator {
    fun afterExercise(plan: WorkoutPlan, cursor: SessionCursor): SessionAdvance {
        val block = plan.blocks[cursor.blockIndex]
        if (cursor.exerciseIndex < block.exercises.lastIndex) {
            return SessionAdvance.Continue(cursor.copy(exerciseIndex = cursor.exerciseIndex + 1))
        }
        if (block.kind != BlockKind.WARM_UP && cursor.roundNumber < block.targetRoundsMax) {
            val nextRound = SessionCursor(cursor.blockIndex, 0, cursor.roundNumber + 1)
            val restSeconds = block.restSecondsBetweenRounds ?: 0
            return if (restSeconds > 0) SessionAdvance.Rest(nextRound, restSeconds)
            else SessionAdvance.Continue(nextRound)
        }
        return nextBlock(plan, cursor.blockIndex)
    }

    fun skipBlock(plan: WorkoutPlan, blockIndex: Int): SessionAdvance {
        require(plan.blocks[blockIndex].isOptional) { "Only optional blocks can be skipped" }
        return nextBlock(plan, blockIndex)
    }

    private fun nextBlock(plan: WorkoutPlan, blockIndex: Int): SessionAdvance =
        if (blockIndex < plan.blocks.lastIndex) {
            SessionAdvance.Continue(SessionCursor(blockIndex + 1, 0, 1))
        } else SessionAdvance.Finished
}
