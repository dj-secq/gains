package com.example.repsgrams.domain.session

import com.example.repsgrams.data.db.BlockKind
import java.util.Locale

/** Stable reps-at-load key. Two decimals so 20 and 20.0 do not become two records. */
fun maxRepsType(weightKg: Float): String =
    "maxReps_${String.format(Locale.US, "%.2f", weightKg)}"

/**
 * Exercises that appear in a working or superset block.
 * A warm-up-only exercise is absent here and does not generate a personal record.
 */
fun prEligibleExerciseIds(plan: WorkoutPlan): Set<Long> =
    plan.blocks
        .filter { it.kind != BlockKind.WARM_UP }
        .flatMap { block -> block.exercises.map { it.id } }
        .toSet()
