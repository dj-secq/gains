package com.example.repsgrams.domain.progression

import com.example.repsgrams.data.db.RepType

data class PriorRound(val value: Int?, val weightKg: Float?)

enum class ProgressionResult { HOLD, INCREASE_LOAD, ADVANCE_TARGET }

data class ProgressionMaps(
    val qualified: Map<Long, Long> = emptyMap(),
    val skipped: Map<Long, Long> = emptyMap(),
)

/** [applySessionId] is a previous qualifying session. Null means prefill the last working set. */
data class ProgressionConsume(
    val maps: ProgressionMaps,
    val applySessionId: Long?,
)

object ProgressionCalculator {
    /**
     * Working sets only. Warm-up rows are excluded by the caller.
     * [INCREASE_LOAD] is the only result that changes a load.
     */
    fun decide(
        enabled: Boolean,
        workingSets: List<PriorRound>,
        targetHigh: Int,
        targetRoundsMin: Int,
        repType: RepType,
        tracksWeight: Boolean,
    ): ProgressionResult {
        if (!enabled || workingSets.size < targetRoundsMin) return ProgressionResult.HOLD
        if (workingSets.any { set -> set.value == null || set.value < targetHigh }) {
            return ProgressionResult.HOLD
        }
        return if (repType == RepType.REPS && tracksWeight) {
            ProgressionResult.INCREASE_LOAD
        } else {
            ProgressionResult.ADVANCE_TARGET
        }
    }

    /**
     * Records a qualification unless this session skipped that exercise.
     * A skip removes a qualification stored for the same session.
     */
    fun recordOnFinish(
        maps: ProgressionMaps,
        exerciseId: Long,
        sessionId: Long,
        qualified: Boolean,
        skipped: Boolean,
    ): ProgressionMaps {
        val qualifiedMap = maps.qualified.toMutableMap()
        val skippedMap = maps.skipped.toMutableMap()
        if (skipped) {
            skippedMap[exerciseId] = sessionId
            if (qualifiedMap[exerciseId] == sessionId) qualifiedMap.remove(exerciseId)
        } else if (qualified) {
            qualifiedMap[exerciseId] = sessionId
            if (skippedMap[exerciseId] == sessionId) skippedMap.remove(exerciseId)
        }
        return ProgressionMaps(qualifiedMap, skippedMap)
    }

    /**
     * Applies a qualification from an earlier session when this exercise was not skipped.
     * Entries from the current session stay, so a skip survives re-entry until a later session loads the exercise.
     */
    fun consumeOnLoad(
        maps: ProgressionMaps,
        exerciseId: Long,
        loadingSessionId: Long,
    ): ProgressionConsume {
        val qualifiedSession = maps.qualified[exerciseId]
        val skippedSession = maps.skipped[exerciseId]
        val previousQualify = qualifiedSession != null && qualifiedSession != loadingSessionId
        val apply = if (previousQualify && skippedSession == null) qualifiedSession else null
        val qualifiedMap = maps.qualified.toMutableMap()
        val skippedMap = maps.skipped.toMutableMap()
        if (qualifiedSession != null && qualifiedSession != loadingSessionId) qualifiedMap.remove(exerciseId)
        if (skippedSession != null && skippedSession != loadingSessionId) skippedMap.remove(exerciseId)
        return ProgressionConsume(ProgressionMaps(qualifiedMap, skippedMap), apply)
    }
}

fun formatProgressionMap(map: Map<Long, Long>): String =
    map.entries.joinToString(",") { (id, session) -> "$id=$session" }

fun parseProgressionMap(stored: String): Map<Long, Long> {
    if (stored.isBlank()) return emptyMap()
    val parsed = LinkedHashMap<Long, Long>()
    for (token in stored.split(',')) {
        val parts = token.split('=')
        if (parts.size != 2) continue
        val id = parts[0].trim().toLongOrNull() ?: continue
        val session = parts[1].trim().toLongOrNull() ?: continue
        parsed[id] = session
    }
    return parsed
}

/** [loadWithUnit] is already formatted, such as `85 kg`. Blank means there is no load to name. */
fun progressionSentence(result: ProgressionResult, loadWithUnit: String?, targetLow: Int): String =
    when (result) {
        ProgressionResult.HOLD -> if (loadWithUnit.isNullOrBlank()) "Hold" else "Hold $loadWithUnit"
        ProgressionResult.INCREASE_LOAD -> "Next time ${loadWithUnit.orEmpty()}, reps from $targetLow"
        ProgressionResult.ADVANCE_TARGET -> "More challenge, same load"
    }
